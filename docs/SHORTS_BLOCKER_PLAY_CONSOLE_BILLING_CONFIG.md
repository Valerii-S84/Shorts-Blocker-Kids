# Shorts Blocker Kids Play Console Billing Config

Status: Prepared. Must be entered and verified in Play Console.

Date: May 24, 2026
Artifact refreshed: July 17, 2026

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

```text
Package name: com.shortsblockerkids
Version name: 0.1.0
Version code: 1
AAB: app/build/outputs/bundle/release/app-release.aab
AAB size: 3,629,462 bytes
AAB SHA-256: 126be877072bc018535c3b842e09227801041c45fdd29cd6e70d507abafc1d7f
Source commit: b5e559f44bdd289d87d20ad52f8e7fa78f79ef92
Production billing backend URL: https://billing.shortsblockerkids.de
RTDN webhook URL: https://billing.shortsblockerkids.de/billing/play/rtdn
Privacy Policy URL: https://shortsblockerkids.de/privacy
Support URL: https://shortsblockerkids.de/support
Publisher / developer name: Valerii Serputko
Public contact email: svalerii535@gmail.com
```

This local AAB was built with
`SBK_BILLING_BACKEND_BASE_URL=https://billing.shortsblockerkids.de`. Play Console
upload and Play App Signing acceptance are still not verified.

## Subscription Product

```text
Product type: Subscription
Product ID: shorts_blocker_kids_monthly
Name: Shorts Blocker Kids Monthly
Status before testing: Active
```

The product ID must match:

```text
BillingAvailability.MONTHLY_SUBSCRIPTION_PRODUCT_ID
```

If Play Console requires a different product ID, update:

- `BillingAvailability.MONTHLY_SUBSCRIPTION_PRODUCT_ID`;
- `BillingAvailabilityTest`;
- `docs/SHORTS_BLOCKER_PHASE_10_BILLING_CHECKLIST.md`;
- `docs/SHORTS_BLOCKER_PLAY_BILLING_INTERNAL_TEST_RUNBOOK.md`;
- this config sheet.

## Subscription Description Draft

```text
Monthly access to Shorts Blocker Kids protection. The app helps parents block
YouTube Shorts on a child's Android phone using a parent PIN and Android
Accessibility Service. Normal YouTube videos remain available.
```

Do not mention website payment, Stripe, license keys, hidden uninstall blocking,
or guarantees of absolute protection.

## Benefits Draft

Use short Play Console benefits that match the app:

```text
Block YouTube Shorts
Parent PIN controls
Temporary allow options
Local-only protection settings
Google Play subscription management
```

Avoid benefits that imply child tracking, cloud monitoring, analytics, browser
history collection, or system-level anti-uninstall behavior.

## Base Plan

```text
Base plan type: Auto-renewing
Base plan ID: monthly
Billing period: Monthly
Regions: confirm final launch regions in Play Console
Price: EUR 2.20/month as the first planned EU price
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
Required scenarios: BIL-01 through BIL-16
```

Do not approve Billing for production release until the runbook evidence is recorded.

## Policy Consistency Check Before Submission

Confirm these remain true after Play Console setup:

- no external payment link in the Play-distributed app;
- no manual license key or code entry activation;
- no ads;
- no analytics SDK;
- billing backend is deployed only for Play purchase verification and RTDN;
- Privacy Policy URL is `https://shortsblockerkids.de/privacy` and explains Google
  Play Billing, backend verification at `https://billing.shortsblockerkids.de`, and
  local entitlement storage;
- Data Safety answers match the Billing SDK, permissions, and local data.
