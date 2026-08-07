import Redis from 'ioredis';
import { RedisCacheBase } from '../RedisCacheBase';

export class ArtworkCache extends RedisCacheBase {
  constructor(redis: Redis, ttlSeconds: number = 604800, keyPrefix?: string) {
    super(redis, 'artwork', ttlSeconds, keyPrefix);
  }

  public async get(id: string): Promise<string | null> {
    return this.safeGet(this.key(id));
  }

  public async set(id: string, url: string): Promise<void> {
    await this.safeSet(this.key(id), url);
  }
}