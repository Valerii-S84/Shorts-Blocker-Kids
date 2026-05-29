# Shorts Blocker Kids Play Console Preparation Package

Status: Prepared locally. External Play Console entry, hosted Privacy Policy
URL, Accessibility demo video URL, production HTTPS backend URL, and valid
production-configured AAB are still pending.

Date: May 29, 2026

## Current App Facts

```text
App name: Shorts Blocker Kids
Package name: com.shortsblockerkids
Version name: 0.1.0
Version code: 1
Min SDK: 26
Target SDK: 36
Category candidate: Parenting
Business model: Google Play monthly subscription
Subscription product ID: shorts_blocker_kids_monthly
Account system: none
Ads: none
Analytics: none
Release AAB status: pending real HTTPS backend URL
```

Current behavior to keep consistent across every Play Console answer:

- The app helps parents block supported short-video surfaces on a child's
  Android phone.
- YouTube Shorts is supported.
- TikTok main, Instagram Reels, and Facebook Reels have code-level detectors
  and still need full real-device QA before final rollout claims.
- TikTok regional package `com.ss.android.ugc.trill`, Facebook Lite,
  Instagram Lite, YouTube TV, and YouTube Music are not supported.
- Protection depends on Android Accessibility Service and the app's Protection
  setting being enabled.
- Rules and PIN hash metadata are stored locally on the device.
- The app does not collect child activity, watch history, video titles, URLs,
  account names, comments, messages, audio, screen recordings, location,
  contacts, browsing history, or raw Accessibility tree dumps.
- Network use is limited to Google Play Billing and production billing backend
  verification/RTDN when a production backend URL is configured.

## Official Sources Checked

- Prepare app for review:
  <https://support.google.com/googleplay/android-developer/answer/9859455>
- Data Safety:
  <https://support.google.com/googleplay/android-developer/answer/10787469>
- AccessibilityService API:
  <https://support.google.com/googleplay/android-developer/answer/10964491>
- Target audience and content:
  <https://support.google.com/googleplay/android-developer/answer/9867159>
- Content ratings:
  <https://support.google.com/googleplay/android-developer/answer/9859655>
- Internal, closed, and open testing:
  <https://support.google.com/googleplay/android-developer/answer/9845334>
- New personal account testing requirements:
  <https://support.google.com/googleplay/android-developer/answer/14151465>
- App access credentials/instructions:
  <https://support.google.com/googleplay/android-developer/answer/15748846>

## Store Listing

Short description:

```text
Block supported short-video surfaces on a child's phone with a parent PIN.
```

Full description:

```text
Shorts Blocker Kids helps parents block supported short-video surfaces on a
child's Android phone.

Set a parent PIN, enable protection, and turn on Shorts Blocker Kids
Protection in Android Accessibility settings. When a supported short-video
surface is detected, the app shows a blocking screen. A parent can temporarily
allow access with the PIN.

Current support:
- YouTube Shorts is supported.
- TikTok main, Instagram Reels, and Facebook Reels are implemented in code and
  must pass final real-device QA before broad rollout claims.
- TikTok regional, Facebook Lite, Instagram Lite, YouTube TV, and YouTube Music
  are not supported.

What the app does:
- blocks supported short-video surfaces when Protection is ON;
- keeps normal YouTube videos and non-target app screens available;
- uses a parent PIN for protection changes and temporary allow;
- stores protection settings locally on the phone;
- uses Google Play Billing for the subscription.

Privacy:
- no account;
- no ads;
- no analytics;
- no cloud sync;
- no child activity, watch history, video titles, URLs, account names,
  comments, messages, audio, screen recording, location, contacts, browsing
  history, or raw Accessibility tree dump collection.

Important limitation:
Shorts Blocker Kids works when Protection is ON and Android Accessibility
Service is active. If that permission is turned off or the app is removed,
blocking stops.
```

## Release Notes

Use for the first internal/closed testing release after a valid
production-configured AAB is built:

```text
Initial Play testing release for Shorts Blocker Kids. Includes parent PIN
setup, Accessibility disclosure and consent, YouTube Shorts blocking, temporary
allow options, Google Play Billing integration, and production billing backend
readiness gates. TikTok main, Instagram Reels, and Facebook Reels remain in
real-device QA before final rollout claims.
```

