# Short Video Blocker Product Plan

Status: Planning. This document defines the target product. It does not claim
that TikTok, Instagram Reels, Facebook Reels, backend, or billing support is
implemented today.

## Product Definition

Short Video Blocker is an Android parental digital wellbeing app that helps
parents block short-video feeds on a child device.

The product target is:

```text
Install app -> create parent PIN -> review Accessibility disclosure -> enable
protection -> short-video feeds are blocked unless the parent temporarily
allows access.
```

The product must stay narrow:

- Block short-video feeds.
- Use one shared blocker overlay.
- Use one parent PIN.
- Use local protection state.
- Offer temporary allow for 5, 10, or 15 minutes.
- Avoid ads.
- Avoid analytics unless explicitly approved later.
- Avoid collection of child viewing history.

## Supported Apps

Target supported apps:

- YouTube app: YouTube Shorts.
- TikTok app: TikTok short-video feed / TikTok usage according to feasible
  detector strategy.
- Instagram app: Instagram Reels.
- Facebook app: Facebook Reels.

Each supported app must be added only after detector QA proves the app can be
blocked with acceptable false-positive and false-negative risk on real devices.

## What Is Blocked

Planned blocked surfaces:

- YouTube Shorts surfaces in `com.google.android.youtube`.
- TikTok short-video feed or, if reliable feed-specific detection is not
  possible, TikTok app usage as a documented fallback.
- Instagram Reels surfaces in `com.instagram.android`.
- Facebook Reels surfaces in `com.facebook.katana`.

Blocking action:

- Show the shared blocker overlay.
- Provide `Exit to phone home`.
- Provide parent PIN entry.
- Provide temporary allow durations: 5, 10, 15 minutes.

## What Is Not Blocked

The product must not claim or attempt to block:

- All video content on the phone.
- Browser-based short-video sites.
- YouTube normal videos unless they are detected as Shorts.
- Instagram main feed, messages, stories, or profile pages unless detector QA
  proves they are part of Reels blocking and the disclosure is updated.
- Facebook main feed, groups, messages, marketplace, or profile pages unless
  detector QA proves they are part of Reels blocking and the disclosure is
  updated.
- TikTok direct messages or account settings.
- App uninstall or Android settings access through aggressive device-owner or
  anti-uninstall behavior.

If a platform does not expose a reliable short-video-specific UI signal, the
product must either:

- block only high-confidence detected short-video screens; or
- document a wider app-level TikTok blocking fallback and disclose it clearly.

## Parent UX

Parent flows:

1. Open app.
2. Create parent PIN.
3. Read prominent AccessibilityService disclosure.
4. Give explicit consent.
5. Enable Android Accessibility Service in system settings.
6. See protection status for supported apps.
7. Turn protection on or off with parent PIN.
8. Enter PIN from blocker overlay.
9. Choose temporary allow: 5, 10, or 15 minutes.
10. See clear status if Accessibility Service is disabled.

Parent-facing copy must be direct:

- The app uses AccessibilityService.
- The app detects supported short-video surfaces.
- The app shows a blocker overlay.
- Parent PIN can temporarily allow access.
- The app does not collect child viewing history.
- Blocking stops if Accessibility Service is disabled or the app is removed.

## Child UX

When a blocked short-video surface is detected, the child sees:

- Product-branded blocker overlay.
- Simple explanation that short videos are blocked.
- `Exit to phone home` action.
- `Enter PIN` action for parent use.

The overlay must not imitate Android system UI. It must be clearly branded as
Short Video Blocker / Shorts Blocker Kids.

## Monetization Model

Target Play Store monetization:

- Google Play Billing subscription.
- No ads.
- No non-Play payment links inside Play-distributed app.
- No manual license key activation for Play-distributed app.

Initial subscription proposal:

- Monthly subscription.
- Parent-controlled purchase screen.
- Restore purchases support.
- Subscription management through Google Play.

Backend verification is planned for production hardening, but the backend must
store only billing technical data and must not receive child viewing history,
short-video events, Accessibility tree dumps, video titles, URLs, messages,
screen recordings, audio, location, contacts, or browsing history.

## Implementation Phases

1. Documentation:
   - Complete product, architecture, detection, billing/backend, testing, and
     Play Store readiness plans.
2. Local Android implementation:
   - Preserve current YouTube blocker logic.
   - Add detector abstraction.
   - Add TikTok, Instagram Reels, and Facebook Reels detectors behind QA gates.
   - Reuse shared overlay, PIN, temporary allow, and local settings.
3. Backend + billing:
   - Maintain the app-side Google Play Billing integration.
   - Verify Play Billing purchase, restore, cancel, expire, pending, and payment
     issue flows with license testers.
   - Add backend purchase verification if approved for production.
   - Add Real-time Developer Notifications processing.
4. Local full verification:
   - Run unit, integration, lint, release build, and real-device app matrix.
   - Verify no child viewing history or unsupported data leaves the device.
5. Production / Play Store package:
   - Finalize Privacy Policy URL.
   - Complete Data Safety and Accessibility declarations.
   - Record demo video.
   - Upload AAB.
   - Run internal and closed testing.
   - Stage rollout.

## Production Acceptance Criteria

Product acceptance:

- All supported app surfaces are documented and tested.
- YouTube Shorts blocker behavior has no regression from current working logic.
- TikTok, Instagram Reels, and Facebook Reels are enabled only after real-device
  detector QA.
- Shared blocker overlay works for every supported app.
- Parent PIN and temporary allow work across every supported app.
- Accessibility disclosure names every supported blocked app/surface.
- Privacy Policy and Data Safety match the final app behavior.
- Billing entitlement is correct for active, canceled-but-active, expired,
  pending, unpaid, and restored subscriptions.
- Backend, if implemented, receives only billing technical data.
- No ads are included.
- No analytics are included unless explicitly approved later and disclosed.
- No child viewing history is collected.
- AAB passes release build, lint, unit tests, and manual QA gates.

## Policy References

- Google Play AccessibilityService API policy:
  https://support.google.com/googleplay/android-developer/answer/10964491
- Google Play User Data policy:
  https://support.google.com/googleplay/android-developer/answer/10144311
- Google Play Data Safety form guidance:
  https://support.google.com/googleplay/android-developer/answer/10787469
- Google Play Families Policies:
  https://support.google.com/googleplay/android-developer/answer/9893335
