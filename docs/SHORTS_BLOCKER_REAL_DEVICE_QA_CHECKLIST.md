# Shorts Blocker Kids Real-Device QA Checklist

Status: Ready for manual real-device execution.

## Scope

This checklist verifies the production-hardening state on real Android phones
with the actual social apps. It does not replace the automated unit and emulator
E2E gates. It does not claim support for packages listed as unsupported.

## Evidence To Capture

Capture evidence for each device and each app run:

- Video recording of the full scenario from opening the target app to the
  expected block or non-block result.
- Screenshot of the blocking overlay when a screen is expected to block.
- Screenshot of the target screen when it is expected not to block.
- Shorts Blocker Kids build variant, version name, and version code.
- Phone manufacturer and model.
- Android version.
- Target app package name and app version.
- Protection state before the run: Protection ON, AccessibilityService enabled,
  temporary allow inactive.
- Result: pass, fail, or blocked by environment.

## Supported Packages To Test

| Platform | Package | Expected supported surface |
|---|---|---|
| YouTube | `com.google.android.youtube` | Shorts |
| TikTok main | `com.zhiliaoapp.musically` | For You |
| Instagram | `com.instagram.android` | Reels |
| Facebook | `com.facebook.katana` | Reels |

## Known Unsupported Packages

Do not claim these as supported unless a future code change and QA pass
explicitly enable them:

- TikTok regional package: `com.ss.android.ugc.trill`
- Facebook Lite: `com.facebook.lite`
- Instagram Lite: `com.instagram.lite`
- YouTube TV: `com.google.android.youtube.tv`
- YouTube Music: `com.google.android.apps.youtube.music`

## Device Matrix

Run on at least:

- One Pixel device.
- One Samsung device.
- One Xiaomi, Redmi, or similar OEM with aggressive background restrictions.
- One low-end Android device if available.
- Android 12, 13, 14, 15, and current target Android version where available.

## Common Setup

1. Fresh install or clear app data.
2. Open Shorts Blocker Kids.
3. Create parent PIN.
4. Review the Accessibility disclosure.
5. Decline once and verify Android Accessibility settings do not open.
6. Reopen setup, accept disclosure, and enable the AccessibilityService.
7. Confirm dashboard shows active protection.
8. Confirm temporary allow is inactive before each app scenario.

## YouTube

Expected to block:

- Open Shorts from bottom tab.
- Open Shorts from home recommendation.
- Open Shorts from search result if available.
- Swipe between Shorts; overlay must not duplicate or flicker.

Expected not to block:

- Open a normal YouTube video.
- Open Home.
- Open Search.
- Open comments on a normal video.
- Open Profile or account page.

Evidence:

- Video showing Shorts blocked.
- Video or screenshots showing normal video, Home, Search, and Profile are not
  blocked.

## TikTok

Expected to block:

- Open For You feed in `com.zhiliaoapp.musically`.
- Swipe to the next For You item; overlay must not duplicate or flicker.

Expected not to block:

- Open Profile.
- Open Search.
- Open Settings.
- Open Inbox.
- Open comments on a non-target screen where available.

Unsupported:

- `com.ss.android.ugc.trill` must remain documented as unsupported.

Evidence:

- Video showing For You blocked.
- Video or screenshots showing Profile, Search, and Settings are not blocked.
- Target package and app version.

## Instagram

Expected to block:

- Open Reels tab.
- Open a Reel from feed if available.
- Swipe between Reels; overlay must not duplicate or flicker.

Expected not to block:

- Open Feed.
- Open Profile.
- Open Comments on a normal post where available.
- Open Direct messages or inbox.
- Open Search.
- Open Settings.
- Open Story.

Evidence:

- Video showing Reels blocked.
- Video or screenshots showing Feed, Profile, and Story are not blocked.
- Target package and app version.

## Facebook

Expected to block:

- Open Reels surface.
- Open a Reel from feed if available.
- Swipe between Reels; overlay must not duplicate or flicker.

Expected not to block:

- Open Feed.
- Open Profile.
- Open Groups.
- Open Comments on a normal post or video where available.
- Open Search.
- Open Settings.
- Open Messages where available.

Unsupported:

- `com.facebook.lite` must remain documented as unsupported.

Evidence:

- Video showing Reels blocked.
- Video or screenshots showing Feed, Profile, and Groups are not blocked.
- Target package and app version.

## Cross-App Scenarios

Run these at least once per device:

- Back from overlay: no loop and no duplicate overlay.
- Home from overlay: returns to launcher and overlay clears.
- Reopen blocked app after Home: expected blocked screen blocks again.
- Enter wrong PIN: temporary allow is not granted.
- Enter correct PIN: temporary allow choices appear.
- Choose 5 minute temporary allow: blocked surfaces are allowed until expiry.
- After temporary allow expiry: blocked surfaces block again.
- Disable global Protection: supported short-video screens do not block.
- Re-enable global Protection: supported short-video screens block again.
- Disable AccessibilityService: no blocking occurs and dashboard reports
  inactive protection.

## Billing And Backend Scenarios

Run these on the Play internal testing build after the subscription product and
backend URL are configured:

- Product details load and show the Play price.
- Purchase approved.
- Purchase pending, then approved.
- Purchase pending, then declined.
- Purchase canceled.
- Cancel active subscription and verify access remains active until Play reports
  expiry.
- Grace period / account hold where Play test controls allow it.
- Restore purchase after reinstall or clear app data.
- Backend online entitlement refresh.
- Backend offline for up to 72 hours after last active verification.
- Backend offline after the conservative window expires.
- RTDN update processed without duplicate state changes.

## Pass Criteria

The real-device QA pass is complete only when:

- Every supported app has one positive blocking video.
- Every supported app has non-blocking evidence for the listed normal screens.
- Unsupported packages are not claimed as supported.
- No duplicate overlay, overlay flicker, or Back/Home loop is observed.
- Device model, Android version, Shorts Blocker Kids build, target package, and
  target app version are recorded for every run.

## Remaining Limits

Real-device QA validates the observed app versions and devices only. Social app
UI changes, locale changes, account-specific UI, OEM background restrictions,
and future Android platform behavior can still require follow-up calibration.
