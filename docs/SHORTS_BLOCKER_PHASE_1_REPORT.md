# Shorts Blocker Kids - Phase 1 Report

Status: Partial

## Scope

- Created a native Android project foundation using Kotlin, Jetpack Compose, and Material 3.
- Added parent PIN setup and PIN entry flow.
- Added local-only settings storage.
- Added the Accessibility disclosure and dashboard screens.
- Added an AccessibilityService skeleton for detecting YouTube Shorts.
- Added a rule-based Shorts detector and a first blocking reaction.
- Added debug-only detector evidence logging.

## Files

- `settings.gradle.kts`
- `build.gradle.kts`
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values/themes.xml`
- `app/src/main/res/xml/shorts_blocker_accessibility_service.xml`
- `app/src/main/java/com/shortsblockerkids/MainActivity.kt`
- `app/src/main/java/com/shortsblockerkids/accessibility/AccessibilityEventRouter.kt`
- `app/src/main/java/com/shortsblockerkids/accessibility/AccessibilityServiceStatus.kt`
- `app/src/main/java/com/shortsblockerkids/accessibility/AccessibilityTreeScanner.kt`
- `app/src/main/java/com/shortsblockerkids/accessibility/BlockOverlayController.kt`
- `app/src/main/java/com/shortsblockerkids/accessibility/Confidence.kt`
- `app/src/main/java/com/shortsblockerkids/accessibility/DebugAccessibilityLogger.kt`
- `app/src/main/java/com/shortsblockerkids/accessibility/DetectionResult.kt`
- `app/src/main/java/com/shortsblockerkids/accessibility/ShortsBlockerAccessibilityService.kt`
- `app/src/main/java/com/shortsblockerkids/accessibility/YouTubeShortsDetector.kt`
- `app/src/main/java/com/shortsblockerkids/core/security/PinHasher.kt`
- `app/src/main/java/com/shortsblockerkids/core/security/PinPolicy.kt`
- `app/src/main/java/com/shortsblockerkids/core/security/PinVerificationResult.kt`
- `app/src/main/java/com/shortsblockerkids/core/storage/AppSettings.kt`
- `app/src/main/java/com/shortsblockerkids/core/storage/SettingsRepository.kt`
- `app/src/main/java/com/shortsblockerkids/feature/blocking/BlockingCopy.kt`
- `app/src/main/java/com/shortsblockerkids/feature/dashboard/DashboardScreen.kt`
- `app/src/main/java/com/shortsblockerkids/feature/onboarding/AccessibilityDisclosureScreen.kt`
- `app/src/main/java/com/shortsblockerkids/feature/onboarding/WelcomeScreen.kt`
- `app/src/main/java/com/shortsblockerkids/feature/pin/PinEntryScreen.kt`
- `app/src/main/java/com/shortsblockerkids/feature/pin/PinSetupScreen.kt`
- `app/src/main/java/com/shortsblockerkids/ui/theme/ShortsBlockerKidsTheme.kt`
- `app/src/test/java/com/shortsblockerkids/accessibility/YouTubeShortsDetectorTest.kt`
- `app/src/test/java/com/shortsblockerkids/core/security/PinHasherTest.kt`
- `app/src/test/java/com/shortsblockerkids/core/security/PinPolicyTest.kt`
- `docs/SHORTS_BLOCKER_PHASE_1_REPORT.md`

## How it works

- `MainActivity` hosts a small Compose flow: welcome, PIN setup, PIN entry, Accessibility disclosure, and dashboard.
- `SettingsRepository` stores local settings in Android `SharedPreferences`.
- `PinHasher` stores only PBKDF2-HMAC-SHA256 PIN hashes and random salts.
- `ShortsBlockerAccessibilityService` is limited to `com.google.android.youtube` in the service XML and ignores other packages in code.
- `AccessibilityEventRouter` scans the current YouTube UI tree, runs the detector, and blocks only HIGH confidence results.
- `BlockOverlayController` currently performs `GLOBAL_ACTION_BACK`. A stable visual overlay was not implemented in Phase 1 to avoid extra overlay behavior before device testing.

## Detector

Signals used:

- package name must be `com.google.android.youtube`;
- view IDs containing reel or Shorts-player style identifiers;
- sanitized content-description signals such as `shorts`, `like`, `comments`, `share`, and `remix`;
- fullscreen vertical layout signals;
- vertical right-side action rail geometry.

Confidence levels:

- `NONE`: not YouTube or no signal.
- `LOW`: weak YouTube-only signal.
- `MEDIUM`: probable Shorts signal, not enough to block.
- `HIGH`: Shorts identifier plus fullscreen vertical structure and either reel/player ID or vertical action rail.

Blocking rule:

- Only `HIGH` confidence is blocked.
- `LOW` and `MEDIUM` are not blocked.

Still unstable:

- YouTube UI IDs and hierarchy can vary by version, account state, locale, and device form factor.
- Real-device evidence is still required to tune the detector.

## Privacy

- No backend was added.
- No account system was added.
- No analytics or tracking was added.
- No `INTERNET` permission was added.
- No watch history is stored.
- No URLs, video titles, comments, account names, messages, screen recordings, or audio recordings are stored.
- Accessibility tree debug logging is guarded by `BuildConfig.DEBUG`.
- The scanner does not read node text and keeps only sanitized content-description signal names.

## Manual testing

- Device used: not tested; no Android device or emulator was attached.
- Android version: not available.
- YouTube version: not available.

Scenarios:

- Open YouTube home: not run.
- Open normal YouTube video: not run.
- Open YouTube Shorts tab: not run.
- Swipe to next Shorts: not run.
- Return from Shorts to YouTube home: not run.
- Turn protection OFF in dashboard: not run on device.
- Turn protection ON again: not run on device.
- Disable Accessibility Service: not run on device.
- Restart app and confirm persisted settings: not run on device.
- Enter wrong PIN several times: implemented in code, not run on device.

## Known limitations

- The app was not installed or opened on a real Android device or emulator in this environment.
- Accessibility Service enablement was not manually verified.
- YouTube Shorts detection was not validated against a live YouTube build.
- The first blocking reaction uses `GLOBAL_ACTION_BACK`, not a blocking overlay screen.
- `temporaryAllowUntil` is stored but no temporary-allow UI was added in Phase 1.
- Detector false positives are reduced by requiring HIGH confidence, but real YouTube UI testing is still needed.

## Verification

Build command:

```bash
ANDROID_HOME=/home/serputko/Android/Sdk gradle :app:assembleDebug :app:testDebugUnitTest
```

Result:

- Passed.
- Debug APK assembled.
- JVM unit tests passed.

Lint command:

```bash
ANDROID_HOME=/home/serputko/Android/Sdk gradle :app:lintDebug
```

Result:

- Passed.

Device check:

```bash
ANDROID_HOME=/home/serputko/Android/Sdk /home/serputko/Android/Sdk/platform-tools/adb devices
```

Result:

- No attached devices or emulators.

## Risks

- Google Play Accessibility review risk: the app uses Accessibility APIs to inspect YouTube UI and block Shorts; the disclosure must remain clear and the purpose narrow.
- YouTube UI change risk: detector rules depend on YouTube UI structure and view IDs that may change.
- Android device compatibility risk: Accessibility event timing, UI trees, and global back behavior can vary by OEM and Android version.
- Product behavior risk: `GLOBAL_ACTION_BACK` can return the user from Shorts, but it is not the same UX as a stable blocking screen.
