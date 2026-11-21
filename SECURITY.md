# Security Policy

## Security Architecture

BTRouter is designed as a direct, offline Bluetooth Classic gateway between two personal Android devices:

1. **Custom Service UUID:** RFCOMM communication uses dedicated UUID `7d4e9a2b-6c14-4f83-9b52-3e8a1d7c4f90`.
2. **Fail-Closed MAC Allowlist:** `BTRouterServer` rejects incoming Bluetooth sockets unless the remote device's MAC address matches the explicitly configured Client device, applying exponential/linear connection backoff against repeated unauthorized attempts.
3. **Mutual Authentication:** Client and Server derive a master key (`SHA-256("BTRouter-PSK-v2:" + normalizedSecret)`) from a 16-character pre-shared pairing key (`>= 12` normalized chars) and perform a mutual HMAC-SHA256 challenge-response handshake using 16-byte `SecureRandom` nonces and constant-time digest comparison (`MessageDigest.isEqual`).
4. **Transport Encryption:** Session payloads are encrypted with directional AES-256-GCM keys (`KEY-C2S`, `KEY-S2C`), 128-bit GCM authentication tags, and 12-byte nonces formed from a 4-byte direction identifier plus an 8-byte strictly monotonic sequence counter authenticated as AAD. Reordered, replayed, or modified frames terminate the session.
5. **Local Secret Storage:** Both apps wrap the stored pairing key using `AndroidKeyStore` (`btrouter_psk_wrap_key`, AES-256-GCM). Cloud backup and device transfer of `sharedpref` are disabled (`android:allowBackup="false"`).
6. **Release Hardening:** Release builds enable R8 minification, resource shrinking, and ProGuard stripping of `android.util.Log.v/d/i` calls.

## Scope & Threat Model Considerations

- Classic Bluetooth system pairing is a prerequisite before using BTRouter; users should only pair trusted personal devices.
- SMS bodies, contact lists, and call metadata transmitted over the encrypted link are stored locally on the Client device in app-private storage.
- The pairing key can be rotated at any time from `BTRouterServer`, which immediately drops any active Client session.

## Reporting a Vulnerability

If you discover a security issue in BTRouter, **please do not open a public issue with exploit details**.

Instead:
- Report privately via GitHub Security Advisories (if enabled on the repository) or contact the repository maintainer directly.
- Include steps to reproduce, affected app (`BTRouterClient` / `BTRouterServer` / `BtProtocol`), and Android version.
