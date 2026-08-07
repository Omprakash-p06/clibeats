import Redis from 'ioredis';
import { Track } from '../../../types/domain';

export class SearchCache {
  constructor(private redis: Redis, private ttlSeconds: number = 3600) {}

  public async get(query: string): Promise<Track[] | null> {
    const key = `search:${query.toLowerCase().trim()}`;
    const raw = await this.redis.get(key);
    if (!raw) return null;
    return JSON.parse(raw) as Track[];
  }

  public async set(query: string, tracks: Track[]): Promise<void> {
    const key = `search:${query.toLowerCase().trim()}`;
    await this.redis.set(key, JSON.stringify(tracks), 'EX', this.ttlSeconds);
  }
}
