import { ProviderAdapter, AdapterHealth } from '../../types/adapter';
import { ProviderCapabilities } from '../../types/capabilities';
import { ProviderContext } from '../../types/context';
import { Album, Artist, Playlist, StreamResult, Track } from '../../types/domain';
import { NotFoundError, RateLimitedError, PlaybackError } from '../../types/error';

// Seedable Pseudo-Random Number Generator (PRNG)
class SeededRandom {
  private seed: number;
  constructor(seed: number = 42) {
    this.seed = seed;
  }
  public next(): number {
    this.seed = (this.seed * 9301 + 49297) % 233280;
    return this.seed / 233280;
  }
  public nextInt(min: number, max: number): number {
    return Math.floor(min + this.next() * (max - min + 1));
  }
}

export class MockProviderAdapter implements ProviderAdapter {
  public readonly capabilities: ProviderCapabilities = {
    search: true,
    playback: true,
    playlists: true,
    albums: true,
    artists: true,
    recommendations: true,
    radio: false,
    downloads: true,
    lyrics: true,
  };

  private tracks: Track[] = [];
  private albums: Album[] = [];
  private artists: Artist[] = [];
  private playlists: Playlist[] = [];

  // Testing flag for failure simulation
  public shouldSimulateError: boolean = false;
  public simulatedErrorCode: 'RATE_LIMITED' | 'PLAYBACK_ERROR' | 'NETWORK_ERROR' = 'PLAYBACK_ERROR';

  constructor(
    public readonly id: string = 'mock',
    seed: number = 42,
    public readonly priority: number = 100
  ) {
    this.name = `Mock Provider (${id})`;
    this.generateDataset(seed);
  }

  public readonly name: string;

  private generateDataset(seed: number): void {
    const rng = new SeededRandom(seed);
    const artistNames = ['CyberPulse', 'Neon Horizon', 'SynthWave Lab', 'Retro Beats', 'Byte Harmonics'];
    const albumTitles = ['Grid Runner', 'Digital Dreams', 'Electric Sunset', 'Terminal Velocity', 'Echo Location'];

    // Generate 5 Artists
    this.artists = artistNames.map((name, idx) => ({
      id: `mock-artist-${idx + 1}`,
      providerId: this.id,
      name,
      avatarUrl: `https://via.placeholder.com/150?text=Artist+${idx + 1}`,
      bio: `Bio for ${name} - AI/Synthwave pioneer.`,
    }));

    // Generate 100 Tracks across 10 Albums
    let trackCounter = 1;
    this.albums = [];

    for (let a = 0; a < 10; a++) {
      const artist = this.artists[a % this.artists.length];
      const albumTitle = `${albumTitles[a % albumTitles.length]} Vol. ${Math.floor(a / 2) + 1}`;
      const albumId = `mock-album-${a + 1}`;
      const albumTracks: Track[] = [];

      for (let t = 0; t < 10; t++) {
        const trackId = `mock-track-${trackCounter}`;
        const track: Track = {
          id: trackId,
          providerId: this.id,
          title: `${albumTitle} Track #${t + 1}`,
          artist: artist.name,
          album: albumTitle,
          durationSeconds: rng.nextInt(120, 360),
          artworkUrl: `https://via.placeholder.com/300?text=Cover+${trackCounter}`,
          explicit: false,
        };
        albumTracks.push(track);
        this.tracks.push(track);
        trackCounter++;
      }

      this.albums.push({
        id: albumId,
        providerId: this.id,
        title: albumTitle,
        artist: artist.name,
        artworkUrl: `https://via.placeholder.com/300?text=Album+${a + 1}`,
        trackCount: albumTracks.length,
        releaseYear: 2020 + (a % 5),
        tracks: albumTracks,
      });
    }

    // Generate 5 Playlists
    this.playlists = Array.from({ length: 5 }).map((_, idx) => ({
      id: `mock-playlist-${idx + 1}`,
      providerId: this.id,
      title: `Curated Cyber Playlist #${idx + 1}`,
      description: `Top synthwave tracks for coding session #${idx + 1}`,
      artworkUrl: `https://via.placeholder.com/300?text=Playlist+${idx + 1}`,
      trackCount: 15,
      tracks: this.tracks.slice(idx * 15, (idx + 1) * 15),
    }));
  }

  public async search(
    query: string,
    context: ProviderContext,
    filterSongs: boolean = true
  ): Promise<Track[]> {
    if (this.shouldSimulateError) this.throwSimulatedError();

    const q = query.toLowerCase().trim();
    if (!q) return this.tracks.slice(0, 20);

    return this.tracks.filter(
      (t) => t.title.toLowerCase().includes(q) || t.artist.toLowerCase().includes(q)
    );
  }

  public async stream(trackId: string, context: ProviderContext): Promise<StreamResult> {
    if (this.shouldSimulateError) this.throwSimulatedError();

    const track = this.tracks.find((t) => t.id === trackId);
    if (!track) {
      throw new NotFoundError(`Track ${trackId} not found in MockProvider`, this.id);
    }

    return {
      trackId: track.id,
      providerId: this.id,
      streamUrl: `https://mock-cdn.clibeats.internal/audio/${track.id}.mp3`,
      mimeType: 'audio/mpeg',
      bitrateKbps: 320,
      expiresAtEpochSeconds: Math.floor(Date.now() / 1000) + 3600,
      headers: {
        Authorization: 'Bearer mock-stream-token',
        'X-Trace-Id': context.traceId,
      },
    };
  }

  public async album(albumId: string, context: ProviderContext): Promise<Album> {
    if (this.shouldSimulateError) this.throwSimulatedError();

    const album = this.albums.find((a) => a.id === albumId);
    if (!album) {
      throw new NotFoundError(`Album ${albumId} not found in MockProvider`, this.id);
    }
    return album;
  }

  public async artist(artistId: string, context: ProviderContext): Promise<Artist> {
    if (this.shouldSimulateError) this.throwSimulatedError();

    const artist = this.artists.find((a) => a.id === artistId);
    if (!artist) {
      throw new NotFoundError(`Artist ${artistId} not found in MockProvider`, this.id);
    }
    return artist;
  }

  public async playlist(playlistId: string, context: ProviderContext): Promise<Playlist> {
    if (this.shouldSimulateError) this.throwSimulatedError();

    const playlist = this.playlists.find((p) => p.id === playlistId);
    if (!playlist) {
      throw new NotFoundError(`Playlist ${playlistId} not found in MockProvider`, this.id);
    }
    return playlist;
  }

  public async healthCheck(): Promise<AdapterHealth> {
    if (this.shouldSimulateError) {
      return {
        status: 'UNHEALTHY',
        score: 0,
        latencyMs: 1500,
        message: 'Simulated health failure',
      };
    }
    return {
      status: 'HEALTHY',
      score: 100,
      latencyMs: 5,
    };
  }

  private throwSimulatedError(): void {
    if (this.simulatedErrorCode === 'RATE_LIMITED') {
      throw new RateLimitedError('MockProvider rate limit exceeded', this.id, 30);
    }
    throw new PlaybackError('MockProvider playback stream failure', this.id);
  }
}
