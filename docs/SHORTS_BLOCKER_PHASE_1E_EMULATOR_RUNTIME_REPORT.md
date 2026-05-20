# Shorts Blocker Kids - Phase 1E Emulator Runtime Report

Status: Done

## Scope

- Fixed the local WSL2 Android Emulator runtime enough to boot the existing API 35 AVD.
- Installed missing host libraries and KVM tooling.
- Verified emulator version and acceleration.
- Booted `ShortsBlocker_Pixel_API35`.
- Installed the debug APK on the emulator.
- Launched the app and verified `MainActivity` is resumed.
- Created Windows emulator fallback instructions.
- Rebuilt `dist/` APK, checksum, and build info.

No product logic, backend, account, analytics, billing, or feature behavior was changed.

## Environment

- OS: Ubuntu 24.04.3 LTS on WSL2.
- Android SDK: `/home/serputko/Android/Sdk`.
- Emulator version: `36.5.11.0`.
- AVD: `ShortsBlocker_Pixel_API35`.
- AVD image: Google Play Android 15 / API 35 / x86_64.
- Emulator device: `emulator-5554`.
- Emulator manufacturer: `Google`.
- Emulator model: `sdk_gphone64_x86_64`.
- Android version: `15`.
- SDK version: `35`.

## Runtime Fixes

Initial checks:

- `groups`: current shell did not include `kvm`.
- `/dev/kvm`: `crw-rw---- root kvm`.
- `emulator -version`: failed before fixes because `libnss3.so` was missing.
- `emulator -accel-check`: failed in the current shell because the active process did not have `kvm` group membership.

System packages installed through WSL root:

```bash
apt-get update
apt-get install -y libnss3 cpu-checker qemu-system-x86
apt-get install -y libxkbfile1
```

Notes:

- `qemu-kvm` has no candidate package in this Ubuntu 24.04 environment.
- Installed `qemu-system-x86`, which provides the relevant QEMU/KVM runtime package set here.
- Added `serputko` to `kvm`:

```bash
usermod -aG kvm serputko
```

Post-fix checks:

- `getent group kvm`: `kvm:x:993:serputko`.
- `emulator -version`: passed.
- `kvm-ok`: `KVM acceleration can be used`.
- `sg kvm -c 'emulator -accel-check'`: `KVM (version 12) is installed and usable`.

Current-session caveat:

- The current shell still does not list `kvm` in `groups`; it needs re-login/restart to pick up the new supplementary group.
- Until then, emulator launch works through `sg kvm -c ...`.

## Files

- Updated `scripts/android_start_emulator.sh` to avoid hanging and to write an emulator log.
- Created `docs/SHORTS_BLOCKER_WINDOWS_EMULATOR_FALLBACK.md`.
- Created `docs/SHORTS_BLOCKER_PHASE_1E_EMULATOR_RUNTIME_REPORT.md`.
- Regenerated:
  - `dist/ShortsBlockerKids-debug.apk`
  - `dist/ShortsBlockerKids-debug.apk.sha256`
  - `dist/BUILD_INFO.txt`

## Emulator Boot

Command used:

```bash
sg kvm -c 'ANDROID_HOME=/home/serputko/Android/Sdk ANDROID_SDK_ROOT=/home/serputko/Android/Sdk /home/serputko/Android/Sdk/emulator/emulator -avd ShortsBlocker_Pixel_API35 -netdelay none -netspeed full -no-snapshot -no-boot-anim -no-window'
```

Result:

- Emulator booted.
- `adb devices` showed:

```text
emulator-5554    device
```

## APK Install And Launch

Install command:

```bash
ANDROID_HOME=/home/serputko/Android/Sdk /home/serputko/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Result:

```text
Performing Streamed Install
Success
package:com.shortsblockerkids
```

Launch command:

```bash
ANDROID_HOME=/home/serputko/Android/Sdk /home/serputko/Android/Sdk/platform-tools/adb shell am start -n com.shortsblockerkids/.MainActivity
```

Result:

- `com.shortsblockerkids/.MainActivity` was reported as `topResumedActivity`.

## APK Artifact

Generated:

- `dist/ShortsBlockerKids-debug.apk`
- `dist/ShortsBlockerKids-debug.apk.sha256`
- `dist/BUILD_INFO.txt`

SHA-256:

```text
735425ce690dc610b57497091b01aa13eae91073dbdc53c651dcd414ff8b0f5b
```

Checksum verification:

```bash
sha256sum -c dist/ShortsBlockerKids-debug.apk.sha256
```

Result:

```text
dist/ShortsBlockerKids-debug.apk: OK
```

## Verification

Build, unit tests, and lint:

```bash
ANDROID_HOME=/home/serputko/Android/Sdk ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Result:

- `assembleDebug`: passed.
- `testDebugUnitTest`: passed.
- `lintDebug`: passed.

## Windows Fallback

Created:

```text
docs/SHORTS_BLOCKER_WINDOWS_EMULATOR_FALLBACK.md
```

It documents:

- Building the APK in WSL2.
- Running Android Emulator from Windows / Android Studio.
- Installing `dist/ShortsBlockerKids-debug.apk` with Windows `adb.exe`.
- Manual emulator and phone checklists.

## What Was Not Tested

- Manual Compose onboarding flow.
- PIN setup by UI interaction.
- Accessibility Service enablement.
- Debug overlay trigger on emulator.
- YouTube app behavior.
- Real phone behavior.

These remain outside Phase 1E because this task was emulator runtime repair and installable APK readiness, not product behavior proof.

## Privacy Confirmation

- No backend added.
- No account system added.
- No analytics added.
- No billing added.
- No cloud added.
- No product logic changed.
- No new Android permissions were added by Phase 1E.

## Known Limitations

- Current shell still needs `sg kvm` until a re-login/restart picks up the `kvm` group membership.
- The emulator was started headlessly.
- `scripts/android_start_emulator.sh` can boot and wait, but when invoked through `sg kvm` from the current shell, a foreground emulator session is more reliable for keeping the emulator alive.
- YouTube/Accessibility behavior is still unproven.

## Risks

- WSL2 emulator behavior may remain fragile across terminal/session restarts.
- Future emulator launches should be retested after reopening WSL.
- Real YouTube behavior still depends on a real device or fully interactive emulator session.

## Next Recommended Task

Phase 1F: emulator UI validation.

Run after reopening WSL so `groups` includes `kvm` without `sg`:

```bash
ANDROID_HOME=/home/serputko/Android/Sdk ./scripts/android_start_emulator.sh
ANDROID_HOME=/home/serputko/Android/Sdk ./scripts/android_install_debug.sh
```

Then manually verify onboarding, PIN, dashboard, Accessibility Service state, Detector Playground, and debug overlay trigger on the emulator.
