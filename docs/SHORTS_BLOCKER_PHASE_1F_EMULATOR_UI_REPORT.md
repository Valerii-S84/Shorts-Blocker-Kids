# Shorts Blocker Kids - Phase 1F Emulator UI Report

Status: Partial

## Scope

- Boot emulator.
- Install debug APK.
- Open app.
- Complete onboarding.
- Create parent PIN.
- Verify dashboard.
- Enable Accessibility Service if possible.
- Test debug detector playground.
- Test debug overlay trigger.
- Document what works and what remains unproven.

No product code, permissions, dependencies, backend, account, analytics, billing, or release configuration were changed.

## Environment

- OS: Ubuntu 24.04.3 LTS on WSL2.
- Android SDK: `/path/to/Android/Sdk`.
- Emulator version: `36.5.11.0`.
- AVD: `ShortsBlocker_Pixel_API35`.
- Emulator device: `emulator-5554`.
- Emulator model: `sdk_gphone64_x86_64`.
- Android version: `15`.
- SDK version: `35`.
- App package: `com.shortsblockerkids`.
- APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Emulator Boot

The project script was attempted first:

```bash
ANDROID_HOME=/path/to/Android/Sdk ANDROID_SDK_ROOT=/path/to/Android/Sdk ./scripts/android_start_emulator.sh
```

Result:

- Failed before ADB connection.
- Emulator log reported that the current shell did not have permission to use `/dev/kvm`.

The documented Phase 1E `sg kvm` path was used:

```bash
sg kvm -c 'ANDROID_HOME=/path/to/Android/Sdk ANDROID_SDK_ROOT=/path/to/Android/Sdk /path/to/Android/Sdk/emulator/emulator -avd ShortsBlocker_Pixel_API35 -netdelay none -netspeed full -no-snapshot -no-boot-anim -no-window'
```

Result:

```text
Boot completed in 47433 ms
emulator-5554 device product:sdk_gphone64_x86_64 model:sdk_gphone64_x86_64 device:emu64xa
```

Observed emulator instability:

- A background `sg kvm` launch exited during startup without reaching ADB.
- A foreground `sg kvm` emulator session stayed alive and booted successfully.
- A transient `System UI isn't responding` dialog appeared after first launch and was recovered with `Wait`.

## APK Install And Clean Launch

Command:

```bash
ADB=/path/to/Android/Sdk/platform-tools/adb
$ADB install -r app/build/outputs/apk/debug/app-debug.apk
$ADB shell pm clear com.shortsblockerkids
$ADB shell monkey -p com.shortsblockerkids -c android.intent.category.LAUNCHER 1
```

Result:

```text
Performing Streamed Install
Success
Success
Events injected: 1
```

The app opened to the first-run welcome screen.

## Onboarding And PIN

Verified by `uiautomator dump` and ADB input:

- Welcome screen showed:
  - `Shorts Blocker Kids`
  - `Start`
- PIN setup screen showed:
  - `Create Parent PIN`
  - `PIN`
  - `Confirm PIN`
  - `Create PIN`
- PIN `2580` was entered and confirmed.
- The app then required `Enter Parent PIN`; entering `2580` unlocked the flow.
- Accessibility disclosure screen showed:
  - `Accessibility Permission`
  - disclosure text stating the app detects YouTube Shorts and does not read messages, record screen/audio, save YouTube watch history, or send data to a server.
  - `I understand and want to enable protection`

## Accessibility Service

The disclosure button opened Android Accessibility settings.

Settings showed:

```text
Shorts Blocker Kids
Off
```

The service detail screen showed:

```text
Use Shorts Blocker Kids
checked=false
```

The Android permission dialog was accepted:

```text
Allow Shorts Blocker Kids to have full control of your device?
Allow
```

After accepting, Settings showed:

```text
Use Shorts Blocker Kids
checked=true
```

ADB verification:

```bash
$ADB shell settings get secure enabled_accessibility_services
$ADB shell settings get secure accessibility_enabled
$ADB shell dumpsys accessibility
```

Result:

```text
com.shortsblockerkids/com.shortsblockerkids.accessibility.ShortsBlockerAccessibilityService
1
Bound services:{Service[label=Shorts Blocker Kids, feedbackType[FEEDBACK_GENERIC], capabilities=1, eventTypes=[TYPE_VIEW_CLICKED, TYPE_WINDOW_STATE_CHANGED, TYPE_WINDOW_CONTENT_CHANGED, TYPE_VIEW_SCROLLED], notificationTimeout=120, requestA11yBtn=false]}
```

## Dashboard

After returning from Settings, dashboard showed:

```text
Dashboard
Protection ON
PIN created
Accessibility Service enabled
Runtime status active
Last detector result none
Detector Playground
```

This confirms the app refreshed Accessibility Service state on resume.

## Detector Playground

The debug-only dashboard button opened:

```text
Detector Playground
```

Non-blocking scenario:

```text
Scenario: Simulate normal YouTube video
confidence: NONE
reasons: []
wouldBlock: false
testOverlay: not requested
```

Shorts scenario:

```text
Scenario: Simulate Shorts HIGH confidence
confidence: HIGH
reasons: [reel/shorts player view id signal, vertical shorts action rail signal, fullscreen vertical layout signal, repeated vertical feed signal]
wouldBlock: true
testOverlay: not requested
```

The detector playground UI is present and the simulated detector results match expected blocking behavior.

## Debug Overlay Trigger

Action:

```text
Show test blocking overlay
```

Observed playground state after tap:

```text
testOverlay: requested; Accessibility Service must be enabled
```

The Accessibility Service was enabled and bound at this time.

Additional events were generated after the request:

- Tapped another playground scenario.
- Pressed Home.
- Launched YouTube with `monkey -p com.google.android.youtube`.

Window verification:

```bash
$ADB shell dumpsys window windows
$ADB shell uiautomator dump /sdcard/window_overlay*.xml
```

Result:

- No `TYPE_ACCESSIBILITY_OVERLAY` or blocking overlay window was visible in `dumpsys window`.
- `uiautomator` did not show `Shorts blocked. Parent PIN required.`
- The screen stayed on the app, launcher, or YouTube permission dialog depending on the event source.

Conclusion:

- The debug overlay request button is present and updates its UI state.
- Actual overlay rendering was not proven on this emulator run.

## YouTube Behavior

YouTube launched, but only the notification permission prompt was reached:

```text
Allow YouTube to send you notifications?
```

No real Shorts navigation or production detector/overlay behavior was verified.

## What Works

- Emulator booted through foreground `sg kvm`.
- APK installed successfully.
- App launched successfully.
- First-run welcome screen rendered.
- Parent PIN was created through emulator UI.
- PIN entry unlocked the app.
- Accessibility disclosure rendered and opened Android Accessibility settings.
- Accessibility Service was enabled through system UI.
- Dashboard reported PIN created, Accessibility Service enabled, and runtime status active.
- Debug Detector Playground opened.
- Debug detector fixtures produced expected non-blocking and blocking results.
- Debug overlay request button changed state to requested.

## What Remains Unproven

- Debug blocking overlay did not visibly render despite the request state and enabled Accessibility Service.
- Real YouTube Shorts detection was not tested.
- Real YouTube Shorts blocking overlay was not tested.
- Real phone behavior remains unproven.

## Files Changed

- Created `docs/SHORTS_BLOCKER_PHASE_1F_EMULATOR_UI_REPORT.md`.
