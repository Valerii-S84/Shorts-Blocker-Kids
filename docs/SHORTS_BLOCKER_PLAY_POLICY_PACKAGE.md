# Shorts Blocker Kids Play Policy Package

Status: FAIL for publication. Repository-side policy material is prepared; the July 17, 2026
Privacy URL observation is historical evidence, not current live verification.
Current candidate checks, owner-approved form answers, Play submission and the
Accessibility demo video URL remain pending. No video is recorded in this phase.

Current Play Console submission source:

```text
docs/SHORTS_BLOCKER_PLAY_CONSOLE_PREPARATION_PACKAGE.md
```

## Current Release Behavior

This package is for the current Shorts Blocker Kids build:

- Android app for blocking supported short-video surfaces.
- Local-only rules and parent PIN.
- Android Accessibility Service detects YouTube Shorts, TikTok short-video
  feed, Instagram Reels, and Facebook Reels when the corresponding supported
  app is active.
- Blocking overlay appears when a supported short-video surface is detected and
  protection is enabled.
- Parent PIN can temporarily allow access for 15, 30, or 60 minutes.
- Optional Device Admin tamper protection can make uninstall harder while admin
  is active. It is disclosed separately, does not replace Accessibility Service,
  and does not block Settings or perform hidden system actions.
- No account system.
- Subscription access follows Google Play Billing purchase state.
- No analytics.
- No ads.
- No watch history storage.
- No video, audio, screen recording, comment, URL, or message collection.
- Google Play Billing is integrated for the planned monthly subscription.
- The in-app subscription card explains monthly auto-renewing Google Play
  billing, the post-free-test requirement, and the Google Play manage/cancel
  path before launching purchase.
- The Billing SDK adds `com.android.vending.BILLING`,
  `android.permission.INTERNET`, and
  `android.permission.ACCESS_NETWORK_STATE`. The app has no
  analytics, ads, or account network flow.
- The app stores local subscription access state.
- Privacy Policy text is available in the app from the dashboard.

## Official Policy Sources Checked

