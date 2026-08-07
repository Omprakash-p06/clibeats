import Redis from 'ioredis';
import { Album } from '../../../types/domain';

export class AlbumCache {
  constructor(private redis: Redis, private ttlSeconds: number = 86400) {}

  public async get(id: string): Promise<Album | null> {
    const raw = await this.redis.get(`albums:${id}`);
    if (!raw) return null;
    return JSON.parse(raw) as Album;
  }

  public async set(id: string, album: Album): Promise<void> {
    await this.redis.set(`albums:${id}`, JSON.stringify(album), 'EX', this.ttlSeconds);
  }
}
