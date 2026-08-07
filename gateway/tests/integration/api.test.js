"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const vitest_1 = require("vitest");
const app_1 = require("../../src/app");
(0, vitest_1.describe)('Gateway Fastify Integration Tests (fastify.inject)', () => {
    let app;
    (0, vitest_1.beforeEach)(async () => {
        app = (0, app_1.buildApp)();
        await app.ready();
    });
    (0, vitest_1.afterEach)(async () => {
        await app.close();
    });
    (0, vitest_1.it)('GET /api/v1/bootstrap returns aggregated cold-start context (ADR-020)', async () => {
        const res = await app.inject({
            method: 'GET',
            url: '/api/v1/bootstrap',
        });
        (0, vitest_1.expect)(res.statusCode).toBe(200);
        const body = JSON.parse(res.payload);
        (0, vitest_1.expect)(body.gatewayVersion).toBeDefined();
        (0, vitest_1.expect)(body.apiVersion).toBe('1.0.0');
        (0, vitest_1.expect)(body.supportedProviders.length).toBeGreaterThan(0);
        (0, vitest_1.expect)(body.supportedProviders[0].id).toBe('mock');
        (0, vitest_1.expect)(body.features.directToCdnStreaming).toBe(true);
    });
    (0, vitest_1.it)('GET /api/v1/search executes provider search and returns canonical tracks', async () => {
        const res = await app.inject({
            method: 'GET',
            url: '/api/v1/search?q=cyber',
        });
        (0, vitest_1.expect)(res.statusCode).toBe(200);
        const body = JSON.parse(res.payload);
        (0, vitest_1.expect)(body.tracks).toBeDefined();
        (0, vitest_1.expect)(Array.isArray(body.tracks)).toBe(true);
        (0, vitest_1.expect)(body.tracks.length).toBeGreaterThan(0);
    });
    (0, vitest_1.it)('POST /api/v1/stream resolves direct-to-CDN stream URL', async () => {
        const res = await app.inject({
            method: 'POST',
            url: '/api/v1/stream',
            payload: { trackId: 'mock-track-1' },
        });
        (0, vitest_1.expect)(res.statusCode).toBe(200);
        const body = JSON.parse(res.payload);
        (0, vitest_1.expect)(body.stream).toBeDefined();
        (0, vitest_1.expect)(body.stream.streamUrl).toContain('mock-cdn.clibeats.internal');
    });
    (0, vitest_1.it)('GET /health returns machine-readable status breakdown', async () => {
        const res = await app.inject({
            method: 'GET',
            url: '/health',
        });
        (0, vitest_1.expect)(res.statusCode).toBe(200);
        const body = JSON.parse(res.payload);
        (0, vitest_1.expect)(body.gateway).toBe('HEALTHY');
        (0, vitest_1.expect)(body.providers.mock.status).toBe('HEALTHY');
    });
    (0, vitest_1.it)('GET /metrics exports Prometheus metrics', async () => {
        const res = await app.inject({
            method: 'GET',
            url: '/metrics',
        });
        (0, vitest_1.expect)(res.statusCode).toBe(200);
        (0, vitest_1.expect)(res.payload).toContain('gateway_requests_total');
    });
    (0, vitest_1.it)('GET /version returns server version', async () => {
        const res = await app.inject({
            method: 'GET',
            url: '/version',
        });
        (0, vitest_1.expect)(res.statusCode).toBe(200);
        const body = JSON.parse(res.payload);
        (0, vitest_1.expect)(body.version).toBeDefined();
    });
    (0, vitest_1.it)('GET /documentation serves OpenAPI Swagger UI', async () => {
        const res = await app.inject({
            method: 'GET',
            url: '/documentation',
        });
        (0, vitest_1.expect)([200, 302]).toContain(res.statusCode);
    });
});
//# sourceMappingURL=api.test.js.map