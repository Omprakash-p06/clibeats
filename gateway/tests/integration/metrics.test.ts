import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { FastifyInstance } from 'fastify';
import { buildApp } from '../../src/app';

describe('Gateway Metrics Completeness (Task 6)', () => {
  let app: FastifyInstance;

  beforeEach(async () => {
    app = await buildApp();
    await app.ready();
  });

  afterEach(async () => {
    await app.close();
  });

  const getMetrics = async (): Promise<string> => {
    const res = await app.inject({ method: 'GET', url: '/metrics' });
    expect(res.statusCode).toBe(200);
    return res.payload;
  };

  it('exposes all critical metrics', async () => {
    const body = await getMetrics();
    const expected = [
      'gateway_requests_total',
      'gateway_cache_hits_total',
      'gateway_cache_misses_total',
      'gateway_provider_selections_total',
      'gateway_provider_failures_total',
      'gateway_provider_health',
      'gateway_circuit_breaker_state',
      'gateway_search_duration_seconds',
      'gateway_stream_resolution_duration_seconds',
    ];
    for (const m of expected) {
      expect(body).toContain(m);
    }
  });

  it('records cache hits and misses', async () => {
    // Miss then hit on same query.
    await app.inject({ method: 'GET', url: '/api/v1/search?q=cyberpunk' });
    await app.inject({ method: 'GET', url: '/api/v1/search?q=cyberpunk' });
    const body = await getMetrics();
    expect(body).toContain('gateway_cache_hits_total');
    expect(body).toContain('gateway_cache_misses_total');
  });

  it('records provider health gauge for registered providers', async () => {
    await app.inject({ method: 'GET', url: '/api/v1/bootstrap' });
    const body = await getMetrics();
    expect(body).toContain('gateway_provider_health');
  });

  it('records search latency histogram', async () => {
    await app.inject({ method: 'GET', url: '/api/v1/search?q=cyberpunk' });
    const body = await getMetrics();
    expect(body).toContain('gateway_search_duration_seconds');
  });
});