import { Readable } from 'stream';
import fastify, { FastifyInstance, FastifyRequest, FastifyReply } from 'fastify';
import cors from '@fastify/cors';
import swagger from '@fastify/swagger';
import swaggerUi from '@fastify/swagger-ui';
import Redis from 'ioredis';
import { loadConfig, GatewayConfig } from './config/config';
import { ProviderRegistry } from './core/registry/ProviderRegistry';
import { ProviderSelectionEngine } from './core/selection/ProviderSelectionEngine';
import { CacheManager } from './core/cache/CacheManager';
import { ProviderContext } from './types/context';
import { register as prometheusRegister, searchLatencyHistogram } from './core/metrics/metrics';
import { globalEventBus } from './core/events/EventBus';
import { ProviderError } from './types/error';
import { RedisHealthChecker } from './core/health/RedisHealthChecker';
import { registerProviders } from './providers/registerProviders';
import { logger } from './core/logging/logger';
import {
  bootstrapSchema,
  searchSchema,
  albumSchema,
  artistSchema,
  playlistSchema,
  streamSchema,
  healthSchema,
  versionSchema,
  providersSchema,
  metricsSchema,
  streamProxySchema,
} from './schemas';

// Fastify TypeScript decoration
declare module 'fastify' {
  interface FastifyInstance {
    config: GatewayConfig;
    registry: ProviderRegistry;
    engine: ProviderSelectionEngine;
    cache: CacheManager;
    health: RedisHealthChecker;
  }
}

function createRedis(config: GatewayConfig): Redis {
  if (process.env.NODE_ENV === 'test') {
    const Mock = require('ioredis-mock');
    return new Mock() as unknown as Redis;
  }
  const client = new Redis(config.cache.redisUrl, {
    lazyConnect: true,
    maxRetriesPerRequest: 1,
    enableOfflineQueue: false,
    retryStrategy(times) {
      if (times > 2) return null;
      return 500;
    },
  });
  client.on('error', (err) => {
    logger.warn({ error: err.message }, 'Redis connection warning (degrading to cache-miss)');
  });
  client.connect().catch((err) => {
    logger.warn({ error: err.message }, 'Redis initial connection failed (degrading to cache-miss)');
  });
  return client;
}

