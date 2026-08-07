# ADR-020: API Versioning & Aggregated Bootstrap Protocol

**Date:** 2026-08-07  
**Status:** Accepted  
**Phase:** Provider Gateway Architecture  

## Context

Mobile client applications (Android APK releases) remain deployed on user hardware for months or years. Making unversioned REST API changes or requiring multiple sequential HTTP calls during app startup increases cold-start latency and introduces breaking changes for older mobile app releases.

---

## Decision

### 1. Mandatory `/api/v1/` Route Prefixing

All gateway client endpoints MUST be version-prefixed under `/api/v1/`.
Breaking schema updates in the future MUST be released under `/api/v2/` to preserve backward compatibility for existing Android APK releases.

---

### 2. Aggregated `GET /api/v1/bootstrap` Endpoint

To eliminate multiple cold-start HTTP requests during Android app launch, the gateway provides an aggregated `/bootstrap` context endpoint:

```json
{
  "serverVersion": "1.0.0",
  "supportedApiVersion": 1,
  "configuration": {
    "defaultProvider": "youtube",
    "streamRefreshBufferSeconds": 300
  },
  "providers": [
    {
      "id": "youtube",
      "name": "YouTube Music",
      "status": "HEALTHY",
      "score": 98,
      "capabilities": {
        "search": true,
        "playback": true,
        "playlists": true,
        "albums": true,
        "artists": true,
        "recommendations": true,
        "radio": true,
        "downloads": false,
        "lyrics": false
      }
    },
    {
      "id": "jellyfin",
      "name": "Jellyfin Server",
      "status": "DISABLED",
      "score": 0,
      "capabilities": {
        "search": true,
        "playback": true,
        "playlists": true,
        "albums": true,
        "artists": true,
        "recommendations": false,
        "radio": false,
        "downloads": true,
        "lyrics": true
      }
    }
  ]
}
```

The Android application makes a single `GET /api/v1/bootstrap` request during splash/startup to receive full capability matrices, health scores, and provider statuses.

---

## Consequences

### Positive
- **Single Cold-Start Roundtrip**: Mobilizes full initialization state in one network request.
- **Long-Term Client Stability**: `/api/v1/` prefix guarantees legacy APKs are never broken by server enhancements.

---

## Referenced Documents
- `docs/adr/ADR-014-provider-capability-negotiation.md`
- `docs/adr/ADR-017-canonical-domain-models.md`
