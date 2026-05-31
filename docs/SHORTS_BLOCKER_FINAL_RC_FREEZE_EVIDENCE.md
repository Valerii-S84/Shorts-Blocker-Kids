# Shorts Blocker Kids Final RC Freeze Evidence

Status: Partial. May 24 local release freeze evidence was recorded from a
clean `main` checkout. The May 29 backend deploy attempt verified local
production-like Compose mechanics. The May 31 update applies the canonical
`movashield.de` production domain values; live DNS/TLS/backend deployment and
Play Console acceptance remain outside the local repository proof.

Date: May 24, 2026
Updated: May 31, 2026

## Source State

Pre-build working tree state:

```text
pre_build_status=
branch=main
commit=032b85d15d6480f037f1d6d61957d93639090030
```

No product code, build configuration, manifest, dependency, or feature behavior
was changed for this freeze pass.

May 29 working tree after backend deploy-readiness pass:

```text
branch=main
commit=02901425da18423bd62fe01cc10196adac0d0382
modified=docs/SHORTS_BLOCKER_FINAL_RC_FREEZE_EVIDENCE.md
modified=scripts/backend_backup.sh
modified=scripts/backend_restore.sh
```

## May 31 Movashield Production Domain Readiness

Canonical production values applied in repository docs and release config:

```text
Public domain: movashield.de
Public website URL: https://movashield.de
Privacy Policy URL: https://movashield.de/privacy
Publisher / developer name: Valerii Serputko
Public contact email: svalerii535@gmail.com
Production billing backend base URL: https://billing.movashield.de
RTDN webhook URL: https://billing.movashield.de/billing/play/rtdn
```

Release config gate:

```text
env -u SBK_BILLING_BACKEND_BASE_URL ... :app:validateProductionReleaseConfig
failed as expected: SBK_BILLING_BACKEND_BASE_URL is required for release builds.

SBK_BILLING_BACKEND_BASE_URL=http://billing.movashield.de ... :app:validateProductionReleaseConfig
failed as expected: SBK_BILLING_BACKEND_BASE_URL must be an https URL with a host for release builds.

SBK_BILLING_BACKEND_BASE_URL=not-a-url ... :app:validateProductionReleaseConfig
failed as expected: SBK_BILLING_BACKEND_BASE_URL must be an https URL with a host for release builds.

SBK_BILLING_BACKEND_BASE_URL=https://billing.movashield.de ... :app:validateProductionReleaseConfig
passed.
```

Production-configured local build:

```bash
SBK_BILLING_BACKEND_BASE_URL=https://billing.movashield.de \
ANDROID_HOME=/home/serputko/Android/Sdk \
ANDROID_SDK_ROOT=/home/serputko/Android/Sdk \
./gradlew :app:testDebugUnitTest :billing-backend:test \
  :app:ktlintCheck :billing-backend:ktlintCheck \
  :app:lintRelease :app:assembleRelease :app:bundleRelease
```

Result:

```text
BUILD SUCCESSFUL in 4m 28s
app unit tests: tests=233 failures=0 errors=0 skipped=0
backend tests: tests=31 failures=0 errors=0 skipped=0
```

Artifacts:

```text
d176570ae344ba8d4b877dc4ac61ed1711e18b7b9e5f818a173fe48d548a2ed4  app/build/outputs/apk/release/app-release.apk
43e1d1f0397f9ecd2296dcddd3893d888541edc019f43a1017e326a12932932b  app/build/outputs/bundle/release/app-release.aab
```

Release `BuildConfig`:

```text
APPLICATION_ID="com.shortsblockerkids"
BUILD_TYPE="release"
VERSION_CODE=1
VERSION_NAME="0.1.0"
BILLING_BACKEND_BASE_URL="https://billing.movashield.de"
```

Source state captured before final task commit:

```text
commit=68d914f91bbc942f9900033840572be349d3973c
git_status=modified working tree for movashield readiness changes
```

Live DNS/TLS/health verification:

