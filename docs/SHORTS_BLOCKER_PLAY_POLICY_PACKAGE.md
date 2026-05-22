# Shorts Blocker Kids Play Policy Package

Status: Partial. Repository-side policy package prepared; external Play Console
submission, hosted Privacy Policy URL, and demo video URL are pending.

## Current Release Behavior

This package is for the current Shorts Blocker Kids build:

- Android app for blocking YouTube Shorts only.
- Local-only rules and parent PIN.
- Android Accessibility Service detects YouTube Shorts when YouTube is active.
- Blocking overlay appears when Shorts is detected and protection is enabled.
- Parent PIN can temporarily allow access for 5, 10, or 15 minutes.
- No account system.
- No app backend.
- No analytics.
- No ads.
- No watch history storage.
- No video, audio, screen recording, comment, URL, or message collection.
- No `INTERNET` permission in the current manifest.
- Google Play Billing is not implemented in the current app.

## Official Policy Sources Checked

- Google Play AccessibilityService API policy:
  <https://support.google.com/googleplay/android-developer/answer/10964491>
- Google Play Permissions and APIs that Access Sensitive Information:
  <https://support.google.com/googleplay/android-developer/answer/9888170>
- Google Play User Data policy:
  <https://support.google.com/googleplay/android-developer/answer/9888076>
- Google Play Data safety guidance:
  <https://support.google.com/googleplay/android-developer/answer/10787469>
- Google Play Payments policy:
  <https://support.google.com/googleplay/android-developer/answer/9858738>
- Android Play Billing subscriptions:
  <https://developer.android.com/google/play/billing/subscriptions>

Policy interpretation for this app:

- The app should not set `isAccessibilityTool=true`, because it is a parental
  digital wellbeing app, not a disability support tool.
- The AccessibilityService use must be disclosed in-app before opening Android
  Accessibility settings and must require affirmative consent.
- The AccessibilityService use must be documented in the Google Play listing.
- Data Safety and Privacy Policy must match the shipped app, SDKs,
  permissions, and data handling.
- Any paid Play-distributed access to app functionality must use Google Play
  Billing.

## Play Accessibility Declaration Draft

Use case:

```text
App functionality.
```

Core feature:

```text
Shorts Blocker Kids is a parental digital wellbeing app. It uses Android
AccessibilityService to detect when the YouTube app is showing YouTube Shorts
on the child's phone. When Shorts is detected and protection is enabled, the app
shows a blocking overlay. A parent PIN can temporarily allow access.
```

Why AccessibilityService is needed:

```text
Android does not provide a normal app API that tells this app when the YouTube
Shorts surface is open. AccessibilityService is used locally only while the
YouTube package is active so the app can inspect YouTube UI state, detect
Shorts, and show the blocking overlay.
```

Data collection or sharing through AccessibilityService:

```text
No. Shorts Blocker Kids does not collect or share personal or sensitive data
using AccessibilityService. Detection runs locally on the device. The app does
not collect or send child data, YouTube history, video data, video titles, URLs,
account names, comments, messages, screen recordings, audio, location,
contacts, or browsing history.
```

Accessibility tool flag:

```text
Do not declare this service as an accessibility tool. Shorts Blocker Kids is not
designed as a disability support tool and should not set isAccessibilityTool=true.
```

## Review Demo Video Script

1. Open Shorts Blocker Kids from the launcher.
2. Create a parent PIN.
3. Show the full Accessibility disclosure.
4. Tap `Not now` and show that Android Accessibility settings do not open.
5. Open setup again and show the disclosure again.
6. Tap `I agree and continue`.
7. Tap `Open Settings`.
8. Enable `Shorts Blocker Kids Protection` in Android Accessibility settings.
9. Return to the app and show protection status.
10. Open a normal YouTube video and show that it is not blocked.
11. Open YouTube Shorts and show that the blocking overlay appears.
12. Tap `Exit to phone home` and show that the device returns to the home
    screen.
13. Open YouTube Shorts again.
14. Tap `Enter PIN`.
15. Enter the parent PIN.
16. Select 5 minutes and show temporary allow behavior.
17. Repeat or briefly show the 10 and 15 minute options.
18. End by showing the dashboard status and the honest limitation text.

Add captions if the system Accessibility settings flow or overlay behavior is
not obvious in the recording.

## Data Safety Draft

Current answer direction for the app itself:

