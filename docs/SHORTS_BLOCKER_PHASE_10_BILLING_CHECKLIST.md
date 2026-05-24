# Shorts Blocker Kids — Phase 10 Billing Checklist

Status: Partial. Active production monetization path is Google Play Store + Android App Bundle + Google Play Billing + server-side Play purchase verification. Website APK + Stripe is deferred until the Play Store production track is complete.

## Execution Lock

- [x] Google Play Billing is the active monetization path for Play Store distribution.
- [x] Website APK + Stripe is not part of the current production execution track.
- [x] No alternate payment links are allowed inside a Play-distributed app.
- [x] No manual license key or code entry activation is allowed in the Play Store track.
- [x] Billing work must wait until P0 policy package and real-device blocker QA are not blocked.

## Phase 10A — Billing Policy Lock

- [x] Confirm final product type: recurring subscription.
- [x] Confirm first subscription price: `€2.20/month`.
- [x] Choose planned Play product ID: `shorts_blocker_kids_monthly`.
- [x] Document entitlement rules for active, canceled, expired, pending, past_due, and unpaid states.
- [x] Confirm no ads monetization in v1.
- [x] Confirm no non-Play payment CTA in Play-distributed app.

Notes:

- Product ID is planned for Play Console setup. If Play Console rejects it or a
  different naming convention is chosen there, update this checklist before
  app-side Billing integration.
- The Play-distributed app must not show website, Stripe, manual license key,
  code entry, or external payment CTAs.
- No ads monetization is planned for v1.

## Entitlement Rules

| State | Protection behavior | Parent settings access | Notes |
|---|---|---|---|
| `UNKNOWN` | Do not newly enable paid protection unless free test is active. | Allowed. | Show a billing unavailable / restore-needed state when Billing is implemented. |
| `ACTIVE` | Protection can run. | Allowed. | Purchase must be acknowledged. |
| `CANCELED_BUT_ACTIVE_UNTIL_END` | Protection can run until paid-through end. | Allowed. | Show active-until date when available. |
| `PENDING` | Paid protection not unlocked yet unless free test is active. | Allowed. | Show pending purchase state. |
| `PAST_DUE` | Keep protection only during a conservative grace window. | Allowed. | Show payment problem. |
| `UNPAID` | Paid protection locked after grace. | Allowed. | Never block parent settings access. |
| `EXPIRED` | Paid protection locked. | Allowed. | Offer restore / subscribe when Billing is implemented. |
| `OFFLINE_GRACE` | Protection can run temporarily from cached active entitlement. | Allowed. | Must not exceed documented grace limits. |

## Phase 10B — Play Console Product Setup

- [ ] Play Console app exists.
- [ ] Play App Signing / upload key flow is configured.
- [ ] Android App Bundle upload path is tested.
- [ ] Subscription product is created in Play Console.
- [ ] Base plan is configured.
- [ ] License testers are configured.
- [ ] Test payment flow is available to testers.
- [x] Internal test runbook is prepared:
  `docs/SHORTS_BLOCKER_PLAY_BILLING_INTERNAL_TEST_RUNBOOK.md`.
- [x] Play Console billing configuration sheet is prepared:
  `docs/SHORTS_BLOCKER_PLAY_CONSOLE_BILLING_CONFIG.md`.

## Phase 10C — App-Side Play Billing Integration

- [x] Play Billing dependency is added.
- [x] BillingClient lifecycle is implemented.
- [x] Product details loading is implemented.
- [x] Purchase flow starts only from a parent-controlled screen.
- [x] Purchase success is handled.
- [x] Purchase canceled is handled.
- [x] Purchase pending is handled if available for the product type.
- [x] Billing unavailable state is handled.
- [x] Purchase acknowledgement is implemented.
- [x] Restore purchases is implemented.
- [x] Manage subscription opens Google Play subscription management.
- [x] Subscription card discloses price when loaded, monthly auto-renewal,
  post-free-test requirement, and Google Play manage/cancel path.
