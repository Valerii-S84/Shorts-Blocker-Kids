# Shorts Blocker Kids AAB Release Readiness

Status: Partial. Local release pipeline passes, release APK smoke passes, and
bundletool AAB-derived emulator install smoke passes. Play Console upload and
Play App Signing validation remain pending.

## Scope

This report records the local Android App Bundle release readiness check run on
May 23, 2026, plus the release gate refresh run on May 24, 2026.

It covers:

- Kotlin style check;
- debug APK build;
- release APK build;
- release AAB build;
- debug unit tests;
- debug lint;
- release lint;
- release manifest privacy checks;
- release debug-tool flag check.

It does not cover Play Console upload, Play App Signing acceptance, production
review, billing tester flows, or final signed release-candidate device QA from
the Play internal testing track.

## May 29, 2026 Production Backend Gate

Release artifacts now fail closed unless `SBK_BILLING_BACKEND_BASE_URL` is
supplied as the canonical HTTPS backend origin. This is wired through
`validateProductionReleaseConfig`, which blocks `assembleRelease`,
`packageRelease`, and `bundleRelease` when the backend URL is blank,
non-HTTPS, invalid, or not the canonical backend origin.

Canonical production values for the current release line:

```text
Public website URL: https://shortsblockerkids.de
Privacy Policy URL: https://shortsblockerkids.de/privacy
Support URL: https://shortsblockerkids.de/support
Publisher / developer name: Valerii Serputko
Public contact email: svalerii535@gmail.com
Production billing backend base URL: https://billing.shortsblockerkids.de
RTDN webhook URL: https://billing.shortsblockerkids.de/billing/play/rtdn
```

Expected production-configured release command:

```bash
SBK_BILLING_BACKEND_BASE_URL=https://billing.shortsblockerkids.de \
ANDROID_HOME=/home/serputko/Android/Sdk \
ANDROID_SDK_ROOT=/home/serputko/Android/Sdk \
./gradlew :app:lintRelease :app:assembleRelease :app:bundleRelease
```

Release fail-closed behavior:

- blank backend URL: build fails before release artifact generation;
- non-HTTPS backend URL: build fails before release artifact generation;
- invalid backend URL: build fails before release artifact generation;
- non-canonical HTTPS backend URL: build fails before release artifact
  generation;
- configured canonical backend URL: release build proceeds and embeds the URL in
  release `BuildConfig.BILLING_BACKEND_BASE_URL`;
- backend verification or refresh failure in the app records a non-active
  entitlement snapshot instead of retaining a stale active grant.

`android.permission.INTERNET` and `android.permission.ACCESS_NETWORK_STATE`
remain documented release permissions. They are merged from the Google Play
Billing dependency graph and are also required for production backend
verification. Do not remove either permission until a separate dependency and
runtime proof shows removal is safe.

## May 31, 2026 Shorts Blocker Kids Production Domain Evidence

Release gate validation:

```text
blank SBK_BILLING_BACKEND_BASE_URL: failed as expected
http://billing.shortsblockerkids.de: failed as expected
not-a-url: failed as expected
non-canonical HTTPS backend URL: failed as expected
https://billing.shortsblockerkids.de: passed
```

Verification/build command:

```bash
SBK_BILLING_BACKEND_BASE_URL=https://billing.shortsblockerkids.de \
ANDROID_HOME=/home/serputko/Android/Sdk \
ANDROID_SDK_ROOT=/home/serputko/Android/Sdk \
./gradlew :app:testDebugUnitTest :billing-backend:test \
  :app:ktlintCheck :billing-backend:ktlintCheck \
  :app:lintRelease :app:assembleRelease :app:bundleRelease
```

Result:

```text
BUILD SUCCESSFUL in 2m 15s
112 actionable tasks: 35 executed, 77 up-to-date
app unit tests: tests=233 failures=0 errors=0 skipped=0
backend tests: tests=31 failures=0 errors=0 skipped=0
ktlintCheck: passed
lintRelease: passed with existing warnings
assembleRelease: passed
bundleRelease: passed
```

Release artifacts:

```text
app/build/outputs/apk/release/app-release.apk     1,709,314 bytes
app/build/outputs/bundle/release/app-release.aab  3,618,118 bytes
```

SHA-256:

```text
97c7ee78aad74c4d3539e032205c151b14f5f66f3a8557488954843c03768c20  app/build/outputs/apk/release/app-release.apk
5e43fdde71a959add94ed38e732162f54124ff38d3e882aa4225781c30237f44  app/build/outputs/bundle/release/app-release.aab
```

Release `BuildConfig`:

