# Shorts Blocker Kids AAB Release Readiness

Status: Partial. Local release pipeline passes and AAB is produced; Play
Console upload and Play App Signing validation remain pending.

## Scope

This report records the local Android App Bundle release readiness check run on
May 23, 2026.

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
review, billing tester flows, or final signed release-candidate device QA.

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
BUILD SUCCESSFUL in 1m 49s
122 actionable tasks: 29 executed, 93 up-to-date
```

## Test Result

```text
tests=105 failures=0 errors=0 skipped=0
```

## Build Outputs

```text
app/build/outputs/bundle/release/app-release.aab  8.7M
app/build/outputs/apk/release/app-release.apk     9.2M
app/build/outputs/apk/debug/app-debug.apk         13M
```

SHA-256:

```text
f41fd59be8ce9c24ac6f391365830b0b06be03c5d7c7f6586ae5716f6e07afb1  app/build/outputs/bundle/release/app-release.aab
e16e3d6f0965ea958baeae3aecdfb04a5820bddbc6de7d7270e96cf56aa06128  app/build/outputs/apk/release/app-release.apk
03bfcaf806b193d0cbddcb0496e5db8a2b510d1df335af2a98074de5b9aafe23  app/build/outputs/apk/debug/app-debug.apk
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
- The app still has no backend, analytics, ads, or account system.
- `android.permission.INTERNET` and `android.permission.ACCESS_NETWORK_STATE`
  are present through the Google Play Billing SDK; there is no custom backend or
  analytics network flow.

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
- Run final smoke QA on the exact AAB-derived install from Play internal
  testing.
