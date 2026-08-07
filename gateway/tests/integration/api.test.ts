import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { FastifyInstance } from 'fastify';
import { buildApp } from '../../src/app';

describe('Gateway Fastify Integration Tests (fastify.inject)', () => {
  let app: FastifyInstance;

  beforeEach(async () => {
    app = await buildApp();
    await app.ready();
  });

  afterEach(async () => {
    await app.close();
  });

  it('GET /api/v1/bootstrap returns aggregated cold-start context (ADR-020)', async () => {
    const res = await app.inject({
      method: 'GET',
      url: '/api/v1/bootstrap',
    });

    expect(res.statusCode).toBe(200);
    const body = JSON.parse(res.payload);
    expect(body.gatewayVersion).toBeDefined();
    expect(body.apiVersion).toBe('1.0.0');
    expect(body.supportedProviders.length).toBeGreaterThan(0);
    expect(body.supportedProviders[0].id).toBe('mock');
    expect(body.features.directToCdnStreaming).toBe(true);
  });

  it('GET /api/v1/search executes provider search and returns canonical tracks', async () => {
    const res = await app.inject({
      method: 'GET',
      url: '/api/v1/search?q=cyber',
    });

    expect(res.statusCode).toBe(200);
    const body = JSON.parse(res.payload);
    expect(body.tracks).toBeDefined();
    expect(Array.isArray(body.tracks)).toBe(true);
    expect(body.tracks.length).toBeGreaterThan(0);
  });

  it('POST /api/v1/stream resolves direct-to-CDN stream URL', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/api/v1/stream',
      payload: { trackId: 'mock-track-1' },
    });

    expect(res.statusCode).toBe(200);
    const body = JSON.parse(res.payload);
    expect(body.stream).toBeDefined();
    expect(body.stream.streamUrl).toContain('mock-cdn.clibeats.internal');
  });

  it('GET /health returns machine-readable status breakdown', async () => {
    const res = await app.inject({
      method: 'GET',
      url: '/health',
    });

    expect(res.statusCode).toBe(200);
    const body = JSON.parse(res.payload);
    expect(body.gateway).toBe('HEALTHY');
    expect(body.providers.mock.status).toBe('HEALTHY');
  });

  it('GET /metrics exports Prometheus metrics', async () => {
    const res = await app.inject({
      method: 'GET',
      url: '/metrics',
    });

    expect(res.statusCode).toBe(200);
    expect(res.payload).toContain('gateway_requests_total');
  });

  it('GET /version returns server version', async () => {
    const res = await app.inject({
      method: 'GET',
      url: '/version',
    });

    expect(res.statusCode).toBe(200);
    const body = JSON.parse(res.payload);
    expect(body.version).toBeDefined();
  });

  it('GET /documentation serves OpenAPI Swagger UI', async () => {
    const res = await app.inject({
      method: 'GET',
      url: '/documentation',
    });

    expect([200, 302]).toContain(res.statusCode);
  });

  it('propagates trace ID for end-to-end request correlation (P4)', async () => {
    const res = await app.inject({
      method: 'GET',
      url: '/api/v1/search?q=trace-test',
      headers: { 'x-trace-id': 'trace-abc-123' },
    });

    expect(res.statusCode).toBe(200);
    expect(res.headers['x-trace-id']).toBe('trace-abc-123');
  });
});
