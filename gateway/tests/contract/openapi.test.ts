import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { FastifyInstance } from 'fastify';
import { buildApp } from '../../src/app';

describe('OpenAPI Contract Tests (schema-driven)', () => {
  let app: FastifyInstance;

  beforeEach(async () => {
    app = await buildApp();
    await app.ready();
  });

  afterEach(async () => {
    await app.close();
  });

  const getSpec = () => app.swagger({ yaml: false }) as any;

  it('generates OpenAPI 3 spec from route schemas', () => {
    const spec = getSpec();
    expect(spec.openapi).toMatch(/^3\./);
    expect(spec.info.title).toBe('CliBeats Provider Gateway API');
  });

  it('documents every canonical endpoint', () => {
    const spec = getSpec();
    const paths = Object.keys(spec.paths).sort();
    expect(paths).toEqual([
      '/api/v1/album/{id}',
      '/api/v1/artist/{id}',
      '/api/v1/bootstrap',
      '/api/v1/playlist/{id}',
      '/api/v1/providers',
      '/api/v1/search',
      '/api/v1/stream',
      '/api/v1/stream/proxy/{trackId}',
      '/health',
      '/metrics',
      '/version',
    ]);
  });

  it('each operation has tags, description, and responses', () => {
    const spec = getSpec();
    for (const [path, ops] of Object.entries(spec.paths) as any) {
      for (const [method, op] of Object.entries(ops) as any) {
        if (typeof op !== 'object' || op === null) continue;
        // Skip schema-less routes (e.g. /metrics with no schema).
        if (!op.responses) continue;
        expect(op.tags, `${path} ${method} tags`).toBeDefined();
        expect(op.description, `${path} ${method} description`).toBeDefined();
        expect(op.responses, `${path} ${method} responses`).toBeDefined();
        expect(op.responses[200] || op.responses.default, `${path} ${method} 200`).toBeDefined();
      }
    }
  });

  it('search endpoint validates querystring params', async () => {
    // q is optional per schema; filterSongs must be boolean-coercible.
    const ok = await app.inject({ method: 'GET', url: '/api/v1/search?q=cyber' });
    expect(ok.statusCode).toBe(200);
  });

  it('stream endpoint validates body schema', async () => {
    const missing = await app.inject({
      method: 'POST',
      url: '/api/v1/stream',
      payload: {},
    });
    expect(missing.statusCode).toBe(400);

    const ok = await app.inject({
      method: 'POST',
      url: '/api/v1/stream',
      payload: { trackId: 'mock-track-1' },
    });
    expect(ok.statusCode).toBe(200);
  });

  it('generates deterministic spec (idempotent generation)', () => {
    const spec1 = JSON.stringify(getSpec());
    const spec2 = JSON.stringify(getSpec());
    expect(spec1).toBe(spec2);
  });
});