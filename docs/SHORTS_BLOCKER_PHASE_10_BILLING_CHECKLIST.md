# Shorts Blocker Kids — Phase 10 Billing Checklist

Status: Partial. First-launch billing path is website APK distribution with Stripe subscription backend. Google Play Billing is deferred.

## Phase 10A — Play Billing Freeze

- [x] Google Play Billing app-side preparation is documented as Partial / Deferred.
- [x] Google Play Billing is not a production blocker for website APK launch.
- [x] Website APK + Stripe is the active first-launch billing path.
- [x] First-launch subscription price is `€2.20/month`.
- [x] No backend code is implemented in Phase 10A.
- [x] No Android Stripe UI is implemented in Phase 10A.
- [x] No production code is changed in Phase 10A.

Deferred Play Billing notes:

- Existing Play Billing code can remain for later reuse.
- Do not require Play Console products, Play Billing test purchases, or Play reviewer paid flow for website APK launch.
- If the app is later distributed through Google Play and sells digital subscription access inside the app, Google Play Billing policy must be reviewed again before release.

## Phase 10B — Website APK Distribution Checklist

- [ ] Website has APK download page.
- [ ] Website explains APK install/sideloading steps clearly.
- [ ] Website explains `€2.20/month` subscription before install/payment.
- [ ] Website links Privacy Policy.
- [ ] Website links Terms and subscription cancellation info.
- [ ] APK release signing process is documented.
- [ ] APK checksum is published on the download page.
- [ ] APK install is tested on a real Android phone.
- [ ] First app launch explains local Shorts blocking, parent PIN, and subscription price.
- [ ] No Play Store-specific billing language blocks website launch.

## Phase 10C — Stripe Backend Production Checklist

- [ ] Backend service exists.
- [ ] Database schema exists.
- [ ] Stripe test mode is configured.
- [ ] Stripe production mode is documented.
- [ ] `POST /billing/checkout-session` creates Stripe Checkout sessions.
- [ ] `POST /billing/webhook/stripe` verifies Stripe webhook signatures.
- [ ] Webhook processing is idempotent by Stripe event ID.
- [ ] `GET /entitlement/status?install_id=...` returns app entitlement state.
- [ ] `POST /billing/customer-portal` creates Stripe Customer Portal sessions.
- [ ] Backend supports active subscription state.
- [ ] Backend supports cancel-at-period-end state.
- [ ] Backend supports expired/canceled state.
- [ ] Backend supports past_due/unpaid state.
- [ ] Backend supports Stripe test and production mode separation.
- [ ] Backend logs contain no child data, YouTube data, browsing history, video data, app usage details, or accessibility tree dumps.

Required Stripe events:

- [ ] `checkout.session.completed`
- [ ] `customer.subscription.created`
- [ ] `customer.subscription.updated`
- [ ] `customer.subscription.deleted`
- [ ] `invoice.payment_succeeded`
- [ ] `invoice.payment_failed`

Required secrets/env vars:

- [ ] `STRIPE_SECRET_KEY`
- [ ] `STRIPE_WEBHOOK_SECRET`
- [ ] `STRIPE_PRICE_MONTHLY_EUR_220`
- [ ] `STRIPE_CUSTOMER_PORTAL_RETURN_URL`
- [ ] `STRIPE_CHECKOUT_SUCCESS_URL`
- [ ] `STRIPE_CHECKOUT_CANCEL_URL`
- [ ] `DATABASE_URL`
- [ ] `APP_LINK_HOST`
- [ ] `BACKEND_PUBLIC_BASE_URL`
- [ ] `ENVIRONMENT`

## Phase 10D — Automatic App Activation Checklist

- [ ] App generates local `install_id`.
- [ ] App generates local `install_secret`.
- [ ] App starts payment inside the app.
- [ ] Stripe Checkout opens in browser/custom tab.
- [ ] Stripe success redirect returns to app App Link or website fallback.
- [ ] Redirect is not trusted as proof of payment.
- [ ] App polls backend entitlement after payment.
- [ ] Webhook-activated entitlement unlocks protection automatically.
- [ ] No manual license key screen exists.
- [ ] No code entry activation exists.
- [ ] Protection logic stays local after entitlement is cached.
- [ ] Backend receives only billing technical data.

## Phase 10E — Subscription Management Checklist

- [ ] App has `Manage subscription` button.
- [ ] Manage button opens Stripe Customer Portal.
- [ ] Parent can cancel subscription through Customer Portal.
- [ ] Cancel-at-period-end keeps protection until `current_period_end`.
- [ ] Expired subscription locks protection.
- [ ] Failed payment shows payment problem state.
- [ ] Restore subscription uses email magic link.
- [ ] Restore flow has no manual license key.
- [ ] Offline grace lasts 72 hours after last successful active entitlement check.
- [ ] Offline grace never extends beyond `current_period_end + 24h`.

## Current Evidence

- Roadmap separates website Stripe launch from future Google Play launch.
- Phase 10 website Stripe plan exists in `docs/SHORTS_BLOCKER_PHASE_10_WEBSITE_STRIPE_PLAN.md`.
- Play Billing is deferred as first-launch production blocker.
- Backend is not implemented yet.
- Android Stripe UI is not implemented yet.
- Website APK page is not implemented yet.
