# ADR-016: Canonical Error Model & Unified Responses

**Date:** 2026-08-07  
**Status:** Accepted  
**Phase:** Provider Gateway Architecture  

## Context

Upstream providers emit vastly different error structures (e.g. YouTube returns `LOGIN_REQUIRED` or `PLAYABILITY_ERROR_CODE_EMBEDDER_IDENTITY_DENIED`; Spotify returns HTTP 429 Retry-After headers; Jellyfin returns 401 Unauthorized HTML).

Surfacing raw provider exceptions to the Android client breaks UI error handling and exposes internal provider implementations.

---

## Decision

### 1. Canonical `ProviderError` Hierarchy

All provider adapters MUST translate upstream exceptions into the canonical gateway error hierarchy:

```text
ProviderError
├── AuthenticationFailed (Invalid token, session expired)
├── RateLimited          (HTTP 429, retry after N seconds)
├── GeoBlocked           (Content unavailable in region)
├── NotFound             (Track / album / artist / playlist missing)
├── Unsupported          (Capability not supported by provider)
├── PlaybackError        (Stream URL expired or unplayable)
├── NetworkError         (Upstream connection failure / timeout)
├── TimeoutError         (Request timeout exceeded)
└── InternalError        (Unexpected gateway error)
```

---

### 2. Standardized Gateway Error Response Schema

The API layer serializes canonical errors into a uniform JSON response structure:

```json
{
  "error": {
    "code": "RATE_LIMITED",
    "message": "Upstream rate limit reached. Retry after 15 seconds.",
    "providerId": "youtube",
    "retryAfterSeconds": 15,
    "traceId": "trace-9a8b7c6d5e"
  }
}
```

The Android application handles standard error codes (`RATE_LIMITED`, `AUTHENTICATION_FAILED`, `NOT_FOUND`) uniformly across all providers without needing provider-specific parsing.

---

## Consequences

### Positive
- **Client Resilience**: The Android app deals strictly with standardized error codes.
- **Provider Abstraction**: Provider-specific stack traces and JSON shapes are isolated inside the adapter layer.

---

## Referenced Documents
- `docs/adr/ADR-013-provider-plugin-architecture.md`
- `docs/adr/ADR-014-provider-capability-negotiation.md`
