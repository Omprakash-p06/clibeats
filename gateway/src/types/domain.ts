export interface Track {
  id: string;
  providerId: string;
  title: string;
  artist: string;
  album?: string;
  durationSeconds: number;
  artworkUrl?: string;
  explicit?: boolean;
}

export interface Album {
  id: string;
  providerId: string;
  title: string;
  artist: string;
  artworkUrl?: string;
  trackCount: number;
  releaseYear?: number;
  tracks: Track[];
}

export interface Artist {
  id: string;
  providerId: string;
  name: string;
  avatarUrl?: string;
  bio?: string;
}

export interface Playlist {
  id: string;
  providerId: string;
  title: string;
  description?: string;
  artworkUrl?: string;
  trackCount: number;
  tracks: Track[];
}

export interface Lyrics {
  trackId: string;
  providerId: string;
  plainText: string;
  syncedLines?: Array<{ timestampMs: number; text: string }>;
}

export interface StreamResult {
  trackId: string;
  providerId: string;
  streamUrl: string;
  mimeType: string;
  bitrateKbps?: number;
  expiresAtEpochSeconds: number;
  headers?: Record<string, string>;
}
