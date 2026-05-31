# Shorts Blocker Kids - Phase 1B Device Calibration Report

Status: Partial

## Scope

- Checked for a real Android device with `adb devices`.
- Built the debug APK locally.
- Added an Accessibility overlay blocking spike.
- Added block cooldown and detection debounce state.
- Updated dashboard runtime status.
- Added debug-only last detector result display.
- Tuned detector confidence rules conservatively without live YouTube evidence.
- Added JVM unit tests for detector, blocking decisions, settings state, and PIN hashing behavior.

Not completed:

- Real-device install.
- Accessibility Service enablement on a phone.
- Live YouTube detector evidence collection.
- Real YouTube Shorts calibration.

## Device

- Phone model: not available.
- Android version: not available.
- YouTube version: not available.
- Device status: no device or emulator was visible through `adb devices`.

Command:

```bash
ANDROID_HOME=/path/to/Android/Sdk /path/to/Android/Sdk/platform-tools/adb devices
```

Result:

```text
List of devices attached
```

## Files

- `app/src/main/java/com/shortsblockerkids/MainActivity.kt`
- `app/src/main/java/com/shortsblockerkids/accessibility/AccessibilityEventRouter.kt`
- `app/src/main/java/com/shortsblockerkids/accessibility/BlockOverlayController.kt`
- `app/src/main/java/com/shortsblockerkids/accessibility/BlockingDecisionController.kt`
- `app/src/main/java/com/shortsblockerkids/accessibility/DetectionResult.kt`
- `app/src/main/java/com/shortsblockerkids/accessibility/RuntimeProtectionState.kt`
- `app/src/main/java/com/shortsblockerkids/accessibility/ShortsBlockerAccessibilityService.kt`
- `app/src/main/java/com/shortsblockerkids/accessibility/YouTubeShortsDetector.kt`
- `app/src/main/java/com/shortsblockerkids/feature/dashboard/DashboardScreen.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/xml/shorts_blocker_accessibility_service.xml`
- `app/src/test/java/com/shortsblockerkids/accessibility/BlockingDecisionControllerTest.kt`
- `app/src/test/java/com/shortsblockerkids/accessibility/YouTubeShortsDetectorTest.kt`
- `app/src/test/java/com/shortsblockerkids/core/storage/AppSettingsTest.kt`
- `docs/SHORTS_BLOCKER_PHASE_1B_DEVICE_CALIBRATION_REPORT.md`

## Detector Evidence

Live detector evidence was not collected because no real Android device was attached.

Implemented detector signals:

- YouTube package check.
- Shorts identifier signal from sanitized generic content descriptions or view IDs.
- Reel / Shorts player / Shorts container view ID signal.
- Fullscreen vertical layout signal.
- Right-side vertical Shorts action rail signal.
- Repeated vertical feed signal from scrollable full-height reel/Shorts/ViewPager/RecyclerView-like structures.

Stable in local synthetic tests:

- Non-YouTube packages return `NONE` and are not blocked.
- Normal watch-like YouTube layout with a Shorts bottom-tab signal is not blocked.
- Shorts-like reel container plus action rail plus vertical fullscreen structure is `HIGH`.
- Reel container plus action rail can be `HIGH` without relying on visible `Shorts` text.
- A Shorts tab-only signal remains `LOW`.

Unstable until device testing:

- Actual YouTube `viewIdResourceName` values.
- Actual class names and layout hierarchy.
- OEM Accessibility tree timing.
- YouTube UI changes by version, locale, account state, and screen size.

Examples of local `HIGH` reasons:

- `reel/shorts player view id signal`
- `vertical shorts action rail signal`
- `fullscreen vertical layout signal`
- `repeated vertical feed signal`

Examples that remain below blocking confidence:

- Non-YouTube package: `NONE`.
- YouTube bottom Shorts tab only: `LOW`.
- Normal YouTube watch-like page with horizontal action controls: not blocked.

## Blocking

Implemented mode: Accessibility overlay plus Exit button back action.

Overlay:

- Uses `TYPE_ACCESSIBILITY_OVERLAY`.
- Text: `Shorts blocked. Parent PIN required.`
- Buttons: `Exit Shorts`, `Enter PIN`.

Behavior implemented:

- `Exit Shorts` performs `GLOBAL_ACTION_BACK` and closes the overlay.
- `Enter PIN` opens the app and closes the overlay.
- Repeated overlays are prevented by overlay state.
- Block cooldown: `1200ms`.
- Detection debounce: `500ms`.
- Protection OFF dismisses overlay and stops block decisions.
- AccessibilityService interrupt/destroy dismisses overlay and resets state.
- Non-YouTube events dismiss/reset without scanning an accessibility tree.

