# Shorts Blocker Kids — Deferred Website + Stripe Plan

Status: Deferred / frozen. This document is retained as a reference only. Do not implement this path until the Play Store production execution track is complete.

Important: every section below is inactive historical planning material. It is not an implementation task list for the current production track.

## Decision

Current production execution track uses:

- Google Play Store;
- Android App Bundle;
- Google Play Billing.

The website APK + Stripe path is deferred and must not be implemented during the active Play Store production track:

- website APK download;
- Telegram ads;
- Stripe Checkout;
- Stripe Customer Portal;
- backend entitlement;
- automatic app activation without license key.

Before this path can become active later, it requires a separate explicit decision, policy review, security review, and implementation plan.

## Deferred Product Constraints

- No manual license key.
- No code entry activation.
- App starts payment inside the app.
- Stripe Checkout opens in browser or custom tab.
- Stripe webhook activates entitlement.
- App polls backend entitlement after payment.
- Protection logic stays local on the phone.
- Backend receives no child data.
- Backend receives no YouTube data, browsing history, video data, URLs, titles, comments, account names, screenshots, audio, or accessibility tree dumps.
- Cancellation and payment method updates happen through Stripe Customer Portal.
- Restore uses email magic link, not manual key/code entry.
- Offline grace is 72 hours and cannot go beyond `current_period_end + 24h`.

## Deferred Price

```text
Product: Shorts Blocker Kids
Price: €2.20/month
Currency: EUR
Billing interval: monthly
Provider: Stripe
Checkout mode: subscription
```

## Deferred Exact User Flow

1. Parent downloads APK from the website.
2. Parent installs APK on the child phone.
3. Parent opens the app.
4. Welcome screen explains:
   - blocks YouTube Shorts for children;
   - works locally on the phone;
   - parent controls it with PIN;
   - subscription costs €2.20/month.
5. Parent creates PIN.
6. App generates local `install_id` and `install_secret`.
7. App shows subscription screen with `Subscribe for €2.20/month`.
8. Parent taps subscribe.
9. App calls backend `POST /billing/checkout-session`.
10. Backend creates Stripe Checkout Session with `install_id` metadata.
11. App opens Checkout in browser/custom tab.
12. Parent pays in Stripe Checkout.
13. Stripe sends webhook to backend.
14. Backend stores subscription state from Stripe.
15. Stripe redirects to app App Link or website success page.
16. App treats redirect only as a signal and calls `GET /entitlement/status`.
17. If backend returns active entitlement, app caches it locally.
18. Protection becomes available.
19. If payment fails or entitlement is inactive, protection stays locked.

## Deferred Screen States

1. Welcome screen.
2. Parent PIN setup.
3. Subscription screen.
4. Payment processing state.
5. Active subscription state.
6. Payment failed state.
7. Subscription expired state.
8. Manage subscription screen.
9. Offline grace state.
10. Protection locked because subscription inactive.

No manual license key screen is allowed.

## Deferred Android App Responsibilities

If this path is explicitly reactivated later, the app would need to:

- generate and persist `install_id`;
- generate and persist `install_secret`;
- store parent PIN hash and salt locally;
- call backend for checkout session creation;
- open Stripe Checkout in browser/custom tab;
- receive App Link/deep link return after payment;
- poll entitlement after payment return;
- recheck entitlement on app start/resume;
- cache entitlement state locally;
- apply offline grace;
- lock/unlock protection from local cached entitlement;
- show subscription status clearly;
- provide `Manage subscription` button;
- never upload child/YouTube/app usage data.

Recommended local fields:

```text
installId: String
installSecretHash or local secret: String
subscriptionStateCached: UNKNOWN | ACTIVE | CANCELED_BUT_ACTIVE_UNTIL_END | PAST_DUE | UNPAID | EXPIRED | OFFLINE_GRACE
currentPeriodEndMillis: Long?
lastEntitlementCheckAtMillis: Long?
offlineGraceUntilMillis: Long?
stripeCustomerPortalLastOpenedAtMillis: Long?
```

## Backend Responsibilities

