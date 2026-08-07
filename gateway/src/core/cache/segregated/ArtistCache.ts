import Redis from 'ioredis';
import { Artist } from '../../../types/domain';
import { RedisCacheBase } from '../RedisCacheBase';

export class ArtistCache extends RedisCacheBase {
  constructor(redis: Redis, ttlSeconds: number = 86400, keyPrefix?: string) {
    super(redis, 'artists', ttlSeconds, keyPrefix);
  }

  public async get(id: string): Promise<Artist | null> {
    const raw = await this.safeGet(this.key(id));
    if (!raw) return null;
    return JSON.parse(raw) as Artist;
  }

  public async set(id: string, artist: Artist): Promise<void> {
    await this.safeSet(this.key(id), JSON.stringify(artist));
  }
}