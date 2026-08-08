import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { FastifyInstance } from 'fastify';
import Redis from 'ioredis';
import { buildApp } from '../../src/app';

describe('Gateway /health Redis Truthfulness', () => {
  it('reports redis UP when healthy', async () => {
    const mockRedis = {
      ping: async () => 'PONG',
      get: async () => null,
      set: async () => 'OK',
    };
    const app = await buildApp({ providers: { mock: { enabled: true, priority: 100 }, youtube: { enabled: false } } }, mockRedis as unknown as Redis);
    await app.ready();
    const res = await app.inject({ method: 'GET', url: '/health' });
    const body = JSON.parse(res.payload);
    expect(res.statusCode).toBe(200);
    expect(body.redis).toBe('UP');
    expect(['HEALTHY', 'DEGRADED']).toContain(body.gateway);
    await app.close();
  }, 10000);

  it('reports redis DOWN when redis unresponsive', async () => {
    const failingRedis = {
      // Simulate an unavailable Redis: all commands reject.
      get: async () => {
        throw new Error('Redis unavailable');
      },
      set: async () => {
        throw new Error('Redis unavailable');
      },
      ping: async () => {
        throw new Error('Redis unavailable');
      },
    };

    const app = await buildApp(
      {},
      failingRedis as unknown as Redis
    );
    await app.ready();
    const res = await app.inject({ method: 'GET', url: '/health' });
    const body = JSON.parse(res.payload);
    expect(body.redis).toBe('DOWN');
    expect(body.gateway).toBe('DEGRADED');
    await app.close();
  });

  it('health does not lie: DOWN is reported even if the endpoint still returns 200', async () => {
    const failingRedis = {
      get: async () => {
        throw new Error('offline');
      },
      set: async () => {
        throw new Error('offline');
      },
      ping: async () => {
        throw new Error('offline');
      },
    };
    const app = await buildApp({}, failingRedis as unknown as Redis);
    await app.ready();
    const res = await app.inject({ method: 'GET', url: '/health' });
    const body = JSON.parse(res.payload);
    expect(body.redis).not.toBe('UP');
    await app.close();
  });

  it('is machine-readable with stable fields', async () => {
    const app = await buildApp();
    await app.ready();
    const res = await app.inject({ method: 'GET', url: '/health' });
    const body = JSON.parse(res.payload);
    expect(typeof body.uptime).toBe('number');
    expect(typeof body.version).toBe('string');
    expect(body.providers).toBeDefined();
    await app.close();
  });
});