The backend must:

- create Stripe Checkout sessions;
- receive Stripe webhooks;
- verify Stripe webhook signatures;
- process webhook events idempotently;
- store subscription state;
- return entitlement status to app;
- create Stripe Customer Portal sessions;
- support cancellation at period end;
- support active, past_due, unpaid, canceled, and expired states;
- support Stripe test mode and production mode;
- avoid logging secrets and personal payment data beyond operational minimum.

Recommended location:

```text
backend/
```

Recommended stack:

```text
Runtime: Node.js
Language: TypeScript
HTTP: Fastify
Database: PostgreSQL
Payments: Stripe SDK
Tests: Vitest
```

Reason: Stripe webhook and Checkout examples are strongest in Node/TypeScript, and keeping backend in the same repo keeps app/API/schema changes versioned together for the first launch.

## API Draft

```http
GET /health
```

Returns backend health.

```http
POST /installations/register
Content-Type: application/json

{
  "install_id": "uuid",
  "install_secret_proof": "hash-or-signed-proof",
  "app_version": "0.1.0"
}
```

Registers or refreshes an installation.

```http
POST /billing/checkout-session
Content-Type: application/json

{
  "install_id": "uuid",
  "install_secret_proof": "hash-or-signed-proof"
}
```

Returns:

```json
{
  "checkout_url": "https://checkout.stripe.com/..."
}
```

```http
POST /billing/webhook/stripe
Stripe-Signature: ...
```

Stripe-only endpoint. Source of truth for subscription activation.

```http
GET /entitlement/status?install_id=...
```

Returns:

```json
{
  "status": "active",
  "current_period_end": "2026-06-20T12:00:00Z",
  "cancel_at_period_end": false,
  "server_time": "2026-05-20T12:00:00Z"
}
```

```http
POST /billing/customer-portal
Content-Type: application/json

{
  "install_id": "uuid",
  "install_secret_proof": "hash-or-signed-proof"
}
```

Returns:

```json
{
  "portal_url": "https://billing.stripe.com/..."
}
```

## Stripe Webhook Events

Must support:

- `checkout.session.completed`
- `customer.subscription.created`
- `customer.subscription.updated`
- `customer.subscription.deleted`
- `invoice.payment_succeeded`
- `invoice.payment_failed`

Webhook processing rules:

- Verify `Stripe-Signature`.
- Store `stripe_event_id`.
- Ignore duplicate event IDs.
- Treat Stripe subscription state as source of truth.
- Use `install_id` from Checkout Session metadata/client reference to attach first subscription.
- Do not activate entitlement from browser redirect alone.

## Subscription State Mapping

```text
Stripe trialing -> ACTIVE
Stripe active -> ACTIVE
Stripe active + cancel_at_period_end -> CANCELED_BUT_ACTIVE_UNTIL_END
Stripe past_due -> PAST_DUE
Stripe unpaid -> UNPAID
Stripe canceled -> EXPIRED
Stripe incomplete_expired -> EXPIRED
Missing subscription -> EXPIRED
No recent backend check but cached active inside grace -> OFFLINE_GRACE
Unknown backend result -> UNKNOWN
```

## Offline Grace

Rules:

- Grace starts only after a successful backend response with active entitlement.
- Duration: 72 hours from the last successful active entitlement check.
- Hard cap: never beyond `current_period_end + 24h`.
- If no previous active entitlement exists, offline grace is not available.
- If local time moves backward, app must not extend grace.

## Restore Flow

No license key and no code entry.

Recommended restore:

1. Parent taps `Restore subscription`.
2. App opens restore page or sends restore request.
3. Parent enters billing email on website/backend form.
4. Backend sends email magic link.
5. Parent opens link on the child phone.
6. App receives App Link with restore token.
7. App calls backend to attach current `install_id` to the Stripe customer/subscription.
8. App refreshes entitlement.

This is the minimum realistic restore path without accounts or manual license keys.

## Cancellation Flow