Not proven on device:

- Overlay rendering stability on a real phone.
- Exit Shorts behavior against live YouTube Shorts.
- Swipe-to-next-Shorts behavior.
- Leave-YouTube behavior across OEM launchers and Android versions.

## Manual Test Matrix

Not run because no real Android device was visible.

- Home: not run.
- Normal video: not run.
- Search: not run.
- Subscriptions / Library: not run.
- Shorts tab: not run.
- Shorts from Home: not run.
- Shorts swipe: not run.
- Return from Shorts: not run.
- Protection OFF: not run on device.
- Accessibility disabled: not run on device.
- App restart: not run on device.

Required manual setup checklist:

1. Connect a real Android phone with YouTube installed.
2. Enable Developer Options and USB debugging.
3. Confirm the phone appears in:

```bash
ANDROID_HOME=/path/to/Android/Sdk /path/to/Android/Sdk/platform-tools/adb devices
```

4. Install the generated APK:

```bash
ANDROID_HOME=/path/to/Android/Sdk /path/to/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

5. Open Shorts Blocker Kids.
6. Create parent PIN.
7. Accept Accessibility disclosure.
8. Open Android Accessibility settings.
9. Enable Shorts Blocker Kids Accessibility Service.
10. Return to app.
11. Turn YouTube Shorts Protection ON.
12. Open YouTube Home and confirm it is not blocked.
13. Open a normal YouTube video and confirm it is not blocked.
14. Open YouTube Shorts and confirm overlay appears.
15. Tap `Exit Shorts` and confirm it returns out of Shorts without a loop.
16. Reopen Shorts and swipe to the next Short; confirm blocking still appears without stacking/flicker.
17. Turn Protection OFF and confirm blocking stops.
18. Disable Accessibility Service and confirm dashboard reports inactive protection.

## Privacy

- No backend was added.
- No account system was added.
- No cloud integration was added.
- No analytics was added.
- No ads or billing were added.
- No `INTERNET` permission was added.
- No watch history is stored.
- No raw accessibility tree is logged in release code paths.
- Debug detector logging remains guarded by `BuildConfig.DEBUG`.
- Debug dashboard detector status remains guarded by `BuildConfig.DEBUG`.
- Non-YouTube events are used only to dismiss/reset overlay state; the service does not read `rootInActiveWindow`, scan, or log detector evidence for non-YouTube packages.

## Verification

Build / unit test / lint command:

```bash
ANDROID_HOME=/path/to/Android/Sdk gradle :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Result:

- Passed.
- Debug APK generated at `app/build/outputs/apk/debug/app-debug.apk`.
- JVM unit tests passed: 15 tests, 0 failures, 0 errors.
- Android lint task passed.

Privacy checks:

```bash
rg -n 'INTERNET|<uses-permission|Log\.|toDebugSummary|BuildConfig.DEBUG|packageNames|rootInActiveWindow' app/src/main
```

Result:

- No `INTERNET` permission found.
- Raw debug summary use is guarded by `BuildConfig.DEBUG`.
- `rootInActiveWindow` is read only for the YouTube package in `ShortsBlockerAccessibilityService`.

## Known Limitations

- No real-device install was performed.
- No live YouTube UI evidence was collected.
- Detector rules are still based on synthetic local snapshots and Phase 1 assumptions.
- Actual YouTube IDs and hierarchy may differ from local detector assumptions.
- Accessibility overlay behavior may vary by Android version and OEM.
- `Enter PIN` opens the app instead of rendering full PIN entry inside the overlay.
- SharedPreferences persistence is not covered by JVM tests because this project has no Robolectric or instrumentation test setup.
- Lint passes but reports pre-existing warnings for dependency freshness, target/compile SDK freshness, backup metadata, missing app icon, unused accessibility label, and SharedPreferences KTX suggestions.

## Risks

- Google Play Accessibility review risk remains high and requires narrow disclosure and behavior.
- YouTube UI changes can break detector confidence.
- Android/OEM Accessibility event timing can cause false negatives or overlay timing issues.
- Listening for non-YouTube events only to dismiss the overlay improves loop prevention but should be reviewed carefully during privacy and Play policy review.

## Next Recommended Task

Phase 1C: real-device calibration run.

Required outcome:

- Install `app/build/outputs/apk/debug/app-debug.apk` on a real Android phone.
- Enable the Accessibility Service.
- Collect sanitized debug detector evidence from YouTube Home, normal video, Search, Shorts tab, Shorts opened from Home, Shorts swipe, and return from Shorts.
- Tune detector rules from real evidence.
- Verify Home and normal videos are not blocked while Shorts is blocked.
