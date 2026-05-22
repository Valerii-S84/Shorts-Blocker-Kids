# Shorts Blocker Kids Accessibility Policy Readiness

Status: Draft package for Play review preparation.

## Scope

This document covers the AccessibilityService policy package for the current
local-only Android app. It does not cover Billing, Stripe, backend, analytics,
ads, profiles, cloud sync, schedules, TikTok/Reels blocking, or device-owner
mode.

## In-App Prominent Disclosure

Before opening Android Accessibility settings, the app must show a prominent
disclosure that says:

- Shorts Blocker Kids uses Android Accessibility Service.
- The service detects when YouTube Shorts is open.
- The app shows a blocking overlay when Shorts is blocked.
- Parent PIN is used for temporary allow access.
- The app does not collect or send child data, YouTube history, video data,
  screen recordings, audio, messages, location, contacts, or browsing history.
- There is no app server, account, analytics, or ads in the current app.
- Rules and PIN protection stay locally on the phone.

The consent action must be separate and affirmative: `I agree and continue`.
The decline action must keep the user in the app and must not open Android
Accessibility settings.

## Play Accessibility Declaration Answers

Recommended declaration wording:

```text
Shorts Blocker Kids is a parental digital wellbeing app. It uses Android
AccessibilityService to detect when the YouTube app is showing YouTube Shorts
on the child's phone. When Shorts is detected and protection is enabled, the app
shows a blocking overlay. A parent PIN can temporarily allow access.
```

Why AccessibilityService is required:

```text
Android does not provide a normal app API for detecting the YouTube Shorts UI.
AccessibilityService is used locally to inspect YouTube UI state only when the
YouTube package is active, so the app can detect Shorts and show the blocking
overlay.
```

Data access and handling:

```text
The app does not collect, transmit, sell, or share child data, YouTube history,
video data, screen recordings, audio, messages, location, contacts, or browsing
history. The app has no backend, no account system, no analytics, and no ads.
Settings and PIN hash metadata are stored locally on the device.
```

Accessibility tool positioning:

```text
Do not mark isAccessibilityTool=true. Shorts Blocker Kids is not positioned as
an accessibility tool for people with disabilities. It is a parental digital
wellbeing and screen-control tool.
```

## Review Demo Video Script

1. Open Shorts Blocker Kids from the launcher.
2. Create the parent PIN.
3. Show the full Accessibility disclosure text.
4. Tap `Not now` and show that Android Accessibility settings do not open.
5. Open setup again, show the disclosure, and tap `I agree and continue`.
6. Tap `Open Settings`.
7. In Android Accessibility settings, enable `Shorts Blocker Kids Protection`.
8. Return to the app and show protection status.
9. Open YouTube Shorts.
10. Show the blocking overlay.
11. Tap the parent PIN action.
12. Enter the parent PIN and select a temporary allow duration.
13. Return to YouTube and show that temporary allow works.
14. Wait or explain that temporary allow expires and blocking resumes.

If the Android Accessibility settings screen or overlay behavior is not obvious
in the recording, add captions that identify each step.

## Privacy Policy Notes

The Privacy Policy should state:

- The app works locally on the phone.
- The app has no account system and no app server.
- Protection rules are stored on the device.
- Parent PIN is stored as hash and salt metadata, not plain text.
- The app uses AccessibilityService to detect YouTube Shorts locally and show a
  blocking overlay.
- The app does not collect or send child data, YouTube history, video data,
  screen recordings, audio, messages, location, contacts, or browsing history.
- The app does not use analytics or ads in the current release.
- If Google Play Billing is added later, Google may process payment data under
  Google Play terms; the app must document the Billing behavior separately.

## Data Safety Notes

For the current local-only app:

- Data collection by this app: no collection for child data, YouTube activity,
  video data, screen recordings, audio, messages, location, contacts, or
  browsing history.
- Data sharing by this app: no sharing for child data, YouTube activity, video
  data, screen recordings, audio, messages, location, contacts, or browsing
  history.
- Device storage: local settings and PIN hash metadata stay on the device.
- Network: the app should not declare `INTERNET` unless a future approved scope
  explicitly requires it.
- Billing: not implemented in the current app. Revisit Data Safety before any
  Play Billing release.

## Current Repo Evidence

- `app/src/main/AndroidManifest.xml` binds
  `ShortsBlockerAccessibilityService` with
  `android.permission.BIND_ACCESSIBILITY_SERVICE`.
- `app/src/main/res/xml/shorts_blocker_accessibility_service.xml` does not set
  `android:isAccessibilityTool="true"`.
- `app/src/main/java/com/shortsblockerkids/feature/onboarding/AccessibilityDisclosureScreen.kt`
  contains the in-app disclosure and decline action.
- `app/src/main/java/com/shortsblockerkids/feature/onboarding/AccessibilityPermissionFlow.kt`
  gates Android Accessibility settings behind affirmative consent.
- `app/src/test/java/com/shortsblockerkids/feature/onboarding/AccessibilityPermissionFlowTest.kt`
  covers disclosure-before-permission, decline, consent gating, and setup bypass
  prevention.

## Not Yet Completed Outside The Repo

- Final hosted Privacy Policy URL.
- Final Play Console Data Safety form submission.
- Final Play Console Accessibility declaration submission.
- Recorded and uploaded Play review demo video.
