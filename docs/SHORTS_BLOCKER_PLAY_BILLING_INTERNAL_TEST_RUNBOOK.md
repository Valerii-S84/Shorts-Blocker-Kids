# Shorts Blocker Kids Play Billing Internal Test Runbook

Status: Prepared. External Play Console execution is pending.

Date: May 24, 2026

## Scope

This runbook is the next required production gate after the local app-side
Google Play Billing SDK integration.

It covers:

- Play Console subscription product setup;
- license tester setup;
- internal testing track setup;
- required subscription lifecycle tests;
- evidence to capture before moving toward production rollout.

It covers app-side Billing behavior first. Backend purchase verification and
Real-time Developer Notifications are now implemented locally and must be
validated against a deployed backend before production rollout.

Use `docs/SHORTS_BLOCKER_PLAY_CONSOLE_BILLING_CONFIG.md` as the exact product
configuration sheet before running this test plan.

## Official Sources Checked

- Android Developers: test Google Play Billing Library integration:
  <https://developer.android.com/google/play/billing/test>
- Play Console Help: test in-app billing with application licensing:
  <https://support.google.com/googleplay/android-developer/answer/6062777>
- Play Console Help: create and manage subscriptions:
  <https://support.google.com/googleplay/android-developer/answer/140504>
- Android Developers: integrate Google Play Billing:
  <https://developer.android.com/google/play/billing/integrate>
- Android Developers: subscriptions:
  <https://developer.android.com/google/play/billing/subscriptions>

## App Facts To Match In Play Console

```text
Package name: com.shortsblockerkids
Version name: 0.1.0
Version code: 1
Subscription product ID: shorts_blocker_kids_monthly
Billing library: com.android.billingclient:billing-ktx:9.0.0
Product type: subscription
Current proposed price: EUR 2.20/month
```

The product ID in Play Console must match
`BillingAvailability.MONTHLY_SUBSCRIPTION_PRODUCT_ID`. If Play Console requires
a different product ID, update the app constant, tests, policy docs, and this
runbook before uploading another AAB.

## Play Console Setup Checklist

1. Confirm the Play Console app exists for package `com.shortsblockerkids`.
2. Configure Play App Signing and upload key flow.
3. Upload the current `app-release.aab` to an internal testing release.
4. In `Monetize with Play > Products > Subscriptions`, create subscription
   product `shorts_blocker_kids_monthly`.
5. Use a user-facing name that accurately describes the paid feature, for
   example `Shorts Blocker Kids Monthly`.
6. Add benefits that describe the paid feature without implying a misleading
   free trial.
7. Create an auto-renewing monthly base plan.
8. Configure country/region availability and price. The planned first price is
   EUR 2.20/month, but final localized pricing must be confirmed in Play
   Console.
9. Activate the base plan.
10. Do not add introductory offers, prepaid plans, or installment plans unless
    the app UI and policy docs are updated first.
11. In Play Console settings, add license testers by email list or Google Group.
12. Add the same tester accounts to the internal testing track.
13. Share the internal testing opt-in URL and confirm every tester opts in.

After publishing to a testing track, allow for Play availability delay before
treating product-loading failures as app bugs.

## Device Setup Checklist

1. Use a real Android device with current Google Play installed.
2. Sign in with the license tester account on the device.
3. Install Shorts Blocker Kids from the internal testing track, not by local APK
   sideload, for Billing verification.
4. If multiple Google accounts are on the device, confirm the purchase dialog
   uses the tester account that installed the app.
5. Keep a second non-license tester account available for one sanity test after
   license-tester flows pass.

## Required Test Matrix

Use the accelerated subscription timelines and delayed test instruments from
Google's Billing testing documentation. Do not encode those tester-only timings
into app logic.