- [x] Subscription status/terms copy is covered by unit tests.
- [x] Manage subscription failure is handled without crashing if no activity can
  open the Google Play subscription URL.

Implementation evidence:

- `com.android.billingclient:billing-ktx:9.0.0` is declared through the Gradle
  version catalog.
- `PlayBillingRepository` owns a single BillingClient connection, product
  loading, purchase launch, purchase updates, purchase acknowledgement, restore,
  and manage-subscription intent.
- Dashboard billing actions are parent-controlled because dashboard access
  requires the parent PIN after initial setup.
- Pending purchases do not unlock protection until purchase state is
  `PURCHASED`.
- Subscription terms are visible before the app launches the Google Play
  purchase flow.
- Billing UI copy is centralized in `BillingCopy` and covered by
  `BillingCopyTest`.

Pending verification:

- Play Console subscription product must exist before product details can load
  in a real Play build.
- License tester purchase, restore, cancel, expire, pending, and payment issue
  flows still require Play Console testing.
- Execute `docs/SHORTS_BLOCKER_PLAY_BILLING_INTERNAL_TEST_RUNBOOK.md`.
- Enter and verify `docs/SHORTS_BLOCKER_PLAY_CONSOLE_BILLING_CONFIG.md` in Play
  Console.

## Phase 10D — Entitlement Integration

- [x] Active subscription enables protection.
- [ ] Canceled-but-active subscription keeps protection until period end.
- [x] Expired subscription disables paid protection after a successful restore/query reports no active purchase.
- [x] Expired subscription never blocks parent access to settings.
- [x] Payment problem state is visible to parent as Billing unavailable / restore / purchase failure messaging.
- [x] Local cached entitlement is conservative.
- [x] Future billing verification timestamps are ignored instead of extending
  entitlement.
- [x] Free test / paid entitlement precedence is documented.
- [x] Unit tests cover entitlement decisions.
- [ ] Manual tests cover purchase, restore, cancel, expire, and payment problem states.

## Phase 10E — Backend Verification

- [x] Decide whether backend verification is required before production rollout.
- [x] Backend decision document is prepared:
  `docs/SHORTS_BLOCKER_BILLING_BACKEND_DECISION.md`.
- [x] Backend module is added under `billing-backend/`.
- [x] Backend verification endpoint is added: `POST /billing/play/verify`.
- [x] Entitlement status endpoint is added: `GET /entitlement/status`.
- [x] RTDN receiver endpoint is added: `POST /billing/play/rtdn`.
- [x] Backend verifies purchase tokens with Google Play Developer API.
- [x] Backend stores only billing technical data.
- [x] Backend does not receive child data, YouTube data, browsing history,
  video data, app usage details, or accessibility tree dumps.
- [x] Android app can opt into backend verification through
  `SBK_BILLING_BACKEND_BASE_URL`.
- [ ] Backend is deployed behind HTTPS with runtime credentials.
- [ ] RTDN Pub/Sub push is configured in Play Console and pointed to deployed backend.
- [ ] Durable production storage and backup policy are configured for entitlement records.
- [ ] Backend rate limiting / edge protection is configured in deployment.
- [ ] End-to-end Play tester verification is recorded against deployed backend.

## Current Evidence

- Main roadmap now locks production work to Play Store / AAB / Google Play Billing.
- Website APK + Stripe plan exists only as a deferred reference.
- Billing policy lock is documented for subscription, price, planned product ID,
  and entitlement behavior.
- App-side Play Billing SDK integration is implemented.
- Play Billing internal test runbook is prepared.
- Play Console billing configuration sheet is prepared.
- Backend verification decision document is prepared.
- Backend verification baseline is implemented locally.
- Billing cannot be approved for production release until Play product setup,
  backend deployment, RTDN configuration, license tester QA, and subscription
  lifecycle QA are complete.