```text
APPLICATION_ID="com.shortsblockerkids"
BUILD_TYPE="release"
VERSION_CODE=1
VERSION_NAME="0.1.0"
BILLING_BACKEND_BASE_URL="https://billing.shortsblockerkids.de"
```

Source state at verification time:

```text
source_commit_before_task=ba9b263cfd7e3124a86b0c9e6125ac621e46ceca
git_status=modified working tree before final commit
final_task_commit=recorded in final report after commit
```

Live domain verification:

```text
dig: not available in this shell
getent hosts shortsblockerkids.de: 185.181.104.242
getent hosts billing.shortsblockerkids.de: 185.181.104.242
openssl s_client billing.shortsblockerkids.de:443: failed, connection refused
curl https://shortsblockerkids.de: failed, could not connect to server
curl https://shortsblockerkids.de/privacy: failed, could not connect to server
curl https://shortsblockerkids.de/support: failed, could not connect to server
curl https://billing.shortsblockerkids.de/health: failed, could not connect to server
```

Conclusion: DNS records are visible locally, but HTTPS/TLS, public root,
Privacy Policy, support page, and backend health are not verified. Live domain
readiness is blocked by owner DNS/hosting/server setup.

## May 24, 2026 Release Gate Refresh

Release gate command:

```bash
sg kvm -c 'set -e; cd "/mnt/c/Users/User/Desktop/Shorts Blocker Kids"; \
  export ANDROID_HOME=/home/serputko/Android/Sdk \
  ANDROID_SDK_ROOT=/home/serputko/Android/Sdk; \
  ./scripts/android_start_emulator.sh; ./gradlew localQualityGate'
```

Result:

```text
BUILD SUCCESSFUL in 5m 32s
331 actionable tasks: 24 executed, 307 up-to-date
```

Release artifacts:

```text
app/build/outputs/apk/release/app-release.apk      9,649,190 bytes
app/build/outputs/bundle/release/app-release.aab   9,148,081 bytes
```

SHA-256:

```text
44df015f486c37ba4c7eb59d378659d3c5f0e03bff9bfb85c45a6afc47cfa521  app/build/outputs/apk/release/app-release.apk
2fee86da1e3b423d4c20007a7bf95d7fb829e11c97559ec2865cb2a8f723d23d  app/build/outputs/bundle/release/app-release.aab
```

APK signing verification:

```text
Verifies
Verified using v2 scheme: true
Number of signers: 1
```

Release metadata from `output-metadata.json`:

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
APPLICATION_ID="com.shortsblockerkids"
BUILD_TYPE="release"
ACCESSIBILITY_DEBUG_TOOLS_ENABLED=false
BILLING_BACKEND_BASE_URL=""
```

Merged release manifest:

```text
versionCode="1"
versionName="0.1.0"
uses-permission: com.android.vending.BILLING
uses-permission: com.shortsblockerkids.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
uses-permission: android.permission.ACCESS_NETWORK_STATE
uses-permission: android.permission.INTERNET
service permission: android.permission.BIND_ACCESSIBILITY_SERVICE
```

Release APK emulator smoke:

```text
AVD: ShortsBlocker_Pixel_API35
Install source: app/build/outputs/apk/release/app-release.apk
Install result: Success
Launch package: com.shortsblockerkids
Runtime package: versionCode=1 minSdk=26 targetSdk=36 versionName=0.1.0
Runtime process: pid 2372
```

AAB-derived emulator smoke:

```bash
BUNDLETOOL_CP=$(find /home/serputko/.gradle/caches/modules-2/files-2.1 -name '*.jar' | paste -sd ':' -)
java -cp "$BUNDLETOOL_CP" com.android.tools.build.bundletool.BundleToolMain \
  build-apks \
  --bundle=app/build/outputs/bundle/release/app-release.aab \
  --output=app/build/outputs/bundle/release/app-release-universal.apks \
  --mode=universal \
  --overwrite \
  --aapt2=/home/serputko/Android/Sdk/build-tools/36.0.0/aapt2
java -cp "$BUNDLETOOL_CP" com.android.tools.build.bundletool.BundleToolMain \
  install-apks \
  --apks=app/build/outputs/bundle/release/app-release-universal.apks
