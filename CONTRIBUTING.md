# Contributing to BTRouter

Thank you for your interest in contributing to BTRouter!

## Development Environment

- **JDK:** 21 (Android Studio JBR or Temurin 21)
- **Android SDK:** `platforms;android-36.1` and `build-tools;36.1.0`
- **Minimum / Target SDK:** `minSdk = 29`, `targetSdk = 36`

Because `BTRouterClient` and `BTRouterServer` are standalone Gradle projects, run Gradle tasks inside each module directory:

```bash
# BTRouterClient
cd BTRouterClient
./gradlew testDebugUnitTest lintDebug assembleDebug

# BTRouterServer
cd ../BTRouterServer
./gradlew testDebugUnitTest lintDebug assembleDebug
```

## Protocol & Compatibility Rules

- `BtProtocol.kt` is shared conceptually between `BTRouterClient` and `BTRouterServer`. Any changes to handshake framing, encryption nonces, or JSON message types (`t`, `id`, `eventId`) must remain compatible across both modules and include unit tests (`BtProtocolTest`).
- Keep `BtProtocol`, `OtpExtractor`, `ChatLogic`, `ContactsHelper`, `HotspotDetector`, and `OutboundSmsQueue` pure-JVM testable with comprehensive JUnit coverage.

## Versioning

- Follow Semantic Versioning (`MAJOR.MINOR.PATCH`).
- Keep `versionName` synchronized between `BTRouterClient/app/build.gradle.kts` and `BTRouterServer/app/build.gradle.kts` when preparing releases, and always increment `versionCode` monotonically.
- Update `CHANGELOG.md` with user-visible changes.

## Pull Request Checklist

1. Run `testDebugUnitTest` and `lintDebug` on both `BTRouterClient` and `BTRouterServer` with zero errors.
2. Verify `assembleRelease` passes with R8 enabled.
3. Never commit `keystore.properties`, `.jks`/`.keystore` files, `local.properties`, or personal Bluetooth MAC addresses/phone numbers.
4. Update `README.md` / `README.ru.md` if user-facing behavior or setup steps changed.
