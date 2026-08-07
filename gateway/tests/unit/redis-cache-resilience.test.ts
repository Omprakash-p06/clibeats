import { test, describe, expect, beforeAll, afterAll, it } from 'vitest';
import RedisMock from 'ioredis-mock';
import { SearchCache } from '../../src/core/cache/segregated/SearchCache';
import { AlbumCache } from '../../src/core/cache/segregated/AlbumCache';
import { SessionCache } from '../../src/core/cache/segregated/SessionCache';
import { Track } from '../../src/types/domain';

describe('Redis Cache Resilience (P3b)', () => {
  let redis: RedisMock;
  let search: SearchCache;

  beforeAll(() => {
    redis = new RedisMock();
    search = new SearchCache(redis as never, 3600, 'clibeats');
  });

  afterAll(async () => {
    await redis.quit();
  });

  it('namespaces keys with the gateway prefix', async () => {
    await search.set('hello', [{ id: 't1', title: 'Hi', artist: 'X', durationMs: 10000 } as Track]);
    const keys = await redis.keys('clibeats:search:*');
    expect(keys).toHaveLength(1);
    expect(keys[0]).toBe('clibeats:search:hello');
  });

  it('invariant: different domain namespaces do not collide', async () => {
    const albums = new AlbumCache(redis as never, 86400, 'clibeats');
    await albums.set('t1', { id: 't1', name: 'Album' } as never);
    const searchTracks = await search.get('t1');
    expect(searchTracks).toBeNull();
  });

  it('returns null (fail-open) when Redis read fails', async () => {
    const broken = new SearchCache(
      { get: async () => { throw new Error('redis down'); } } as never,
      60,
      'clibeats'
    );
    const result = await broken.get('anything');
    expect(result).toBeNull();
  });

  it('does not throw when Redis write fails', async () => {
    const broken = new SearchCache(
      { set: async () => { throw new Error('redis down'); } } as never,
      60,
      'clibeats'
    );
    await expect(broken.set('query', [])).resolves.toBeUndefined();
  });

  it('invalidate removes a single key', async () => {
    await search.set('inval', [{ id: 'x', title: 't' } as Track]);
    expect(await search.get('inval')).not.toBeNull();
    await search.invalidate('inval');
    expect(await search.get('inval')).toBeNull();
  });

  it('session cache applies explicit TTL with namespaced key', async () => {
    const session = new SessionCache(redis as never, 'clibeats');
    await session.setSession('mock', 'u1', 'data', 100);
    const val = await redis.get('clibeats:session:mock:u1');
    expect(val).toBe('data');
    const ttl = await redis.ttl('clibeats:session:mock:u1');
    expect(ttl).toBeGreaterThan(0);
  });
});