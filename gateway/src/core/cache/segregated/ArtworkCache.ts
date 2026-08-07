import Redis from 'ioredis';

export class ArtworkCache {
  constructor(private redis: Redis, private ttlSeconds: number = 604800) {}

  public async get(id: string): Promise<string | null> {
    return this.redis.get(`artwork:${id}`);
  }

  public async set(id: string, url: string): Promise<void> {
    await this.redis.set(`artwork:${id}`, url, 'EX', this.ttlSeconds);
  }
}
