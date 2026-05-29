# Shorts Blocker Kids Tech Debt Report

Date: 2026-05-28
Status: Partial

## Closed In This Pass

- Detector copy-paste reduced: shared action-rail geometry, fullscreen/feed size
  thresholds, weighted scoring, result construction, and localized text signals
  now live in `ShortVideoDetectionHeuristics.kt` and
  `ShortVideoTextSignals.kt`.
- Platform detector files are below the production target file size after the
  refactor: YouTube 124 lines, TikTok 148 lines, Instagram 120 lines,
  Facebook 133 lines.
- Executable Android scripts and the root `installFakeSocialApps` Gradle task
  no longer fall back to a machine-local Android SDK path. They require
  `ANDROID_HOME` or `ANDROID_SDK_ROOT`.
- Current README and `.agent/project` Android commands no longer hardcode a
  local SDK path.
- Release app builds now enable R8 minification and resource shrinking.
- The release `DetectorPlaygroundScreen` stub no longer calls navigation as a
  composition side effect.
- Backend request logs now include sanitized exception type metadata
  (`error_type`) without logging exception messages or request bodies.

## Not Safely Closed

- Historical audit/evidence docs still contain local paths such as
  `/home/serputko/Android/Sdk` and `/mnt/c/...`.
  Reason: those files record dated local verification evidence; mass-editing
  them would rewrite audit history instead of fixing current executable config.
- Historical docs still mention `isMinifyEnabled=false`.
  Reason: the current build config and README were updated; old release
  evidence should remain as historical evidence unless the release-readiness
  docs are intentionally regenerated.
- Privacy and Play policy docs still contain TODO placeholders for public
  privacy contact and publisher identity.
  Reason: the repository does not contain authoritative public contact or Play
  listing entity data, and inventing those values would be unsafe.
- `keystore.properties` exists locally and `.env.example` exists in the repo.
  Reason: credential-like files were identified only by filename; their
  contents were not opened under the repository security rule for sensitive
  files. `keystore.properties` is ignored by `.gitignore`.
- Large orchestration/UI/billing/backend files remain:
  `MainActivity.kt` 481 lines, `PlayBillingRepository.kt` 463 lines,
  `DashboardScreen.kt` 388 lines, `BillingBackendServer.kt` 346 lines.
  Reason: safely splitting these requires broader navigation, billing, UI, and
  backend regression coverage than this scoped debt pass.
- Debug-only app logs remain in `DebugAccessibilityLogger`.
  Reason: they are gated by `ACCESSIBILITY_DEBUG_TOOLS_ENABLED=false` in
  release and log sanitized summaries rather than raw accessibility trees.
- `StructuredLogger` uses `println` for JSON backend logs.
  Reason: this is the backend's structured stdout logger, not ad-hoc debug
  printing.
