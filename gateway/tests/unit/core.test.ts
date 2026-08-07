import { describe, it, expect, beforeEach } from 'vitest';
import { ProviderRegistry } from '../../src/core/registry/ProviderRegistry';
import { ProviderSelectionEngine } from '../../src/core/selection/ProviderSelectionEngine';
import { CircuitBreaker } from '../../src/core/circuit/CircuitBreaker';
import { MockProviderAdapter } from '../../src/providers/mock/MockProviderAdapter';
import { ProviderContext } from '../../src/types/context';
import { RateLimitedError, NotFoundError } from '../../src/types/error';

describe('Gateway Core Unit Tests', () => {
  let registry: ProviderRegistry;
  let engine: ProviderSelectionEngine;
  let mockAdapter: MockProviderAdapter;
  const dummyContext: ProviderContext = {
    country: 'US',
    language: 'en',
    authenticated: false,
    preferredAudioQuality: 'HIGH',
    device: 'mobile',
    traceId: 'test-trace-123',
  };

  beforeEach(() => {
    registry = new ProviderRegistry();
    engine = new ProviderSelectionEngine(registry);
    mockAdapter = new MockProviderAdapter('mock', 42);
    registry.register(mockAdapter);
  });

  it('ProviderRegistry registers and retrieves sorted adapters', () => {
    expect(registry.getAll().length).toBe(1);
    expect(registry.get('mock')).toBe(mockAdapter);
    expect(registry.getSortedByPriority()[0].id).toBe('mock');
  });

  it('ProviderSelectionEngine computes health score correctly', async () => {
    const score = await engine.computeScore(mockAdapter, 'search', dummyContext);
    expect(score).toBeGreaterThan(100);
  });

  it('ProviderSelectionEngine selects best available provider', async () => {
    const selected = await engine.selectBestProvider('search', dummyContext);
    expect(selected.id).toBe('mock');
  });

  it('CircuitBreaker state transitions from CLOSED to OPEN after failure threshold', () => {
    const cb = new CircuitBreaker('test-provider', 3, 60);
    expect(cb.getState()).toBe('CLOSED');
    expect(cb.isAvailable()).toBe(true);

    cb.recordFailure();
    cb.recordFailure();
    expect(cb.getState()).toBe('CLOSED');

    cb.recordFailure(); // 3rd failure
    expect(cb.getState()).toBe('OPEN');
    expect(cb.isAvailable()).toBe(false);

    cb.recordSuccess();
    expect(cb.getState()).toBe('CLOSED');
  });

  it('MockProviderAdapter dataset generation produces 100+ tracks', async () => {
    const searchResults = await mockAdapter.search('', dummyContext);
    expect(searchResults.length).toBe(20); // First 20 items on empty search

    const allTracks = await mockAdapter.search('track', dummyContext);
    expect(allTracks.length).toBeGreaterThan(50);
  });

  it('MockProviderAdapter stream resolution returns valid signed CDN URL', async () => {
    const stream = await mockAdapter.stream('mock-track-1', dummyContext);
    expect(stream.trackId).toBe('mock-track-1');
    expect(stream.streamUrl).toContain('mock-cdn.clibeats.internal');
    expect(stream.mimeType).toBe('audio/mpeg');
  });

  it('MockProviderAdapter throws NotFoundError for unknown track', async () => {
    await expect(mockAdapter.stream('unknown-id', dummyContext)).rejects.toThrow(NotFoundError);
  });

  it('MockProviderAdapter simulates RateLimitedError when flag enabled', async () => {
    mockAdapter.shouldSimulateError = true;
    mockAdapter.simulatedErrorCode = 'RATE_LIMITED';
    await expect(mockAdapter.search('query', dummyContext)).rejects.toThrow(RateLimitedError);
  });
});
