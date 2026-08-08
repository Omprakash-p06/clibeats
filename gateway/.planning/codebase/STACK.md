# Technology Stack

**Analysis Date:** 2026-08-09

## Languages

**Primary:**
- TypeScript 5.7.3 — All source code in `src/` (compiled via `tsc`); target `ES2022`, module `CommonJS`

**Secondary:**
- JavaScript — Test files and scripts in `tests/`, `scripts/` can be run via `ts-node`/Node at runtime (e.g., `tests/load/load-test.ts`, `scripts/generate-openapi.ts`)

## Runtime

**Environment:**
- Node.js 20 (Docker base image `node:20-alpine` in `Dockerfile` lines 1 and 9)

**Package Manager:**
- npm (lockfile `package-lock.json` present at repo root)

## Frameworks

**Core:**
- Fastify 5.2.1 — HTTP/web framework; app built in `src/app.ts:2` via `fastify({ logger: false })`

**Testing:**
- Vitest 3.0.4 — Test runner with `@vitest/coverage-v8` 3.2.7 for coverage (config: `vitest.config.ts`)
- fast-check 3.23.2 — Property-based testing (`tests/property/`)
- autocannon 8.0.0 + `@types/autocannon` 7.12.7 — Load testing (`tests/load/load-test.ts`)
- ioredis-mock 8.9.0 + `@types/ioredis-mock` 8.2.8 — Redis mocking for tests

**Build/Dev:**
- TypeScript 5.7.3 — Compiler (`tsc`); config in `tsconfig.json`
- ts-node 10.9.2 — TypeScript execution for scripts
- ts-node-dev 2.0.0 — Hot-reload dev server (`npm run dev` → `ts-node-dev --respawn --transpile-only src/server.ts`)
- redis-commands 1.7.0 — Redis protocol types used in tests

## Key Dependencies

**Critical:**
- `youtubei.js` 17.2.0 — YouTube Internal API client (Innertube protocol); used in `src/providers/youtube/YouTubeProviderAdapter.ts:1`
- `fastify` 5.2.1 — Core web framework
- `ioredis` 5.4.2 — Redis client for caching (`src/app.ts:6`, `src/core/cache/RedisCacheBase.ts:1`)
- `prom-client` 15.1.3 — Prometheus metrics (`src/core/metrics/metrics.ts:1`)
- `pino` 9.6.0 + `pino-pretty` 13.0.0 — Structured logging (`src/core/logging/logger.ts:1`)

**Infrastructure:**
- `@fastify/cors` 10.0.1 — CORS middleware (registered in `src/app.ts:86`)
- `@fastify/swagger` 9.4.0 + `@fastify/swagger-ui` 5.2.0 — OpenAPI spec generation and Swagger UI (registered in `src/app.ts:87-106`)
- `yaml` 2.7.0 — YAML parsing for `config/gateway.yaml` (`src/config/config.ts:3`)
- `dotenv` 16.4.7 — Environment variable loading
- `bgutils-js` 4.0.3 — Bungie.net API utilities (present in `package.json` but not yet imported in `src/`; reserved for future Destiny 2 integration)

## Configuration

**Environment:**
- Config loaded via `loadConfig()` in `src/config/config.ts:45`
- Supports YAML file (`config/gateway.yaml`) with env var overrides
- `GATEWAY_CONFIG_PATH` env var to override config file path (`src/config/config.ts:47`)
- Fallback defaults applied when config file is absent (lines 54-83)

**Build:**
- `tsconfig.json` — TypeScript compilation config (output to `./dist`, excludes `tests`)
- `Dockerfile` — Multi-stage build: `builder` stage (line 1) compiles with `npm ci` + `tsc`; `runner` stage (line 9) runs production with `npm ci --omit=dev`
- `docker-compose.yml` — Local dev stack: gateway + redis:7-alpine with healthcheck
- `render.yaml` — Render.com deployment config (Docker, free plan, PORT=8080)

## Platform Requirements

**Development:**
- Node.js 20+
- Redis server (for cache and health checks) — can be started via `docker-compose up`
- npm for dependency installation

**Production:**
- Deployment target: Docker container (node:20-alpine) or Render.com (via `render.yaml`)
- External dependencies: Redis instance accessible via `REDIS_URL`
- Exposes port 8080 (`Dockerfile` line 17, `src/config/config.ts:57`)
- Healthcheck: HTTP GET `/health` (Docker `Dockerfile` line 18-19)
- Prometheus metrics: HTTP GET `/metrics`

---

*Stack analysis: 2026-08-09*
