# CLIBeats 🎵

> **Free, Open-Source, Privacy-First Android Music Client & Provider Gateway**  
> Inspired by Terminal User Interfaces (TUI), featuring a high-density monospaced aesthetic, local-first data ownership, and a provider-agnostic architecture.

[![CI Pipeline](https://github.com/Omprakash-p06/clibeats/actions/workflows/ci.yml/badge.svg)](https://github.com/Omprakash-p06/clibeats/actions/workflows/ci.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-SDK%2034-green.svg)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Railway Deploy](https://img.shields.io/badge/Deploy%20on-Railway-0B0D0E?logo=railway)](https://railway.app)

---

## 📸 Screenshots & Evidence Showcase

| Home & Startup | Search View | Search Results | Track Artwork |
|:---:|:---:|:---:|:---:|
| ![Startup](docs/evidence/final-release/01-startup.png) | ![Search](docs/evidence/final-release/02-search.png) | ![Results](docs/evidence/final-release/03-results.png) | ![Artwork](docs/evidence/final-release/04-artwork.png) |

| Now Playing | Seek Controls | Notification Controls | Background Playback |
|:---:|:---:|:---:|:---:|
| ![Playing](docs/evidence/final-release/06-playing.png) | ![Seek](docs/evidence/final-release/08-seek.png) | ![Notification](docs/evidence/final-release/07-notification.png) | ![Background](docs/evidence/final-release/11-background.png) |

---

## 🌟 Vision & Core Principles

- 🔒 **Privacy-First & Local-First**: No ads, no tracking IDs, no telemetry, no mandatory cloud account.
- 📱 **User Owns Everything**: All library data, history, and playlists stored locally in Room SQLite database.
- ⚙️ **Provider-Agnostic**: Decoupled `MusicProvider` engine using a stateless Node.js Fastify Gateway.
- 🎨 **TUI Aesthetic**: Monospaced JetBrains Mono typography, high-contrast monochrome palette (`#0D0D0D` background, `#1DB954` accent), 0dp flat surfaces.
- ⚡ **Range-Safe Audio Streaming**: Seamless HTTP 206 audio relay preventing YouTube 403 authorization drops.

---

## 🏗 Architecture Overview

CliBeats separates presentation on Android from provider complexity via a dedicated Provider Gateway middleware:

```
┌────────────────────────────────────────────────────────┐
│                 Android Mobile Client                  │
│   Jetpack Compose (TUI) · Room DB · AndroidX Media3    │
└──────────────────────────┬─────────────────────────────┘
                           │
                           │ HTTPS REST / Range-safe Proxy
                           ▼
┌────────────────────────────────────────────────────────┐
│             Provider Gateway (Railway PaaS)            │
│  Fastify · Node.js 22 · youtubei.js (ANDROID_VR)       │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
                 Music Source Providers
```

---

## 🚀 Hosting the Gateway on Railway

The Provider Gateway can be hosted for **free** on [Railway](https://railway.app) with zero cold-start delay:

### 1. One-Click Railway Setup
1. Fork or clone this repository.
2. Log into [Railway](https://railway.app) and click **New Project** ➔ **Deploy from GitHub repo**.
3. Select the `clibeats` repository and set the root directory to `gateway`.
4. Railway will automatically detect `gateway/Dockerfile` and `gateway/railway.toml`.
5. Set Environment Variables in Railway Dashboard:
   - `NODE_ENV`: `production`
   - `PORT`: `8080`
   - `PROXY_STREAMING`: `true`

### 2. Connect Android App to Railway
Once deployed, copy your Railway public URL (e.g., `https://clibeats-gateway.up.railway.app/`) and compile the Android release APK:

```powershell
# Build release APK pointing to your Railway Gateway
./gradlew assembleRelease -PGATEWAY_URL=https://clibeats-gateway.up.railway.app/
```

---

## 💻 Local Development Setup

### Running Gateway Locally
```powershell
cd gateway
npm install
npm run dev
# Gateway runs on http://localhost:8080/
```

### Building Android App Locally
```powershell
# Run Unit Tests
./gradlew testDebugUnitTest

# Compile Release APK (with local Gateway)
./gradlew assembleRelease -PGATEWAY_URL=http://192.168.0.106:8080/
```

---

## 📚 Architectural Documentation

All architectural plans and specs are available in [`docs/architecture/`](docs/architecture/):

- 🗺 **[Master Product Roadmap](docs/architecture/MASTER_ROADMAP.md)**
- 🔌 **[Provider Strategy & Matrix](docs/architecture/PROVIDER_STRATEGY.md)**
- ☁️ **[Railway Hosting Strategy](docs/architecture/HOSTING_PLAN_RAILWAY.md)**
- 📦 **[Portable Library Format Spec (`.clibeats`)](docs/architecture/PORTABLE_LIBRARY_SPEC.md)**
- 🛡 **[Privacy Model & GDPR Assessment](docs/architecture/PRIVACY_MODEL.md)**
- 📊 **[Production Operations & Observability](docs/architecture/PRODUCTION_OPERATIONS.md)**
- 🔧 **[Technical Debt Audit](docs/architecture/TECHNICAL_DEBT.md)**
- 🎯 **[Execution Sequence & Dependency Graph](docs/architecture/EXECUTION_ORDER.md)**
- ⚠️ **[Comprehensive Risk Register](docs/architecture/RISK_REGISTER.md)**
- 🏛 **[Master System Architecture](docs/architecture/FINAL_ARCHITECTURE.md)**

---

## 📄 License

Distributed under the MIT License. See [`LICENSE`](LICENSE) for details.
