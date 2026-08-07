import Redis from 'ioredis';
import { Playlist } from '../../../types/domain';
import { RedisCacheBase } from '../RedisCacheBase';

export class PlaylistCache extends RedisCacheBase {
  constructor(redis: Redis, ttlSeconds: number = 86400, keyPrefix?: string) {
    super(redis, 'playlists', ttlSeconds, keyPrefix);
  }

  public async get(id: string): Promise<Playlist | null> {
    const raw = await this.safeGet(this.key(id));
    if (!raw) return null;
    return JSON.parse(raw) as Playlist;
  }

  public async set(id: string, playlist: Playlist): Promise<void> {
    await this.safeSet(this.key(id), JSON.stringify(playlist));
  }
}