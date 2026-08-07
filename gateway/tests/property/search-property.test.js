"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const vitest_1 = require("vitest");
const fast_check_1 = __importDefault(require("fast-check"));
const app_1 = require("../../src/app");
(0, vitest_1.describe)('Gateway Property-Based Tests (fast-check)', () => {
    let app;
    (0, vitest_1.beforeEach)(async () => {
        app = (0, app_1.buildApp)();
        await app.ready();
    });
    (0, vitest_1.afterEach)(async () => {
        await app.close();
    });
    (0, vitest_1.it)('GET /api/v1/search never crashes or violates schema across 1,000 random queries', async () => {
        await fast_check_1.default.assert(fast_check_1.default.asyncProperty(fast_check_1.default.fullUnicodeString({ maxLength: 100 }), async (query) => {
            const res = await app.inject({
                method: 'GET',
                url: `/api/v1/search?q=${encodeURIComponent(query)}`,
            });
            (0, vitest_1.expect)(res.statusCode).toBe(200);
            const body = JSON.parse(res.payload);
            (0, vitest_1.expect)(body).toHaveProperty('tracks');
            (0, vitest_1.expect)(Array.isArray(body.tracks)).toBe(true);
            for (const track of body.tracks) {
                (0, vitest_1.expect)(typeof track.id).toBe('string');
                (0, vitest_1.expect)(typeof track.providerId).toBe('string');
                (0, vitest_1.expect)(typeof track.title).toBe('string');
                (0, vitest_1.expect)(typeof track.artist).toBe('string');
                (0, vitest_1.expect)(typeof track.durationSeconds).toBe('number');
            }
        }), { numRuns: 100 });
    });
});
//# sourceMappingURL=search-property.test.js.map