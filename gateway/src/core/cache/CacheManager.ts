import Redis from 'ioredis';
import { GatewayConfig } from '../../config/config';
import { SearchCache } from './segregated/SearchCache';
import { AlbumCache } from './segregated/AlbumCache';
import { ArtistCache } from './segregated/ArtistCache';
import { PlaylistCache } from './segregated/PlaylistCache';
import { SessionCache } from './segregated/SessionCache';
import { ArtworkCache } from './segregated/ArtworkCache';
import { HealthCache } from './segregated/HealthCache';

export class CacheManager {
  public readonly search: SearchCache;
  public readonly albums: AlbumCache;
  public readonly artists: ArtistCache;
  public readonly playlists: PlaylistCache;
  public readonly session: SessionCache;
  public readonly artwork: ArtworkCache;
  public readonly health: HealthCache;

  constructor(public readonly redis: Redis, config: GatewayConfig) {
    const prefix = config.cache.keyPrefix ?? 'clibeats';
    this.search = new SearchCache(redis, config.cache.searchTTLSeconds, prefix);
    this.albums = new AlbumCache(redis, config.cache.metadataTTLSeconds, prefix);
    this.artists = new ArtistCache(redis, config.cache.metadataTTLSeconds, prefix);
    this.playlists = new PlaylistCache(redis, config.cache.metadataTTLSeconds, prefix);
    this.session = new SessionCache(redis, prefix);
    this.artwork = new ArtworkCache(redis, config.cache.artworkTTLSeconds, prefix);
    this.health = new HealthCache(redis, undefined, prefix);
  }
}
