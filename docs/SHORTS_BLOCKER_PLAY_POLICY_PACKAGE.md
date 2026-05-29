# Shorts Blocker Kids Play Policy Package

Status: Partial. Repository-side policy package prepared; external Play Console
submission, hosted Privacy Policy URL, and demo video URL are pending.

## Current Release Behavior

This package is for the current Shorts Blocker Kids build:

- Android app for blocking supported short-video surfaces.
- Local-only rules and parent PIN.
- Android Accessibility Service detects YouTube Shorts, TikTok, Instagram
  Reels, and Facebook Reels when the corresponding supported app is active.
- Blocking overlay appears when a supported short-video surface is detected and
  protection is enabled.
- Parent PIN can temporarily allow access for 5, 10, or 15 minutes.
- No account system.
- Billing backend only for Google Play purchase verification and RTDN.
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
  `android.permission.ACCESS_NETWORK_STATE`; production-configured builds may
  also use network access for billing backend verification. The app has no
  analytics, ads, or account network flow.
- The app stores local subscription entitlement status and last verification
  timestamp. The backend stores only billing technical entitlement records and
  hashed purchase tokens.
- Privacy Policy text is available in the app from the dashboard.

## Official Policy Sources Checked

- Google Play AccessibilityService API policy:
  <https://support.google.com/googleplay/android-developer/answer/10964491>
- Google Play Permissions and APIs that Access Sensitive Information:
  <https://support.google.com/googleplay/android-developer/answer/9888170>
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

## Play Accessibility Declaration Draft

Use case:

```text
App functionality.
```

Core feature:

```text
Shorts Blocker Kids is a parental digital wellbeing app. It uses Android
AccessibilityService to detect supported short-video surfaces on the child's
phone. YouTube Shorts is supported. TikTok main, Instagram Reels, and Facebook
Reels have code-level detectors and still need real-device QA. TikTok regional
package com.ss.android.ugc.trill and Facebook Lite are not supported. When a
supported short-video surface is detected and protection is enabled, the app
shows a blocking overlay. A parent PIN can temporarily allow access.
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
3. Show the full Accessibility disclosure.
4. Tap `Not now` and show that Android Accessibility settings do not open.
5. Open setup again and show the disclosure again.
6. Tap `I agree and continue`.
7. Tap `Open Settings`.
8. Enable `Shorts Blocker Kids Protection` in Android Accessibility settings.
9. Return to the app and show protection status.
10. Open a normal YouTube video and show that it is not blocked.
11. Open YouTube Shorts and show that the blocking overlay appears.
12. Run and record the TikTok main, Instagram Reels, and Facebook Reels
    real-device QA scenarios before claiming full support for those surfaces.
13. Tap `Exit to phone home` and show that the device returns to the home
    screen.
14. Open a supported short-video surface again.
15. Tap `Enter PIN`.
16. Enter the parent PIN.
17. Select 5 minutes and show temporary allow behavior.
18. Repeat or briefly show the 10 and 15 minute options.
19. End by showing the dashboard status and the honest limitation text.

Add captions if the system Accessibility settings flow or overlay behavior is
not obvious in the recording.

## Data Safety Draft

Current answer direction for the app:

- Data collection: yes for billing technical data only when Google Play Billing
  and backend verification are used. No child data or supported app activity is
  collected.
- Data sharing: no sale or sharing of user data by the app. Google Play Billing
  processes payments under Google Play terms.
- Location: not collected.
- Personal info: not collected.
- Financial info: payment data is processed by Google Play; this app does not
  collect payment card data, order IDs, billing addresses, or payment account
  details. Subscription entitlement state and purchase token verification are
  used only for app functionality.
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
- Device or other IDs: declare a random app installation ID if backend
  verification is enabled. It is not a hardware ID, advertising ID, account
  name, phone number, or child identifier.
- Diagnostics: no analytics or crash SDK is currently integrated.
- Account creation: no account system.
- Data deletion: no account data exists on a server. Local settings and PIN
  metadata can be removed by clearing app data or uninstalling the app.
  Billing entitlement records are retained only as needed for subscription
  verification, fraud prevention, RTDN idempotency, and entitlement support.

Revisit this section before release if crash reporting, analytics, support
forms, or any non-billing app network flow is added.

## Privacy Policy Draft

Standalone draft:

```text
docs/SHORTS_BLOCKER_PRIVACY_POLICY_DRAFT.md
```

The hosted Privacy Policy must be public, active, non-PDF, non-geofenced, and
must include the developer or company name plus a privacy contact.

Draft text:

```text
# Privacy Policy

