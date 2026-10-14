# Shorts Blocker Kids Play Console Preparation Package

Status: Prepared locally for `shortsblockerkids.de`. Public HTTPS checks of the
Privacy, Support, Terms and root URLs on July 17, 2026 are historical evidence
recorded in the [dated deployment record](SHORTS_BLOCKER_PRODUCTION_DEPLOYMENT_2026-05-31.md),
not a current live verification. Before submission, verify the current signed AAB
and public URLs using [AAB release readiness](SHORTS_BLOCKER_AAB_RELEASE_READINESS.md)
and the [website deployment checks](../website/README.md). External Play Console
entry, Accessibility demo video URL and Play Console submission remain pending.

Date: July 17, 2026

## Current App Facts

```text
App name: Shorts Blocker Kids
Package name: com.shortsblockerkids
Version name: 0.1.0
Version code: 1
Upload AAB: pending rebuild from reviewed source
Upload AAB size: pending
Upload AAB SHA-256: pending
Upload source commit: pending
Min SDK: 26
Target SDK: 36
Category candidate: Parenting
Business model: Google Play monthly subscription
Subscription product ID: shorts_blocker_kids_monthly
Account system: none
Ads: none
Analytics: none
Release AAB status: stale local artifact must not be uploaded; rebuild from reviewed source
Public website URL: https://shortsblockerkids.de
Privacy Policy URL: https://shortsblockerkids.de/privacy
Support URL: https://shortsblockerkids.de/support
Publisher / developer name: Valerii Serputko
Public contact email: svalerii535@gmail.com
```

Current behavior to keep consistent across every Play Console answer:

- The app helps parents block supported short-video surfaces on a child's
  Android phone.
- YouTube Shorts is supported and has prior real-device smoke evidence.
- TikTok short-video feed, Instagram Reels, and Facebook Reels are included with
  detector/unit coverage and require real-device QA before they are
  production-verified.
- TikTok regional package `com.ss.android.ugc.trill`, Facebook Lite,
  Instagram Lite, YouTube TV, and YouTube Music are not supported.
- Protection depends on Android Accessibility Service and the app's Protection
  setting being enabled.
- Optional Device Admin tamper protection can make uninstall harder while admin
  is active. It is disclosed separately, does not replace Accessibility Service,
  and does not block Settings or perform hidden system actions.
- Rules and PIN hash metadata are stored locally on the device.
- The app does not collect child activity, watch history, video titles, URLs,
  account names, comments, messages, audio, screen recordings, location,
  contacts, browsing history, or raw Accessibility tree dumps.
- Subscription purchases and access state use Google Play Billing.

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
Protection in Android Accessibility settings. Select protected apps: YouTube,
TikTok, Instagram, and Facebook. When a supported short-video surface is
detected, the app shows a blocking screen. A parent can temporarily allow
access with the PIN. Optional Device Admin tamper protection can make uninstall
harder while active after a separate parent-facing disclosure.

Current app-side support:
- YouTube Shorts is supported and has prior real-device smoke evidence.
- TikTok short-video feed, Instagram Reels, and Facebook Reels are included in
  this build with detector/unit coverage and must keep the real-device QA
  caveat until the full device matrix records evidence.
- TikTok regional, Facebook Lite, Instagram Lite, YouTube TV, and YouTube Music
  are not supported.

Real-device QA caveat:
Do not submit production Play listing copy that presents TikTok, Instagram
Reels, or Facebook Reels as fully production-verified until the QA matrix in
`docs/SHORTS_BLOCKER_REAL_DEVICE_QA_CHECKLIST.md` has recorded passing evidence.
Internal testers should verify those surfaces during Play testing.

