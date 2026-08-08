import { Innertube, ClientType } from 'youtubei.js';
import { ProviderAdapter, AdapterHealth } from '../../types/adapter';
import { ProviderCapabilities } from '../../types/capabilities';
import { ProviderContext } from '../../types/context';
import { Album, Artist, Playlist, StreamResult, Track } from '../../types/domain';
import {
  NotFoundError,
  NetworkError,
  PlaybackError,
  TimeoutError,
  RateLimitedError,
  GeoBlockedError,
  UnsupportedError,
  ProviderError,
} from '../../types/error';
import { YOUTUBE_PROVIDER_ID, parseRawItem, parseSubtitle } from './media';

export interface YouTubeProviderOptions {
  priority?: number;
  clientType?: ClientType;
  streamingClientType?: ClientType;
  timeoutMs?: number;
  language?: string;
  country?: string;
}

const OPERATION_TIMEOUT_MS = 30_000;

export class YouTubeProviderAdapter implements ProviderAdapter {
  public readonly id: string = YOUTUBE_PROVIDER_ID;
  public readonly name: string = 'YouTube Music';
  public readonly capabilities: ProviderCapabilities = {
    search: true,
    playback: true,
    playlists: true,
    albums: true,
    artists: true,
    recommendations: false,
    radio: false,
    downloads: false,
    lyrics: false,
  };

  private readonly clientType: ClientType;
  private readonly streamingClientType: ClientType;
  private readonly timeoutMs: number;
  private readonly language?: string;
  private readonly country?: string;
  private sessionPromise?: Promise<Innertube>;
  private streamingSessionPromise?: Promise<Innertube>;

  constructor(
    public readonly priority: number = 60,
    options: YouTubeProviderOptions = {}
  ) {
    this.clientType = options.clientType ?? ClientType.MUSIC;
    this.streamingClientType = options.streamingClientType ?? ClientType.IOS;
    this.timeoutMs = options.timeoutMs ?? OPERATION_TIMEOUT_MS;
    this.language = options.language;
    this.country = options.country;
  }

  private getSession(): Promise<Innertube> {
    if (!this.sessionPromise) {
      this.sessionPromise = Innertube.create({
        client_type: this.clientType,
        lang: this.language,
        country: this.country,
      } as Parameters<typeof Innertube.create>[0]);
    }
    return this.sessionPromise;
  }

  private getStreamingSession(): Promise<Innertube> {
    if (!this.streamingSessionPromise) {
      this.streamingSessionPromise = Innertube.create({
        client_type: this.streamingClientType,
        lang: this.language,
        country: this.country,
      } as Parameters<typeof Innertube.create>[0]);
    }
    return this.streamingSessionPromise;
  }

  private async withTimeout<T>(
    op: (yt: Innertube) => Promise<T>,
    session: 'metadata' | 'streaming' = 'metadata'
  ): Promise<T> {
    const yt = session === 'streaming' ? await this.getStreamingSession() : await this.getSession();
    const timeout = new Promise<never>((_, reject) => {
      const timer = setTimeout(
        () =>
          reject(
            new TimeoutError(`YouTube provider operation timed out after ${this.timeoutMs}ms`, this.id)
          ),
        this.timeoutMs
      );
      timer.unref?.();
    });
    return Promise.race([op(yt), timeout]);
  }

  private errorCode(e: unknown, fallback: 'NOT_FOUND' | 'PLAYBACK_ERROR' = 'PLAYBACK_ERROR'): never {
    if (e instanceof ProviderError) throw e;
    const msg = e instanceof Error ? e.message : String(e);
    const providerId = this.id;
    if (/rate\s*limit|too many|quota/i.test(msg)) throw new RateLimitedError(msg, providerId, 60);
    if (/geo|region|country|unavailable in your area/i.test(msg))
      throw new GeoBlockedError(msg, providerId);
    if (/login|auth|po\s*token|verification/i.test(msg)) throw new UnsupportedError(msg, providerId);
    if (/network|fetch|socket|econn/i.test(msg)) throw new NetworkError(msg, providerId);
    if (fallback === 'NOT_FOUND') throw new NotFoundError(msg, providerId);
    throw new PlaybackError(msg, providerId);
  }

  public async search(
    query: string,
    _context: ProviderContext,
    filterSongs: boolean = true
  ): Promise<Track[]> {
    if (!query.trim()) return [];
    try {
      const res = await this.withTimeout((yt) =>
        yt.music.search(query, filterSongs ? { type: 'song' } : undefined)
      );
      const tracks: Track[] = [];
      const sections = (res.contents ?? []) as any[];

      for (const section of sections) {
        const rawItems = Array.isArray(section?.contents) ? section.contents : [section];
        for (const rawItem of rawItems) {
          const item = parseRawItem(rawItem);
          if (!item.id || item.itemType === 'artist') continue;
          const isSong = !item.itemType || item.itemType === 'song' || item.itemType.includes('song') || item.id.length === 11;
          if (filterSongs && !isSong) continue;

          const title = item.title ?? item.columns?.[0];
          if (!title) continue;

          const subtitle = item.columns?.[1];
          const { artist, album } = parseSubtitle(subtitle);

          tracks.push({
            id: item.id,
            providerId: this.id,
            title,
            artist: item.artists?.[0]?.name ?? artist ?? 'Unknown Artist',
            album: item.albumName ?? album,
            durationSeconds: item.durationSeconds ?? 0,
            artworkUrl: item.artworkUrl,
          });
          if (tracks.length >= 50) break;
        }
        if (tracks.length >= 50) break;
      }
      return tracks;
    } catch (e) {
      this.errorCode(e);
    }
  }

