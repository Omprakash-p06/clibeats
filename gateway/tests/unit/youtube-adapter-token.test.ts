import { describe, it, expect, vi, beforeEach } from 'vitest';
import { YouTubeProviderAdapter } from '../../src/providers/youtube/YouTubeProviderAdapter';
import { ProviderTokenService } from '../../src/providers/youtube/ProviderTokenService';
import { ProviderContext } from '../../src/types/context';

const ctx: ProviderContext = {
  country: 'US',
  language: 'en',
  authenticated: false,
  preferredAudioQuality: 'HIGH',
  device: 'mobile',
  traceId: 'token-test',
};

const mockCreate = vi.fn();
const mockGetBasicInfo = vi.fn();

vi.mock('youtubei.js', () => ({
  ClientType: { MUSIC: 'MUSIC', ANDROID_VR: 'ANDROID_VR' },
  Innertube: {
    create: (...args: unknown[]) => mockCreate(...args),
  },
}));

function fakeTokenService(mint?: (log?: (msg: string) => void) => Promise<{ poToken: string; visitorData: string; ttlSeconds: number }>) {
  return new ProviderTokenService(
    mint ??
      (async () => ({ poToken: 'po-token-1', visitorData: 'visitor-1', ttlSeconds: 7200 }))
  );
}

function okResponse(url: string) {
  return {
    playability_status: { status: 'OK', reason: '' },
    streaming_data: {
      expires: new Date(Date.now() + 3600_000).toISOString(),
      adaptive_formats: [
        { itag: 251, mime_type: 'audio/webm; codecs="opus"', average_bitrate: 140000, url },
      ],
    },
  };
}

describe('YouTubeProviderAdapter PO token integration (RECOVERY-10)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockCreate.mockImplementation(async () => ({
      music: { search: vi.fn() },
      getBasicInfo: mockGetBasicInfo,
    }));
  });

  it('passes visitor data + PO token to the streaming session and the player request', async () => {
    mockGetBasicInfo.mockResolvedValue(okResponse('https://cdn.example/251'));
    const adapter = new YouTubeProviderAdapter(60, { tokenService: fakeTokenService() });
    const result = await adapter.stream('video-1', ctx);
    expect(result.streamUrl).toBe('https://cdn.example/251');
    expect(mockCreate).toHaveBeenCalledWith(
      expect.objectContaining({
        client_type: 'ANDROID_VR',
        visitor_data: 'visitor-1',
        po_token: 'po-token-1',
      })
    );
    expect(mockGetBasicInfo).toHaveBeenCalledWith(
      'video-1',
      expect.objectContaining({ po_token: 'po-token-1' })
    );
  });

  it('behaves without a token service (no visitor data / po token attached)', async () => {
    mockGetBasicInfo.mockResolvedValue(okResponse('https://cdn.example/251'));
    const adapter = new YouTubeProviderAdapter(60);
    const result = await adapter.stream('video-1', ctx);
    expect(result.streamUrl).toBe('https://cdn.example/251');
    expect(mockCreate).toHaveBeenCalledWith(
      expect.not.objectContaining({ visitor_data: expect.anything() })
    );
    expect(mockGetBasicInfo).toHaveBeenCalledWith('video-1', undefined);
  });

  it('refreshes the token once and retries when playback returns LOGIN_REQUIRED', async () => {
    const mint = vi
      .fn<() => Promise<{ poToken: string; visitorData: string; ttlSeconds: number }>>()
      .mockResolvedValueOnce({ poToken: 'po-old', visitorData: 'visitor-old', ttlSeconds: 7200 })
      .mockResolvedValueOnce({ poToken: 'po-new', visitorData: 'visitor-new', ttlSeconds: 7200 });
    const tokenService = fakeTokenService(mint);
    mockGetBasicInfo
      .mockResolvedValueOnce({
        playability_status: { status: 'LOGIN_REQUIRED', reason: 'Sign in to confirm you are not a bot' },
        streaming_data: null,
      })
      .mockResolvedValueOnce(okResponse('https://cdn.example/new'));
    const adapter = new YouTubeProviderAdapter(60, { tokenService });
    const result = await adapter.stream('video-1', ctx);
    expect(result.streamUrl).toBe('https://cdn.example/new');
    expect(mint).toHaveBeenCalledTimes(2);
    // Streaming session rebuilt with the refreshed token's visitor data.
    expect(mockCreate).toHaveBeenLastCalledWith(
      expect.objectContaining({ visitor_data: 'visitor-new', po_token: 'po-new' })
    );
  });

  it('does not retry more than once when the fresh token is also rejected', async () => {
    const mint = vi.fn(
      async () => ({ poToken: 'po-x', visitorData: 'visitor-x', ttlSeconds: 7200 })
    );
    const tokenService = fakeTokenService(mint);
    mockGetBasicInfo.mockResolvedValue({
      playability_status: { status: 'LOGIN_REQUIRED', reason: 'bot check' },
      streaming_data: null,
    });
    const adapter = new YouTubeProviderAdapter(60, { tokenService });
    await expect(adapter.stream('video-1', ctx)).rejects.toThrow();
    expect(mint).toHaveBeenCalledTimes(2);
  });

  it('exposes the token service for diagnostics', () => {
    const tokenService = fakeTokenService();
    const adapter = new YouTubeProviderAdapter(60, { tokenService });
    expect(adapter.tokenService).toBe(tokenService);
    const plain = new YouTubeProviderAdapter(60);
    expect(plain.tokenService).toBeUndefined();
  });
});
