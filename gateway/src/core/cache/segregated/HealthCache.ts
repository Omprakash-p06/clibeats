import Redis from 'ioredis';
import { RedisCacheBase } from '../RedisCacheBase';

export interface ProviderHealthRecord {
  status: 'HEALTHY' | 'DEGRADED' | 'UNHEALTHY';
  score: number;
  latencyMs: number;
  lastChecked: string;
}

export class HealthCache extends RedisCacheBase {
  constructor(redis: Redis, ttlSeconds: number = 300, keyPrefix?: string) {
    super(redis, 'provider-health', ttlSeconds, keyPrefix);
  }

  public async getHealth(providerId: string): Promise<ProviderHealthRecord | null> {
    const raw = await this.safeGet(this.key(providerId));
    if (!raw) return null;
    return JSON.parse(raw) as ProviderHealthRecord;
  }

  public async setHealth(providerId: string, record: ProviderHealthRecord): Promise<void> {
    await this.safeSet(this.key(providerId), JSON.stringify(record));
  }
}