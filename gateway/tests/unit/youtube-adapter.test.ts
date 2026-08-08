import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ClientType } from 'youtubei.js';
import { YouTubeProviderAdapter } from '../../src/providers/youtube/YouTubeProviderAdapter';
import { parseRawItem, parseSubtitle, largestArtworkUrl } from '../../src/providers/youtube/media';
import { ProviderContext } from '../../src/types/context';
import { NotFoundError } from '../../src/types/error';

const ctx: ProviderContext = {
  country: 'US',
  language: 'en',
  authenticated: false,
  preferredAudioQuality: 'HIGH',
  device: 'mobile',
  traceId: 'test-trace',
};

const mockSearch = vi.fn();
const mockGetBasicInfo = vi.fn();
const mockCreate = vi.fn();

vi.mock('youtubei.js', () => ({
  ClientType: { MUSIC: 'MUSIC', IOS: 'IOS' },
  Innertube: {
    create: (...args: unknown[]) => mockCreate(...args),
  },
}));

describe('YouTubeProviderAdapter', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockCreate.mockImplementation(async () => ({
      music: { search: mockSearch },
      getBasicInfo: mockGetBasicInfo,
    }));
  });

  it('has correct identity and capabilities', () => {
    const adapter = new YouTubeProviderAdapter(60);
    expect(adapter.id).toBe('youtube');
    expect(adapter.priority).toBe(60);
    expect(adapter.capabilities.search).toBe(true);
    expect(adapter.capabilities.playback).toBe(true);
    expect(adapter.capabilities.lyrics).toBe(false);
  });

  it('search maps music items to canonical tracks', async () => {
    mockSearch.mockResolvedValue({
      contents: [
        {
          contents: [
            {
              id: 'video-1',
              item_type: 'song',
              flex_columns: [
                { title: { text: 'Never Gonna Give You Up' } },
                { title: { text: 'Song • Rick Astley' } },
                { title: { text: '2B plays' } },
              ],
              artists: [{ name: 'Rick Astley', channelId: 'UC1' }],
              duration: { seconds: 213 },
              thumbnail: {
                contents: [
                  { url: 'https://i.ytimg.com/vi/x/w480-h360-l90-k.jpg' },
                  { url: 'https://i.ytimg.com/vi/x/w120-h120-l90-k.jpg' },
                ],
              },
            },
            { id: 'album-1', item_type: 'album', flex_columns: [{ title: { text: 'Whatever' } }] },
          ],
        },
      ],
    });

    const adapter = new YouTubeProviderAdapter(60);
    const tracks = await adapter.search('never gonna', ctx);
    expect(tracks).toHaveLength(1);
    expect(tracks[0]).toMatchObject({
      id: 'video-1',
      providerId: 'youtube',
      title: 'Never Gonna Give You Up',
      artist: 'Rick Astley',
      durationSeconds: 213,
    });
    expect(tracks[0].artworkUrl).toContain('w480-h360');
  });

  it('search without filterSongs keeps non-song results', async () => {
    mockSearch.mockResolvedValue({
      contents: [
        {
          contents: [
            {
              id: 'album-1',
              item_type: 'album',
              flex_columns: [
                { title: { text: 'Discovery' } },
                { title: { text: 'Album • Daft Punk' } },
              ],
            },
          ],
        },
      ],
    });

    const adapter = new YouTubeProviderAdapter(60);
    const tracks = await adapter.search('discovery', ctx, false);
    expect(tracks).toHaveLength(1);
    expect(tracks[0].title).toBe('Discovery');
  });

  it('stream returns a resolvable StreamResult with audio format', async () => {
    mockGetBasicInfo.mockResolvedValue({
      streaming_data: {
        expires: new Date(Date.now() + 3600_000).toISOString(),
        adaptive_formats: [
          { itag: 251, mime_type: 'audio/webm', average_bitrate: 140000, url: 'https://cdn.example/251' },
          { itag: 140, mime_type: 'audio/mp4', average_bitrate: 130000, url: 'https://cdn.example/140' },
          { itag: 18, mime_type: 'video/mp4', average_bitrate: 1000000, url: 'https://cdn.example/18' },
        ],
      },
    });

    const adapter = new YouTubeProviderAdapter(60);
    const result = await adapter.stream('video-1', ctx);
    expect(result.providerId).toBe('youtube');
    expect(result.streamUrl).toBe('https://cdn.example/251');
    expect(result.mimeType).toContain('audio/');
    expect(result.bitrateKbps).toBe(140);
    expect(result.expiresAtEpochSeconds).toBeGreaterThan(Date.now() / 1000);
  });

  it('stream throws NotFoundError when no audio format exists', async () => {
    mockGetBasicInfo.mockResolvedValue({ streaming_data: { adaptive_formats: [] } });
    const adapter = new YouTubeProviderAdapter(60);
    await expect(adapter.stream('video-1', ctx)).rejects.toThrow(NotFoundError);
  });

  it('search maps rate-limit upstream errors to RateLimitedError', async () => {
    mockSearch.mockRejectedValue(new Error('Too many requests'));
    const adapter = new YouTubeProviderAdapter(60);
    await expect(adapter.search('x', ctx)).rejects.toMatchObject({ code: 'RATE_LIMITED' });
  });

  it('healthCheck reports HEALTHY on success', async () => {
    mockSearch.mockResolvedValue({ contents: [] });
    const adapter = new YouTubeProviderAdapter(60);
    const health = await adapter.healthCheck();
    expect(health.status).toBe('HEALTHY');
    expect(health.score).toBe(100);
  });

  it('healthCheck reports UNHEALTHY on failure', async () => {
    mockSearch.mockRejectedValue(new Error('network error'));
    const adapter = new YouTubeProviderAdapter(60);
    const health = await adapter.healthCheck();
    expect(health.status).toBe('UNHEALTHY');
    expect(health.score).toBe(0);
  });
});

