# Shorts Blocker Kids — Phase 10 Billing Checklist

Status: Partial. Active production monetization path is Google Play Store + Android App Bundle + Google Play Billing. Website APK + Stripe is deferred until the Play Store production track is complete.

## Execution Lock

- [x] Google Play Billing is the active monetization path for Play Store distribution.
- [x] Website APK + Stripe is not part of the current production execution track.
- [x] No alternate payment links are allowed inside a Play-distributed app.
- [x] No manual license key or code entry activation is allowed in the Play Store track.
- [x] Billing work must wait until P0 policy package and real-device blocker QA are not blocked.

## Phase 10A — Billing Policy Lock

- [ ] Confirm final product type: recurring subscription.
- [ ] Confirm first subscription price: `€2.20/month`.
- [ ] Choose final Play product ID.
- [ ] Document entitlement rules for active, canceled, expired, pending, past_due, and unpaid states.
- [ ] Confirm no ads monetization in v1.
- [ ] Confirm no non-Play payment CTA in Play-distributed app.

## Phase 10B — Play Console Product Setup

- [ ] Play Console app exists.
- [ ] Play App Signing / upload key flow is configured.
- [ ] Android App Bundle upload path is tested.
- [ ] Subscription product is created in Play Console.
- [ ] Base plan is configured.
- [ ] License testers are configured.
- [ ] Test payment flow is available to testers.

## Phase 10C — App-Side Play Billing Integration

- [ ] Play Billing dependency is added.
- [ ] BillingClient lifecycle is implemented.
- [ ] Product details loading works.
- [ ] Purchase flow starts only from a parent-controlled screen.
- [ ] Purchase success is handled.
- [ ] Purchase canceled is handled.
- [ ] Purchase pending is handled if available for the product type.
- [ ] Billing unavailable state is handled.
- [ ] Purchase acknowledgement is implemented.
- [ ] Restore purchases works after reinstall.
- [ ] Manage subscription opens Google Play subscription management.

## Phase 10D — Entitlement Integration

- [ ] Active subscription enables protection.
- [ ] Canceled-but-active subscription keeps protection until period end.
- [ ] Expired subscription disables paid protection.
- [ ] Expired subscription never blocks parent access to settings.
- [ ] Payment problem state is visible to parent.
- [ ] Local cached entitlement is conservative.
- [ ] Free test / paid entitlement precedence is documented.
- [ ] Unit tests cover entitlement decisions.
- [ ] Manual tests cover purchase, restore, cancel, expire, and payment problem states.

## Phase 10E — Optional Backend Verification Decision

- [ ] Decide whether backend verification is required before production rollout.
- [ ] If backend is added, it verifies purchase tokens with Google Play Developer API.
- [ ] If backend is added, it stores only billing technical data.
- [ ] If backend is added, it does not receive child data, YouTube data, browsing history, video data, app usage details, or accessibility tree dumps.
- [ ] If backend is deferred, residual fraud risk is documented.

## Current Evidence

- Main roadmap now locks production work to Play Store / AAB / Google Play Billing.
- Website APK + Stripe plan exists only as a deferred reference.
- Backend is not implemented.
- App-side Play Billing is not implemented.
- Billing cannot be considered production-ready until policy, QA, Play product setup, app integration, and entitlement tests are complete.
