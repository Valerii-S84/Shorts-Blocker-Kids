# Shorts Blocker Kids Tech Debt Report

Date: 2026-05-28
Last updated: 2026-05-29
Status: Done

All remaining findings from this report are either closed or explicitly
accepted with a bounded reason.

## Closed

- Detector copy-paste reduced: shared action-rail geometry, fullscreen/feed size
  thresholds, weighted scoring, result construction, and localized text signals
  live in `ShortVideoDetectionHeuristics.kt` and
  `ShortVideoTextSignals.kt`.
- Platform detector files are below the production target file size after the
  refactor: YouTube 124 lines, TikTok 148 lines, Instagram 120 lines,
  Facebook 133 lines.
- Executable Android scripts and the root `installFakeSocialApps` Gradle task
  require `ANDROID_HOME` or `ANDROID_SDK_ROOT` instead of falling back to a
  machine-local Android SDK path.
- Current README and `.agent/project` Android commands no longer hardcode a
  local SDK path.
- Release app builds enable R8 minification and resource shrinking in
  `app/build.gradle.kts`.
- The release `DetectorPlaygroundScreen` stub no longer calls navigation as a
  composition side effect.
- Backend request logs include sanitized exception type metadata
  (`error_type`) without logging exception messages or request bodies.
- Privacy and Play policy TODO placeholders are closed with the owner-provided
  values:
  - public privacy contact: `svalerii535@gmail.com`;
  - personal developer / publisher name: `Valerii Serputko`.
- `ReleaseCopyInvariantTest` now verifies that the privacy and Play policy docs
  contain the final personal developer contact values and no longer contain the
  previous contact/publisher TODO text.

## Accepted Debt

- Historical audit/evidence docs still contain local paths such as
  `/home/serputko/Android/Sdk` and `/mnt/c/...`.
  Reason: those files record dated local verification evidence. Current
  executable scripts, Gradle tasks, README commands, and `.agent/project`
  commands use `ANDROID_HOME` / `ANDROID_SDK_ROOT` instead of a hardcoded local
  SDK path.
- `docs/SHORTS_BLOCKER_AAB_RELEASE_READINESS.md` still records
  `isMinifyEnabled=false`.
  Reason: that document explicitly records the May 23-24, 2026 AAB release
  readiness evidence. Current release config is `isMinifyEnabled=true` and
  `isShrinkResources=true` in `app/build.gradle.kts`.
- Credential-like filenames remain: `.env.example` is tracked and
  `keystore.properties` exists locally.
  Reason: this pass checked filenames only and did not open, print, diff, or
  copy sensitive-file contents. `keystore.properties` is ignored by
  `.gitignore`; `.env.example` remains a tracked template-like file.
- `MainActivity.kt` remains 481 lines.
  Reason: it exceeds the target size but is below the soft 500-line limit.
  Splitting it would move Compose navigation state and Activity lifecycle
  wiring that do not currently have focused JVM/UI regression coverage.
- `PlayBillingRepository.kt` remains 463 lines.
  Reason: it exceeds the target size but is below the soft 500-line limit.
  Splitting it safely would require a broader BillingClient seam and callback
  regression coverage than this debt pass can add without changing behavior.
- `DashboardScreen.kt` remains 388 lines.
  Reason: it is below the 400-line production target and already has focused
  local composables for rows, billing actions, and error text.
- `BillingBackendServer.kt` remains 359 lines.
  Reason: it is below the 400-line production target and has endpoint tests in
  `BillingBackendServerTest`; splitting it is not required for this pass.
- Debug-only app logs remain in `DebugAccessibilityLogger`.
  Reason: they are gated by `ACCESSIBILITY_DEBUG_TOOLS_ENABLED=false` in
  release builds and log detection reasons, matched signal IDs, and sanitized
  snapshot summaries rather than raw Accessibility trees or private UI text.
- `StructuredLogger` uses `println` for JSON backend logs.
  Reason: this is the backend's structured stdout logger; current request logs
  avoid request bodies, purchase tokens, exception messages, and raw secrets.

## Blocked

- None.