```text
dig: not available in this shell
getent hosts movashield.de: 185.181.104.242
getent hosts billing.movashield.de: 46.225.181.45
openssl s_client billing.movashield.de:443: failed, tlsv1 alert internal error
curl https://billing.movashield.de/health: failed, TLS alert internal error
curl https://movashield.de/privacy: failed, could not connect to server
```

Live domain conclusion: DNS resolves locally, but HTTPS/TLS, backend health,
and public Privacy Policy hosting are not verified. This is blocked by
owner/server DNS/TLS/web hosting setup, not by local repository tasks.

## May 29 Backend Production Deploy Readiness Attempt

Real production deploy status:

```text
Status: Incomplete
Reason: production runtime env/secrets were not present in this shell.
Missing/unset at verification time:
SBK_ENV
SBK_PUBLIC_BASE_URL
SBK_BACKEND_PORT
SBK_DATABASE_USER
SBK_DATABASE_PASSWORD
SBK_GOOGLE_APPLICATION_CREDENTIALS_HOST
SBK_RTDN_PUBSUB_AUDIENCE
SBK_RTDN_PUBSUB_SERVICE_ACCOUNT_EMAIL
SBK_BILLING_BACKEND_BASE_URL
```

Backend unit gate:

```bash
./gradlew :billing-backend:test
```

Result:

```text
BUILD SUCCESSFUL in 17s
4 actionable tasks: 4 up-to-date
```

Local isolated Docker Compose smoke:

```text
COMPOSE_PROJECT_NAME=sbk-smoke-20260529084411
SBK_BACKEND_IMAGE=shorts-blocker-kids-billing-backend:smoke-20260529T084411Z
```

Verified locally with non-production dummy env and a temporary non-secret
credentials placeholder:

```text
./scripts/backend_deploy_compose.sh: passed
GET http://127.0.0.1:8080/health: {"status":"ok"}
post-migration backup: postgres-20260529T084434Z.dump
restore pre-restore backup: pre-restore-20260529T084438Z.dump
schema_migrations: 001, 002
./scripts/backend_rollback_compose.sh <current-smoke-image>: passed
post-rollback GET /health: {"status":"ok"}
```

Deploy script fix made during this pass:

```text
scripts/backend_backup.sh and scripts/backend_restore.sh now wait for
Docker Compose PostgreSQL readiness before pg_dump/pg_restore.
```

Not proven by the local smoke:

```text
real HTTPS DNS/TLS edge
real production host deployment
real Google service-account credential parsing/loading
Google Play Developer API purchase verification
Play Console RTDN Pub/Sub authenticated push delivery
license-tester purchase/restore/lifecycle flows
```

Android release gate after stale artifact cleanup:

```bash
ANDROID_HOME=/home/serputko/Android/Sdk \
ANDROID_SDK_ROOT=/home/serputko/Android/Sdk \
./gradlew :app:clean
```

Result:

```text
BUILD SUCCESSFUL in 1m 45s
```

Release AAB build without a real backend URL was intentionally blocked:

```bash
ANDROID_HOME=/home/serputko/Android/Sdk \
ANDROID_SDK_ROOT=/home/serputko/Android/Sdk \
./gradlew :app:bundleRelease
```

Result:

```text
Execution failed for task ':app:validateProductionReleaseConfig'.
SBK_BILLING_BACKEND_BASE_URL is required for release builds.
BUILD FAILED in 1m 17s
```

Current release artifact state after the cleanup and blocked build:

```text
No app/build/outputs APK or AAB artifacts are present.
No release AAB was generated with a placeholder backend URL.
```

## Historical May 24 Release Build Evidence

The May 24 artifacts below are historical local freeze evidence. They are not
valid production-backend release artifacts because they did not embed a
production HTTPS billing backend URL and were removed from `app/build/outputs`
during the May 29 cleanup.

Command:

```bash
ANDROID_HOME=/home/serputko/Android/Sdk \
  ./gradlew clean :app:assembleRelease :app:bundleRelease
```

Result:

```text
BUILD SUCCESSFUL in 2m 54s
62 actionable tasks: 33 executed, 27 from cache, 2 up-to-date
```

Artifacts:

```text
app/build/outputs/apk/release/app-release.apk      9649190 bytes
app/build/outputs/bundle/release/app-release.aab   9148082 bytes
```