Do not upload release notes that claim the backend is deployed or that the
current AAB is production-ready until the final AAB is built with the real HTTPS
backend URL.

## Privacy Policy

Local source file:

```text
docs/SHORTS_BLOCKER_PRIVACY_POLICY_DRAFT.md
```

Play Console field:

```text
Privacy Policy URL: TBD hosted HTTPS URL
```

Hosting requirements for this package:

- public HTTPS URL;
- no login, geofence, PDF-only access, or blocked crawlers;
- developer/publisher name: `Valerii Serputko`;
- privacy contact: `svalerii535@gmail.com`;
- text must match the shipped app, Billing SDK, backend behavior, and
  AccessibilityService use.

## Data Safety Draft Answers

This is a draft for Play Console. Re-check it against the final uploaded AAB,
merged manifest, SDK list, and deployed backend before submission.

Overview answers:

| Play Console question | Draft answer |
|---|---|
| Does the app collect or share any required user data types? | Yes, for Google Play Billing/backend entitlement verification only. |
| Is user data shared? | No sale or developer sharing of child data. Verify final Google Play Billing SDK disclosure before submission. |
| Is all collected user data encrypted in transit? | Yes, only after the app uses HTTPS Google/production backend endpoints. Do not submit before HTTPS backend is live. |
| Can users request deletion? | Local app data can be deleted by clearing app data/uninstalling. Server billing entitlement deletion/support requests use the privacy contact, subject to subscription, fraud prevention, RTDN idempotency, and legal retention needs. |
| Is data collection optional? | Billing/backend verification data is required for paid subscription entitlement. Accessibility detection data is processed locally and is not transmitted off device. |

Declare collected data types for production backend builds:

| Data type | Collected? | Shared? | Purpose | Notes |
|---|---:|---:|---|---|
| Financial info: purchase history | Yes | No developer sale/sharing | App functionality, payment entitlement, fraud prevention/security | Google Play purchase token/product entitlement is used to verify subscription access. No card, bank, billing address, or payment account data is collected by this app/backend. |
| Device or other IDs | Yes | No | App functionality, fraud prevention/security | Random app installation ID for entitlement lookup. Not advertising ID, hardware ID, phone number, account name, or child identity. |

Do not declare these as collected by the app/backend unless behavior changes:

- location;
- personal info;
- health and fitness;
- messages;
- photos or videos;
- audio files or recordings;
- files and docs;
- calendar;
- contacts;
- web browsing;
- supported app activity;
- watch history;
- video titles;
- URLs;
- account names;
- comments;
- raw Accessibility tree dumps;
- crash logs, diagnostics, or analytics.

Security/privacy flags:

```text
Data encrypted in transit: Yes, once production HTTPS backend is deployed.
Data is processed ephemerally: purchase token is used during verification; backend stores hashed purchase token and entitlement records.
Data is required: Yes for paid entitlement verification.
User account creation: No.
Ads: No.
```

## Accessibility Declaration Draft

AccessibilityService status:

```text
Do not set isAccessibilityTool=true. Shorts Blocker Kids is a parental digital
wellbeing app, not a disability support tool.
```

Use case:

```text
App functionality.
```

Core feature that requires AccessibilityService:

```text
Shorts Blocker Kids uses Android AccessibilityService to detect supported
short-video surfaces on a child's Android phone when Protection is enabled.
YouTube Shorts is supported. TikTok main, Instagram Reels, and Facebook Reels
have code-level detectors and still need full real-device QA before final
rollout claims. When a supported short-video surface is detected, the app shows
a blocking overlay. A parent PIN can temporarily allow access.
```

Why AccessibilityService is needed:

```text
Android does not provide a normal app API that tells Shorts Blocker Kids when
these short-video surfaces are open inside other apps. AccessibilityService is
used locally only while supported packages are active so the app can inspect UI
state, detect protected short-video surfaces, and show the blocking overlay.
```

Personal or sensitive data collected/shared using AccessibilityService:

```text
No. AccessibilityService processing stays local on the device. Shorts Blocker
Kids does not collect or share child data, supported app activity, watch
history, video titles, URLs, account names, comments, messages, audio, screen
recordings, location, contacts, browsing history, or raw Accessibility tree
dumps.
```

Demo video URL:

```text
TBD hosted video URL
```

Demo video must show:

1. app launch;
2. parent PIN setup;
3. full Accessibility disclosure;
4. decline path where Android Accessibility settings do not open;
5. affirmative consent path;
6. enabling `Shorts Blocker Kids Protection` in Android Accessibility settings;
7. dashboard status after enablement;
8. normal YouTube video not blocked;
9. YouTube Shorts blocked;
10. exit to phone home;
11. PIN temporary allow flow.

## Content Rating Notes

Draft questionnaire direction:

- App type: app, not game.
- Primary function: parental digital wellbeing / short-video blocking.
- No user-generated content inside Shorts Blocker Kids.
- No social features, chat, comments, public profiles, or user-to-user content.
- No gambling, contests, or simulated gambling.
- No sexual content, nudity, violence, hate, drugs, tobacco, alcohol sales, or
  weapons content in the app itself.
- No ads.
- No location sharing.
- No web browser or unrestricted web access.
- No in-app purchases outside Google Play Billing.
- Mentions YouTube, TikTok, Instagram, and Facebook only to identify supported
  or unsupported external app surfaces.

Do not pre-claim the final rating. The final rating is assigned by Play/IARC
from the questionnaire responses.

## Target Audience Notes

Recommended draft target audience answer:

```text
Target audience: parents and guardians, 18+.
```

Rationale:

```text
The app is purchased, configured, and controlled by a parent or guardian. It may
be installed on a child's Android phone, but the app is not designed as a child
entertainment app and does not offer child-directed content, ads, social
features, or account creation.
```

Important Play Console risk:

- Because the product name includes `Kids` and the app is used on a child's
  phone, Play review may treat child-safety and Families-policy consistency as
  high risk.
- If any target age group under 13 is selected, Families policy obligations may
  apply. Do not select under-13 target groups unless the owner intentionally
  accepts that policy path and rechecks ads, SDKs, data handling, store
  listing, screenshots, and privacy policy under Families requirements.
- Keep screenshots and store copy parent-facing. Do not make the listing look
  like a child entertainment app.

Target audience form dependencies:

- Ads declaration: No.
- Privacy Policy URL: must be hosted first.
- App access instructions: provide the no-account and paywall/access notes
  below.

## App Access And Tester Instructions

Play review app access field:

```text
No account is required.

To review the app:
1. Install Shorts Blocker Kids from the Play testing track.
2. Open the app.
3. Create any 4-digit parent PIN.
4. Read the Accessibility disclosure.
5. Tap Not now once to verify Android Accessibility settings do not open.
6. Start setup again, tap I agree and continue, then open Android
   Accessibility settings.
7. Enable Shorts Blocker Kids Protection.
8. Return to the app and confirm protection status is active.
9. Open YouTube and play a normal video; it should not be blocked.
10. Open YouTube Shorts; the blocking overlay should appear.
11. Tap Exit to phone home.
12. Open YouTube Shorts again, tap Enter PIN, enter the PIN created in step 3,
    and choose a 5, 10, or 15 minute temporary allow option.

The app has no username/password login. Subscription access is through Google
Play Billing. If review needs access behind the subscription paywall, use a
license tester account added to the Play Console license testing list and the
same testing track.
```

Tester setup instructions:

```text
1. Use a real Android device with Google Play installed.
2. Sign in to Google Play with the tester account.
3. Use the Play opt-in link from the internal or closed testing track.
4. Install from Google Play only; do not sideload for Billing tests.
5. If testing subscription flows, confirm the tester account is also in Play
   Console license testing.
6. Send feedback to svalerii535@gmail.com with device model, Android version,
   app version, scenario ID, and screenshots/video where useful.
```

## Internal Testing Checklist

Before internal release:

- Build release AAB with a real HTTPS `SBK_BILLING_BACKEND_BASE_URL`.
- Confirm `docs/SHORTS_BLOCKER_FINAL_RC_FREEZE_EVIDENCE.md` records the AAB
  hash and backend URL origin.
