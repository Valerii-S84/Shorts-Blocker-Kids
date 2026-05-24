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

## May 24, 2026 Release Gate Refresh

Release build command:

```bash
ANDROID_HOME=/home/serputko/Android/Sdk ./gradlew \
  :app:assembleRelease \
  :app:bundleRelease \
  :app:lintRelease
```

Result:

```text
BUILD SUCCESSFUL in 19s
61 actionable tasks: 7 executed, 54 up-to-date
```

Release artifacts:

```text
app/build/outputs/apk/release/app-release.apk      9,632,411 bytes
app/build/outputs/bundle/release/app-release.aab   9,131,002 bytes
```

SHA-256:

```text
0c33cb46074f3499ca58bfff0630b4f4bd9103105b83cdb63b503ca5be0244c3  app/build/outputs/apk/release/app-release.apk
c508aea82a9212f193de6cdccbe2694463d1537ff7420666a5de0adca83bbc41  app/build/outputs/bundle/release/app-release.aab
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
Runtime process: pid 2349
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
Runtime process: pid 3217
```

AAB-derived `.apks` SHA-256:

```text
b99bb40e0d01b9ab91023e4f4794cd1cb1f23a1359b7fd7b0e7a8aa0f3235c79  app/build/outputs/bundle/release/app-release-universal.apks
```

Release minification decision remains unchanged:

```text
isMinifyEnabled=false
```

Reason: keep R8 disabled for this release-candidate line until final
release-device regression coverage is complete for the AccessibilityService
detector path.

Release lint result:

```text
0 errors, 20 warnings
```

Warnings are dependency/version freshness, Android backup-rule modernization,
launcher icon shape, one unused string, and one KTX suggestion. None changed
the release gate result because `lintRelease` completed successfully.

## Command

```bash
ANDROID_HOME=/home/serputko/Android/Sdk ./gradlew \
  :app:ktlintCheck \
  :app:assembleDebug \
  :app:assembleRelease \
  :app:bundleRelease \
  :app:testDebugUnitTest \
  :app:lintDebug \
  :app:lintRelease
```

Result:

```text
BUILD SUCCESSFUL in 2m 5s
122 actionable tasks: 38 executed, 2 from cache, 82 up-to-date
```

## Test Result

```text
tests=121 failures=0 errors=0 skipped=0
```

## Build Outputs

```text
app/build/outputs/bundle/release/app-release.aab  8.7M
app/build/outputs/apk/release/app-release.apk     9.2M
app/build/outputs/apk/debug/app-debug.apk         13M
```

SHA-256:

```text
658b5bf57d249c83239fa9ba1b49951b84a95375e507858086523dfe3f081920  app/build/outputs/bundle/release/app-release.aab
b180737f029acbd5ade21cd08f01e6f42f92f97c4f198465a21102ee066b1bb1  app/build/outputs/apk/release/app-release.apk
c261d2d0843c9587245434793471664dc1124b8e9c31bf2da962a83962dd84b8  app/build/outputs/apk/debug/app-debug.apk
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
- Release minification remains disabled until final release-device regression
  coverage is complete for the AccessibilityService detector path.
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
- Add hosted Privacy Policy URL.
- Complete Data Safety form.
- Complete Accessibility declaration.
- Upload Accessibility review demo video.
- Add store screenshots and feature graphic.
- Execute `docs/SHORTS_BLOCKER_PLAY_BILLING_INTERNAL_TEST_RUNBOOK.md` after the
  subscription product and internal testing track are active.
- Deploy and verify the billing backend before a production-configured AAB.
- Use `docs/SHORTS_BLOCKER_PLAY_CONSOLE_BILLING_CONFIG.md` for the exact
  subscription product/base-plan values.
- Run final smoke QA on the exact AAB-derived install from Play internal
  testing.