SHA-256:

```text
410b4a8ff513b5764b0c4c93412548181bbaac0e9acb824d4da6e1322787e82f  app/build/outputs/apk/release/app-release.apk
6c16ade95c802f5653c37b803477be39db6a83377f4df202d9b333ad5c98534e  app/build/outputs/bundle/release/app-release.aab
```

Release APK signature verification:

```text
Verifies
Verified using v2 scheme: true
Number of signers: 1
```

`jarsigner` was not available in the local shell, so AAB JAR signature
verification was not run separately.

Release metadata:

```text
applicationId=com.shortsblockerkids
variantName=release
versionCode=1
versionName=0.1.0
minSdkVersionForDexing=26
```

Release `BuildConfig`:

```text
DEBUG=false
ACCESSIBILITY_DEBUG_TOOLS_ENABLED=false
BILLING_BACKEND_BASE_URL=""
```

Additional local gate:

```bash
ANDROID_HOME=/home/serputko/Android/Sdk ./gradlew :app:lintRelease
```

Result:

```text
BUILD SUCCESSFUL in 7s
```

## Platform Support Freeze

Enabled/protected apps in `PlatformSupportMatrix`:

| Surface | Package | Release support status |
| --- | --- | --- |
| YouTube Shorts | `com.google.android.youtube` | supported |
| TikTok main | `com.zhiliaoapp.musically` | supported by code; needs real-device QA |
| Instagram Reels | `com.instagram.android` | supported by code; needs real-device QA |
| Facebook Reels | `com.facebook.katana` | supported by code; needs real-device QA |

Explicit unsupported entries in `PlatformSupportMatrix`:

| Surface | Package |
| --- | --- |
| TikTok regional | `com.ss.android.ugc.trill` |
| Facebook Lite | `com.facebook.lite` |

Unsupported alias packages covered by detector false-positive tests:

```text
com.google.android.youtube.tv
com.google.android.apps.youtube.music
com.zhiliaoapp.musically.go
com.instagram.lite
com.instagram.android.direct
com.facebook.orca
```

Targeted support-matrix, unsupported-alias, and billing-backend unit tests:

```bash
ANDROID_HOME=/home/serputko/Android/Sdk \
  ./gradlew :app:testDebugUnitTest \
  --tests com.shortsblockerkids.accessibility.PlatformSupportMatrixTest \
  --tests com.shortsblockerkids.accessibility.DetectorBranchHardeningTest \
  --tests com.shortsblockerkids.core.billing.BillingBackendClientTest
```

Result:

```text
BUILD SUCCESSFUL in 19s
```

## Debug QA And Analytics Audit

Release debug QA behavior:

- `ACCESSIBILITY_DEBUG_TOOLS_ENABLED=false` in release `BuildConfig`.
- `MainActivity` only exposes the debug QA entry point when
  `BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED` is true.
- The release source-set `DetectorPlaygroundScreen` immediately calls `onBack()`.
- `RuntimeProtectionState`, `DebugAccessibilityLogger`, and
  `DetectorDebugSnapshotStore` no-op when debug tools are disabled.
- Debug snapshots, when enabled in debug builds, are sanitized summaries and are
  not part of release behavior.

Dependency audit:

- No Analytics, Firebase Analytics, Crashlytics, Mixpanel, Amplitude, or Segment
  SDK dependency is declared.
- `firebase-encoders` appears only transitively through Play Billing 9.0.0; no
  Firebase Analytics SDK is present.

Policy/documentation alignment:

- The in-app disclosure and privacy draft state that the app does not collect
  child history, video titles, URLs, comments, messages, screen recordings,
  audio, location, contacts, browsing history, or raw Accessibility tree dumps.

## Accessibility Disclosure Audit

Source-level gate:

- `AccessibilityPermissionFlow.destinationAfterPinCreated()` routes to
  `Disclosure`.
- `AccessibilityPermissionFlow.settingsRequest(false)` returns `ShowDisclosure`.
- `settingsRequest(true)` is required before opening Android Accessibility
  settings.
- Dashboard access to Accessibility settings is routed through the same consent
  check.