- Confirm backend `/health`, migrations, backup/restore, RTDN readiness, and
  rollback evidence are current.
- Host Privacy Policy at public HTTPS URL.
- Upload Accessibility demo video and store the URL.
- Complete store listing draft from this package.
- Complete App content drafts: Data Safety, Accessibility declaration, Target
  Audience, Content Rating, Ads = No, App Access.
- Configure Play App Signing.
- Upload AAB to internal testing.
- Create/activate subscription `shorts_blocker_kids_monthly`.
- Add license tester accounts.
- Add the same accounts to internal testing.
- Share opt-in link and tester instructions.

Internal test execution:

- Product details load.
- Purchase approved.
- Purchase canceled.
- Pending purchase approves.
- Pending purchase declines.
- Restore after reinstall or clear app data.
- Manage subscription opens Google Play.
- Cancel active subscription remains active until Play reports expiry.
- Expired subscription locks paid protection after refresh/restore.
- Backend online entitlement refresh.
- Backend offline within conservative window.
- Backend offline after conservative window.
- RTDN lifecycle event processed once.
- Accessibility disclosure decline and accept paths.
- YouTube normal video not blocked.
- YouTube Shorts blocked.
- Parent PIN temporary allow 5, 10, and 15 minutes.

Evidence to record:

- AAB SHA-256;
- tester account domain;
- device model and Android version;
- Play Store version;
- app version name/code;
- backend origin;
- scenario ID;
- pass/fail;
- screenshots or screen recordings for policy-sensitive flows.

## Closed Testing Checklist If Required

Closed testing is required if Play Console requires production access for a
newly created personal developer account. As of the official Play Console help
checked on May 29, 2026, personal developer accounts created after
November 13, 2023 must run a closed test with at least 12 opted-in testers for
14 continuous days before applying for production access.

Use closed testing when required by the account:

- Finish Play Console app setup.
- Upload the same validated AAB or a newer validated AAB.
- Create a closed testing track.
- Add at least 12 testers; use 15-20 to reduce dropout risk.
- Confirm every tester opts in and stays opted in for 14 continuous days.
- Give testers the app access and scenario instructions from this package.
- Collect feedback by email or Play private feedback.
- Track real usage across onboarding, Accessibility enablement, blocking,
  temporary allow, Billing, restore, backend, and RTDN scenarios.
- Record bugs and fixes.
- When eligible, apply for production access and summarize:
  - how testers were recruited;
  - whether testers used all main features;
  - key feedback received;
  - fixes made from testing;
  - why the app is ready for production.

If Play Console does not require closed testing for this account, keep this
section as a rollout-risk checklist before production.

## Real-Device QA Checklist

Canonical local checklist:

```text
docs/SHORTS_BLOCKER_REAL_DEVICE_QA_CHECKLIST.md
```

Minimum Play Console readiness evidence:

- one Pixel device;
- one Samsung device;
- one Xiaomi/Redmi or similar OEM with aggressive background restrictions;
- one low-end Android device if available;
- Android 12, 13, 14, 15, and current target Android version where available;
- YouTube Shorts positive blocking evidence;
- normal YouTube video non-blocking evidence;
- TikTok main, Instagram Reels, and Facebook Reels retested before claiming
  final support for those surfaces;
- unsupported packages remain not claimed;
- Accessibility Service disable stops blocking and dashboard reports inactive
  protection;
- no duplicate overlay, flicker, or Back/Home loop;
- Billing/backend scenarios run on the Play-installed internal testing build.

## Submission Blockers

- Real HTTPS backend URL and live deploy are not yet verified.
- Valid release AAB with real HTTPS backend URL is not currently present.
- Hosted Privacy Policy URL is TBD.
- Accessibility demo video URL is TBD.
- Play Console Data Safety form is not submitted.
- Play Console Accessibility declaration is not submitted.
- Content Rating questionnaire is not submitted.
- Target Audience and Content section is not submitted.
- Internal testing Play install/Billing evidence is pending.
- Closed testing is pending if required by the developer account.
- Full real-device QA matrix is pending.