| ID | Scenario | Steps | Expected result |
|---|---|---|---|
| BIL-01 | Product details load | Open dashboard after installing the internal test build. | Subscription row shows Google Play product availability, price, monthly auto-renewal terms, manage/cancel text, and an enabled subscribe button. |
| BIL-02 | Billing unavailable | Test on a device/account where Play Billing cannot load the product, or before product activation. | App shows a recoverable Billing unavailable/product unavailable state and does not crash. |
| BIL-03 | Purchase approved | Tap `Subscribe with Google Play` and use a test payment method that approves. | Purchase completes, app acknowledges it, subscription becomes active, and protection can run after free-test expiry. |
| BIL-04 | Purchase canceled | Start purchase and cancel the Play purchase sheet. | No paid entitlement is granted; dashboard remains usable; restore remains available. |
| BIL-05 | Pending declines | Use a delayed test instrument that declines after a few minutes. Restart app before completion. | Pending state does not unlock paid protection; after decline, no paid entitlement is active. |
| BIL-06 | Pending approves | Use a delayed test instrument that approves after a few minutes. | Pending state does not unlock immediately; after approval and refresh, paid entitlement becomes active. |
| BIL-07 | Restore after reinstall | Purchase, uninstall or clear app data, reinstall from internal testing, then tap restore. | Active subscription is restored without manual license key or external payment flow. |
| BIL-08 | Manage subscription | With active subscription, tap `Manage subscription`. | Google Play subscription management opens for this product. |
| BIL-09 | Cancel but still active | Cancel the test subscription in Google Play before paid-through end. | App continues to treat subscription as active while Play still reports the purchase as active. |
| BIL-10 | Expired | Let the test subscription expire or use Play test lifecycle controls. | After restore/refresh reports no active purchase, paid protection locks but parent settings access remains available. |
| BIL-11 | Payment problem | Change the subscription payment method to a failing test instrument. | App shows payment/Billing problem messaging when Play reports a problem; protection does not remain unlocked after active entitlement is gone. |
| BIL-12 | Non-license sanity | Repeat purchase visibility with a non-license tester internal-test account. | App does not rely on license-tester-only logic; real Play purchase UX is still coherent. |
| BIL-13 | Backend health | Start the deployed backend and call `GET /health`. | Health endpoint returns `{"status":"ok"}` without logging secrets or child data. |
| BIL-14 | Backend offline window | With an active backend-verified entitlement, make the backend unreachable and reopen the app within 72 hours. | App uses the conservative offline entitlement window and does not require non-billing data. |
| BIL-15 | Backend offline expired | Simulate or wait until the conservative backend offline window expires. | Paid protection no longer relies on stale backend verification. |
| BIL-16 | RTDN lifecycle | Trigger or wait for RTDN updates for cancel, grace/account hold where available, and expiry. | Backend processes each message once and app entitlement reflects Google Play state after refresh. |

## Evidence To Record

For every required scenario, record:

- date and tester email domain;
- device manufacturer/model;
- Android version;
- Google Play Store version;
- app version name/code;
- AAB SHA-256;
- scenario ID;
- pass/fail;
- exact observed dashboard subscription status text;
- whether protection was active, inactive, or locked;
- screenshot or screen recording when useful;
- any Play Console error, purchase response, or tester account mismatch.

## Current Artifact

```text
app/build/outputs/bundle/release/app-release.aab
SHA-256: 2fee86da1e3b423d4c20007a7bf95d7fb829e11c97559ec2865cb2a8f723d23d
```

If the AAB is rebuilt, update the hash here before testing.

## Pass Gate

Billing can move from `Partial` to `Done` only when:

- Play Console product `shorts_blocker_kids_monthly` is active;
- internal testing AAB is installable from Play;
- BIL-01 through BIL-16 are recorded;
- backend verification is deployed and the app build uses
  `SBK_BILLING_BACKEND_BASE_URL`;
- RTDN delivery is configured and verified against the deployed backend;
- no non-Play payment path is visible in the Play-distributed app;
- Privacy Policy, Data Safety, and store listing copy still match the shipped
  SDKs, permissions, and payment behavior.
