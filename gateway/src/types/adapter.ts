import { ProviderCapabilities } from './capabilities';
import { ProviderContext } from './context';
import { Album, Artist, Playlist, StreamResult, Track } from './domain';

export interface AdapterHealth {
  status: 'HEALTHY' | 'DEGRADED' | 'UNHEALTHY';
  score: number; // 0 to 100
  latencyMs: number;
  message?: string;
}

export interface ProviderAdapter {
  readonly id: string;
  readonly name: string;
  readonly priority: number;
  readonly capabilities: ProviderCapabilities;

  search(query: string, context: ProviderContext, filterSongs?: boolean): Promise<Track[]>;
  stream(trackId: string, context: ProviderContext): Promise<StreamResult>;
  album(albumId: string, context: ProviderContext): Promise<Album>;
  artist(artistId: string, context: ProviderContext): Promise<Artist>;
  playlist(playlistId: string, context: ProviderContext): Promise<Playlist>;
  healthCheck(): Promise<AdapterHealth>;
}