- Data collection: No user data collected by the app.
- Data sharing: No user data shared by the app.
- Location: not collected.
- Personal info: not collected.
- Financial info: not collected by this app in the current build.
- Health and fitness: not collected.
- Messages: not collected.
- Photos and videos: not collected.
- Audio files or recordings: not collected.
- Files and docs: not collected.
- Calendar: not collected.
- Contacts: not collected.
- App activity: no YouTube watch history, video titles, URLs, or browsing
  history collected.
- Web browsing: not collected.
- App info and performance: no analytics or crash SDK is currently integrated.
- Device or other IDs: no app-specific tracking ID is currently collected or
  transmitted by this app.
- Account creation: no account system.
- Data deletion: no account data exists on a server. Local settings and PIN
  metadata can be removed by clearing app data or uninstalling the app.

Revisit this section before release if Google Play Billing, backend purchase
verification, crash reporting, analytics, support forms, or any network
permission is added.

## Privacy Policy Draft

The hosted Privacy Policy must be public, active, non-PDF, non-geofenced, and
must include the developer or company name plus a privacy contact.

Draft text:

```text
# Privacy Policy

Effective date: May 22, 2026

Shorts Blocker Kids is a parental digital wellbeing app that helps parents
block YouTube Shorts on a child's Android phone.

The app works locally on the device. It does not require an account and does
not use an app server in the current release.

Shorts Blocker Kids uses Android AccessibilityService to detect when the
YouTube app is showing YouTube Shorts. When Shorts is detected and protection is
enabled, the app shows a blocking overlay. A parent PIN can temporarily allow
access.

The app stores protection settings and parent PIN hash metadata locally on the
device. The parent PIN is not stored as plain text.

Shorts Blocker Kids does not collect, transmit, sell, or share child data,
YouTube watch history, video titles, URLs, account names, comments, messages,
screen recordings, audio, location, contacts, browsing history, or raw
Accessibility tree dumps.

The current app has no analytics, no ads, no account system, no backend, and no
cloud sync.

If Google Play Billing is added in a future release, Google may process payment
and subscription data under Google Play terms. This policy and the Google Play
Data Safety form must be updated before that release.

Blocking works only when Protection is ON and Android Accessibility Service is
active. If that permission is turned off or the app is removed, blocking stops.

To remove local app data, uninstall Shorts Blocker Kids or clear the app data in
Android settings.

Privacy contact: TODO before submission - add the public privacy contact.
Developer / publisher: TODO before submission - add the Play listing entity.
```

## Store Listing Draft

Short description:

```text
Block YouTube Shorts on a child's Android phone with a parent PIN.
```

Full description:

```text
Shorts Blocker Kids helps parents block YouTube Shorts on a child's Android
phone.

Set a parent PIN, enable protection, and turn on the Android Accessibility
Service. When YouTube Shorts is detected, Shorts Blocker Kids shows a blocking
screen. A parent can temporarily allow access with the PIN.

What the app does:
- blocks YouTube Shorts;
- keeps normal YouTube videos available;
- uses a parent PIN for protection changes and temporary allow;
- stores rules locally on the phone.

Privacy:
- no account;
- no app server;
- no analytics;
- no ads;
- no YouTube watch history storage;
- no video, audio, screen recording, messages, location, contacts, or browsing
  history collection.

Important limitation:
Shorts Blocker Kids works when Protection is ON and Android Accessibility
Service is active. If that permission is turned off or the app is removed,
blocking stops.
```

## App Access Instructions For Review

```text
No account is required.

To test:
1. Install the app.
2. Open Shorts Blocker Kids.
3. Create a parent PIN.
4. Review the Accessibility disclosure.
5. Tap I agree and continue.
6. Open Android Accessibility settings from the app.
7. Enable Shorts Blocker Kids Protection.
8. Return to the app and confirm Protection is ON.
9. Open YouTube and play a normal video; it should not be blocked.
10. Open YouTube Shorts; the blocking overlay should appear.
11. Tap Exit to phone home to leave Shorts.
12. Open Shorts again, tap Enter PIN, enter the parent PIN, and choose 5, 10,
    or 15 minutes for temporary allow.
```

## External Submission Blockers

- Hosted Privacy Policy URL.
- Public privacy contact email or contact form.
- Final developer / publisher entity name.
- Play Console Data Safety form submission.
- Play Console Accessibility declaration submission.
- Demo video recording and URL.
- Store screenshots and feature graphic.
- Final target audience and content rating answers.