Effective date: May 24, 2026

Shorts Blocker Kids is a parental digital wellbeing app that helps parents
block supported short-video surfaces on a child's Android phone.

The app works locally on the device for protection behavior. It does not
require an account. For production subscription verification, the app may use a
billing backend limited to Google Play purchase verification and entitlement
status.

Shorts Blocker Kids uses Android AccessibilityService to detect when supported
short-video surfaces are open. YouTube Shorts is supported. TikTok main,
Instagram Reels, and Facebook Reels have code-level detectors and still need
real-device QA. TikTok regional package com.ss.android.ugc.trill and Facebook
Lite are not supported. When a supported short-video surface is detected and
protection is enabled, the app shows a blocking overlay. A parent PIN can
temporarily allow access.

The app stores protection settings and parent PIN hash metadata locally on the
device. The parent PIN is not stored as plain text.

Shorts Blocker Kids does not collect, transmit, sell, or share child data,
supported app activity, watch history, video titles, URLs, account names,
comments, messages, screen recordings, audio, location, contacts, browsing
history, or raw Accessibility tree dumps.

The current app has no analytics, no ads, no account system, and no cloud sync.

Subscriptions are handled through Google Play Billing. Google Play may process
payment and subscription data under Google Play terms. This app stores local
subscription entitlement status and the last verification timestamp. If backend
verification is enabled, the backend stores only billing technical entitlement
records, processed RTDN message IDs, and a hashed purchase token.

Blocking works only when Protection is ON and Android Accessibility Service is
active. If that permission is turned off or the app is removed, blocking stops.

To remove local app data, uninstall Shorts Blocker Kids or clear the app data in
Android settings.

Privacy contact: svalerii535@gmail.com
Developer / publisher: Valerii Serputko
```

## Store Listing Draft

Short description:

```text
Block supported short-video surfaces on a child's Android phone with a parent
PIN.
```

Full description:

```text
Shorts Blocker Kids helps parents block supported short-video surfaces on a
child's Android phone.

Set a parent PIN, enable protection, and turn on the Android Accessibility
Service. YouTube Shorts is supported. TikTok main, Instagram Reels, and
Facebook Reels have code-level detectors and still need real-device QA. TikTok
regional package com.ss.android.ugc.trill and Facebook Lite are not supported.
When a supported short-video surface is detected, Shorts Blocker Kids shows a
blocking screen. A parent can temporarily allow access with the PIN.

What the app does:
- blocks YouTube Shorts;
- includes code-level detectors for TikTok main, Instagram Reels, and Facebook
  Reels that still require real-device QA before full support is claimed;
- keeps normal YouTube videos and non-target app screens available;
- uses a parent PIN for protection changes and temporary allow;
- stores rules locally on the phone.

Privacy:
- no account;
- billing backend only for Google Play subscription verification;
- no analytics;
- no ads;
- no watch history storage;
- no supported app activity, video titles, URLs, account names, comments,
  messages, video, audio, screen recording, location, contacts, or browsing
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
11. For TikTok main, Instagram Reels, and Facebook Reels, run the real-device
    QA scenarios before claiming full support.
12. Tap Exit to phone home to leave the blocked surface.
13. Open a supported short-video surface again, tap Enter PIN, enter the parent
    PIN, and choose 5, 10, or 15 minutes for temporary allow.
```

## External Submission Blockers

- Hosted Privacy Policy URL.
- Hosted Privacy Policy URL must use the same public privacy contact:
  `svalerii535@gmail.com`.
- Play Console developer / publisher name for the current personal developer
  release: `Valerii Serputko`.
- Play Console Data Safety form submission.
- Play Console Accessibility declaration submission.
- Demo video recording and URL.
- Store screenshots and feature graphic.
- Final target audience and content rating answers.
