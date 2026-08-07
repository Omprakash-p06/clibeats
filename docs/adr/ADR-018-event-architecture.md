# ADR-018: Event-Driven Gateway Architecture & Tracing

**Date:** 2026-08-07  
**Status:** Accepted  
**Phase:** Provider Gateway Architecture  

## Context

Request pipelines in complex gateways involve multiple asynchronous stages (receiving HTTP request, checking Redis cache, evaluating provider health scores, invoking adapter methods, fallback execution, and writing metric histograms).

Synchronous procedural code makes tracing failures across retries and failovers difficult to audit.

---

## Decision

### 1. Internal Event Lifecycle

The gateway core operates on an internal event-driven architecture using an in-process EventEmitter / EventBus. Every incoming request emits discrete lifecycle events:

```text
SearchRequested / StreamRequested
               │
               ▼
       CacheCheckCompleted (Hit / Miss)
               │
               ▼
       ProviderSelected (Adapter: YouTube, HealthScore: 95)
               │
               ▼
       MetadataResolved / PlaybackResolved
               │
               ▼
       CacheUpdated & MetricEmitted
```

---

### 2. Standardized Gateway Events

```typescript
export type GatewayEvent =
  | { type: 'REQUEST_RECEIVED'; traceId: string; endpoint: string; clientIp: string }
  | { type: 'CACHE_CHECKED'; traceId: string; namespace: string; hit: boolean }
  | { type: 'PROVIDER_SELECTED'; traceId: string; providerId: string; score: number }
  | { type: 'PROVIDER_FAILED'; traceId: string; providerId: string; error: string }
  | { type: 'PROVIDER_FAILOVER'; traceId: string; from: string; to: string }
  | { type: 'STREAM_RESOLVED'; traceId: string; trackId: string; durationMs: number };
```

- Structured loggers and Prometheus metrics collectors subscribe to these internal events.
- **Trace ID Propagation**: `traceId` is assigned at the API layer and attached to every downstream event and log entry.

---

## Consequences

### Positive
- **Auditing & Telemetry**: Every failover, cache hit, and provider delay is captured as a structured event.
- **Decoupled Observability**: Loggers and metrics exporters consume events asynchronously without impacting request latency.

---

## Referenced Documents
- `docs/adr/ADR-014-provider-capability-negotiation.md`
- `docs/adr/ADR-016-canonical-error-model.md`
