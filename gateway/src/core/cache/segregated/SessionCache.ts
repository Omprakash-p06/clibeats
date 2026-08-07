import Redis from 'ioredis';
import { RedisCacheBase } from '../RedisCacheBase';

export class SessionCache extends RedisCacheBase {
  constructor(redis: Redis, keyPrefix?: string) {
    super(redis, 'session', 0, keyPrefix);
  }

  public async getSession(providerId: string, userId: string): Promise<string | null> {
    return this.safeGet(this.key(`${providerId}:${userId}`));
  }

  public async setSession(
    providerId: string,
    userId: string,
    sessionData: string,
    ttlSeconds: number = 86400
  ): Promise<void> {
    await this.safeSetTtl(this.key(`${providerId}:${userId}`), sessionData, ttlSeconds);
  }
}