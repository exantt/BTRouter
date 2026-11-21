## Summary
Briefly describe what this PR changes and why.

## Target Modules
- [ ] `BTRouterClient`
- [ ] `BTRouterServer`
- [ ] Documentation / CI

## Checklist
- [ ] `./gradlew testDebugUnitTest lintDebug` passes for all modified modules
- [ ] Release build (`assembleRelease`) succeeds without new ProGuard/R8 warnings
- [ ] No keystore, secrets, `local.properties`, or personal device info included
- [ ] If protocol or version changed, both Client and Server compatibility and `CHANGELOG.md` are updated