1. Parent opens `Manage subscription`.
2. App calls `POST /billing/customer-portal`.
3. App opens Stripe Customer Portal.
4. Parent cancels subscription or updates payment method.
5. Stripe sends webhook.
6. Backend updates subscription state.
7. App refreshes entitlement.
8. If `cancel_at_period_end=true`, protection remains available until `current_period_end`.
9. After expiry, protection locks.

## Database Schema Draft

```sql
installations(
  install_id uuid primary key,
  install_secret_hash text not null,
  app_version text,
  created_at timestamptz not null,
  last_seen_at timestamptz not null
)

stripe_customers(
  stripe_customer_id text primary key,
  livemode boolean not null,
  email_hash text,
  created_at timestamptz not null,
  updated_at timestamptz not null
)

subscriptions(
  stripe_subscription_id text primary key,
  install_id uuid references installations(install_id),
  stripe_customer_id text references stripe_customers(stripe_customer_id),
  status text not null,
  current_period_end timestamptz,
  cancel_at_period_end boolean not null default false,
  price_id text not null,
  activation_status text not null,
  livemode boolean not null,
  created_at timestamptz not null,
  updated_at timestamptz not null
)

checkout_sessions(
  stripe_session_id text primary key,
  install_id uuid references installations(install_id),
  stripe_customer_id text,
  status text not null,
  expires_at timestamptz,
  created_at timestamptz not null,
  updated_at timestamptz not null
)

stripe_webhook_events(
  stripe_event_id text primary key,
  type text not null,
  livemode boolean not null,
  payload_digest text not null,
  processed_at timestamptz not null
)
```

## Required Secrets And Env Vars

Do not commit real values.

```text
STRIPE_SECRET_KEY
STRIPE_WEBHOOK_SECRET
STRIPE_PRICE_MONTHLY_EUR_220
STRIPE_CUSTOMER_PORTAL_RETURN_URL
STRIPE_CHECKOUT_SUCCESS_URL
STRIPE_CHECKOUT_CANCEL_URL
DATABASE_URL
APP_LINK_HOST
BACKEND_PUBLIC_BASE_URL
ENVIRONMENT=test|production
```

## Production Evidence Required

Before production:

- Stripe test mode checkout succeeds with test card.
- Stripe test mode failed payment is handled.
- Webhook signature verification is tested.
- Duplicate webhook delivery is idempotent.
- Customer Portal cancellation is tested.
- Cancel-at-period-end keeps entitlement until period end.
- Expired/unpaid locks protection.
- App auto-activates after payment without license key.
- App restore via email magic link works on a real phone.
- Offline grace works for 72 hours and respects `current_period_end + 24h`.
- Backend logs verified to exclude child/YouTube/app usage data.
- Website APK download and install are tested on a real Android phone.

## Implementation Phases

### Phase A — Freeze Play Billing

- Keep existing Play Billing code if useful.
- Disable it from first production flow later through app config/build flag.
- If this deferred website path is explicitly reactivated later, mark Google
  Play Billing as deferred for that separate non-Play distribution track.

### Phase B — Backend Billing Skeleton

- Add `backend/`.
- Add database schema.
- Add Stripe test mode.
- Add checkout session creation.
- Add entitlement status endpoint.
- Add Stripe webhook processing.

### Phase C — Android Payment Integration

- Generate `install_id`.
- Add payment screen.
- Open Stripe Checkout.
- Poll entitlement after payment.
- Cache entitlement locally.
- Lock/unlock protection based on entitlement.

### Phase D — Subscription Management

- Add manage subscription button.
- Open Stripe Customer Portal.
- Handle cancel at period end.
- Handle failed payment.
- Handle expired subscription.

### Phase E — Tests

- Unit tests for entitlement resolver.
- Backend tests for webhook processing.
- Android tests for locked/unlocked states.
- Manual test checklist with Stripe test cards.
- Real phone APK install test.

### Phase F — Production Readiness

- Production signing.
- Website APK download page.
- Privacy Policy.
- Terms and subscription cancellation info.
- Stripe production keys.
- Server deployment.
- Backup/restore.
- Monitoring/logging.
- Release checklist.
