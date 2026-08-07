# ADR-015: Authentication & Provider Session Management

**Date:** 2026-08-07  
**Status:** Accepted  
**Phase:** Provider Gateway Architecture  

## Context

Different music sources require distinct authentication mechanisms (e.g. YouTube uses unauthenticated visitor sessions / cookies; Spotify requires OAuth2 PKCE flow; Jellyfin requires API keys / user tokens; Navidrome uses Subsonic token salt hashing).

Leaking provider-specific auth flows into the core gateway or Android application creates tight coupling and security risks.

---

## Decision

### 1. Isolated Provider Session Lifecycle

Each `ProviderAdapter` manages its own authentication lifecycle in isolation via a standard session state machine:

$$\text{Anonymous} \longrightarrow \text{Authenticating} \longrightarrow \text{Active Session} \longrightarrow \text{Refreshing} \longrightarrow \text{Expired / Revoked}$$

```text
                    ┌────────────────────────┐
                    │      Anonymous         │
                    └───────────┬────────────┘
                                │ Login / Auth Request
                                ▼
                    ┌────────────────────────┐
                    │     Authenticating     │
                    └───────────┬────────────┘
                                │ Success
                                ▼
┌───────────────┐   Refresh     ┌────────────────────────┐
│   Refreshing  │ ◄──────────── │     Active Session     │
└───────┬───────┘               └───────────┬────────────┘
        │                                   │ Token Expiry / Revocation
        ▼                                   ▼
┌───────────────┐               ┌────────────────────────┐
│ Active Session│               │   Expired / Revoked    │
└───────────────┘               └────────────────────────┘
```

---

### 2. Provider Auth Encapsulation

- **`YouTubeAdapter`**: Manages `visitorData` tokens, cookie jars, and PO Token resolution.
- **`SpotifyAdapter`**: Manages OAuth2 authorization codes, refresh tokens, and automatic token renewal.
- **`JellyfinAdapter`**: Encapsulates `X-Emby-Token` authentication and user session headers.
- **`NavidromeAdapter`**: Manages Subsonic REST salt/token generation (`u`, `t`, `s` params).

The core gateway stores encrypted provider session tokens in the `session:` Redis namespace and never exposes raw provider credentials to the mobile client.

---

## Consequences

### Positive
- **Security & Isolation**: Credential handling is encapsulated per provider.
- **Transparent Renewal**: Adapters renew expired tokens automatically during requests without interrupting client playback.

---

## Referenced Documents
- `docs/adr/ADR-012-clibeats-gateway-provider-architecture.md`
- `docs/adr/ADR-013-provider-plugin-architecture.md`
