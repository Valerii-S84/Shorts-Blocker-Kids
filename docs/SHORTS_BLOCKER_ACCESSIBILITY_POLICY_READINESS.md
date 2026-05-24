# Shorts Blocker Kids Accessibility Policy Readiness

Status: Partial. Repository-side Play review package is prepared; external
Play Console submission and demo video URL are pending.

## Scope

This document covers the AccessibilityService policy package for the current
Android app. Google Play Billing and a billing-only backend verification module
are integrated for subscription entitlement, but this document does not cover
Stripe, analytics, ads, profiles, cloud sync, schedules, or device-owner mode.

## Official Policy Sources Checked

- Google Play AccessibilityService API policy:
  <https://support.google.com/googleplay/android-developer/answer/10964491>
- Google Play Permissions and APIs that Access Sensitive Information:
  <https://support.google.com/googleplay/android-developer/answer/9888170>
- Google Play User Data policy:
  <https://support.google.com/googleplay/android-developer/answer/9888076>
- Google Play Data safety guidance:
  <https://support.google.com/googleplay/android-developer/answer/10787469>

Current interpretation:

- Do not set `isAccessibilityTool=true`.
- Keep the separate in-app prominent Accessibility disclosure.
- Require affirmative consent before opening Android Accessibility settings.
- Document AccessibilityService use in the Google Play listing.
- Keep Data Safety and Privacy Policy consistent with the shipped build.

## In-App Prominent Disclosure

Before opening Android Accessibility settings, the app must show a prominent
disclosure that says:

- Shorts Blocker Kids uses Android Accessibility Service.
- The service detects supported short-video surfaces. YouTube Shorts is
  supported. TikTok main, Instagram Reels, and Facebook Reels have code-level
  detectors and still need real-device QA. TikTok regional package
  `com.ss.android.ugc.trill` and Facebook Lite are not supported.
- The app shows a blocking overlay when protected short-video surfaces are
  blocked.
- Parent PIN is used for temporary allow access.
- The app does not collect or send child data, watch history, video data,
  video titles, URLs, account names, comments, screen recordings, audio,
  messages, location, contacts, browsing history, or raw Accessibility tree
  dumps.
- There is no account system, analytics, or ads in the current app.
- Backend behavior is limited to Google Play billing verification and RTDN.
- Rules and PIN protection stay locally on the phone.

The consent action must be separate and affirmative: `I agree and continue`.
The decline action must keep the user in the app and must not open Android
Accessibility settings.

## Play Accessibility Declaration Answers

Recommended declaration wording:

```text
Shorts Blocker Kids is a parental digital wellbeing app. It uses Android
AccessibilityService to detect supported short-video surfaces on the child's
phone. YouTube Shorts is supported. TikTok main, Instagram Reels, and Facebook
Reels have code-level detectors and still need real-device QA. TikTok regional
package com.ss.android.ugc.trill and Facebook Lite are not supported. When a
supported short-video surface is detected and protection is enabled, the app
shows a blocking overlay. A parent PIN can temporarily allow access.
```

Why AccessibilityService is required:

```text
Android does not provide a normal app API for detecting these short-video
surfaces across supported apps. AccessibilityService is used locally only while
a supported package is active, so the app can inspect UI state, detect the
protected surface, and show the blocking overlay.
```

Data access and handling:

```text
The app does not collect, transmit, sell, or share child data, supported app
activity, watch history, video titles, URLs, account names, comments, messages,
screen recordings, audio, location, contacts, browsing history, or raw
Accessibility tree dumps. The app has no account system, analytics, or ads.
Backend behavior is limited to Google Play billing verification and RTDN.
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
9. Open YouTube Shorts and show the blocking overlay.
10. For TikTok main, Instagram Reels, and Facebook Reels, run and record the
    real-device QA scenarios before claiming full support.
11. Tap the parent PIN action.
12. Enter the parent PIN and select a temporary allow duration.
13. Return to a supported short-video surface and show that temporary allow works.
14. Wait or explain that temporary allow expires and blocking resumes.

If the Android Accessibility settings screen or overlay behavior is not obvious
in the recording, add captions that identify each step.

## Privacy Policy Notes

The Privacy Policy should state:

- The app works locally on the phone.
- The app has no account system.
- Backend behavior is limited to Google Play subscription verification and
  entitlement status.
- Protection rules are stored on the device.
- Parent PIN is stored as hash and salt metadata, not plain text.
- The app uses AccessibilityService to detect supported short-video surfaces
  locally and show a blocking overlay.
- The app does not collect or send child data, watch history, video data,
  video titles, URLs, account names, comments, screen recordings, audio,
  messages, location, contacts, browsing history, or raw Accessibility tree
  dumps.
- The app does not use analytics or ads in the current release.
- Google Play Billing may process payment and subscription data under Google
  Play terms; the app stores local subscription entitlement status and the last
  verification timestamp. The billing backend stores only billing technical
  entitlement data and hashed purchase tokens.

## Data Safety Notes

For the current app:

- Data collection by this app: billing technical data only when Google Play
  Billing and backend verification are used. No child data, supported app
  activity, video titles, URLs, account names, comments, screen recordings,
  audio, messages, location, contacts, browsing history, or raw Accessibility
  tree dumps.
- Data sharing by this app: no sale or sharing of child data, supported app
  activity, video titles, URLs, account names, comments, screen recordings,
  audio, messages, location, contacts, browsing history, or raw Accessibility
  tree dumps.
- Device storage: local settings and PIN hash metadata stay on the device.
- Network: `INTERNET` is present for Google Play Billing and production billing
  backend verification only.
- Billing: Google Play Billing and billing backend verification are integrated.
  Revisit Data Safety before any non-billing network flow is added.

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
- `app/src/main/java/com/shortsblockerkids/feature/privacy/PrivacyPolicyScreen.kt`
  makes the current local privacy policy text available inside the app.
- `docs/SHORTS_BLOCKER_REAL_DEVICE_SMOKE_REPORT.md` records user-confirmed
  real-device smoke success across five devices.
- `docs/SHORTS_BLOCKER_PLAY_POLICY_PACKAGE.md` contains Play Console draft
  answers, demo video script, Data Safety direction, Privacy Policy draft, store
  listing draft, and reviewer instructions.

## Real-Device Smoke Evidence

On May 22, 2026, the product owner reported this YouTube-focused flow passed on
five real devices:

- install;
- parent PIN setup and PIN entry;
- Accessibility Service enablement;
- normal YouTube videos are not blocked;
- YouTube Shorts are blocked immediately;
- exit to phone home works;
- parent PIN temporary allow works for 5, 10, and 15 minutes.

No problems were observed in that smoke pass. TikTok main, Instagram Reels, and
Facebook Reels still require separate real-device QA before full support is
claimed.

## Not Yet Completed Outside The Repo

- Final hosted Privacy Policy URL.
- Public privacy contact and final developer / publisher entity name.
- Final Play Console Data Safety form submission.
- Final Play Console Accessibility declaration submission.
- Recorded and uploaded Play review demo video.
- Store screenshots and feature graphic.
- Final target audience and content rating answers.
