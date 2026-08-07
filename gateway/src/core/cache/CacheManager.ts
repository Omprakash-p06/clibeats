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
    this.search = new SearchCache(redis, config.cache.searchTTLSeconds);
    this.albums = new AlbumCache(redis, config.cache.metadataTTLSeconds);
    this.artists = new ArtistCache(redis, config.cache.metadataTTLSeconds);
    this.playlists = new PlaylistCache(redis, config.cache.metadataTTLSeconds);
    this.session = new SessionCache(redis);
    this.artwork = new ArtworkCache(redis, config.cache.artworkTTLSeconds);
    this.health = new HealthCache(redis);
  }
}
