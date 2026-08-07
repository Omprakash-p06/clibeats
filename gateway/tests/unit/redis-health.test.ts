import { describe, it, expect, vi } from 'vitest';
import { RedisHealthChecker } from '../../src/core/health/RedisHealthChecker';

describe('RedisHealthChecker', () => {
  const makeMockRedis = (ping: () => Promise<string>) =>
    ({ ping } as any);

  it('reports UP when PING returns PONG', async () => {
    const checker = new RedisHealthChecker(makeMockRedis(() => Promise.resolve('PONG')), {
      timeoutMs: 100,
    });
    const result = await checker.check();
    expect(result.status).toBe('UP');
    expect(result.latencyMs).toBeGreaterThanOrEqual(0);
  });

  it('reports DEGRADED when PING returns unexpected reply', async () => {
    const checker = new RedisHealthChecker(makeMockRedis(() => Promise.resolve('PINGPONG')), {
      timeoutMs: 100,
    });
    const result = await checker.check();
    expect(result.status).toBe('DEGRADED');
    expect(result.message).toContain('Unexpected Redis PING reply');
  });

  it('reports DOWN when PING rejects', async () => {
    const checker = new RedisHealthChecker(
      makeMockRedis(() => Promise.reject(new Error('connection refused'))),
      { timeoutMs: 100 }
    );
    const result = await checker.check();
    expect(result.status).toBe('DOWN');
    expect(result.message).toBe('connection refused');
  });

  it('reports DOWN on timeout when PING never resolves', async () => {
    const checker = new RedisHealthChecker(
      makeMockRedis(() => new Promise(() => {})),
      { timeoutMs: 50 }
    );
    vi.useFakeTimers();
    const pending = checker.check();
    await vi.advanceTimersByTimeAsync(60);
    const result = await pending;
    expect(result.status).toBe('DOWN');
    expect(result.message).toContain('timed out');
    vi.useRealTimers();
  });
});