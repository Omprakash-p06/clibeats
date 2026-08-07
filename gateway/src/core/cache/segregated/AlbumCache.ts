import Redis from 'ioredis';
import { Album } from '../../../types/domain';
import { RedisCacheBase } from '../RedisCacheBase';

export class AlbumCache extends RedisCacheBase {
  constructor(redis: Redis, ttlSeconds: number = 86400, keyPrefix?: string) {
    super(redis, 'albums', ttlSeconds, keyPrefix);
  }

  public async get(id: string): Promise<Album | null> {
    const raw = await this.safeGet(this.key(id));
    if (!raw) return null;
    return JSON.parse(raw) as Album;
  }

  public async set(id: string, album: Album): Promise<void> {
    await this.safeSet(this.key(id), JSON.stringify(album));
  }
}