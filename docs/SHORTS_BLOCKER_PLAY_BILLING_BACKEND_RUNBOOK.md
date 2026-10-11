# Shorts Blocker Kids — Google Play Billing Integration

Status: Current app-side integration contract. Real Play tester lifecycle acceptance remains pending.

This legacy filename is retained because ReleaseCopyInvariantTest reads it. It is not a backend deployment runbook. The own billing backend and app integration were removed; no server deploy, database, RTDN, Developer API credential or backend URL is required.

Public website: https://shortsblockerkids.de
Privacy: https://shortsblockerkids.de/privacy
Support: https://shortsblockerkids.de/support
Publisher: Valerii Serputko
Contact: svalerii535@gmail.com

## Components And Product

- `infrastructure/billing/PlayBillingConfig.kt`: product `shorts_blocker_kids_monthly`, Billing enabled, Google Play management URL.
- `GooglePlayBillingGateway`: connection lifecycle, product/offer/price load, launch purchase, query/restore, management and acknowledgement; emits plain PlayBillingEvents.
- `application/billing/SyncBillingEntitlementUseCase.kt`: purchase-summary to local access outcome.
- `presentation/billing/BillingCoordinator.kt`: UI events, persistence callback and acknowledgement sequence.
- `infrastructure/storage/DataStoreSettingsStore.kt`: local paid-access flag in Preferences DataStore.

The gateway uses the first available subscription offer/pricing phase. The configured v1 is one monthly auto-renewing base plan without introductory/prepaid/installment offers. Do not promise an offer-selection policy that the code does not implement. [Billing Config](SHORTS_BLOCKER_PLAY_CONSOLE_BILLING_CONFIG.md) is the configuration sheet; prices are obtained from Play product details.

## Purchase-State Contract

| Successful Play query result for this product | Application outcome | Paid access |
|---|---|---|
| At least one PURCHASED subscription | ACTIVE | Allowed, subject to normal protection prerequisites. |
| No purchased subscription, at least one PENDING | PENDING | Not granted by payment pending; an independently active free test may still apply. |
| Neither purchased nor pending | INACTIVE | No paid access; free-test/protection policy determines the remaining UI/eligibility. |

These are app-side purchase observations, not a server subscription lifecycle database. Do not restore retired ON_HOLD/REVOKED/UNKNOWN timestamps or 72-hour grace logic. Cancellation may leave a purchase reported active through the paid period; reflect the next successful Play query rather than claiming immediate server notification.

## Persist Before Acknowledgement

On PurchasesObserved, the use case returns the entitlement snapshot and unacknowledged PURCHASED tokens. Coordinator calls persistEntitlement/onEntitlementChanged first. If persistence fails, it shows ENTITLEMENT_SAVE_FAILED and returns without acknowledging. After successful persistence it acknowledges each required token. Pending purchases are not acknowledged as paid grants.

An acknowledgement error is shown as a Billing operation failure; it does not erase already persisted access. Later purchase queries can surface still-unacknowledged purchased tokens for another acknowledgement attempt. Do not mark acknowledgement or durability verified without the recorded test/Play evidence.

## Restore, Management And Recovery

Restore queries Google Play purchases and applies the same summary/persist/ack sequence. After reinstall/clear data, restore needs the Play account that owns an active purchase; no manual key, separate account or external payment URL is used. Manage subscription opens Google's subscription management page for this package/product.

Connection/product loading/purchase cancellation/SDK failure remain recoverable UI states. A failed query does not produce an empty successful summary and does not by itself overwrite a previous stored paid flag. Failure to load prices is not proof of no subscription. Keep parent setup/PIN/settings and recovery flows usable; verify this in the tester matrix.

## Offline And Lifecycle Limits

The current persisted paid-access flag has no server verification timestamp, active-until or 72-hour offline timer. When a query fails, prior local access is retained; no new active grant is inferred from an SDK error. Do not describe cached access as freshly verified or promise an enforced offline expiry.

Expiry, refund/revocation, grace/account hold and account changes must be observed through successful Play refresh/restore and actual tester evidence. There is no own RTDN receiver or guaranteed background server lifecycle update while the app is unused. Record what Play reports and the resulting local state; do not manufacture test outcomes.

## Free Test, Privacy And Distribution

The 20-day local free test starts once after full protection activation; it is not an introductory Play offer and does not auto-enroll a purchase. After expiry, paid access follows the table above. Temporary allow changes local blocking eligibility, not Billing entitlement.

The Play-distributed app has no Stripe/website payment link, manual license activation or non-Play checkout. Existing SDK permissions support Play Billing, not child tracking/cloud/analytics. Purchase tokens are passed to Google Play for acknowledgement, not to an own server. Do not log raw tokens, credentials or private Accessibility content; no child history/profile/activity belongs in billing payloads.

## Evidence Gate

Use [Billing Internal Testing](SHORTS_BLOCKER_PLAY_BILLING_INTERNAL_TEST_RUNBOOK.md) BIL-01…BIL-12, including query failure/cache, persist/ack and lifecycle variants. Billing Config must match the app constant and a fresh AAB documented in [AAB Readiness](SHORTS_BLOCKER_AAB_RELEASE_READINESS.md). Sideload/fixture tests are not proof of Play purchase acceptance.

Applicable app-side flows/privacy boundaries consolidated from the retained Billing Backend Plan, Decision and Phase 10 Checklist. Their former server/deployment requirements are inactive history, not current tasks.
