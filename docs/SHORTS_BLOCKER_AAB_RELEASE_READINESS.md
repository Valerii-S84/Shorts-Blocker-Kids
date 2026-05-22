# Shorts Blocker Kids AAB Release Readiness

Status: Partial. Local release pipeline passes and AAB is produced; Play
Console upload and Play App Signing validation remain pending.

## Scope

This report records the local Android App Bundle release readiness check run on
May 22, 2026.

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
BUILD SUCCESSFUL in 29s
122 actionable tasks: 8 executed, 114 up-to-date
```

## Test Result

```text
tests=100 failures=0 errors=0 skipped=0
```

## Build Outputs

```text
app/build/outputs/bundle/release/app-release.aab  8.0M
app/build/outputs/apk/release/app-release.apk     8.4M
app/build/outputs/apk/debug/app-debug.apk         12M
```

SHA-256:

```text
20ce523f75f2fa5b3a73fbc0f43fef4aa77d7d567994024e7ebfec345dc3d8a3  app/build/outputs/bundle/release/app-release.aab
18292ac73e97e9d1c5bfa56ccfbcc7862ae5853b61a10545281e3f083eab9069  app/build/outputs/apk/release/app-release.apk
6df1c7a88d62740149c14480619fb7040a01dca39c4833e84487955fc0a304fd  app/build/outputs/apk/debug/app-debug.apk
```

## Release Manifest Checks

From the merged release manifest:

```text
versionCode="1"
versionName="0.1.0"
uses-permission: com.shortsblockerkids.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
service permission: android.permission.BIND_ACCESSIBILITY_SERVICE
```

No release manifest matches were found for:

- `android.permission.INTERNET`;
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
- Google Play Billing is not implemented yet.
- The app still has no backend, analytics, ads, account system, or `INTERNET`
  permission.

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
- Run final smoke QA on the exact AAB-derived install from Play internal
  testing.
