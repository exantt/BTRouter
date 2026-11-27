# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-11-27

First public-ready repository release unifying `BTRouterClient` (`versionCode` 24) and `BTRouterServer` (`versionCode` 20) at version `1.1.0`.

### Added
- **Repository & Release Automation:**
  - GitHub Actions CI workflow verifying unit tests, Android Lint, debug APKs, and release builds on every push/PR to `main`.
  - GitHub Actions Release workflow building, signing, `apksigner`-verifying, checksum-hashing (`SHA256SUMS.txt`), and publishing `BTRouterClient` + `BTRouterServer` APKs on `v*.*.*` tags.
  - Environment-variable and optional `keystore.properties` release signing configuration for both modules.
  - Bilingual project documentation (`README.md`, `README.ru.md`), `RELEASE.md`, `SECURITY.md`, `CONTRIBUTING.md`, `LICENSE` (MIT), and issue/PR templates.
- **Bilingual Server UI & Automatic Recovery Sync:**
  - Added full English / Russian localization and runtime RU/EN switcher to `BTRouterServer` (UI, dialogs, notifications, and service states) while keeping wire strings compatible with `BTRouterClient`'s `ServerMessageLocalizer`.
  - Automatic `SYNC_DISCONNECT` trigger upon reconnection after a disconnect whenever Server missed SMS or calls while offline, delivering missed calls to the Missed Calls log and SMS to chats without manual action.
  - Added Samsung One UI remote toggle verification with early notification dismissal once state switches and a 8-second timeout alert if a Server routine fails to trigger.
  - Instant status notification restoration on Client when swiped away, plus long-press context menu in chats for copying and deleting individual messages.

### Core Capabilities Included in v1.1.0
- **Encrypted Bluetooth Classic RFCOMM Channel:**
  - Dedicated service UUID, fail-closed single-MAC allowlist on Server with progressive backoff, mutual HMAC-SHA256 challenge-response handshake over a 16-character PSK, directional AES-256-GCM framing with monotonic sequence counters and AAD, `PING`/`PONG` keepalive probes, and `AndroidKeyStore` key wrapping.
- **`BTRouterClient`:**
  - Live Server telemetry bar and persistent status notification (battery, charging, carrier, signal bars, internet status).
  - SQLite-backed SMS chat threads (`WAL` enabled) with pinning (up to 5 chats), archiving/muting, message copy/delete actions, `ClipDescription.EXTRA_IS_SENSITIVE` clipboard tagging, and offline send queue (TTL 30 min).
  - Multilingual OTP extraction (EN/RU/TR) with one-tap notification copy and optional clipboard auto-copy.
  - Real-time incoming call banner/notification with remote decline, missed calls log with ID-tracked unread badge, and remote outgoing dialing.
  - Client & Server address book browser, recent SMS contacts, manual & automatic missed-event sync (`SYNC_5MIN` / `SYNC_DISCONNECT`), Samsung One UI 5+ quick toggles (Mobile Data / Hotspot), and runtime RU/EN UI localization.
- **`BTRouterServer`:**
  - Foreground `connectedDevice|shortService` gateway service with boot auto-start, serialized multipart SMS queue (up to 10 parts) with modem delivery reporting, 24h unacknowledged live-SMS persistence, offline `SMS_SENT` buffering, chunked contacts export with USSD/extension filtering, and real-time SoftAP / mobile data state detection.

### Fixed
- Resolved all Android Lint `MissingPermission` and `PermissionImpliesUnsupportedChromeOsHardware` errors across both modules so `lintDebug` passes cleanly in CI.
- Hardened foreground service startup (`shortService` fallback without BT permission), DB-first `EVENT_ACK` confirmation, OTP regex backtracking (ReDoS prevention), monotonic keepalive tracking, edge-to-edge dark system bars with `displayCutout` insets, international vs local phone deduplication, and multi-SIM / `wlan0` hotspot compatibility.
- Replaced hardcoded version fallback strings in `MainActivity` with `BuildConfig.VERSION_NAME`.
