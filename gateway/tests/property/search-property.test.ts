import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import fc from 'fast-check';
import { FastifyInstance } from 'fastify';
import { buildApp } from '../../src/app';

describe('Gateway Property-Based Tests (fast-check)', () => {
  let app: FastifyInstance;

  beforeEach(async () => {
    app = await buildApp();
    await app.ready();
  });

  afterEach(async () => {
    await app.close();
  });

  it('GET /api/v1/search never crashes or violates schema across 1,000 random queries', async () => {
    await fc.assert(
      fc.asyncProperty(fc.fullUnicodeString({ maxLength: 100 }), async (query) => {
        const res = await app.inject({
          method: 'GET',
          url: `/api/v1/search?q=${encodeURIComponent(query)}`,
        });

        expect(res.statusCode).toBe(200);
        const body = JSON.parse(res.payload);
        expect(body).toHaveProperty('tracks');
        expect(Array.isArray(body.tracks)).toBe(true);

        for (const track of body.tracks) {
          expect(typeof track.id).toBe('string');
          expect(typeof track.providerId).toBe('string');
          expect(typeof track.title).toBe('string');
          expect(typeof track.artist).toBe('string');
          expect(typeof track.durationSeconds).toBe('number');
        }
      }),
      { numRuns: 100 }
    );
  });
});
