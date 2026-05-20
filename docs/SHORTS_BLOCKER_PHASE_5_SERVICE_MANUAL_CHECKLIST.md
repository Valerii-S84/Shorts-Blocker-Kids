# Shorts Blocker Kids — Phase 5 Accessibility Service Manual Checklist

Status: manual checklist prepared; device execution required.

## Preconditions

- Install a debug build on a real Android device or emulator.
- Install YouTube with package `com.google.android.youtube`.
- Create a parent PIN in Shorts Blocker Kids.
- Accept the Accessibility disclosure.
- Open Android Accessibility settings from the app.

## Checklist

1. Enable `Shorts Blocker Kids Protection` in Android Accessibility settings.
2. Return to the app and confirm the dashboard shows Accessibility Service enabled.
3. Open a non-YouTube app and confirm no blocking overlay appears.
4. Open YouTube home and confirm the service does not block ordinary home/search/library screens.
5. Open a normal YouTube video and confirm no blocking overlay appears.
6. Open YouTube Shorts and confirm the blocking overlay appears only after a Shorts detection.
7. Tap `Exit Shorts` and confirm the overlay closes and Android navigates back.
8. Tap `Enter PIN`, enter the parent PIN, choose a temporary allow duration, and confirm the overlay is not shown while allow is active.
9. Disable the Accessibility Service in Android settings and confirm blocking stops.
10. Build a release APK and confirm there are no raw accessibility-tree logs in logcat from `ShortsBlocker`.

## Privacy Checks

- Confirm the manifest does not request `INTERNET`.
- Confirm non-YouTube events only dismiss/reset the overlay state and do not scan accessibility trees.
- Confirm debug detector output is unavailable in release builds.
- Confirm no YouTube titles, URLs, comments, account names, messages, screen recordings, or audio are stored.
