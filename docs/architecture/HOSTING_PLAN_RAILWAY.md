# HOSTING_PLAN_RAILWAY.md — Gateway Hosting Strategy (Railway)

> **Milestone:** ARCHITECTURE-ROADMAP-01  
> **Status:** Planning Only — No Deployment  
> **Date:** 2026-08-09

---

## Target Platform

**Railway** — https://railway.app  
Railway is a PaaS platform that supports Node.js, Docker, Redis, and GitHub Actions deployment natively. It offers a free starter plan and hobby tier ($5/month credit), with automatic HTTPS, custom domains, and zero-config deployments.

---

## Why Railway?

| Criterion | Railway | Render | Fly.io |
|---|---|---|---|
| Node.js native support | ✅ | ✅ | ✅ |
| Free tier | ✅ ($5 credit/month) | ✅ (750 hrs) | ✅ (shared) |
| Redis included | ✅ native plugin | ✅ managed | ✅ upstash |
| GitHub Actions deploy | ✅ | ✅ | ✅ |
| Custom domain + HTTPS | ✅ auto | ✅ auto | ✅ |
| Sleep on inactivity | NO (always-on) | YES (free tier) | NO |
| Cold-start penalty | None | ~15s on free | None |
| Persistent storage | ✅ volumes | ✅ disks | ✅ volumes |
| Rollback | ✅ 1-click | Manual | `fly deploy --image` |

**Conclusion:** Railway is the recommended platform. No cold-start penalty, Redis is a native plugin, and GitHub Actions integration is first-class.

---

## Architecture Overview

```
                    ┌──────────────────────────────────┐
                    │         Railway Project           │
                    │                                  │
                    │  ┌────────────────────────────┐  │
                    │  │   Gateway Service (Node.js) │  │
                    │  │   Fastify + youtubei.js     │  │
                    │  │   Port: 8080 (internal)     │  │
                    │  │   Public: HTTPS (Railway)   │  │
                    │  └────────────────┬───────────┘  │
                    │                  │               │
                    │  ┌───────────────▼───────────┐   │
                    │  │      Redis Plugin          │   │
                    │  │   Cache TTL: 900s streams  │   │
                    │  │   Cache TTL: 604800s art   │   │
                    │  └───────────────────────────┘   │
                    └──────────────────────────────────┘
                              │
                    ┌─────────▼──────────┐
                    │   Android App      │
                    │   (External Client)│
                    │   HTTPS/HTTP 206   │
                    └────────────────────┘
```

---

## Railway Deployment Checklist

### Pre-Deployment
- [ ] `gateway/Dockerfile` created (see Docker Requirements section)
- [ ] `railway.toml` configuration file created
- [ ] All secrets stored in Railway environment variables (NOT in code)
- [ ] `GATEWAY_URL` in Android app updated to Railway public URL
- [ ] Android release APK rebuilt with Railway URL
- [ ] Health check endpoint `/health` tested locally

