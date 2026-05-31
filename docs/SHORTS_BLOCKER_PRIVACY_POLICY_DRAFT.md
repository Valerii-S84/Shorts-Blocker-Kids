# Shorts Blocker Kids Privacy Policy Draft

Status: Draft for publication at `https://movashield.de/privacy`. Public
privacy/support contact and personal developer / publisher name are filled for
the current Google Play submission path. Live hosting still must be verified
before Play Console submission.

Effective date: May 24, 2026

Public website: https://movashield.de
Privacy Policy URL: https://movashield.de/privacy
Support/privacy contact: svalerii535@gmail.com
Developer / publisher: Valerii Serputko

## Overview

Shorts Blocker Kids is a parental digital wellbeing app that helps parents
block supported short-video surfaces on a child's Android phone. Current
app-side support is:

| Platform | Status |
|---|---|
| YouTube Shorts | supported |
| TikTok main `com.zhiliaoapp.musically` | supported by code; needs real-device QA |
| TikTok regional `com.ss.android.ugc.trill` | not supported |
| Instagram Reels | supported by code; needs real-device QA |
| Facebook Reels | supported by code; needs real-device QA |
| Facebook Lite `com.facebook.lite` | not supported |

The app works locally on the device for protection behavior. It does not
require an account. For production subscription verification, the app may use a
billing backend limited to Google Play purchase verification and entitlement
status at `https://billing.movashield.de`.

## Accessibility Service

Shorts Blocker Kids uses Android AccessibilityService to detect when supported
short-video surfaces are open. When a supported surface is detected and
protection is enabled, the app shows a blocking overlay. A parent PIN can
temporarily allow access.

AccessibilityService is used only for the app's blocking function. Detection
runs locally on the device.

## Local Data

Shorts Blocker Kids stores protection settings and parent PIN hash metadata
locally on the device. The parent PIN is not stored as plain text.

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

Google Play Billing and production backend subscription verification may send
only billing technical data needed to verify and maintain subscription access.
The production backend base URL configured for release builds is
`https://billing.movashield.de`.

- random app installation ID;
- app package name and app version;
- Google Play subscription product ID;
- Google Play purchase token;
- entitlement verification timestamp and status.

The billing backend stores entitlement state, verification timestamps,
processed RTDN message IDs, and a hashed purchase token. It must not receive
child data, supported app activity, watch history, video titles, URLs,
comments, account names, messages, screen recordings, audio, location,
contacts, browsing history, or raw Accessibility tree dumps.

## Payments

Subscriptions are handled through Google Play Billing. Google Play may process
payment and subscription data under Google Play terms.

Shorts Blocker Kids stores local subscription entitlement status and the last
verification timestamp. If backend verification is enabled, the backend stores
minimal billing entitlement records, processed RTDN message IDs, and a hashed
purchase token. The app and backend do not store payment card data, billing
addresses, or payment account details.

## Limitations

Blocking works only when Protection is ON and Android Accessibility Service is
active. If that permission is turned off or the app is removed, blocking stops.

## Removing Local Data

To remove local app data, uninstall Shorts Blocker Kids or clear the app data in
Android settings.

## Contact

Privacy contact: svalerii535@gmail.com

Support contact: svalerii535@gmail.com

Developer / publisher: Valerii Serputko
