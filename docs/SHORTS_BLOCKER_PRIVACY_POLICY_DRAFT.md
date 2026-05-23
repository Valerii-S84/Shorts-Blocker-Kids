# Shorts Blocker Kids Privacy Policy Draft

Status: Draft for hosting. Replace the TODO privacy contact and publisher name
before submitting this URL to Google Play.

Effective date: May 22, 2026

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

The app works locally on the device. It does not require an account and does
not use an app server in the current release.

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
supported app activity, video data, screen recordings, audio, messages,
location, contacts, or browsing history.

The current app has:

- no analytics;
- no ads;
- no account system;
- no backend;
- no cloud sync.

Google Play Billing adds `android.permission.INTERNET` and
`android.permission.ACCESS_NETWORK_STATE` through the Billing SDK. The app does
not use a custom backend, analytics, ads, account, or cloud sync network flow.

## Payments

Subscriptions are handled through Google Play Billing. Google Play may process
payment and subscription data under Google Play terms.

Shorts Blocker Kids stores only local subscription entitlement status and the
last local verification timestamp. The app does not store payment card data,
order IDs, billing addresses, or payment account details.

## Limitations

Blocking works only when Protection is ON and Android Accessibility Service is
active. If that permission is turned off or the app is removed, blocking stops.

## Removing Local Data

To remove local app data, uninstall Shorts Blocker Kids or clear the app data in
Android settings.

## Contact

Privacy contact: TODO before submission - add the public privacy contact.

Developer / publisher: TODO before submission - add the Play listing entity.
