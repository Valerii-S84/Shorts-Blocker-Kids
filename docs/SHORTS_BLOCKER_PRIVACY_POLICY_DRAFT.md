# Shorts Blocker Kids Privacy Policy Draft

Status: Draft aligned with website source for publication at
`https://shortsblockerkids.de/privacy`. Public privacy/support contact and
personal developer / publisher name are filled for the current Google Play
submission path. Live hosting still must be verified before Play Console
submission.

Effective date: July 17, 2026
Website source: `website/src/pages/privacy.mjs`

Public website: https://shortsblockerkids.de
Privacy Policy URL: https://shortsblockerkids.de/privacy
Support URL: https://shortsblockerkids.de/support
Support/privacy contact: svalerii535@gmail.com
Developer / publisher: Valerii Serputko

## Overview

Shorts Blocker Kids is a parental digital wellbeing app that helps parents
reduce children's exposure to supported short-video surfaces on an Android
phone. Current app-side support is:

| Platform | Status |
|---|---|
| YouTube Shorts | supported |
| TikTok short-video feed `com.zhiliaoapp.musically` | supported by code; needs real-device QA |
| TikTok regional `com.ss.android.ugc.trill` | not supported |
| Instagram Reels | supported by code; needs real-device QA |
| Facebook Reels | supported by code; needs real-device QA |
| Facebook Lite `com.facebook.lite` | not supported |

The app works locally on the device for protection behavior. It does not
require an account. Subscription access uses Google Play Billing purchase state.

## Accessibility Service

Shorts Blocker Kids uses Android AccessibilityService to detect when supported
short-video surfaces are open. When a supported surface is detected and
protection is enabled, the app shows a blocking overlay. A parent PIN can
temporarily allow access.

AccessibilityService is used only for the app's blocking function. Detection
runs locally on the device.

## Tamper Protection

Optional Device Admin tamper protection can make Shorts Blocker Kids harder to
uninstall while it is active. It does not replace AccessibilityService. It does
not block Android Settings, wipe the device, lock the screen, reset passwords,
change hidden system settings, or manage other apps. If someone tries to disable
Device Admin status, Android shows a warning before allowing admin status to be
turned off.

## Local Data

Shorts Blocker Kids stores protection settings, enabled platform choices,
temporary allow timing, Accessibility disclosure acceptance, free-test state,
billing entitlement status, and parent PIN hash metadata locally on the device.
The parent PIN is not stored as plain text.

The app does not store watch history, video titles, URLs, comments, account
names, messages, screen recordings, audio, location, contacts, browsing history,
or raw Accessibility tree dumps.

## Data Collection And Sharing

Shorts Blocker Kids does not collect, transmit, sell, or share child data,
supported app activity, watch history, video titles, URLs, account names,
comments, messages, screen recordings, audio, location, contacts, browsing
history, or raw Accessibility tree dumps.

The current app has:

- no analytics;
- no ads;
- no account system;
- no cloud sync.

Google Play Billing adds `android.permission.INTERNET` and
`android.permission.ACCESS_NETWORK_STATE` through the Billing SDK. The app does
not use analytics, ads, account, or cloud sync network flow.

Google Play Billing processes subscription purchases. The app reads Google Play
purchase state and stores local subscription access state. It passes purchase
tokens to Google Play for acknowledgment. It does not send child data or
supported app activity to a billing server.

## Payments

Subscriptions are handled through Google Play Billing. Google Play may process
payment and subscription data under Google Play terms.

Shorts Blocker Kids stores local subscription access state. The app does not
store payment card data, billing addresses, or payment account details.

## Limitations

Blocking works only when Protection is ON and Android Accessibility Service is
active. Optional Device Admin tamper protection can make uninstall harder while
active, but if Accessibility permission is turned off, blocking stops.

## Removing Local Data

To remove local app data, disable Device Admin tamper protection if it is active,
then uninstall Shorts Blocker Kids or clear the app data in Android settings.

Support emails may be retained for as long as needed to handle the request and
maintain support history.

## Contact

Privacy contact: svalerii535@gmail.com

Support contact: svalerii535@gmail.com

Developer / publisher: Valerii Serputko

## Repository Publication Responsibility

The public route is generated from website/src/pages/privacy.mjs with date/contact configuration in website/src/config.mjs. This Markdown preserves the reviewed app-scope text and existing test dependency; changes to data handling or public policy need a separately authorized synchronized website/app review. Effective date above remains the existing policy date, not a new publication claim. See [Play Policy](SHORTS_BLOCKER_PLAY_POLICY_PACKAGE.md) for declaration/Data Safety drafts and [Deployment Evidence](SHORTS_BLOCKER_PRODUCTION_DEPLOYMENT_2026-05-31.md) for historical URLs.
