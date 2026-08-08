# HOSTING_PLAN_RENDER.md — Gateway Hosting Strategy (Render.com)

> **Milestone:** ARCHITECTURE-ROADMAP-01  
> **Status:** Specification Only — Render.com Blueprint  
> **Date:** 2026-08-09

---

## Target Platform

**Render** — https://render.com  
Render is a modern cloud PaaS platform supporting Docker web services, managed Redis instances, automatic TLS/HTTPS certificates, and native GitHub repository integrations via Render Blueprints (`render.yaml`).

---

## Why Render?

| Criterion | Render.com | Railway | Fly.io |
|---|---|---|---|
| Node.js / Docker support | ✅ Native | ✅ Native | ✅ Native |
| Free Tier | ✅ Free web service | ~$5 credit | Shared |
| Automatic HTTPS | ✅ Free Let's Encrypt | ✅ Auto | ✅ Auto |
| Blueprint Deploy (`render.yaml`) | ✅ Infrastructure as Code | `railway.toml` | `fly.toml` |
| Managed Redis | ✅ Native Plugin | ✅ Plugin | Upstash |
| Custom Domains | ✅ Supported | ✅ Supported | ✅ Supported |

---

## Architecture Overview

```
                    ┌──────────────────────────────────┐
                    │         Render Cloud              │
                    │                                  │
                    │  ┌────────────────────────────┐  │
                    │  │   Gateway Service (Node.js) │  │
                    │  │   Fastify + youtubei.js     │  │
                    │  │   Port: 8080 (internal)     │  │
                    │  │   Public: HTTPS (.onrender.com)│
                    │  └────────────────┬───────────┘  │
                    │                  │               │
                    │  ┌───────────────▼───────────┐   │
                    │  │     Render Redis Service   │   │
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

## Render Deployment Checklist

### Pre-Deployment
- [x] `gateway/Dockerfile` created
- [x] `gateway/render.yaml` blueprint configuration created
- [x] Secrets stored in Render environment variables
- [x] Health check endpoint `/health` operational

### Deployment
- [ ] Connect GitHub repository to Render dashboard (https://dashboard.render.com).
- [ ] Select **New Blueprint Instance** and point to `gateway/render.yaml`.
- [ ] Render provisions the Web Service and assigns a public URL (e.g. `https://clibeats-gateway.onrender.com`).
- [ ] (Optional) Add Render Redis service if distributed caching is desired.

### Post-Deployment Verification
- [ ] `GET https://clibeats-gateway.onrender.com/health` returns `200 HEALTHY`
- [ ] `GET https://clibeats-gateway.onrender.com/version` returns `1.0.0`
- [ ] `GET https://clibeats-gateway.onrender.com/api/v1/search?q=Wonderwall` returns tracks
- [ ] `POST https://clibeats-gateway.onrender.com/api/v1/stream` returns proxy stream URL
- [ ] Android app compiled with `-PGATEWAY_URL=https://clibeats-gateway.onrender.com/` plays audio cleanly

---

## Environment Variables Configuration

| Variable | Required | Production Value |
|---|---|---|
| `NODE_ENV` | YES | `production` |
| `PORT` | YES | `8080` |
| `PROXY_STREAMING` | YES | `true` |
| `REDIS_URL` | OPTIONAL | `redis://...` (Render Redis Internal URL) |
| `LOG_LEVEL` | NO | `info` |

---

## Migration Command (Android Release APK)

```powershell
./gradlew.bat assembleRelease -PGATEWAY_URL=https://clibeats-gateway.onrender.com/
```
