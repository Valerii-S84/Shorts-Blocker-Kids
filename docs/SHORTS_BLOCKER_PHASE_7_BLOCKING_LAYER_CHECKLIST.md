# Shorts Blocker Kids — Phase 7 Blocking Layer Manual Checklist

Status: real-device smoke passed by user report on five devices; detailed
device matrix still pending.

## Smoke Result

On May 22, 2026, the product owner reported that this flow passed on five real
devices:

- install;
- parent PIN setup and PIN entry;
- Accessibility Service enablement;
- normal YouTube videos are not blocked;
- YouTube Shorts are blocked immediately;
- exit to phone home works;
- parent PIN temporary allow works for 5, 10, and 15 minutes.

No problems were observed in that smoke pass.

## Preconditions

- Install a debug or release build on a real Android device or emulator.
- Install YouTube with package `com.google.android.youtube`.
- Create a parent PIN in Shorts Blocker Kids.
- Accept the Accessibility disclosure.
- Enable `Shorts Blocker Kids Protection` in Android Accessibility settings.
- Confirm Protection is ON in the app.

## Blocking Checks

1. Open a non-YouTube app and confirm no blocking screen appears.
2. Open YouTube Home and confirm no blocking screen appears.
3. Open YouTube Search and confirm no blocking screen appears.
4. Open a normal YouTube video and confirm no blocking screen appears.
5. Open YouTube Shorts and confirm a full-screen Shorts Blocker Kids blocking screen appears.
6. Confirm the blocking screen says `Shorts Blocker Kids`, `Shorts blocked`, and does not look like an Android system warning.
7. Tap `Exit Shorts` and confirm Android navigates back and the blocking screen closes.
8. Open YouTube Shorts again, tap `Enter PIN`, enter the parent PIN, and confirm the temporary allow options open.
9. Choose 5 minutes and confirm the blocking screen closes while temporary allow is active.
10. Turn Protection OFF from the app and confirm any visible blocking screen closes.
11. Leave YouTube with the blocking screen visible and confirm the blocking screen closes after the app switch.

## Layout Checks

1. Repeat the Shorts blocking check in portrait.
2. Repeat the Shorts blocking check in landscape.
3. Repeat the Shorts blocking check on the smallest available device or emulator screen.
4. Confirm the title, message, and both buttons remain readable.
5. Confirm the screen can scroll if the vertical space is too small.

## Privacy And Permission Checks

1. Confirm `INTERNET` and `ACCESS_NETWORK_STATE` are used only for Google Play
   Billing and billing backend verification, not analytics or ads.
2. Confirm the manifest does not request `SYSTEM_ALERT_WINDOW`.
3. Confirm non-YouTube events only dismiss/reset the blocking state and do not scan accessibility trees.
4. Confirm no YouTube titles, URLs, comments, account names, messages, screen recordings, or audio are stored.

## Result Recording

Record:

- Device manufacturer and model.
- Android version and SDK.
- YouTube version.
- Build type tested.
- Pass/fail for each checklist item.