```

Result:

```text
APKs generated from release AAB: app-release-universal.apks
Local bundletool signing: debug keystore, for emulator install only
Install target: emulator-5554
Launch package: com.shortsblockerkids
Runtime package: versionCode=1 minSdk=26 targetSdk=36 versionName=0.1.0
Runtime process: pid 3602
```

AAB-derived `.apks` SHA-256:

```text
5bb72e6c1575c9868f129c9094b9b8f32f85571e8cab3dee05ffaec88d7432a4  app/build/outputs/bundle/release/app-release-universal.apks
```

Release minification decision:

```text
isMinifyEnabled=true
isShrinkResources=true
```

R8/resource shrinking are enabled for the current release build config.

Release lint result:

```text
0 errors, 20 warnings
```

Warnings are dependency/version freshness, Android backup-rule modernization,
launcher icon shape, one unused string, and one KTX suggestion. None changed
the release gate result because `lintRelease` completed successfully.

## Command

```bash
sg kvm -c 'set -e; cd "/mnt/c/Users/User/Desktop/Shorts Blocker Kids"; \
  export ANDROID_HOME=/home/serputko/Android/Sdk \
  ANDROID_SDK_ROOT=/home/serputko/Android/Sdk; \
  ./scripts/android_start_emulator.sh; ./gradlew localQualityGate'
```

Result:

```text
BUILD SUCCESSFUL in 5m 32s
331 actionable tasks: 24 executed, 307 up-to-date
```

## Test Result

```text
tests=187 failures=0 errors=0 skipped=0
```

## Build Outputs

```text
app/build/outputs/bundle/release/app-release.aab  9,148,081 bytes
app/build/outputs/apk/release/app-release.apk     9,649,190 bytes
app/build/outputs/apk/debug/app-debug.apk         14,339,477 bytes
```

SHA-256:

```text
2fee86da1e3b423d4c20007a7bf95d7fb829e11c97559ec2865cb2a8f723d23d  app/build/outputs/bundle/release/app-release.aab
44df015f486c37ba4c7eb59d378659d3c5f0e03bff9bfb85c45a6afc47cfa521  app/build/outputs/apk/release/app-release.apk
af7a10802172447eced1fc624faeca6d6ca7c2a57f09dd5cc97f39de562e5cd6  app/build/outputs/apk/debug/app-debug.apk
```

## Release Manifest Checks

From the merged release manifest:

```text
versionCode="1"
versionName="0.1.0"
uses-permission: com.shortsblockerkids.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
uses-permission: com.android.vending.BILLING
uses-permission: android.permission.ACCESS_NETWORK_STATE
uses-permission: android.permission.INTERNET
service permission: android.permission.BIND_ACCESSIBILITY_SERVICE
```

No release manifest matches were found for:

- `SYSTEM_ALERT_WINDOW`;
- `QUERY_ALL_PACKAGES`;
- `CAMERA`;
- `RECORD_AUDIO`;
- `ACCESS_FINE_LOCATION`;
- `READ_CONTACTS`;
- `READ_SMS`.

The AccessibilityService XML does not declare `isAccessibilityTool=true`.

## Release Debug Flags

Generated release `BuildConfig`:

```text
DEBUG = false
APPLICATION_ID = "com.shortsblockerkids"
BUILD_TYPE = "release"
ACCESSIBILITY_DEBUG_TOOLS_ENABLED = false
```

## Current Release Decisions

- `versionName = 0.1.0` and `versionCode = 1` remain a controlled release
  candidate line.
- Release builds use R8 minification and resource shrinking.
- Google Play Billing is integrated for the planned monthly subscription.
- App-side detector support includes YouTube Shorts, TikTok primary package,
  Instagram Reels, and Facebook Reels.
- The app still has no analytics, ads, or account system.
- A billing backend module exists for Play purchase verification; Android builds
  use it only when `SBK_BILLING_BACKEND_BASE_URL` is supplied.
- `android.permission.INTERNET` and `android.permission.ACCESS_NETWORK_STATE`
  are present through the Google Play Billing SDK and may also be used for
  billing backend verification in production-configured builds.

## Pending Before Play Internal Testing

- Create / confirm Play Console app.
- Configure Play App Signing and upload key flow.
- Upload `app-release.aab` to internal testing.
- Confirm Play Console accepts the AAB.
- Submit Privacy Policy URL `https://shortsblockerkids.de/privacy`.
- Complete Data Safety form.
- Complete Accessibility declaration.
- Upload Accessibility review demo video.
- Add store screenshots and feature graphic.
- Execute `docs/SHORTS_BLOCKER_PLAY_BILLING_INTERNAL_TEST_RUNBOOK.md` after the
  subscription product and internal testing track are active.
- Deploy and verify the billing backend at `https://billing.shortsblockerkids.de`
  before Play production rollout.
- Use `docs/SHORTS_BLOCKER_PLAY_CONSOLE_BILLING_CONFIG.md` for the exact
  subscription product/base-plan values.
- Run final smoke QA on the exact AAB-derived install from Play internal
  testing.
