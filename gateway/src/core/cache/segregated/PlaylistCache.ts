import Redis from 'ioredis';
import { Playlist } from '../../../types/domain';

export class PlaylistCache {
  constructor(private redis: Redis, private ttlSeconds: number = 86400) {}

  public async get(id: string): Promise<Playlist | null> {
    const raw = await this.redis.get(`playlists:${id}`);
    if (!raw) return null;
    return JSON.parse(raw) as Playlist;
  }

  public async set(id: string, playlist: Playlist): Promise<void> {
    await this.redis.set(`playlists:${id}`, JSON.stringify(playlist), 'EX', this.ttlSeconds);
  }
}
