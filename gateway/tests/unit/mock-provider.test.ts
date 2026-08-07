import { describe, it, expect } from 'vitest';
import { MockProviderAdapter, MOCK_PROVIDER_STATES } from '../../src/providers/mock/MockProviderAdapter';
import { ProviderContext } from '../../src/types/context';
import {
  NetworkError,
  RateLimitedError,
  AuthenticationFailedError,
  GeoBlockedError,
  InternalError,
  PlaybackError,
} from '../../src/types/error';

const ctx: ProviderContext = {
  country: 'US',
  language: 'en',
  authenticated: false,
  preferredAudioQuality: 'HIGH',
  device: 'mobile',
  traceId: 'test-trace',
};

describe('MockProvider Failure Matrix (Task 4)', () => {
  it('exposes all required failure states', () => {
    expect(MOCK_PROVIDER_STATES).toEqual([
      'HEALTHY',
      'SLOW',
      'OFFLINE',
      'MALFORMED',
      'RATE_LIMITED',
      'AUTHENTICATION_FAILED',
      'GEO_BLOCKED',
      'INTERNAL_ERROR',
    ]);
  });

  it('HEALTHY returns results', async () => {
    const mock = new MockProviderAdapter('mock', 42, 100);
    mock.state = 'HEALTHY';
    const results = await mock.search('cyber', ctx);
    expect(results.length).toBeGreaterThan(0);
    expect(await mock.healthCheck()).toMatchObject({ status: 'HEALTHY', score: 100 });
  });

  it('SLOW delays and reports DEGRADED health', async () => {
    const mock = new MockProviderAdapter('mock', 42, 100);
    mock.state = 'SLOW';
    mock.slowLatencyMs = 20;
    const start = Date.now();
    const results = await mock.search('cyber', ctx);
    expect(Date.now() - start).toBeGreaterThanOrEqual(20);
    expect(results.length).toBeGreaterThan(0);
    expect(await mock.healthCheck()).toMatchObject({ status: 'DEGRADED' });
  });

  it('OFFLINE throws NetworkError (no fall-through to PlaybackError)', async () => {
    const mock = new MockProviderAdapter('mock', 42, 100);
    mock.state = 'OFFLINE';
    await expect(mock.search('cyber', ctx)).rejects.toThrow(NetworkError);
    expect(await mock.healthCheck()).toMatchObject({ status: 'UNHEALTHY', score: 0 });
  });

  it('MALFORMED throws a non-canonical parse error', async () => {
    const mock = new MockProviderAdapter('mock', 42, 100);
    mock.state = 'MALFORMED';
    await expect(mock.stream('mock-track-1', ctx)).rejects.toThrow(SyntaxError);
  });

  it('RATE_LIMITED throws RateLimitedError', async () => {
    const mock = new MockProviderAdapter('mock', 42, 100);
    mock.state = 'RATE_LIMITED';
    await expect(mock.search('cyber', ctx)).rejects.toThrow(RateLimitedError);
  });

  it('AUTHENTICATION_FAILED throws AuthenticationFailedError', async () => {
    const mock = new MockProviderAdapter('mock', 42, 100);
    mock.state = 'AUTHENTICATION_FAILED';
    await expect(mock.search('cyber', ctx)).rejects.toThrow(AuthenticationFailedError);
  });

  it('GEO_BLOCKED throws GeoBlockedError', async () => {
    const mock = new MockProviderAdapter('mock', 42, 100);
    mock.state = 'GEO_BLOCKED';
    await expect(mock.search('cyber', ctx)).rejects.toThrow(GeoBlockedError);
  });

  it('INTERNAL_ERROR throws InternalError', async () => {
    const mock = new MockProviderAdapter('mock', 42, 100);
    mock.state = 'INTERNAL_ERROR';
    await expect(mock.search('cyber', ctx)).rejects.toThrow(InternalError);
  });

  it('legacy PLAYBACK_ERROR shim still throws PlaybackError', async () => {
    const mock = new MockProviderAdapter('mock', 42, 100);
    mock.state = 'PLAYBACK_ERROR' as never;
    await expect(mock.search('cyber', ctx)).rejects.toThrow(PlaybackError);
  });
});