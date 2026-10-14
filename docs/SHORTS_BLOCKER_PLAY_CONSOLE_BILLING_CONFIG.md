# Shorts Blocker Kids Play Console Billing Config

Status: Prepared. Must be entered and verified in Play Console.

Date: May 24, 2026
Artifact status updated: July 17, 2026

## Scope

This is the exact configuration sheet for the first Play Console subscription
setup. It exists so the Play Console product, app code, policy docs, and
internal test runbook stay aligned.

## Official Sources

- Play Console Help: create and manage subscriptions:
  <https://support.google.com/googleplay/android-developer/answer/140504>
- Android Developers: subscriptions:
  <https://developer.android.com/google/play/billing/subscriptions>
- Android Developers: test Google Play Billing Library integration:
  <https://developer.android.com/google/play/billing/test>

## App Artifact To Upload

Use [AAB Readiness](SHORTS_BLOCKER_AAB_RELEASE_READINESS.md) for source commit, fresh signed AAB/version/hash and Play acceptance. Package com.shortsblockerkids; current source versionName 0.1.0, versionCode 1. No artifact/upload is verified here. Canonical Privacy: https://shortsblockerkids.de/privacy; Support: https://shortsblockerkids.de/support; Publisher Valerii Serputko, svalerii535@gmail.com.

## Subscription Product

```text
Product type: Subscription
Product ID: shorts_blocker_kids_monthly
Name: Shorts Blocker Kids Monthly
Status before testing: Active
```

The product ID must match:

```text
PlayBillingConfig.MONTHLY_SUBSCRIPTION_PRODUCT_ID
```

If Play Console requires a different product ID, update:

- `PlayBillingConfig.MONTHLY_SUBSCRIPTION_PRODUCT_ID`;
- `PlayBillingConfigTest`;
- `docs/SHORTS_BLOCKER_PLAY_BILLING_BACKEND_RUNBOOK.md`;
- `docs/SHORTS_BLOCKER_PLAY_BILLING_INTERNAL_TEST_RUNBOOK.md`;
- this config sheet.

## Subscription Description Draft

Monthly access to supported short-video protection, using parent PIN and Android AccessibilityService. Current app-side support includes YouTube Shorts and primary TikTok/Instagram/Facebook detectors. Keep the real-device QA caveat until those surfaces are verified; do not claim all videos/apps or normal-screen blocking.

## Benefits Draft

- Supported short-video surface protection, subject to the documented platform/device scope.
- Parent PIN and 15/30/60 minute temporary allow.
- Local protection settings.
- Google Play monthly subscription management.

No child tracking, analytics/cloud monitoring or absolute anti-uninstall promises.

## Base Plan

```text
Base plan type: Auto-renewing
Base plan ID: monthly
Billing period: Monthly
Regions: confirm final launch regions in Play Console
Price is configured per country in Google Play Console. Planned launch pricing: Europe €2.99/month, Ukraine approx. €0.99/month equivalent.
Offers: none for v1 unless app UI and docs are updated first
```

The app UI currently says "Monthly auto-renewing subscription" and gets the
localized formatted price from Google Play product details. If the billing
period or offer structure changes, update the app UI copy and tests first.

## License Tester Setup

1. Add license tester accounts in Play Console.
2. Add the same accounts to the internal testing track.
3. Confirm each tester uses the internal testing opt-in link.
4. Confirm the tester installs from Google Play, not from a sideloaded APK.
5. Confirm the purchase sheet shows the same Google account that installed the
   app.

## Required Internal Testing Track

```text
Track: Internal testing
Release artifact: app-release.aab
Test source: Google Play install only
Billing test runbook: docs/SHORTS_BLOCKER_PLAY_BILLING_INTERNAL_TEST_RUNBOOK.md
Required scenarios: BIL-01 through BIL-12
```

Do not approve Billing for production release until the runbook evidence is recorded.

## Policy Consistency Check Before Submission

Confirm these remain true after Play Console setup:

- no external payment link in the Play-distributed app;
- no manual license key or code entry activation;
- no ads;
- no analytics SDK;
- Privacy Policy URL is `https://shortsblockerkids.de/privacy` and explains Google
  Play Billing and local subscription access state;
- Data Safety answers match the Billing SDK, permissions, and local data.

## Verification Status

Product ID shorts_blocker_kids_monthly matches PlayBillingConfig in source; base-plan ID monthly, launch regions/prices and product activation are planned configuration until entered/verified in Console. The gateway currently uses the first available offer/pricing phase; keep v1 without additional offers or get explicit scope for matching UI/code/tests first. BIL-01…BIL-12 must be recorded before Billing is approved for release.
