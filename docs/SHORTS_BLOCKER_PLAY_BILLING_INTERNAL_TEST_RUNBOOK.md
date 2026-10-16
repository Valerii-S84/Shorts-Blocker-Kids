# Shorts Blocker Kids Play Billing Internal Test Runbook

Status: FAIL / incomplete overall release preparation. Billing runbook prepared; external Play execution pending.

Date: May 24, 2026
Artifact status updated: July 17, 2026

## Scope

This runbook is the next required production gate after the local app-side
Google Play Billing SDK integration.

It covers:

- Play Console subscription product setup;
- license tester setup;
- internal testing track setup;
- required subscription lifecycle tests;
- evidence to capture before moving toward production rollout.

It covers app-side Google Play Billing behavior.

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
Price is configured per country in Google Play Console. Planned launch pricing: Europe €2.99/month, Ukraine approx. €0.99/month equivalent.
Privacy Policy URL: https://shortsblockerkids.de/privacy
```

The product ID in Play Console must match
`PlayBillingConfig.MONTHLY_SUBSCRIPTION_PRODUCT_ID`. If Play Console requires
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
8. Configure country/region availability and price. Price is configured per
   country in Google Play Console. Planned launch pricing: Europe €2.99/month,
   Ukraine approx. €0.99/month equivalent.
9. Activate the base plan.
10. Do not add introductory offers, prepaid plans, or installment plans unless
    the app UI and policy docs are updated first.
11. In Play Console settings, add license testers by email list or Google Group.
12. Add the same tester accounts to the internal testing track.
13. Share the internal testing opt-in URL and confirm every tester opts in.

After publishing to a testing track, allow for Play availability delay before
treating product-loading failures as app bugs.

## Device Setup Checklist

1. Use a Play-enabled Android device (physical device or Google Play AVD) with
   the current Google Play app installed.
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
| BIL-03 | Purchase approved | Tap `Subscribe with Google Play` and use a test payment method that approves. | Purchase completes; local access is persisted before acknowledgement, then subscription/protection can become active after free-test expiry. Exercise persistence failure: show recovery and do not acknowledge before save succeeds. |
| BIL-04 | Purchase canceled | Start purchase and cancel the Play purchase sheet. | No paid entitlement is granted; dashboard remains usable; restore remains available. |
| BIL-05 | Pending declines | Use a delayed test instrument that declines after a few minutes. Restart app before completion. | Pending state does not unlock paid protection; after decline, no paid entitlement is active. |
| BIL-06 | Pending approves | Use a delayed test instrument that approves after a few minutes. | Pending state does not unlock immediately; after approval and refresh, paid entitlement becomes active. |
| BIL-07 | Restore after reinstall | Purchase, uninstall or clear app data, reinstall from internal testing, then tap restore. | Active subscription is restored without manual license key or external payment flow. |
| BIL-08 | Manage subscription | With active subscription, tap `Manage subscription`. | Google Play subscription management opens for this product. |
| BIL-09 | Cancel but still active | Cancel the test subscription in Google Play before paid-through end. | App continues to treat subscription as active while Play still reports the purchase as active. |
| BIL-10 | Expired | Let the test subscription expire or use Play test lifecycle controls. | After restore/refresh reports no active purchase, paid protection locks but parent settings access remains available. |
| BIL-11 | Payment problem | Change the subscription payment method to a failing test instrument. | Record SDK messaging and actual successful refresh state; when Play reports no active purchase, local paid access is cleared. A failed query alone is not proof of expiry and does not clear cached access. |
| BIL-12 | Non-license sanity | Repeat purchase visibility with a non-license tester internal-test account. | App does not rely on license-tester-only logic; real Play purchase UX is still coherent. |

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

See [AAB Readiness](SHORTS_BLOCKER_AAB_RELEASE_READINESS.md) for the authoritative fresh-artifact provenance record. The old AAB predates reviewed source and must not be uploaded. Before testing, record the new signed AAB hash/source/version and confirm Play App Signing acceptance. No rebuild/upload was performed by documentation preparation.

## Pass Gate

Billing can move from `Partial` to `Done` only when:

- Play Console product `shorts_blocker_kids_monthly` is active;
- internal testing AAB is installable from Play;
- BIL-01 through BIL-12 are recorded;
- no non-Play payment path is visible in the Play-distributed app;
- Privacy Policy, Data Safety, and store listing copy still match the shipped
  SDKs, permissions, and payment behavior.

## Current Contract And Required Variants

### Local Source And Unit Evidence (2026-10-16 local)

`SyncBillingEntitlementUseCaseTest` executes the use case for PURCHASED (including PURCHASED plus PENDING), already-acknowledged purchases, PENDING without a paid grant/ack and an empty successful query. `BillingCoordinatorTest` executes persistence before acknowledgement, a suspended persistence operation, failed writes without acknowledgement followed by restore recovery, cancellation and SDK/UI failure states. These are behavioral tests with recorded snapshots/tokens/messages; their presence is not a fresh pass result.

The inspected implementation preserves this ordering: `BillingCoordinator` awaits entitlement persistence before invoking acknowledgement; failure returns with `ENTITLEMENT_SAVE_FAILED`. `MainActivity.onResume()` calls coordinator `start()`. A ready gateway then queries product details and purchases; successful setup also queries them. A failed purchase query emits an operation failure instead of an empty successful purchase summary. The stored paid flag has no expiry timestamp or 72-hour timer. Verify device/SDK recovery and account changes with actual Play evidence; this source trace does not certify those flows.

Current unit XML: SyncBillingEntitlementUseCaseTest **4/0/0/0**, BillingCoordinatorTest **11/0/0/0**. These are local PURCHASED/PENDING/absence, persistence-before-acknowledge, storage failure, query/restore/recovery and foreground-resync evidence only. **Local preparation: FAIL. Overall publication readiness: FAIL.** The reserved final frozen-source phase started 2026-10-15T22:53:35Z (16 October in Europe/Berlin). No automatic retry or source change followed. Ktlint failed; fresh unit XML is **594 / 1 / 0 / 0** (tests/failures/errors/skipped). Debug/release builds and both lint variants completed. Coverage verification/report did not execute after the failing unit prerequisite; no new achieved ratios are claimed. The entire 32-test instrumentation task started once without a filter, but terminated with only **13 completed XML tests / 0 failures / 0 errors / 0 skipped**, leaving **19 without results**. This is incomplete, never a whole-suite pass. Both Pevisor reviews remain unavailable and unfulfilled. AAB Readiness identifies the fresh artifact; no Play acceptance is implied. All BIL-01 through BIL-12 Play-installed purchase/refund/account and lifecycle scenarios remain pending tester evidence. No backend, RTDN, database or 72-hour grace was restored. No real transaction or account action occurred.

Follow [Billing Integration](SHORTS_BLOCKER_PLAY_BILLING_BACKEND_RUNBOOK.md). Under BIL-02, test offline/disconnection/product unavailability both with and without prior paid access; record recoverable UI and retained local cache. There is no 72-hour timer or server verification. Under BIL-03, verify persist-before-ack, save failure and acknowledgement-error recovery. Under BIL-09/10/11, record cancellation through paid period, expiry, refund/revocation and grace/account-hold observations where Play test controls support them, then refresh/restore. Do not claim instantaneous server/RTDN propagation. Pending never grants new paid access; a separate active free test may still enable protection.

This is 12 scenario IDs, BIL-01 through BIL-12, with variants—not a BIL-16 suite. Existing SDK version/configuration facts must be checked against the exact release and Play product before execution. Prices remain planned Console configuration, not a verified live offer. Local free test does not auto-enroll a Play subscription. Record parent-settings/recovery accessibility after expiry and SDK errors, with exact UI text and redacted evidence. No real purchase results are claimed by this preparation.
