import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { FastifyInstance } from 'fastify';
import { buildApp } from '../../src/app';
import { MockProviderAdapter } from '../../src/providers/mock/MockProviderAdapter';

describe('Gateway Failover & Circuit Breaker Integration Tests', () => {
  let app: FastifyInstance;
  let primaryMock: MockProviderAdapter;
  let secondaryMock: MockProviderAdapter;

  beforeEach(async () => {
    app = await buildApp({ providers: { mock: { enabled: false, priority: 0 } } });

    primaryMock = new MockProviderAdapter('primary-mock', 42, 300);
    secondaryMock = new MockProviderAdapter('secondary-mock', 100, 100);

    app.registry.register(primaryMock);
    app.registry.register(secondaryMock);

    await app.ready();
  });

  afterEach(async () => {
    await app.close();
  });

  it('Transparent failover routes request to secondary provider when primary fails', async () => {
    // Enable failure simulation on primary
    primaryMock.shouldSimulateError = true;
    primaryMock.simulatedErrorCode = 'PLAYBACK_ERROR';

    const res = await app.inject({
      method: 'GET',
      url: '/api/v1/search?q=cyber',
    });

    expect(res.statusCode).toBe(200);
    const body = JSON.parse(res.payload);
    expect(body.tracks.length).toBeGreaterThan(0);
    // Secondary mock track provider ID
    expect(body.tracks[0].providerId).toBe('secondary-mock');
  });

  it('Circuit breaker trips after 3 failures and bypasses primary provider', async () => {
    primaryMock.shouldSimulateError = true;

    // Fail 3 times with unique queries to bypass search cache
    for (let i = 0; i < 3; i++) {
      await app.inject({ method: 'GET', url: `/api/v1/search?q=uncached-query-${i}` });
    }

    const cb = app.engine.getCircuitBreaker('primary-mock');
    expect(cb.getState()).toBe('OPEN');
    expect(cb.isAvailable()).toBe(false);
  });
});
