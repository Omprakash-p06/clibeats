# PRODUCTION_OPERATIONS.md — CliBeats Gateway Operational Architecture

> **Milestone:** ARCHITECTURE-ROADMAP-01  
> **Status:** Specification Only — No Code Changes  
> **Date:** 2026-08-09

---

## Observability Overview

The CliBeats Provider Gateway is designed for **zero-maintenance, high-reliability operation**.  
Observability relies on four pillars:
1. **Structured JSON Logging** (Pino)
2. **Prometheus Metrics Exporter** (`/metrics`)
3. **Machine-Readable Health Probes** (`/health`, `/version`, `/api/v1/bootstrap`)
4. **Circuit Breakers & Provider Health Monitoring**

---

## 1. Structured Logging Architecture

The gateway emits all logs to `stdout` in **JSON Lines format** using Pino.

### Log Format Spec
```json
{
  "level": 30,
  "time": "2026-08-09T00:25:00.123Z",
  "service": "clibeats-gateway",
  "environment": "production",
  "traceId": "trace-9x2f7a11b",
  "method": "POST",
  "url": "/api/v1/stream",
  "statusCode": 200,
  "durationMs": 612,
  "providerId": "youtube",
  "msg": "Stream URL resolved successfully"
}
```

### Log Retention & Log Rotation
- **Development / Local:** Printed to stdout.
- **Production (Railway / Cloud):** Stdout captured by Railway container logging. Retained for 14 days in cloud log drain (e.g. Datadog / Papertrail / Vector).
- **Redaction Rules:** Authorization headers, client IP addresses (optional mask), and private cookie strings are redacted via Pino `redact` paths.

---

## 2. Distributed Tracing (`x-trace-id`)

Every request across the ecosystem carries a unique trace ID.

```
Android App (GatewayApi.kt)
  │  Generates Header: x-trace-id: trace-android-[UUID]
  ▼
Gateway Fastify Server (app.ts)
  │  Extracts x-trace-id or creates trace-gw-[UUID]
  │  Attaches to Fastify request context & Pino child logger
  ▼
Provider Adapter (YouTubeProviderAdapter.ts)
  │  Logs InnerTube execution with trace-id
  ▼
Gateway HTTP Response
     Returns Header: x-trace-id: trace-android-[UUID]
```

---

## 3. Metrics Exporter (`GET /metrics`)

Exposed via Prometheus standard format for Grafana scraping:

| Metric Name | Type | Labels | Description |
|---|---|---|---|
| `http_requests_total` | Counter | `method`, `route`, `status` | Total HTTP requests handled |
| `http_request_duration_seconds` | Histogram | `method`, `route` | Latency distribution |
| `search_latency_ms` | Histogram | `provider_id` | Search resolution duration |
| `stream_resolution_ms` | Histogram | `provider_id`, `client_type` | Stream extraction duration |
| `provider_health_score` | Gauge | `provider_id` | Health score (0–100) |
| `circuit_breaker_state` | Gauge | `provider_id` | 0=Closed, 1=HalfOpen, 2=Open |
| `cache_hits_total` | Counter | `cache_type` | Cache hit count |
| `cache_misses_total` | Counter | `cache_type` | Cache miss count |

---

## 4. Circuit Breakers & Failover Engine

The `ProviderSelectionEngine` manages automated circuit breaking per provider:

```
                  ┌──────────────────────┐
                  │    CLOSED (Normal)   │ ◄───────┐
                  └──────────┬───────────┘         │
                             │ Consecutive         │ Health Check
                             │ Errors > Threshold  │ Passes
                             ▼                     │
                  ┌──────────────────────┐         │
                  │     OPEN (Failing)   │         │
                  └──────────┬───────────┘         │
                             │ Cooldown Period     │
                             │ Expired (60s)       │
                             ▼                     │
                  ┌──────────────────────┐         │
                  │   HALF-OPEN (Test)   ├─────────┘
                  └──────────────────────┘
```

1. **Failure Threshold:** 5 consecutive failed calls within 30s transitions circuit to **OPEN**.
2. **Fallback Activation:** When primary provider is OPEN, `ProviderSelectionEngine` routes calls automatically to the secondary provider.
3. **Cooldown & Recovery:** After 60 seconds, circuit enters **HALF-OPEN** to test a probe request. If successful, state resets to **CLOSED**.

---

## 5. Health Endpoints

- `GET /health` — Returns aggregate health status (HTTP 200 if operational, HTTP 503 if all providers offline).
- `GET /version` — Returns `{ "version": "1.0.0" }`.
- `GET /api/v1/bootstrap` — Returns active capabilities, supported providers, and feature flags.

---

## 6. Alerting Thresholds (Grafana / PagerDuty Rules)

| Alert Trigger | Severity | Action |
|---|---|---|
| Gateway `/health` HTTP 503 for >2m | **CRITICAL** | Page On-Call / Restart Railway Container |
| YouTube Provider Error Rate > 15% | **HIGH** | Check youtubei.js InnerTube version update |
| Redis Connection `DOWN` | **MEDIUM** | Gateway operates degraded (cache-miss mode) |
| p95 Stream Resolution > 3,000ms | **MEDIUM** | Inspect CDN relay bandwidth |