export async function buildApp(customConfig?: Partial<GatewayConfig>, redisClient?: Redis): Promise<FastifyInstance> {
  const app = fastify({ logger: false });
  const config = { ...loadConfig(), ...customConfig };

  // Setup Redis (use ioredis-mock in test mode if no redis client provided)
  const redis = redisClient || createRedis(config);

  const cache = new CacheManager(redis, config);
  const registry = new ProviderRegistry();
  const engine = new ProviderSelectionEngine(registry);
  const healthChecker = new RedisHealthChecker(redis, { timeoutMs: config.health?.redisTimeoutMs });

  // Decorate fastify instance for DI
  app.decorate('config', config);
  app.decorate('registry', registry);
  app.decorate('engine', engine);
  app.decorate('cache', cache);
  app.decorate('health', healthChecker);

  // Register plugins (awaited so hooks register before routes are added below)
  await app.register(cors, { origin: config.server.corsOrigins });
  await app.register(swagger, {
    openapi: {
      info: {
        title: 'CliBeats Provider Gateway API',
        description: 'Provider-agnostic audio gateway REST API specification',
        version: config.server.apiVersion,
      },
      tags: [
        { name: 'Bootstrap', description: 'Client initialization context' },
        { name: 'Search', description: 'Canonical search across providers' },
        { name: 'Albums', description: 'Album details and tracklists' },
        { name: 'Artists', description: 'Artist profiles' },
        { name: 'Playlists', description: 'Playlist structures' },
        { name: 'Playback', description: 'Direct-to-CDN stream resolution' },
        { name: 'Providers', description: 'Registered provider adapters' },
        { name: 'Observability', description: 'Health, version, and metrics' },
      ],
    },
  });
  await app.register(swaggerUi, { routePrefix: '/documentation' });

  // Discover and register provider adapters from gateway.yaml config (ADR-013, ADR-019).
  // Swagger is registered above so routes added below are captured by the spec.
  await registerProviders(app, registry, config);

  // Trace ID middleware
  app.addHook('onRequest', async (req: FastifyRequest) => {
    const traceId =
      (req.headers['x-trace-id'] as string) ||
      `trace-${Math.random().toString(36).substring(2, 11)}`;
    req.headers['x-trace-id'] = traceId;
    logger.info({ traceId, method: req.method, url: req.url, clientIp: req.ip }, 'incoming request');
    globalEventBus.emitEvent({
      type: 'REQUEST_RECEIVED',
      traceId,
      endpoint: req.url,
      clientIp: req.ip,
    });
  });

  // Response logging with traceId for end-to-end correlation (P4)
  app.addHook('onResponse', async (req: FastifyRequest, reply: FastifyReply) => {
    const traceId = (req.headers['x-trace-id'] as string) || 'trace-unknown';
    logger.info(
      { traceId, statusCode: reply.statusCode, method: req.method, url: req.url },
      'request completed'
    );
  });

  // Echo traceId back to the client so it can correlate with server logs (P4)
  app.addHook('onSend', async (req: FastifyRequest, reply: FastifyReply) => {
    const traceId = (req.headers['x-trace-id'] as string) || 'trace-unknown';
    void reply.header('x-trace-id', traceId);
  });

  // Global Error Handler (ADR-016)
  app.setErrorHandler((error: unknown, req: FastifyRequest, reply: FastifyReply) => {
    const traceId = (req.headers['x-trace-id'] as string) || 'trace-unknown';
    if (error instanceof ProviderError) {
      return reply.status(error.statusCode).send({
        error: {
          code: error.code,
          message: error.message,
          providerId: error.providerId,
          retryAfterSeconds: error.retryAfterSeconds,
          traceId,
        },
      });
    }

    // Fastify schema validation errors (e.g. missing required body field) carry a 4xx statusCode.
    const err = error as { statusCode?: number; message?: string };
    if (typeof err?.statusCode === 'number' && err.statusCode >= 400 && err.statusCode < 500) {
      return reply.status(err.statusCode).send({
        error: {
          code: 'INVALID_REQUEST',
          message: err.message || 'Invalid request',
          providerId: 'gateway',
          traceId,
        },
      });
    }

    const message = error instanceof Error ? error.message : 'Internal Server Error';
    return reply.status(500).send({
      error: {
        code: 'INTERNAL_ERROR',
        message,
        providerId: 'gateway',
        traceId,
      },
    });
  });

  // Helper function to extract ProviderContext
  const getContext = (req: FastifyRequest): ProviderContext => ({
    country: (req.headers['x-country'] as string) || 'US',
    language: (req.headers['x-language'] as string) || 'en',
    authenticated: req.headers['authorization'] != null,
    preferredAudioQuality: (req.headers['x-audio-quality'] as any) || 'HIGH',
    device: (req.headers['x-device'] as any) || 'mobile',
    traceId: req.headers['x-trace-id'] as string,
  });

  const cdnUrlCache = new Map<string, { url: string; total: number; expiresAt: number }>();

  const resolveCdnStreamUrl = async (
    trackId: string,
    context: ProviderContext,
  ): Promise<{ ok: true; url: string; total: number } | { ok: false; message: string }> => {
    const cached = cdnUrlCache.get(trackId);
    if (cached && cached.expiresAt > Date.now()) return { ok: true, url: cached.url, total: cached.total };
    try {
      const streamResult = await engine.executeWithFailover('playback', context, (adapter) =>
        adapter.stream(trackId, context),
      );
      if (!streamResult?.streamUrl) {
        return { ok: false, message: `No stream URL resolvable for ${trackId}` };
      }
      const probe = await fetch(streamResult.streamUrl, {
        headers: { Range: 'bytes=0-0', 'User-Agent': 'CliBeatsGateway/1.0 (media relay)' },
        redirect: 'follow',
      });
      let total = -1;
      const cr = probe.headers.get('content-range');
      if (probe.status === 206 && cr) {
        const m = /^bytes \d+-\d+\/(\d+)$/.exec(cr);
        if (m) total = parseInt(m[1], 10);
      } else {
        const cl = probe.headers.get('content-length');
        if (cl) total = parseInt(cl, 10);
      }
      await probe.body?.cancel();
      if (total <= 0) {
        return { ok: false, message: `CDN probe failed for ${trackId} (HTTP ${probe.status})` };
      }
      cdnUrlCache.set(trackId, {
        url: streamResult.streamUrl,
        total,
        expiresAt: Date.now() + (config.cache.streamTTLSeconds ?? 900) * 1000,
      });
      logger.info({ trackId, total }, 'proxy stream CDN URL resolved');
      return { ok: true, url: streamResult.streamUrl, total };
    } catch (err) {
      return { ok: false, message: (err as Error).message };
    }
  };

  // 1. GET /api/v1/bootstrap (ADR-020 & user spec)
  app.get('/api/v1/bootstrap', { schema: bootstrapSchema }, async (req, reply) => {
    const providersHealth = await Promise.all(
      registry.getAll().map(async (p) => {
        const h = await p.healthCheck();
        return {
          id: p.id,
          name: p.name,
          status: h.status,
          score: h.score,
          latencyMs: h.latencyMs,
          capabilities: p.capabilities,
        };
      })
    );

    return reply.send({
      gatewayVersion: config.server.gatewayVersion,
      apiVersion: config.server.apiVersion,
      minimumAndroidVersion: config.server.minimumAndroidVersion,
      supportedProviders: providersHealth,
      features: {
        directToCdnStreaming: !config.stream.proxyStreaming,
        streamUrlAutoRefresh: true,
        circuitBreakerEnabled: true,
      },
      configuration: {
        streamRefreshBufferSeconds: config.stream.urlRefreshBufferSeconds,
      },
    });
  });

  // 2. GET /api/v1/search
  app.get('/api/v1/search', { schema: searchSchema }, async (req, reply) => {
    const { q, filterSongs } = req.query as { q?: string; filterSongs?: string };
    const query = q || '';
    const context = getContext(req);
    const start = Date.now();

    // Check Redis search cache
    const cached = await cache.search.get(query);
    if (cached) {
      globalEventBus.emitEvent({ type: 'CACHE_CHECKED', traceId: context.traceId, namespace: 'search', hit: true });
      searchLatencyHistogram.observe({ cached: 'true' }, (Date.now() - start) / 1000);
      return reply.send({ tracks: cached, cached: true });
    }
    globalEventBus.emitEvent({ type: 'CACHE_CHECKED', traceId: context.traceId, namespace: 'search', hit: false });

    // Execute with failover via ProviderSelectionEngine
    const tracks = await engine.executeWithFailover('search', context, (adapter) =>
      adapter.search(query, context, filterSongs !== 'false')
    );
    searchLatencyHistogram.observe({ cached: 'false' }, (Date.now() - start) / 1000);

    await cache.search.set(query, tracks);
    return reply.send({ tracks, cached: false });
  });

  // 3. GET /api/v1/album/:id
  app.get('/api/v1/album/:id', { schema: albumSchema }, async (req, reply) => {
    const { id } = req.params as { id: string };
    const context = getContext(req);

    const cached = await cache.albums.get(id);
    if (cached) return reply.send({ album: cached, cached: true });

    const album = await engine.executeWithFailover('albums', context, (adapter) =>
      adapter.album(id, context)
    );

    await cache.albums.set(id, album);
    return reply.send({ album, cached: false });
  });

  // 4. GET /api/v1/artist/:id
  app.get('/api/v1/artist/:id', { schema: artistSchema }, async (req, reply) => {
    const { id } = req.params as { id: string };
    const context = getContext(req);

    const cached = await cache.artists.get(id);
    if (cached) return reply.send({ artist: cached, cached: true });

    const artist = await engine.executeWithFailover('artists', context, (adapter) =>
      adapter.artist(id, context)
    );

    await cache.artists.set(id, artist);
    return reply.send({ artist, cached: false });
  });

  // 5. GET /api/v1/playlist/:id
  app.get('/api/v1/playlist/:id', { schema: playlistSchema }, async (req, reply) => {
    const { id } = req.params as { id: string };
    const context = getContext(req);

    const cached = await cache.playlists.get(id);
    if (cached) return reply.send({ playlist: cached, cached: true });

    const playlist = await engine.executeWithFailover('playlists', context, (adapter) =>
      adapter.playlist(id, context)
    );

    await cache.playlists.set(id, playlist);
    return reply.send({ playlist, cached: false });
  });

  // 6. POST /api/v1/stream (Direct-to-CDN Stream Resolution)
  app.post('/api/v1/stream', { schema: streamSchema }, async (req, reply) => {
    const { trackId } = req.body as { trackId: string };
    const context = getContext(req);
    const startTime = Date.now();

    const streamResult = await engine.executeWithFailover('playback', context, (adapter) =>
      adapter.stream(trackId, context)
    );

    const durationMs = Date.now() - startTime;
    globalEventBus.emitEvent({
      type: 'STREAM_RESOLVED',
      traceId: context.traceId,
      trackId,
      durationMs,
    });

    if (config.stream.proxyStreaming && streamResult?.streamUrl) {
      const host = req.headers.host ?? `localhost:${config.server.port}`;
      return reply.send({
        stream: {
          ...streamResult,
          streamUrl: `http://${host}/api/v1/stream/proxy/${encodeURIComponent(trackId)}`,
          expiresAtEpochSeconds: 0,
        },
      });
    }

    return reply.send({ stream: streamResult });
  });

  // 6b. GET /api/v1/stream/proxy/:trackId (CDN media relay — Range-safe)
  app.get('/api/v1/stream/proxy/:trackId', { schema: streamProxySchema }, async (req, reply) => {
    const { trackId } = req.params as { trackId: string };
    const context = getContext(req);

    const resolved = await resolveCdnStreamUrl(trackId, context);
    if (!resolved.ok) {
      return reply.code(404).send({
        error: {
          code: 'STREAM_NOT_FOUND',
          message: resolved.message,
          providerId: 'youtube',
        },
      });
    }

    const clientRange = (req.headers.range as string | undefined) ?? '';
    const abort = new AbortController();
    req.raw.on('close', () => abort.abort());

    const rangeMatch = /^bytes=(\d*)-(\d*)$/i.exec(clientRange);
    const hasRange = !!rangeMatch;
    const from = rangeMatch?.[1] ? parseInt(rangeMatch[1], 10) : undefined;
    const to = rangeMatch?.[2] ? parseInt(rangeMatch[2], 10) : undefined;
    const upstreamRange = hasRange
      ? `bytes=${from ?? 0}-${to ?? Math.max(resolved.total - 1, 0)}`
      : `bytes=0-${Math.max(resolved.total - 1, 0)}`;

    let upstream: Response;
    try {
      upstream = await fetch(resolved.url, {
        headers: {
          Range: upstreamRange,
          'User-Agent': 'CliBeatsGateway/1.0 (media relay)',
        },
        signal: abort.signal,
        redirect: 'follow',
      });
    } catch (err) {
      logger.warn({ error: (err as Error).message, trackId }, 'CDN relay fetch failed');
      if (!reply.raw.writableEnded) reply.raw.destroy();
      return reply;
    }

    if (!upstream.ok && upstream.status !== 206) {
      return reply.code(502).send({
        error: {
          code: 'STREAM_UPSTREAM_ERROR',
          message: `CDN responded with HTTP ${upstream.status}`,
          providerId: 'youtube',
        },
      });
    }

    const contentType = upstream.headers.get('content-type') ?? 'audio/mp4';

    reply.header('Accept-Ranges', 'bytes');
    reply.header('Content-Type', contentType);

    if (hasRange) {
      const start = from ?? 0;
      const end = to ?? resolved.total - 1;
      reply.code(206);
      reply.header('Content-Range', `bytes ${start}-${end}/${resolved.total}`);
      reply.header('Content-Length', String(end - start + 1));
    } else {
      reply.code(200);
      reply.header('Content-Length', String(resolved.total));
    }

    return reply.send(Readable.fromWeb(upstream.body as any));
  });

  // 7. GET /health (Machine-Readable Aggregate Health)
  app.get('/health', { schema: healthSchema }, async (req, reply) => {
    const providersHealth: Record<string, unknown> = {};
    for (const p of registry.getAll()) {
      providersHealth[p.id] = await p.healthCheck();
    }

    const redisHealth = await healthChecker.check();
    const isGatewayHealthy = redisHealth.status !== 'DOWN';

    return reply.send({
      gateway: isGatewayHealthy ? 'HEALTHY' : 'DEGRADED',
      redis: redisHealth.status,
      redisLatencyMs: redisHealth.latencyMs,
      providers: providersHealth,
      uptime: process.uptime(),
      version: config.server.gatewayVersion,
    });
  });

  // 8. GET /metrics (Prometheus Metrics Exporter)
  app.get('/metrics', { schema: metricsSchema }, async (req, reply) => {
    reply.header('Content-Type', prometheusRegister.contentType);
    return reply.send(await prometheusRegister.metrics());
  });

  // 9. GET /version & GET /api/v1/providers
  app.get('/version', { schema: versionSchema }, async (req, reply) =>
    reply.send({ version: config.server.gatewayVersion })
  );
  app.get('/api/v1/providers', { schema: providersSchema }, async (req, reply) => {
    const active = registry.getAll().map((p) => ({
      id: p.id,
      name: p.name,
      priority: p.priority,
      capabilities: p.capabilities,
    }));
    return reply.send({ providers: active });
  });

  return app;
}