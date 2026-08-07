"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const vitest_1 = require("vitest");
const ProviderRegistry_1 = require("../../src/core/registry/ProviderRegistry");
const ProviderSelectionEngine_1 = require("../../src/core/selection/ProviderSelectionEngine");
const CircuitBreaker_1 = require("../../src/core/circuit/CircuitBreaker");
const MockProviderAdapter_1 = require("../../src/providers/mock/MockProviderAdapter");
const error_1 = require("../../src/types/error");
(0, vitest_1.describe)('Gateway Core Unit Tests', () => {
    let registry;
    let engine;
    let mockAdapter;
    const dummyContext = {
        country: 'US',
        language: 'en',
        authenticated: false,
        preferredAudioQuality: 'HIGH',
        device: 'mobile',
        traceId: 'test-trace-123',
    };
    (0, vitest_1.beforeEach)(() => {
        registry = new ProviderRegistry_1.ProviderRegistry();
        engine = new ProviderSelectionEngine_1.ProviderSelectionEngine(registry);
        mockAdapter = new MockProviderAdapter_1.MockProviderAdapter('mock', 42);
        registry.register(mockAdapter);
    });
    (0, vitest_1.it)('ProviderRegistry registers and retrieves sorted adapters', () => {
        (0, vitest_1.expect)(registry.getAll().length).toBe(1);
        (0, vitest_1.expect)(registry.get('mock')).toBe(mockAdapter);
        (0, vitest_1.expect)(registry.getSortedByPriority()[0].id).toBe('mock');
    });
    (0, vitest_1.it)('ProviderSelectionEngine computes health score correctly', async () => {
        const score = await engine.computeScore(mockAdapter, 'search', dummyContext);
        (0, vitest_1.expect)(score).toBeGreaterThan(100);
    });
    (0, vitest_1.it)('ProviderSelectionEngine selects best available provider', async () => {
        const selected = await engine.selectBestProvider('search', dummyContext);
        (0, vitest_1.expect)(selected.id).toBe('mock');
    });
    (0, vitest_1.it)('CircuitBreaker state transitions from CLOSED to OPEN after failure threshold', () => {
        const cb = new CircuitBreaker_1.CircuitBreaker('test-provider', 3, 60);
        (0, vitest_1.expect)(cb.getState()).toBe('CLOSED');
        (0, vitest_1.expect)(cb.isAvailable()).toBe(true);
        cb.recordFailure();
        cb.recordFailure();
        (0, vitest_1.expect)(cb.getState()).toBe('CLOSED');
        cb.recordFailure(); // 3rd failure
        (0, vitest_1.expect)(cb.getState()).toBe('OPEN');
        (0, vitest_1.expect)(cb.isAvailable()).toBe(false);
        cb.recordSuccess();
        (0, vitest_1.expect)(cb.getState()).toBe('CLOSED');
    });
    (0, vitest_1.it)('MockProviderAdapter dataset generation produces 100+ tracks', async () => {
        const searchResults = await mockAdapter.search('', dummyContext);
        (0, vitest_1.expect)(searchResults.length).toBe(20); // First 20 items on empty search
        const allTracks = await mockAdapter.search('track', dummyContext);
        (0, vitest_1.expect)(allTracks.length).toBeGreaterThan(50);
    });
    (0, vitest_1.it)('MockProviderAdapter stream resolution returns valid signed CDN URL', async () => {
        const stream = await mockAdapter.stream('mock-track-1', dummyContext);
        (0, vitest_1.expect)(stream.trackId).toBe('mock-track-1');
        (0, vitest_1.expect)(stream.streamUrl).toContain('mock-cdn.clibeats.internal');
        (0, vitest_1.expect)(stream.mimeType).toBe('audio/mpeg');
    });
    (0, vitest_1.it)('MockProviderAdapter throws NotFoundError for unknown track', async () => {
        await (0, vitest_1.expect)(mockAdapter.stream('unknown-id', dummyContext)).rejects.toThrow(error_1.NotFoundError);
    });
    (0, vitest_1.it)('MockProviderAdapter simulates RateLimitedError when flag enabled', async () => {
        mockAdapter.shouldSimulateError = true;
        mockAdapter.simulatedErrorCode = 'RATE_LIMITED';
        await (0, vitest_1.expect)(mockAdapter.search('query', dummyContext)).rejects.toThrow(error_1.RateLimitedError);
    });
});
//# sourceMappingURL=core.test.js.map