Targeted tests:

```bash
ANDROID_HOME=/home/serputko/Android/Sdk \
  ./gradlew :app:testDebugUnitTest \
  --tests com.shortsblockerkids.feature.onboarding.AccessibilityPermissionFlowTest \
  --tests com.shortsblockerkids.accessibility.ReleaseCopyInvariantTest
```

Result:

```text
BUILD SUCCESSFUL in 34s
```

Manual real-device visual confirmation was not run in this freeze pass.

## Release Manifest And Play Requirements Audit

Release SDK and version:

```text
minSdkVersion=26
targetSdkVersion=36
versionCode=1
versionName=0.1.0
```

Release manifest permissions and sensitive APIs:

```text
uses-permission: com.android.vending.BILLING
uses-permission: com.shortsblockerkids.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
uses-permission: android.permission.ACCESS_NETWORK_STATE
uses-permission: android.permission.INTERNET
service permission: android.permission.BIND_ACCESSIBILITY_SERVICE
```

Release manifest billing metadata:

```text
com.google.android.play.billingclient.version=9.0.0
```

Accessibility service metadata:

```text
accessibilityEventTypes=typeWindowStateChanged|typeWindowContentChanged|typeViewScrolled|typeWindowsChanged
accessibilityFlags=flagReportViewIds|flagRetrieveInteractiveWindows
canRetrieveWindowContent=true
description=@string/accessibility_service_description
settingsActivity=com.shortsblockerkids.MainActivity
```

`android:isAccessibilityTool="true"` is not set. This matches the current
positioning: the app is a parental-control blocker, not a disability support
tool, so it requires prominent disclosure, affirmative consent, Play Console
Accessibility declaration, and Google Play listing explanation.

Play requirement check, using official Google documentation current on
May 24, 2026:

- Target SDK: Google Play requires new mobile apps and updates to target Android
  15/API 35 or higher from August 31, 2025. This release targets API 36.
- Billing Library: the app uses Play Billing Library 9.0.0. Official release
  notes list 9.0.0 as available on May 19, 2026, and the deprecation table still
  allows PBL 7 until August 31, 2026 and PBL 8 until August 31, 2027. PBL 9.0.0
  is therefore not deprecated.
- Billing testing: official guidance recommends license testers, Play Billing
  Lab, and Google Play test tracks for Billing integration validation. Those
  purchase flows were not executed locally in this freeze pass.
- Data Safety: Google requires a Data safety form for published apps including
  closed, open, and production testing tracks; internal-only testing is exempt.
  The project has draft Data Safety/Privacy content, but Play Console submission
  remains pending.
- Accessibility API: Google requires Play listing documentation, Play Console
  declaration/demo material, in-app prominent disclosure, affirmative consent,
  and strict limitation to disclosed purposes for non-accessibility-tool uses.
  The local app flow and docs match this requirement, but Play Console
  submission remains pending.

Official sources checked:

- Target API level requirements:
  <https://support.google.com/googleplay/android-developer/answer/11926878?hl=en>
- Play Billing Library release notes:
  <https://developer.android.com/google/play/billing/release-notes?hl=en>
- Play Billing Library deprecation FAQ:
  <https://developer.android.com/google/play/billing/deprecation-faq?hl=en>
- Play Billing testing:
  <https://developer.android.com/google/play/billing/test?hl=en>
- Data Safety form:
  <https://support.google.com/googleplay/android-developer/answer/10787469?hl=en>
- User Data policy:
  <https://support.google.com/googleplay/android-developer/answer/10144311?hl=en>
- Permissions and APIs that Access Sensitive Information / Accessibility API:
  <https://support.google.com/googleplay/android-developer/answer/16558241?hl=en>

## External Gates Not Completed

- Play Console upload was not performed.
- Play App Signing acceptance was not verified.
- Data Safety form was not submitted in Play Console.
- Accessibility declaration/demo was not submitted in Play Console.
- License tester purchase, pending purchase, cancel, grace, restore, and renewal
  flows were not executed through Google Play.
- Real-device detector QA for TikTok, Instagram, and Facebook remains required
  before claiming production support beyond code-level readiness.
