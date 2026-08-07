import Redis from 'ioredis';
import { Artist } from '../../../types/domain';

export class ArtistCache {
  constructor(private redis: Redis, private ttlSeconds: number = 86400) {}

  public async get(id: string): Promise<Artist | null> {
    const raw = await this.redis.get(`artists:${id}`);
    if (!raw) return null;
    return JSON.parse(raw) as Artist;
  }

  public async set(id: string, artist: Artist): Promise<void> {
    await this.redis.set(`artists:${id}`, JSON.stringify(artist), 'EX', this.ttlSeconds);
  }
}
