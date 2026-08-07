import Redis from 'ioredis';
import { Track } from '../../../types/domain';
import { RedisCacheBase } from '../RedisCacheBase';

export class SearchCache extends RedisCacheBase {
  constructor(redis: Redis, ttlSeconds: number = 3600, keyPrefix?: string) {
    super(redis, 'search', ttlSeconds, keyPrefix);
  }

  public async get(query: string): Promise<Track[] | null> {
    const raw = await this.safeGet(this.key(query.toLowerCase().trim()));
    if (!raw) return null;
    return JSON.parse(raw) as Track[];
  }

  public async set(query: string, tracks: Track[]): Promise<void> {
    await this.safeSet(this.key(query.toLowerCase().trim()), JSON.stringify(tracks));
  }
}