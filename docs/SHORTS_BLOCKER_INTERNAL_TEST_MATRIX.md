# Shorts Blocker Kids Internal Test Matrix

Status: Ready for internal testing execution. Local automated gates can be run
from this repository; Play internal testing and real-device evidence must be
recorded by testers.

Date: May 24, 2026

## Release Under Test

```text
Package: com.shortsblockerkids
Version name: 0.1.0
Version code: 1
AAB: app/build/outputs/bundle/release/app-release.aab
AAB SHA-256: 2fee86da1e3b423d4c20007a7bf95d7fb829e11c97559ec2865cb2a8f723d23d
```

## Device Matrix

Record every internal testing device before scenario execution.

| Device ID | OEM / model | Android version | Install source | App version | Tester account | Result |
|---|---|---|---|---|---|---|
| DEV-01 | TBD | TBD | Play internal testing | 0.1.0 (1) | TBD | Pending |
| DEV-02 | TBD | TBD | Play internal testing | 0.1.0 (1) | TBD | Pending |
| DEV-03 | TBD | TBD | Play internal testing | 0.1.0 (1) | TBD | Pending |
| DEV-04 | TBD | TBD | Play internal testing | 0.1.0 (1) | TBD | Pending |

Required coverage:

- Pixel or close-to-AOSP device.
- Samsung device.
- Xiaomi, Redmi, or another OEM with aggressive background restrictions.
- Low-end Android device if available.
- Android 12, 13, 14, 15, and current available Android version where available.

## App Matrix

| App | Package | Version tested | Target surface | Non-target surfaces | Result |
|---|---|---|---|---|---|
| YouTube | `com.google.android.youtube` | TBD | Shorts | normal video, Home, Search, comments, profile/account | Pending |
| TikTok short-video feed | `com.zhiliaoapp.musically` | TBD | For You | Profile, Search, Settings, Inbox, comments where available | Pending |
| Instagram | `com.instagram.android` | TBD | Reels | Feed, Profile, Story, Comments, Direct/messages, Search, Settings | Pending |
| Facebook | `com.facebook.katana` | TBD | Reels | Feed, Profile, Groups, Comments, Search, Settings, Messages | Pending |

Unsupported packages must not be claimed as supported:

- `com.ss.android.ugc.trill`
- `com.facebook.lite`
- `com.instagram.lite`
- `com.google.android.youtube.tv`
- `com.google.android.apps.youtube.music`

## Scenario Matrix

| ID | Scenario | Expected result | Evidence |
|---|---|---|---|
| DET-01 | Block target surface for each supported app. | Overlay appears once without flicker or duplicate launch. | Screen recording per app. |
| DET-02 | Do not block false-positive surfaces for each supported app. | No overlay on listed non-target screens. | Screenshot or recording per app/surface. |
| DET-03 | Unsupported package lookalike. | No overlay and no supported-platform claim. | Recording and package name. |
| OVR-01 | Repeated high-confidence events. | Single stable overlay. | Recording or emulator E2E output. |
| OVR-02 | Exit to phone home. | Overlay clears and launcher opens. | Recording. |
| PIN-01 | Wrong PIN. | Temporary allow is not granted. | Recording. |
| PIN-02 | Correct PIN. | Temporary allow choices appear. | Recording. |
| TMP-01 | Temporary allow 5 minutes. | Supported short-video surfaces are allowed until expiry. | Recording and timestamp. |
| TMP-02 | Temporary allow 10 minutes. | Supported short-video surfaces are allowed until expiry. | Recording and timestamp. |
| TMP-03 | Temporary allow 15 minutes. | Supported short-video surfaces are allowed until expiry. | Recording and timestamp. |
| SUB-01 | Subscription active. | Paid protection entitlement is active. | Dashboard status and Play tester evidence. |
| SUB-02 | Subscription pending. | Paid protection is not unlocked until approval. | Dashboard status and Play tester evidence. |
| SUB-03 | Subscription canceled but still active. | Access remains active until Play reports expiry. | Dashboard status and Play tester evidence. |
| SUB-04 | Grace period / account hold. | App reflects Google Play state after refresh. | Dashboard status and backend evidence. |
| SUB-05 | Subscription expired. | Paid protection locks after active entitlement ends. | Dashboard status and Play tester evidence. |
| BACK-01 | Backend health. | `GET /health` returns `{"status":"ok"}`. | Command output. |
| BACK-02 | Backend offline within 72 hours. | Conservative offline entitlement window applies. | Dashboard status and timestamp. |
| BACK-03 | Backend offline after 72 hours. | Stale backend entitlement no longer unlocks paid protection. | Dashboard status and timestamp. |
| BILL-01 | Restore purchase. | Active subscription is restored after reinstall or clear data. | Dashboard status and Play tester evidence. |

## Pass Criteria

- Every supported app has target-surface blocking evidence.
- Every supported app has non-target false-positive evidence.
- Overlay does not flicker, duplicate, or trap Back/Home flows.
- PIN flow remains stable during launch grace.
- Temporary allow works for 5, 10, and 15 minutes.
- Billing states active, pending, canceled active, grace/account hold where
  available, expired, and restore are recorded.
- Backend health, RTDN, and offline entitlement behavior are recorded.
- Privacy Policy, Data Safety, Accessibility declaration, and store listing
  match the exact release under test.
- No analytics, ads, account, child history, raw Accessibility tree dumps, or
  non-Play payment paths are introduced.

## Current Evidence

Local automated evidence recorded in this repository:

- Release build gate and AAB-derived emulator install:
  `docs/SHORTS_BLOCKER_AAB_RELEASE_READINESS.md`
- Backend local production-like healthcheck:
  `docs/SHORTS_BLOCKER_PLAY_BILLING_BACKEND_RUNBOOK.md`
- Real-device YouTube smoke evidence from May 22, 2026:
  `docs/SHORTS_BLOCKER_REAL_DEVICE_SMOKE_REPORT.md`

External evidence still to collect:

- Play internal testing install evidence.
- Real-device TikTok, Instagram, and Facebook detector evidence.
- Device Admin tamper protection disclosure and disable-warning evidence.
- Play Billing tester lifecycle evidence.
- Deployed backend RTDN and durable storage evidence.
