import Redis from 'ioredis';
import { globalEventBus } from '../events/EventBus';

/**
 * Resilient Redis-backed cache primitive (P3b resolution).
 *
 * Guarantees:
 * - Keys are namespaced (`clibeats:<namespace>:<key>`) to avoid collisions on shared Redis.
 * - Reads/writes/deletes are fail-open: a Redis outage degrades to a cache miss
 *   instead of crashing the request (ADR-013 high availability).
 * - Optional per-key TTL is applied on write.
 */
export abstract class RedisCacheBase {
  protected readonly prefix: string;

  constructor(
    protected readonly redis: Redis,
    protected readonly namespace: string,
    protected readonly ttlSeconds: number = 0,
    keyPrefix: string = 'clibeats'
  ) {
    this.prefix = keyPrefix ? `${keyPrefix}:${namespace}:` : `${namespace}:`;
  }

  protected key(id: string): string {
    return `${this.prefix}${id}`;
  }

  protected async safeGet(key: string): Promise<string | null> {
    try {
      return await this.redis.get(key);
    } catch {
      globalEventBus.emitEvent({ type: 'CACHE_ERROR', traceId: 'internal', namespace: this.namespace, operation: 'get' });
      return null;
    }
  }

  protected async safeSet(key: string, value: string): Promise<void> {
    await this.safeSetTtl(key, value, this.ttlSeconds);
  }

  protected async safeSetTtl(key: string, value: string, ttlSeconds: number): Promise<void> {
    try {
      if (ttlSeconds > 0) {
        await this.redis.set(key, value, 'EX', ttlSeconds);
      } else {
        await this.redis.set(key, value);
      }
    } catch {
      globalEventBus.emitEvent({ type: 'CACHE_ERROR', traceId: 'internal', namespace: this.namespace, operation: 'set' });
    }
  }

  protected async safeDel(key: string): Promise<void> {
    try {
      await this.redis.del(key);
    } catch {
      globalEventBus.emitEvent({ type: 'CACHE_ERROR', traceId: 'internal', namespace: this.namespace, operation: 'del' });
    }
  }

  /** Delete a single cached entry by its logical id. */
  public async invalidate(id: string): Promise<void> {
    await this.safeDel(this.key(id));
  }
}