### Deployment
- [ ] Railway project created at railway.app
- [ ] GitHub repository connected to Railway project
- [ ] Redis plugin added to Railway project
- [ ] All environment variables set in Railway dashboard
- [ ] Deploy triggered from `main` branch
- [ ] Custom domain configured (optional)
- [ ] HTTPS certificate active (Railway auto-provisions via Let's Encrypt)

### Post-Deployment
- [ ] `GET https://[railway-url]/health` returns `200 HEALTHY`
- [ ] `GET https://[railway-url]/version` returns correct version
- [ ] `GET https://[railway-url]/api/v1/search?q=test` returns tracks
- [ ] `POST https://[railway-url]/api/v1/stream` returns proxy URL
- [ ] `GET https://[railway-url]/api/v1/stream/proxy/:trackId` returns HTTP 206 on Range request
- [ ] Android app connected to Railway URL plays audio end-to-end

---

## Required Environment Variables

| Variable | Description | Required | Example |
|---|---|---|---|
| `NODE_ENV` | Runtime environment | YES | `production` |
| `PORT` | Gateway listen port | YES | `8080` |
| `REDIS_URL` | Railway Redis plugin URL | YES | `redis://default:token@host:port` |
| `GATEWAY_VERSION` | Semantic version string | YES | `1.0.0` |
| `LOG_LEVEL` | Pino log level | NO | `info` |
| `PROXY_STREAMING` | Enable CDN relay | YES | `true` |
| `STREAM_TTL_SECONDS` | Stream URL cache TTL | NO | `900` |
| `ARTWORK_TTL_SECONDS` | Artwork cache TTL | NO | `604800` |
| `CORS_ORIGIN` | Allowed origins | NO | `*` |
| `RATE_LIMIT_MAX` | Requests per minute | NO | `100` |
| `YOUTUBE_COOKIES` | Optional YT cookies (base64) | NO | `[base64 netscape cookies]` |

---

## Dockerfile Requirements

```dockerfile
# gateway/Dockerfile
FROM node:22-alpine AS builder

WORKDIR /app
COPY package*.json ./
RUN npm ci --omit=dev

COPY . .
RUN npm run build   # tsc → dist/

FROM node:22-alpine AS runtime

WORKDIR /app
COPY --from=builder /app/dist ./dist
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/config ./config
COPY package.json ./

# Non-root user for security
RUN addgroup -S clibeats && adduser -S clibeats -G clibeats
USER clibeats

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD wget -qO- http://localhost:8080/health || exit 1

CMD ["node", "dist/server.js"]
```

---

## railway.toml

```toml
[build]
builder = "DOCKERFILE"
dockerfilePath = "gateway/Dockerfile"

[deploy]
startCommand = "node dist/server.js"
healthcheckPath = "/health"
healthcheckTimeout = 100
restartPolicyType = "ON_FAILURE"
restartPolicyMaxRetries = 3

[[deploy.environmentVariables]]
name = "NODE_ENV"
value = "production"
```

---

## Redis Requirements

Railway provides a managed Redis plugin.

| Parameter | Value |
|---|---|
| Redis version | 7.x |
| Persistence | RDB snapshots (Railway default) |
| Max memory | 25MB (hobby) / 512MB (pro) |
| Max memory policy | `allkeys-lru` |
| Connection | TLS via `REDIS_URL` env var |
| TTL Strategy | Stream URLs: 900s, Artwork: 604800s |

Redis is **optional for operation** — the gateway degrades gracefully to cache-miss mode if Redis is unavailable (already implemented with `ioredis-mock` fallback).

---

## Persistent Storage Requirements

The gateway is **stateless** by design. No persistent volumes are required.  
All state is in Redis (cache) or in the Android client (library, playlists).

If YouTube cookies are needed (future), they are stored as an environment variable, not a file.

---

## Health Checks

| Endpoint | Method | Expected Response | Purpose |
|---|---|---|---|
| `/health` | GET | `200 {"gateway":"HEALTHY"}` | Load balancer probe |
| `/version` | GET | `200 {"version":"1.0.0"}` | Deployment version check |
| `/metrics` | GET | `200 [Prometheus text]` | Grafana scraping |

Railway uses `/health` as the default healthcheck path (configured in `railway.toml`).

---

## Monitoring Strategy

### Prometheus Metrics (already implemented)
- `http_request_duration_seconds` — latency histogram by endpoint
- `search_latency_ms` — YouTube search latency
- `stream_resolution_ms` — stream URL resolution latency
- Provider health scores
- Redis cache hit/miss rate

### Grafana Cloud (free tier)
Connect Grafana Cloud to scrape `GET /metrics` on Railway every 60s.

**Key Dashboards:**
1. Gateway Overview (requests/min, error rate, p50/p95 latency)
2. Provider Health (YouTube health score, circuit breaker state)
3. Stream Performance (resolution latency, proxy throughput)
4. Redis Performance (hit rate, memory usage, evictions)

### Alerts
| Alert | Condition | Channel |
|---|---|---|
| Gateway Down | `/health` returns non-200 for 2min | Email / Discord webhook |
| Provider Degraded | YouTube health score < 50 for 5min | Discord webhook |
| High Latency | p95 latency > 5s for 10min | Email |
| Redis Full | Memory > 90% | Email |

---

## Secrets Management

| Secret | Storage Location |
|---|---|
| `REDIS_URL` | Railway Environment Variables (injected at runtime) |
| `YOUTUBE_COOKIES` | Railway Environment Variables (base64 encoded) |
| Android signing keystore | GitHub Actions secrets (`KEYSTORE_BASE64`) |
| Railway API token (for CI deploy) | GitHub Actions secrets (`RAILWAY_TOKEN`) |

**Never commit secrets to git.** Use `.env` locally (already in `.gitignore`).

---

## GitHub Actions Deployment Flow

```yaml
# .github/workflows/deploy-gateway.yml (to be created in Phase 3)
name: Deploy Gateway to Railway

on:
  push:
    branches: [main]
    paths:
      - 'gateway/**'

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: '22' }
      - run: npm ci
        working-directory: gateway
      - run: npm test
        working-directory: gateway

  deploy:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: railwayapp/deploy-action@v1
        with:
          service: gateway
          token: ${{ secrets.RAILWAY_TOKEN }}
```

---

## Scaling Strategy

Railway auto-scales horizontally on the Pro plan.

| Traffic Level | Config |
|---|---|
| < 100 req/min | 1 replica, 512MB RAM, 0.5 vCPU |
| 100–1000 req/min | 2 replicas, 1GB RAM, 1 vCPU |
| > 1000 req/min | Evaluate migration to Fly.io with global edge |

The gateway is **stateless** (all state in Redis), so horizontal scaling works without coordination.

---

## Rollback Plan

1. Railway provides 1-click rollback to any previous deployment in the dashboard.
2. Each GitHub commit produces a Railway deployment; rollback = deploy previous commit hash.
3. For emergency rollback: `railway rollback` via Railway CLI.

---

## Disaster Recovery

| Scenario | Recovery Action | RTO |
|---|---|---|
| Railway outage | Users fall back to previous cached stream URLs in Android | 0s (cached) |
| Redis eviction | Cache miss; gateway re-resolves from YouTube | < 3s |
| YouTube API breakage | youtubei.js auto-updates; gateway redeploys | < 1hr |
| Accidental data deletion | No persistent state to recover — stateless gateway | N/A |
| Compromised secrets | Rotate `RAILWAY_TOKEN` + `YOUTUBE_COOKIES` in Railway dashboard | < 5min |

---

## Domain Configuration

| Domain | Purpose |
|---|---|
| `[project].up.railway.app` | Auto-assigned Railway subdomain (no cost) |
| `gateway.clibeats.app` (recommended) | Custom domain (Phase 3, requires DNS configuration) |

**DNS Setup for custom domain:**
1. Add `CNAME gateway.clibeats.app → [project].up.railway.app` in DNS provider
2. Railway auto-provisions TLS certificate via Let's Encrypt
3. HTTPS enforced by Railway (HTTP redirects to HTTPS)

---

## Rate Limiting

Implement in gateway using `@fastify/rate-limit` (Phase 3):

| Endpoint | Limit | Window |
|---|---|---|
| `GET /api/v1/search` | 60 req | per minute per IP |
| `POST /api/v1/stream` | 30 req | per minute per IP |
| `GET /api/v1/stream/proxy/:id` | 200 req | per minute per IP |
| `GET /health` | Unlimited | — |
| `GET /metrics` | 10 req | per minute |

---

## Cost Estimates

| Plan | Cost | Included |
|---|---|---|
| Hobby ($5/month credit) | ~$0–5/month | 512MB RAM, 1 vCPU, Redis 25MB |
| Pro | $20+/month | 8GB RAM, 8 vCPU, Redis 512MB, custom domains |

**Estimated monthly cost for solo/small community use:** **$0–5/month** (within hobby credit).

---

## Migration Path: Local → Railway

1. **Step 1** (Now): App uses `http://192.168.0.106:8080/` — LAN only
2. **Step 2** (Phase 3): Deploy gateway to Railway, get HTTPS public URL
3. **Step 3** (Phase 3): Rebuild Android release APK with `-PGATEWAY_URL=https://[railway-url]/`
4. **Step 4** (Phase 3): Distribute updated APK via GitHub Releases / F-Droid
5. **Step 5** (Phase 4): Users can also specify their own self-hosted gateway URL in Settings