describe('media mappers', () => {
  it('parseRawItem extracts id, columns, artists, and artwork', () => {
    const snap = parseRawItem({
      id: 'v1',
      item_type: 'song',
      flex_columns: [
        { title: { text: 'Title' } },
        { title: { text: 'Song • Artist' } },
      ],
      artists: [{ name: 'Artist', channelId: 'UC' }],
      thumbnail: { contents: [{ url: 'https://x/w480-h360.jpg' }, { url: 'https://x/w120.jpg' }] },
    });
    expect(snap.id).toBe('v1');
    expect(snap.columns?.[0]).toBe('Title');
    expect(snap.artists?.[0]?.name).toBe('Artist');
    expect(snap.artworkUrl).toContain('w480-h360');
  });

  it('parseSubtitle splits "Song • Artist" into artist', () => {
    expect(parseSubtitle('Song • Rick Astley')).toEqual({ artist: 'Rick Astley' });
    expect(parseSubtitle('Album • 2013')).toEqual({ artist: '2013' });
    expect(parseSubtitle(undefined)).toEqual({});
  });

  it('largestArtworkUrl prefers wN-hN sized thumbnails', () => {
    expect(
      largestArtworkUrl(['https://x/w120.jpg', 'https://x/w480-h360.jpg', 'https://x/w960-h960.jpg'])
    ).toContain('w480');
  });
});

describe('YouTubeProviderAdapter client options', () => {
  it('uses MUSIC client by default', async () => {
    mockSearch.mockResolvedValue({ contents: [] });
    const adapter = new YouTubeProviderAdapter(60);
    await adapter.healthCheck();
    expect(mockCreate).toHaveBeenCalledWith(expect.objectContaining({ client_type: 'MUSIC' }));
  });

  it('honours an explicit IOS client', async () => {
    mockSearch.mockResolvedValue({ contents: [] });
    const adapter = new YouTubeProviderAdapter(60, { clientType: ClientType.IOS });
    await adapter.healthCheck();
    expect(mockCreate).toHaveBeenCalledWith(expect.objectContaining({ client_type: 'IOS' }));
  });

  it('uses IOS as the streaming session for playback', async () => {
    mockGetBasicInfo.mockResolvedValue({
      streaming_data: {
        expires: new Date(Date.now() + 3600_000).toISOString(),
        adaptive_formats: [
          { itag: 140, mime_type: 'audio/mp4', average_bitrate: 130000, url: 'https://cdn.example/140' },
        ],
      },
    });
    const adapter = new YouTubeProviderAdapter(60);
    await adapter.stream('video-1', ctx);
    expect(mockCreate).toHaveBeenCalledWith(expect.objectContaining({ client_type: 'IOS' }));
  });
});