import fastify, { FastifyInstance, FastifyRequest, FastifyReply } from 'fastify';
import cors from '@fastify/cors';
import swagger from '@fastify/swagger';
import swaggerUi from '@fastify/swagger-ui';
import Redis from 'ioredis';
import RedisMock from 'ioredis-mock';
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
    return new RedisMock() as unknown as Redis;
  }
  return new Redis(config.cache.redisUrl);
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
        directToCdnStreaming: true,
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

    return reply.send({ stream: streamResult });
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