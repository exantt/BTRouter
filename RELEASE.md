# Release & Signing Guide / Инструкция по подписи и релизу

## 1. Generate a Release Keystore / Создание релизного keystore

> [!CAUTION]
> Keep your `.jks` keystore and passwords strictly private and backed up securely outside the repository. Losing the keystore prevents signing future updates over installed APKs. Never commit `.jks`, `.keystore`, or `keystore.properties` files!

Run `keytool` (included with JDK 21 / Android Studio JBR) **outside** the repository folder (for example, in the parent directory `..`):

```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkeypair `
  -v `
  -keystore ..\btrouter-release.jks `
  -alias btrouter `
  -keyalg RSA `
  -keysize 4096 `
  -validity 10000
```

Using a single keystore for both `BTRouterClient` and `BTRouterServer` is supported by the release workflow.

---

## 2. Configure GitHub Actions Secrets / Настройка секретов в GitHub

Encode the keystore file to a single-line Base64 string in PowerShell (without putting it into any tracked file):

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("..\btrouter-release.jks")) | Set-Clipboard
```

Open your GitHub repository **Settings → Secrets and variables → Actions → New repository secret** (or use `gh secret set`) and create these 4 secrets:

| Secret Name | Value |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | Single-line Base64 string copied above |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore (`-storepass`) password |
| `ANDROID_KEY_ALIAS` | Key alias (e.g. `btrouter`) |
| `ANDROID_KEY_PASSWORD` | Key (`-keypass`) password |

Example via `gh` CLI:

```powershell
$b64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes("..\btrouter-release.jks"))
$b64 | gh secret set ANDROID_KEYSTORE_BASE64 -R exantt/BTRouter
gh secret set ANDROID_KEYSTORE_PASSWORD -R exantt/BTRouter
gh secret set ANDROID_KEY_ALIAS -R exantt/BTRouter
gh secret set ANDROID_KEY_PASSWORD -R exantt/BTRouter
```

---

## 3. Publish Release `v1.1.0` / Публикация релиза `v1.1.0`

Make sure `versionName` in both [BTRouterClient/app/build.gradle.kts](BTRouterClient/app/build.gradle.kts) and [BTRouterServer/app/build.gradle.kts](BTRouterServer/app/build.gradle.kts) matches the release tag (without `v`), and [CHANGELOG.md](CHANGELOG.md) contains a `## [1.1.0]` section.

Create and push the tag:

```bash
git tag -a v1.1.0 -m "Release v1.1.0"
git push origin v1.1.0
```

GitHub Actions (`.github/workflows/release.yml`) will automatically:
1. Validate version parity (`1.1.0`) across tag and both Gradle configs.
2. Run unit tests (`testDebugUnitTest`) and Android Lint (`lintDebug`) for both apps.
3. Build signed `release` APKs and verify signatures with `apksigner`.
4. Generate `SHA256SUMS.txt`, extract release notes from `CHANGELOG.md`, and publish the GitHub Release with:
   - `BTRouterClient-v1.1.0.apk`
   - `BTRouterServer-v1.1.0.apk`
   - `SHA256SUMS.txt`

---

## 4. Local Signed Release Builds / Локальная подписанная сборка

1. Copy [keystore.properties.example](keystore.properties.example) to `keystore.properties` (already in `.gitignore`):
   ```powershell
   Copy-Item keystore.properties.example keystore.properties
   ```
2. Fill in your real `storeFile`, `storePassword`, `keyAlias`, and `keyPassword`.
3. Build signed APKs:
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
   cd BTRouterClient; .\gradlew.bat assembleRelease --no-daemon; cd ..
   cd BTRouterServer; .\gradlew.bat assembleRelease --no-daemon; cd ..
   ```
   Signed outputs are written to `app/build/outputs/apk/release/app-release.apk` in each module.