Rechecked on 2026-10-15: [AccessibilityService declaration and video guidance](https://support.google.com/googleplay/android-developer/answer/10964491?hl=en), [Data safety requirements](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en), [target audience](https://support.google.com/googleplay/android-developer/answer/9867159?hl=en), [Families policies](https://support.google.com/googleplay/android-developer/answer/9893335?hl=en) and [content rating requirements](https://support.google.com/googleplay/android-developer/answer/9859655?hl=en). This is a documentation check, not Google approval of the app or declarations.

[Review preparation](https://support.google.com/googleplay/android-developer/answer/9859455?hl=en-en) also requires instructions for restricted app areas and an active, app-specific Privacy Policy URL linked in the app and listing where required. Recheck access behind the PIN/paywall and current public URLs; the historical hosting observation does not prove present availability. **Local preparation: FAIL. Overall publication readiness: FAIL.** The reserved final frozen-source phase started 2026-10-15T22:53:35Z (16 October in Europe/Berlin). No automatic retry or source change followed. Ktlint failed; fresh unit XML is **594 / 1 / 0 / 0** (tests/failures/errors/skipped). Debug/release builds and both lint variants completed. Coverage verification/report did not execute after the failing unit prerequisite; no new achieved ratios are claimed. The entire 32-test instrumentation task started once without a filter, but terminated with only **13 completed XML tests / 0 failures / 0 errors / 0 skipped**, leaving **19 without results**. This is incomplete, never a whole-suite pass. Both Pevisor reviews remain unavailable and unfulfilled. Provenance is in [AAB Readiness](SHORTS_BLOCKER_AAB_RELEASE_READINESS.md); policy forms and demo remain preparation material.

For this non-accessibility-tool app, keep a separate in-app disclosure explaining Accessibility access/use and require affirmative consent. The declaration video must show app launch, the complete disclosure (slowly scroll if necessary), consent and permission granting, refusal and a later repeated request, and a core Accessibility feature. Add captions/voice-over when the behavior is unclear. Existing service metadata text cannot replace the in-app disclosure. Keep the recording pending until the verified candidate is available.

Data Safety must include applicable third-party SDK handling; local-only detection does not settle Billing SDK answers. Google exempts apps exclusively on the internal testing track from the Data safety section, while closed/open/production tracks require the form. Final answers remain the owner's responsibility after checking the exact shipped SDKs, permissions and transfers. Target age groups must reflect the actual intended audience; including children invokes Families requirements. Do not infer an 18+ answer or an IARC rating from parent configuration alone.

- Google Play AccessibilityService API policy:
  <https://support.google.com/googleplay/android-developer/answer/10964491>
- Google Play Permissions and APIs that Access Sensitive Information:
  <https://support.google.com/googleplay/android-developer/answer/16558241>
- Android DeviceAdminReceiver documentation:
  <https://developer.android.com/reference/android/app/admin/DeviceAdminReceiver>
- Google Play User Data policy:
  <https://support.google.com/googleplay/android-developer/answer/9888076>
- Google Play Data safety guidance:
  <https://support.google.com/googleplay/android-developer/answer/10787469>
- Google Play User Data policy:
  <https://support.google.com/googleplay/android-developer/answer/10144311>
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
- Device Admin tamper protection must remain an explicit parent/guardian flow
  and must not be used to hide settings access or perform undisclosed system
  actions.

## Play Accessibility Declaration Draft

Use case:

```text
App functionality.
```

Core feature:

```text
Shorts Blocker Kids is a parental digital wellbeing app. It uses Android
AccessibilityService to detect supported short-video surfaces on the child's
phone: YouTube Shorts, TikTok short-video feed, Instagram Reels, and Facebook
Reels. TikTok regional package com.ss.android.ugc.trill and Facebook Lite are
not supported. When a supported short-video surface is detected and protection
is enabled, the app shows a blocking overlay. A parent PIN can temporarily
allow access.
```

Why AccessibilityService is needed:

```text
Android does not provide a normal app API that tells this app when these
short-video surfaces are open across supported apps. AccessibilityService is
used locally only while a supported package is active so the app can inspect UI
state, detect the protected surface, and show the blocking overlay.
```

Data collection or sharing through AccessibilityService:

```text
No. Shorts Blocker Kids does not collect or share personal or sensitive data
using AccessibilityService. Detection runs locally on the device. The app does
not collect or send child data, watch history, video data, video titles, URLs,
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
3. Select protected apps: YouTube, TikTok, Instagram, and Facebook.
4. Show the full Accessibility disclosure, slowly scrolling if required.
5. Tap `Not now` and show that Android Accessibility settings do not open.
6. Open setup again and show the disclosure again.
7. Tap `I agree and continue`.
8. Tap `Open Settings`.
9. Enable `Shorts Blocker Kids Protection` in Android Accessibility settings.
10. Return to the app and show the green setup checklist and protection status.
11. Open a normal YouTube video and show that it is not blocked.
12. Open YouTube Shorts and show that the blocking overlay appears.
13. Open TikTok short-video feed and show that the blocking overlay appears.
14. Open Instagram Reels and show that the blocking overlay appears.
15. Open Facebook Reels and show that the blocking overlay appears.
16. Tap the current localized close-video action and show the verified intended
    exit and overlay dismissal. Confirm the exact label against the final build.
17. Open a supported short-video surface again.
18. Tap `Enter PIN`.
19. Enter the parent PIN.
20. Select 15 minutes and show temporary allow behavior.
21. Repeat or briefly show the 30 and 60 minute options.
22. Open the Device Admin tamper protection screen, show the separate
    disclosure, and show the Android Device Admin confirmation screen.
23. End by showing the dashboard status and the honest limitation text.

Add captions if the system Accessibility settings flow or overlay behavior is
not obvious in the recording.

## Data Safety Draft

Current answer direction for the app:

- Data collection: Google Play Billing processes subscription purchases. No
  child data or supported app activity is collected by the app.
- Data sharing: no sale or sharing of user data by the app. Google Play Billing
  processes payments under Google Play terms.
- Location: not collected.
- Personal info: not collected.
- Financial info: payment data is processed by Google Play; this app does not
  collect payment card data, order IDs, billing addresses, or payment account
  details. Local subscription access state is used for app functionality.
- Health and fitness: not collected.
- Messages: not collected.
- Photos and videos: not collected.
- Audio files or recordings: not collected.
- Files and docs: not collected.
- Calendar: not collected.
- Contacts: not collected.
- Web browsing: not collected.
- App activity: no supported app activity, watch history, video titles, URLs,
  comments, messages, or browsing history collected.
- Diagnostics: no analytics or crash SDK is currently integrated.
- Account creation: no account system.
- Data deletion: no account data exists on a server. Local settings and PIN
  metadata can be removed by clearing app data or uninstalling the app.

Revisit this section before release if crash reporting, analytics, support
forms, or any non-billing app network flow is added.

## Privacy Policy Draft

Use [Privacy Draft](SHORTS_BLOCKER_PRIVACY_POLICY_DRAFT.md), aligned with website/src/pages/privacy.mjs. Canonical publication: https://shortsblockerkids.de/privacy; support: https://shortsblockerkids.de/support. Publisher Valerii Serputko; privacy/support contact svalerii535@gmail.com. Public availability and the exact uploaded-artifact/SDK disclosures require a fresh check. [Website README](../website/README.md) owns deployment steps.

## Store Listing Draft

Use [Console Package — Store Listing](SHORTS_BLOCKER_PLAY_CONSOLE_PREPARATION_PACKAGE.md#store-listing). Preserve parent-facing scope, normal-screen availability, no monitoring/accounts and honest Accessibility/tamper limits; do not claim production-verified TikTok/Reels without device evidence.

## App Access Instructions For Review

Use [Console Package — App Access](SHORTS_BLOCKER_PLAY_CONSOLE_PREPARATION_PACKAGE.md#app-access-and-tester-instructions), including no-account setup, non-weak PIN, decline/consent, protected surfaces, temporary allow, expiry/Billing and separate Device Admin flow. No hidden credentials or alternate payment path is required.

## External Submission Blockers

- Submit the verified `https://shortsblockerkids.de/privacy` URL in Play
  Console.
- Use the same public privacy/support contact: `svalerii535@gmail.com`.
- Play Console developer / publisher name for the current personal developer
  release: `Valerii Serputko`.
- Play Console Data Safety form submission.
- Play Console Accessibility declaration submission.
- Demo video recording and URL.
- Store screenshots and feature graphic.
- Final target audience and content rating answers.

## In-App Disclosure And Draft Evidence Boundary

Before Android Accessibility settings, show the prominent purpose/data-use disclosure for the supported four-platform scope. Require affirmative I agree and continue; Not now/decline keeps the user in-app and does not open settings. Explain local UI inspection and blocking without private messages/history/screen recording. Device Admin consent/Android confirmation is separate and optional; it does not replace Accessibility or grant destructive device policies.

Data Safety above remains a draft. Recheck final SDK disclosures, merged permissions and the actual Console wording; local-only protection does not by itself prove every third-party Billing disclosure answer. Dated URL evidence is not current policy/hosting approval. Accessibility declaration/demo, screenshots/feature graphic, target audience/content rating and testing submissions remain external evidence gates. Details consolidated from Accessibility Policy Readiness and Play Store Readiness; retained sources do not authorize backend/Stripe work.

A 2026-10-15 live Privacy URL check could not access the route through the available web tool. Current public availability remains unverified; this is not evidence of a website outage. No production website change or URL retry was performed.
