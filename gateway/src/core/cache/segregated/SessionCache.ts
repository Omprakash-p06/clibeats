import Redis from 'ioredis';

export class SessionCache {
  constructor(private redis: Redis) {}

  public async getSession(providerId: string, userId: string): Promise<string | null> {
    return this.redis.get(`session:${providerId}:${userId}`);
  }

  public async setSession(
    providerId: string,
    userId: string,
    sessionData: string,
    ttlSeconds: number = 86400
  ): Promise<void> {
    await this.redis.set(`session:${providerId}:${userId}`, sessionData, 'EX', ttlSeconds);
  }
}
