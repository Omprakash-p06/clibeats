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
import { MockProviderAdapter } from './providers/mock/MockProviderAdapter';
import { ProviderContext } from './types/context';
import { register as prometheusRegister } from './core/metrics/metrics';
import { globalEventBus } from './core/events/EventBus';
import { ProviderError } from './types/error';

// Fastify TypeScript decoration
declare module 'fastify' {
  interface FastifyInstance {
    config: GatewayConfig;
    registry: ProviderRegistry;
    engine: ProviderSelectionEngine;
    cache: CacheManager;
  }
}

export function buildApp(customConfig?: Partial<GatewayConfig>, redisClient?: Redis): FastifyInstance {
  const app = fastify({ logger: false });
  const config = { ...loadConfig(), ...customConfig };

  // Setup Redis (use ioredis-mock in test mode if no redis client provided)
  const redis =
    redisClient ||
    (process.env.NODE_ENV === 'test'
      ? (new RedisMock() as unknown as Redis)
      : new Redis(config.cache.redisUrl));

  const cache = new CacheManager(redis, config);
  const registry = new ProviderRegistry();
  const engine = new ProviderSelectionEngine(registry);

  // Register default MockProviderAdapter in Milestone 0
  if (config.providers.mock?.enabled !== false) {
    registry.register(new MockProviderAdapter());
  }

  // Decorate fastify instance for DI
  app.decorate('config', config);
  app.decorate('registry', registry);
  app.decorate('engine', engine);
  app.decorate('cache', cache);

  // Register plugins
  app.register(cors, { origin: config.server.corsOrigins });
  app.register(swagger, {
    openapi: {
      info: {
        title: 'CliBeats Provider Gateway API',
        description: 'Provider-agnostic audio gateway REST API specification',
        version: config.server.apiVersion,
      },
    },
  });
  app.register(swaggerUi, { routePrefix: '/documentation' });

  // Trace ID middleware
  app.addHook('onRequest', async (req: FastifyRequest) => {
    const traceId = (req.headers['x-trace-id'] as string) || `trace-${Math.random().toString(36).substring(2, 11)}`;
    req.headers['x-trace-id'] = traceId;
    globalEventBus.emitEvent({
      type: 'REQUEST_RECEIVED',
      traceId,
      endpoint: req.url,
      clientIp: req.ip,
    });
  });

  // Global Error Handler (ADR-016)
  app.setErrorHandler((error: Error, req: FastifyRequest, reply: FastifyReply) => {
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

    return reply.status(500).send({
      error: {
        code: 'INTERNAL_ERROR',
        message: error.message || 'Internal Server Error',
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
  app.get('/api/v1/bootstrap', async (req, reply) => {
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
  app.get('/api/v1/search', async (req, reply) => {
    const { q, filterSongs } = req.query as { q?: string; filterSongs?: string };
    const query = q || '';
    const context = getContext(req);

    // Check Redis search cache
    const cached = await cache.search.get(query);
    if (cached) {
      globalEventBus.emitEvent({ type: 'CACHE_CHECKED', traceId: context.traceId, namespace: 'search', hit: true });
      return reply.send({ tracks: cached, cached: true });
    }
    globalEventBus.emitEvent({ type: 'CACHE_CHECKED', traceId: context.traceId, namespace: 'search', hit: false });

    // Execute with failover via ProviderSelectionEngine
    const tracks = await engine.executeWithFailover('search', context, (adapter) =>
      adapter.search(query, context, filterSongs !== 'false')
    );

    await cache.search.set(query, tracks);
    return reply.send({ tracks, cached: false });
  });

  // 3. GET /api/v1/album/:id
  app.get('/api/v1/album/:id', async (req, reply) => {
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
  app.get('/api/v1/artist/:id', async (req, reply) => {
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
  app.get('/api/v1/playlist/:id', async (req, reply) => {
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
  app.post('/api/v1/stream', async (req, reply) => {
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
  app.get('/health', async (req, reply) => {
    const providersHealth: Record<string, any> = {};
    for (const p of registry.getAll()) {
      providersHealth[p.id] = await p.healthCheck();
    }

    return reply.send({
      gateway: 'HEALTHY',
      redis: 'CONNECTED',
      providers: providersHealth,
      uptime: process.uptime(),
      version: config.server.gatewayVersion,
    });
  });

  // 8. GET /metrics (Prometheus Metrics Exporter)
  app.get('/metrics', async (req, reply) => {
    reply.header('Content-Type', prometheusRegister.contentType);
    return reply.send(await prometheusRegister.metrics());
  });

  // 9. GET /version & GET /api/v1/providers
  app.get('/version', async (req, reply) => reply.send({ version: config.server.gatewayVersion }));
  app.get('/api/v1/providers', async (req, reply) => {
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
