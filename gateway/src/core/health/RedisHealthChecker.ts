import Redis from 'ioredis';

export type RedisHealthStatus = 'UP' | 'DEGRADED' | 'DOWN';

export interface RedisHealthResult {
  status: RedisHealthStatus;
  latencyMs: number;
  message?: string;
}

export interface RedisHealthOptions {
  timeoutMs: number;
}

export class RedisHealthChecker {
  private readonly timeoutMs: number;

  constructor(redis: Redis, options?: Partial<RedisHealthOptions>) {
    this.redis = redis;
    this.timeoutMs = options?.timeoutMs ?? 1000;
  }

  private readonly redis: Redis;

  public async check(): Promise<RedisHealthResult> {
    const start = Date.now();
    try {
      const result = await this.pingWithTimeout();
      const latencyMs = Date.now() - start;
      if (result === 'PONG') {
        return { status: 'UP', latencyMs };
      }
      return {
        status: 'DEGRADED',
        latencyMs,
        message: `Unexpected Redis PING reply: ${result}`,
      };
    } catch (err: any) {
      const latencyMs = Date.now() - start;
      return {
        status: 'DOWN',
        latencyMs,
        message: err?.message || 'Redis PING failed',
      };
    }
  }

  private pingWithTimeout(): Promise<string> {
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        reject(new Error(`Redis health check timed out after ${this.timeoutMs}ms`));
      }, this.timeoutMs);

      this.redis
        .ping()
        .then((result) => {
          clearTimeout(timer);
          resolve(result);
        })
        .catch((err) => {
          clearTimeout(timer);
          reject(err);
        });
    });
  }
}