  public async stream(trackId: string, _context: ProviderContext): Promise<StreamResult> {
    if (!trackId) this.errorCode(new Error('Missing video id'), 'NOT_FOUND');
    try {
      const result = await this.withTimeout(
        async (yt) => {
          const info = await yt.getBasicInfo(trackId);
          const sd = info.streaming_data;
          const audio = (sd?.adaptive_formats ?? []).filter((f) =>
            String(f.mime_type ?? '').startsWith('audio/')
          );
          if (audio.length === 0) {
            throw new NotFoundError(`No audio stream available for ${trackId}`, this.id);
          }
          audio.sort((a, b) => (b.average_bitrate ?? 0) - (a.average_bitrate ?? 0));
          const fmt = audio[0];
          const url = fmt.url;
          if (!url) {
            throw new PlaybackError(
              `Stream URL not resolvable for ${trackId} (decipher/PO token required)`,
              this.id
            );
          }
          const expires = sd?.expires;
          return {
            url,
            mimeType: fmt.mime_type ?? 'audio/mp4',
            bitrateKbps: fmt.average_bitrate ? Math.round(fmt.average_bitrate / 1000) : undefined,
            expiresAtEpochSeconds: expires
              ? Math.floor(new Date(expires).getTime() / 1000)
              : Math.floor(Date.now() / 1000) + 3600,
          };
        },
        'streaming'
      );
      return {
        trackId,
        providerId: this.id,
        streamUrl: result.url,
        mimeType: result.mimeType,
        bitrateKbps: result.bitrateKbps,
        expiresAtEpochSeconds: result.expiresAtEpochSeconds,
      };
    } catch (e) {
      this.errorCode(e);
    }
  }

  public async album(albumId: string, _context: ProviderContext): Promise<Album> {
    try {
      const album = await this.withTimeout((yt) => yt.music.getAlbum(albumId));
      const header = album.header as
        | {
            title?: { text?: string };
            subtitle?: { text?: string };
            thumbnail?: { contents?: Array<{ url?: string }> };
          }
        | undefined;
      const title = header?.title?.text ?? 'Unknown Album';
      const subtitle = header?.subtitle?.text ?? '';
      const { artist } = parseSubtitle(subtitle);
      const yearMatch = subtitle.match(/(\d{4})/);
      const rawItems = (album.contents ?? []) as unknown[];
      const tracks: Track[] = rawItems
        .map((rawItem) => {
          const item = parseRawItem(rawItem);
          if (!item.id || !item.columns?.[0]) return undefined;
          return {
            id: item.id,
            providerId: this.id,
            title: item.columns[0],
            artist: item.artists?.[0]?.name ?? artist ?? 'Unknown Artist',
            durationSeconds: item.durationSeconds ?? 0,
          } as Track;
        })
        .filter((t): t is Track => t !== undefined);

      return {
        id: albumId,
        providerId: this.id,
        title,
        artist: artist ?? 'Unknown Artist',
        artworkUrl: header?.thumbnail?.contents?.find(
          (t) => typeof t.url === 'string' && /w\d+-h\d+/.test(t.url)
        )?.url,
        trackCount: tracks.length,
        releaseYear: yearMatch ? parseInt(yearMatch[1], 10) : undefined,
        tracks,
      };
    } catch (e) {
      this.errorCode(e, 'NOT_FOUND');
    }
  }

  public async artist(artistId: string, _context: ProviderContext): Promise<Artist> {
    try {
      const artist = await this.withTimeout((yt) => yt.music.getArtist(artistId));
      const header = artist.header as
        | { title?: { text?: string }; thumbnail?: { contents?: Array<{ url?: string }> } }
        | undefined;
      const name = header?.title?.text ?? 'Unknown Artist';
      const avatarUrl = header?.thumbnail?.contents?.find(
        (t) => typeof t.url === 'string' && /s\d+-/.test(t.url)
      )?.url;
      return {
        id: artistId,
        providerId: this.id,
        name,
        avatarUrl,
      };
    } catch (e) {
      this.errorCode(e, 'NOT_FOUND');
    }
  }

  public async playlist(playlistId: string, _context: ProviderContext): Promise<Playlist> {
    try {
      const playlist = await this.withTimeout((yt) => yt.music.getPlaylist(playlistId));
      const header = playlist.header as
        | { title?: { text?: string }; subtitle?: { text?: string } }
        | undefined;
      const title = header?.title?.text ?? 'Unknown Playlist';
      const rawItems = (playlist.contents ?? []) as unknown[];
      const tracks: Track[] = rawItems
        .map((rawItem) => {
          const item = parseRawItem(rawItem);
          if (!item.id || !item.columns?.[0]) return undefined;
          return {
            id: item.id,
            providerId: this.id,
            title: item.columns[0],
            artist: item.artists?.[0]?.name ?? 'Unknown Artist',
            album: item.albumName,
            durationSeconds: item.durationSeconds ?? 0,
          } as Track;
        })
        .filter((t): t is Track => t !== undefined);

      return {
        id: playlistId,
        providerId: this.id,
        title,
        trackCount: tracks.length,
        tracks,
      };
    } catch (e) {
      this.errorCode(e, 'NOT_FOUND');
    }
  }

  public async healthCheck(): Promise<AdapterHealth> {
    const start = Date.now();
    try {
      await this.withTimeout((yt) => yt.music.search('a'));
      const latencyMs = Date.now() - start;
      return { status: 'HEALTHY', score: 100, latencyMs };
    } catch (e) {
      const latencyMs = Date.now() - start;
      const msg = e instanceof Error ? e.message : String(e);
      if (msg.includes('rate limit')) {
        return { status: 'DEGRADED', score: 40, latencyMs, message: msg };
      }
      return { status: 'UNHEALTHY', score: 0, latencyMs, message: msg };
    }
  }
}