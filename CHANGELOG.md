# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-11-22

First public-ready repository release unifying `BTRouterClient` (`versionCode` 19) and `BTRouterServer` (`versionCode` 15) at version `1.1.0`.

### Added
- **Repository & Release Automation:**
  - GitHub Actions CI workflow verifying unit tests, Android Lint, debug APKs, and release builds on every push/PR to `main`.
  - GitHub Actions Release workflow building, signing, `apksigner`-verifying, checksum-hashing (`SHA256SUMS.txt`), and publishing `BTRouterClient` + `BTRouterServer` APKs on `v*.*.*` tags.
  - Environment-variable and optional `keystore.properties` release signing configuration for both modules.
  - Bilingual project documentation (`README.md`, `README.ru.md`), `RELEASE.md`, `SECURITY.md`, `CONTRIBUTING.md`, `LICENSE` (MIT), and issue/PR templates.

### Core Capabilities Included in v1.1.0
- **Encrypted Bluetooth Classic RFCOMM Channel:**
  - Dedicated service UUID, fail-closed single-MAC allowlist on Server with progressive backoff, mutual HMAC-SHA256 challenge-response handshake over a 16-character PSK, directional AES-256-GCM framing with monotonic sequence counters and AAD, and `AndroidKeyStore` key wrapping.
- **`BTRouterClient`:**
  - Live Server telemetry bar and persistent status notification (battery, charging, carrier, signal bars, internet status).
  - SQLite-backed SMS chat threads with pinning (up to 5 chats), archiving/muting, message actions, and offline send queue (TTL 30 min).
  - Multilingual OTP extraction (EN/RU/TR) with one-tap notification copy and optional clipboard auto-copy.
  - Real-time incoming call banner/notification with remote decline, missed calls log with unread badge, and remote outgoing dialing.
  - Client & Server address book browser, recent SMS contacts, missed-event sync (`SYNC_5MIN` / `SYNC_DISCONNECT`), Samsung One UI 5+ quick toggles (Mobile Data / Hotspot), and runtime RU/EN UI localization.
- **`BTRouterServer`:**
  - Foreground `connectedDevice` gateway service with boot auto-start, serialized multipart SMS queue with modem delivery reporting, 24h unacknowledged live-SMS buffer, chunked contacts export, and real-time SoftAP / mobile data state detection.

### Fixed
- Resolved all Android Lint `MissingPermission` and `PermissionImpliesUnsupportedChromeOsHardware` errors across both modules so `lintDebug` passes cleanly in CI.
- Replaced hardcoded version fallback strings in `MainActivity` with `BuildConfig.VERSION_NAME`.
