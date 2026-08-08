# CliBeats Phase 7 Network Configuration & Security Report

This document records the network configuration environment rules, build-type mappings, and network security policies.

---

## 1. Environment-Aware Gateway Configuration

| Build Type | Environment Target | Default `GATEWAY_BASE_URL` | Override Source | Cleartext Allowed |
| :--- | :--- | :--- | :--- | :--- |
| **Debug (Emulator)** | Android Emulator | `http://10.0.2.2:8080/` | `GATEWAY_URL` env var | Yes (Dev Domains) |
| **Debug (Physical Device)** | Local Wi-Fi LAN | `http://192.168.0.106:8080/` | `GATEWAY_URL` env var | Yes (Dev Domains) |
| **Release** | Production Cloud | `https://gateway.clibeats.io/` | `GATEWAY_URL` env var | **NO (HTTPS Enforced)** |

---

## 2. Network Security Configuration Policy (`@xml/network_security_config`)

- Cleartext HTTP (`cleartextTrafficPermitted="true"`) is strictly scoped to local development domains (`192.168.0.106`, `10.0.2.2`, `127.0.0.1`, `localhost`).
- All production release traffic requires TLS 1.3 / HTTPS.

---

## 3. Dynamic Base URL Resolution

`CLIBeatsApp.kt` logs the resolved base URL on startup:
```kotlin
Log.i("CLIBeatsApp", "Resolved GATEWAY_BASE_URL: ${BuildConfig.GATEWAY_BASE_URL}")
```
