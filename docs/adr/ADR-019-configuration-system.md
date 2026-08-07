# ADR-019: Gateway Configuration & Provider Management System

**Date:** 2026-08-07  
**Status:** Accepted  
**Phase:** Provider Gateway Architecture  

## Context

Hardcoding provider priorities, Redis TTLs, circuit breaker thresholds, or stream validation flags directly in application code prevents zero-downtime configuration updates and forces rebuilds when adjusting operational parameters.

---

## Decision

### 1. External YAML & Environment Configuration

The gateway uses a declarative configuration system (`gateway/config/gateway.yaml` overridden by environment variables):

```yaml
version: "1.0"

server:
  port: 8080
  host: "0.0.0.0"
  corsOrigins: ["*"]

providers:
  youtube:
    enabled: true
    priority: 100
    circuitBreaker:
      failureThreshold: 3
      cooldownSeconds: 60

  piped:
    enabled: true
    priority: 80
    instanceUrl: "https://pipedapi.kavin.rocks"

  jellyfin:
    enabled: false
    priority: 90
    serverUrl: ""

cache:
  redisUrl: "redis://localhost:6379"
  metadataTTLSeconds: 86400    # 24 hours
  searchTTLSeconds: 3600       # 1 hour
  streamTTLSeconds: 900        # 15 minutes

stream:
  validateHeadRequests: true
  urlRefreshBufferSeconds: 300 # 5 minutes before expiry
```

---

### 2. Hot-Reload & Dynamic Provider Toggling

- `ProviderManager` watches for configuration changes or environment overrides.
- Providers can be dynamically enabled/disabled (`providers.youtube.enabled = false`) via configuration without restarting the process.

---

## Consequences

### Positive
- **Operational Control**: Operations teams can adjust provider priorities, enable fallback services, or tweak Redis TTLs without code modification.
- **Environment Flexibility**: Local development, staging, and production share the same configuration schema.

---

## Referenced Documents
- `docs/adr/ADR-013-provider-plugin-architecture.md`
- `docs/adr/ADR-014-provider-capability-negotiation.md`
