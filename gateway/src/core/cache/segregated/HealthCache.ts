import Redis from 'ioredis';

export interface ProviderHealthRecord {
  status: 'HEALTHY' | 'DEGRADED' | 'UNHEALTHY';
  score: number;
  latencyMs: number;
  lastChecked: string;
}

export class HealthCache {
  constructor(private redis: Redis, private ttlSeconds: number = 300) {}

  public async getHealth(providerId: string): Promise<ProviderHealthRecord | null> {
    const raw = await this.redis.get(`provider-health:${providerId}`);
    if (!raw) return null;
    return JSON.parse(raw) as ProviderHealthRecord;
  }

  public async setHealth(providerId: string, record: ProviderHealthRecord): Promise<void> {
    await this.redis.set(`provider-health:${providerId}`, JSON.stringify(record), 'EX', this.ttlSeconds);
  }
}
