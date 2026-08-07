"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const vitest_1 = require("vitest");
const app_1 = require("../../src/app");
const MockProviderAdapter_1 = require("../../src/providers/mock/MockProviderAdapter");
(0, vitest_1.describe)('Gateway Failover & Circuit Breaker Integration Tests', () => {
    let app;
    let primaryMock;
    let secondaryMock;
    (0, vitest_1.beforeEach)(async () => {
        app = (0, app_1.buildApp)({ providers: { mock: { enabled: false, priority: 0 } } });
        primaryMock = new MockProviderAdapter_1.MockProviderAdapter('primary-mock', 42, 300);
        secondaryMock = new MockProviderAdapter_1.MockProviderAdapter('secondary-mock', 100, 100);
        app.registry.register(primaryMock);
        app.registry.register(secondaryMock);
        await app.ready();
    });
    (0, vitest_1.afterEach)(async () => {
        await app.close();
    });
    (0, vitest_1.it)('Transparent failover routes request to secondary provider when primary fails', async () => {
        // Enable failure simulation on primary
        primaryMock.shouldSimulateError = true;
        primaryMock.simulatedErrorCode = 'PLAYBACK_ERROR';
        const res = await app.inject({
            method: 'GET',
            url: '/api/v1/search?q=cyber',
        });
        (0, vitest_1.expect)(res.statusCode).toBe(200);
        const body = JSON.parse(res.payload);
        (0, vitest_1.expect)(body.tracks.length).toBeGreaterThan(0);
        // Secondary mock track provider ID
        (0, vitest_1.expect)(body.tracks[0].providerId).toBe('secondary-mock');
    });
    (0, vitest_1.it)('Circuit breaker trips after 3 failures and bypasses primary provider', async () => {
        primaryMock.shouldSimulateError = true;
        // Fail 3 times with unique queries to bypass search cache
        for (let i = 0; i < 3; i++) {
            await app.inject({ method: 'GET', url: `/api/v1/search?q=uncached-query-${i}` });
        }
        const cb = app.engine.getCircuitBreaker('primary-mock');
        (0, vitest_1.expect)(cb.getState()).toBe('OPEN');
        (0, vitest_1.expect)(cb.isAvailable()).toBe(false);
    });
});
//# sourceMappingURL=failover.test.js.map