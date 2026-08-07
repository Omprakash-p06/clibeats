# ADR-010: Beta Telemetry & Privacy Architecture

**Date:** 2026-08-06
**Status:** Accepted
**Phase:** 10 — Beta Validation & Telemetry

## Context

CLIBeats requires structured telemetry logging and crash reporting (`REQ-ENG-10`) without compromising user privacy or leaking bearer tokens and PII.

## Decision

### 1. Privacy-First Abstraction Layer
- `TelemetryTracker` interface logs structured `AnalyticsEvent` payloads without sensitive fields.
- `CrashReporter` interface wraps error reporting with regex-based redaction of auth tokens (`Bearer ...`).

### 2. Beta Build Readiness
- ProGuard rules and release configurations enabled for beta distribution readiness.

## Consequences

### Positive
- Zero PII / auth token leaks in telemetry and crash logs.
- High observability across playback and search events.