What the app does:
- blocks supported short-video surfaces when Protection is ON;
- keeps normal YouTube videos and non-target app screens available;
- uses a parent PIN for protection changes and temporary allow;
- stores protection settings locally on the phone;
- optionally uses transparent Device Admin tamper protection to make uninstall
  harder while admin is active;
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
setup, protected app selection for YouTube, TikTok, Instagram, and Facebook,
Accessibility disclosure and consent, blocking for YouTube Shorts, TikTok
short-video feed, Instagram Reels, and Facebook Reels, temporary allow options,
optional Device Admin tamper protection and Google Play Billing integration.
```

## Privacy Policy

Current privacy text: [Privacy Draft](SHORTS_BLOCKER_PRIVACY_POLICY_DRAFT.md); website source website/src/pages/privacy.mjs. Canonical https://shortsblockerkids.de/privacy and https://shortsblockerkids.de/support. Publisher Valerii Serputko, contact svalerii535@gmail.com. [Website README](../website/README.md) owns hosting instructions; [Deployment Evidence](SHORTS_BLOCKER_PRODUCTION_DEPLOYMENT_2026-05-31.md) contains dated observations, not a fresh live check.

## Data Safety Draft Answers

Use the authoritative [Data Safety Draft](SHORTS_BLOCKER_PLAY_POLICY_PACKAGE.md#data-safety-draft). Check it against the final uploaded AAB, merged manifest, SDK/version disclosures and current Console questions before submission. Local protection settings and paid-access state are stored on-device; no own server receives child/app activity. Google Play processes purchases under its rules. No current form submission or policy acceptance is proved here.

## Accessibility Declaration Draft

Use the [Play Accessibility Declaration Draft and Demo Script](SHORTS_BLOCKER_PLAY_POLICY_PACKAGE.md). Consent must precede settings enablement, decline must not open settings, all four protected packages need accurate scope and real-device evidence, and Device Admin has separate optional disclosure/confirmation. No declaration/demo submission was completed here.

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

Final target age selection remains pending owner/legal review of the actual
audience, applicable Families obligations and SDK/data disclosures. The 18+
answer above is a draft, not an approved final form answer.

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
3. Create a non-weak 4–6 digit parent PIN with matching confirmation.
4. Select protected apps: YouTube, TikTok, Instagram, and Facebook.
5. Read the Accessibility disclosure.
6. Tap Not now once to verify Android Accessibility settings do not open.
7. Start setup again, tap I agree and continue, then open Android
   Accessibility settings.
8. Enable Shorts Blocker Kids Protection.
9. Return to the app and confirm protection status is active.
10. Open YouTube and play a normal video; it should not be blocked.
11. Open YouTube Shorts; the blocking overlay should appear.
12. Open TikTok short-video feed, Instagram Reels, and Facebook Reels; the
    blocking overlay should appear for each protected surface.
13. Tap Exit to phone home.
14. Open a protected short-video surface again, tap Enter PIN, enter the PIN
    created in step 3,
    and choose a 15, 30, or 60 minute temporary allow option.
15. Optionally open Tamper Protection, read the separate disclosure, and enable
    Device Admin from the Android confirmation screen.

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

- Build release AAB from reviewed source.
- Confirm `docs/SHORTS_BLOCKER_AAB_RELEASE_READINESS.md` records the AAB hash.
- Re-check the verified Privacy Policy URL
  `https://shortsblockerkids.de/privacy` before final Play submission.
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
- Accessibility disclosure decline and accept paths.
- YouTube normal video not blocked.
- YouTube Shorts blocked.
- TikTok short-video feed blocked.
- Instagram Reels blocked.
- Facebook Reels blocked.
- Parent PIN temporary allow 15, 30, and 60 minutes.
- Optional Device Admin tamper protection disclosure and enable flow.

Evidence to record:

- AAB SHA-256;
- tester account domain;
- device model and Android version;
- Play Store version;
- app version name/code;
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
  temporary allow, Billing, and restore scenarios.
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
- TikTok short-video feed, Instagram Reels, and Facebook Reels positive
  blocking evidence;
- unsupported packages remain not claimed;
- Accessibility Service disable stops blocking and dashboard reports inactive
  protection;
- Device Admin disable warning appears when admin is turned off;
- no duplicate overlay, flicker, or Back/Home loop;
- Billing scenarios run on the Play-installed internal testing build.

## Submission Blockers

- A Play-uploaded release AAB from current source is not yet accepted by Play Console.
- Accessibility demo video URL is TBD.
- Play Console Data Safety form is not submitted.
- Play Console Accessibility declaration is not submitted.
- Content Rating questionnaire is not submitted.
- Target Audience and Content section is not submitted.
- Internal testing Play install/Billing evidence is pending.
- Closed testing is pending if required by the developer account.
- Full real-device QA matrix is pending.

## Artifact And Rollout Ownership

[AAB Readiness](SHORTS_BLOCKER_AAB_RELEASE_READINESS.md) owns fresh provenance, technical/coverage gates and staged rollout; [Device QA](SHORTS_BLOCKER_REAL_DEVICE_QA_CHECKLIST.md) owns the full matrix and dated smoke limitations; [Billing Config](SHORTS_BLOCKER_PLAY_CONSOLE_BILLING_CONFIG.md) and [Billing Testing](SHORTS_BLOCKER_PLAY_BILLING_INTERNAL_TEST_RUNBOOK.md) own product/setup and BIL-01…BIL-12. Existing target-audience 18+ and content-rating directions are drafts, not approved answers; check the actual account/current Console, including Families and closed-testing conditions, before submission. Historical Roadmap durations/backend gates do not replace that check.
