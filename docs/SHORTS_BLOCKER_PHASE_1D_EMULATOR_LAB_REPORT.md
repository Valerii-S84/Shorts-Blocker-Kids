# Shorts Blocker Kids - Phase 1D Emulator Lab Report

Status: Partial

## Scope

- Installed/verified local Android SDK emulator packages.
- Created a Google Play API 35 AVD.
- Added safe repo-root Android helper scripts.
- Added Gradle wrapper for `./gradlew` workflows.
- Added detector JSON fixtures and JVM fixture tests.
- Added a debug-only Detector Playground.
- Added a debug-only test blocking overlay trigger.
- Generated a distributable debug APK, SHA-256 checksum, build info, install guide, and phone checklist.
- Added a privacy and permissions audit.

Not completed:

- Emulator boot.
- App install on emulator.
- Emulator UI testing.
- Emulator Accessibility Service enablement.
- Live YouTube validation.

## Environment

- OS: Ubuntu 24.04.3 LTS on WSL2.
- Kernel: Linux `6.6.87.2-microsoft-standard-WSL2`.
- ANDROID_HOME used: `/home/serputko/Android/Sdk`.
- Android SDK packages installed:
  - `build-tools;36.0.0`
  - `emulator` 36.5.11
  - `platform-tools` 37.0.0
  - `platforms;android-35`
  - `platforms;android-36`
  - `system-images;android-35;google_apis_playstore;x86_64`
- AVD name: `ShortsBlocker_Pixel_API35`.
- AVD device: `pixel_6`.
- AVD target: Google Play Android 15.0, API 35, `google_apis_playstore/x86_64`.

Emulator blockers:

- `/home/serputko/Android/Sdk/emulator/emulator -version` failed with missing host library `libnss3.so`.
- `scripts/android_start_emulator.sh` started the emulator launcher, but the x86_64 emulator exited before adb connection because the current user is not in the `kvm` group and x86_64 emulation requires hardware acceleration.
- `/dev/kvm` exists, but permissions are `root:kvm`, and user `serputko` is not a member of `kvm`.

## Files

- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.jar`
- `gradle/wrapper/gradle-wrapper.properties`
- `scripts/android_setup_emulator.sh`
- `scripts/android_create_avd.sh`
- `scripts/android_start_emulator.sh`
- `scripts/android_install_debug.sh`
- `scripts/android_run_checks.sh`
- `app/src/main/java/com/shortsblockerkids/MainActivity.kt`
- `app/src/main/java/com/shortsblockerkids/accessibility/AccessibilityEventRouter.kt`
- `app/src/main/java/com/shortsblockerkids/accessibility/RuntimeProtectionState.kt`
- `app/src/main/java/com/shortsblockerkids/feature/dashboard/DashboardScreen.kt`
- `app/src/main/java/com/shortsblockerkids/feature/debug/DetectorPlaygroundScreen.kt`
- `app/src/test/java/com/shortsblockerkids/accessibility/DetectorFixtureLoader.kt`
- `app/src/test/java/com/shortsblockerkids/accessibility/YouTubeShortsDetectorFixtureTest.kt`
- `app/src/test/resources/fixtures/youtube_home.json`
- `app/src/test/resources/fixtures/youtube_normal_video.json`
- `app/src/test/resources/fixtures/youtube_search.json`
- `app/src/test/resources/fixtures/youtube_shorts_tab_low.json`
- `app/src/test/resources/fixtures/youtube_shorts_reel_high.json`
- `app/src/test/resources/fixtures/youtube_shorts_swipe_high.json`
- `dist/ShortsBlockerKids-debug.apk`
- `dist/ShortsBlockerKids-debug.apk.sha256`
- `dist/INSTALL_ON_PHONE.md`
- `dist/TEST_ON_PHONE_CHECKLIST.md`
- `dist/BUILD_INFO.txt`
- `docs/SHORTS_BLOCKER_PRIVACY_PERMISSIONS_AUDIT.md`
- `docs/SHORTS_BLOCKER_PHASE_1D_EMULATOR_LAB_REPORT.md`

## What Was Implemented

- Android SDK setup script that installs required SDK packages from the official SDK manager.
- AVD creation script for `ShortsBlocker_Pixel_API35`.
- Emulator start script with early failure detection and adb boot wait.
- Debug install script.
- Build/check script.
- Gradle wrapper pinned to Gradle 9.4.1.
- Debug-only Detector Playground reachable from the dashboard only when `BuildConfig.DEBUG` is true.
- Debug-only detector scenarios:
  - Simulate YouTube Home.
  - Simulate normal YouTube video.
  - Simulate Search.
  - Simulate Shorts HIGH confidence.
  - Simulate non-YouTube app.
- Debug-only test overlay request path through `RuntimeProtectionState`.
- Phone-ready `dist/` artifact set.

## Emulator Testing

Completed:

- Installed SDK emulator package and Google Play API 35 system image.
- Created AVD `ShortsBlocker_Pixel_API35`.
- Verified adb has no connected devices before emulator launch.

Blocked:

- Emulator did not boot.
- APK was not installed on emulator.
- App UI was not opened on emulator.
- Accessibility Service was not enabled on emulator.
- Dashboard state was not verified on emulator.

Start command:

```bash
ANDROID_HOME=/home/serputko/Android/Sdk ./scripts/android_start_emulator.sh
```

Result:

- Failed before adb connection.
- Primary runtime blocker: current user has no KVM permission.
- Additional direct emulator-version blocker: missing host `libnss3.so`.

## Detector Harness

Added JSON fixtures under `app/src/test/resources/fixtures/`:

- `youtube_home.json`
- `youtube_normal_video.json`
- `youtube_search.json`
- `youtube_shorts_tab_low.json`
- `youtube_shorts_reel_high.json`
- `youtube_shorts_swipe_high.json`

Added fixture tests:

- Home fixture: not blocked, `NONE` or `LOW`.
- Normal video fixture: not blocked, `NONE` or `LOW`.
- Search fixture: not blocked, `NONE` or `LOW`.
- Shorts tab-only fixture: `LOW`, not blocked.
- Shorts reel fixture: `HIGH`, blocked.
- Shorts swipe fixture: `HIGH`, blocked.
- Non-YouTube package with Shorts structure: `NONE`, not blocked.

Total JVM tests after Phase 1D:

- 22 tests.
- 0 failures.
- 0 errors.

## Overlay Debug Test

Implemented but not emulator-tested.

Debug-only behavior:

- Dashboard shows `Detector Playground` only in debug builds.
- Playground has `Show test blocking overlay`.
- Button sets an in-process debug overlay request.
- AccessibilityService consumes the request on the next accessibility event.
- The same overlay path is used, so repeated requests still pass through existing overlay/cooldown state.

Not proven:

- Overlay rendering on emulator.
- Overlay buttons on emulator.
- Overlay cleanup on emulator service stop.

## APK Artifact

Generated:

- `dist/ShortsBlockerKids-debug.apk`
- `dist/ShortsBlockerKids-debug.apk.sha256`
- `dist/BUILD_INFO.txt`

SHA-256:

```text
735425ce690dc610b57497091b01aa13eae91073dbdc53c651dcd414ff8b0f5b
```

Build info includes:

- git commit hash: unavailable because this directory is not a git repository.
- build date.
- versionName `0.1.0`.
- versionCode `1`.
- minSdk `26`.
- targetSdk `36`.
- permissions list.
- APK checksum.
- build commands used.

## Phone Install Instructions

Created:

- `dist/INSTALL_ON_PHONE.md`
- `dist/TEST_ON_PHONE_CHECKLIST.md`

ADB install command:

```bash
ANDROID_HOME=/home/serputko/Android/Sdk /home/serputko/Android/Sdk/platform-tools/adb install -r dist/ShortsBlockerKids-debug.apk
```

## Verification

SDK package install command:

```bash
yes | env -u HTTP_PROXY -u HTTPS_PROXY -u http_proxy -u https_proxy ANDROID_HOME=/home/serputko/Android/Sdk ANDROID_SDK_ROOT=/home/serputko/Android/Sdk /home/serputko/Android/Sdk/cmdline-tools/latest/bin/sdkmanager --install "emulator" "platforms;android-35" "system-images;android-35;google_apis_playstore;x86_64"
```

AVD create command:

```bash
printf 'no\n' | ANDROID_HOME=/home/serputko/Android/Sdk ANDROID_SDK_ROOT=/home/serputko/Android/Sdk sh /home/serputko/Android/Sdk/cmdline-tools/latest/bin/avdmanager create avd --force --name ShortsBlocker_Pixel_API35 --package 'system-images;android-35;google_apis_playstore;x86_64' --device pixel_6
```

Build, unit test, and lint:

```bash
ANDROID_HOME=/home/serputko/Android/Sdk ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Result:

- `assembleDebug`: passed.
- `testDebugUnitTest`: passed.
- `lintDebug`: passed.

Script verification:

```bash
./scripts/android_create_avd.sh
ANDROID_HOME=/home/serputko/Android/Sdk ./scripts/android_run_checks.sh
ANDROID_HOME=/home/serputko/Android/Sdk ./scripts/android_start_emulator.sh
```

Results:

- `android_create_avd.sh`: AVD already exists.
- `android_run_checks.sh`: passed.
- `android_start_emulator.sh`: failed before adb connection due KVM permission blocker.

Checksum:

```bash
sha256sum dist/ShortsBlockerKids-debug.apk
```

Result:

```text
735425ce690dc610b57497091b01aa13eae91073dbdc53c651dcd414ff8b0f5b  dist/ShortsBlockerKids-debug.apk
```

## Privacy Confirmation

- No backend added.
- No account system added.
- No cloud sync added.
- No analytics added.
- No ads added.
- No billing added.
- No tracking added.
- No watch history storage added.
- No language/content analysis added.
- No video/audio analysis added.
- No `INTERNET` permission.
- No `CAMERA`, `MICROPHONE`, `LOCATION`, `CONTACTS`, or `SMS` permission.
- Debug detector playground uses synthetic local snapshots only.
- Debug overlay trigger is guarded by `BuildConfig.DEBUG`.
- Raw accessibility tree summaries remain debug-only.

Detailed audit:

- `docs/SHORTS_BLOCKER_PRIVACY_PERMISSIONS_AUDIT.md`

## What Is Still Not Proven

- App install on emulator.
- App launch on emulator.
- PIN/onboarding/dashboard behavior on emulator.
- Accessibility Service enablement on emulator.
- Overlay rendering and buttons on emulator.
- YouTube availability in the Google Play emulator image.
- Live YouTube Home/video/Search/Shorts behavior.
- Real phone behavior.

## Known Limitations

- Emulator cannot boot until host dependencies and KVM permissions are fixed.
- Debug overlay trigger requires the Accessibility Service to be enabled and an accessibility event to be delivered.
- Detector fixtures are synthetic and do not replace live YouTube evidence.
- `Enter PIN` still opens the app/dashboard instead of rendering full PIN inside the overlay.
- Lint passes but reports warnings for SDK/dependency freshness, backup metadata, missing app icon, unused accessibility label, and KTX suggestions.

## Risks

- Google Play Accessibility review risk remains.
- YouTube UI changes can break detector rules.
- Android/OEM Accessibility event behavior can differ from emulator and fixtures.
- WSL2/emulator host setup may remain fragile without KVM group membership and native library fixes.

## Next Recommended Task

Phase 1E: fix emulator host runtime and complete emulator UI validation.

Required host actions:

- Add user `serputko` to the `kvm` group or otherwise grant `/dev/kvm` access.
- Install the missing host library that provides `libnss3.so`.
- Reopen the shell/session after group changes.
- Run `ANDROID_HOME=/home/serputko/Android/Sdk ./scripts/android_start_emulator.sh`.
- Run `ANDROID_HOME=/home/serputko/Android/Sdk ./scripts/android_install_debug.sh`.
- Complete the emulator onboarding/PIN/dashboard/accessibility/overlay checklist.
