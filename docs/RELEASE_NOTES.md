# CLIBeats v1.0 — Release Notes

**Release Date:** 2026-08-07  
**Build Target:** Android 8.0+ (API 26+)  

CLIBeats v1.0 is a terminal-inspired, privacy-first audio streaming and media player app for Android. Built with Jetpack Compose, Clean Architecture, and AndroidX Media3.

## Key Features
- **TUI Monospace Interface**: Terminal UI design system with monospace typography, dense data tables, and high-contrast styling.
- **Multi-Provider Architecture**: Plug-and-play streaming backend supporting local storage and YouTube Music (InnerTube API).
- **Background Playback Engine**: AndroidX Media3 MediaSessionService providing continuous background playback, audio focus handling, and notification controls.
- **Offline Caching & Downloads**: 500 MB LRU disk cache with automatic offline fallback resolution.
- **Settings & Preference Controls**: Adjustable disk cache limits (256MB-2GB), active provider selector, streaming quality toggles, and cache maintenance.
- **Accessibility & Performance**: 60 FPS list scrolling, cold start <2s budget, and WCAG AA TalkBack compliance.
