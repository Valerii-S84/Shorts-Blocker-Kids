# Short Video Blocker Billing And Backend Plan

Status: Partial. App-side Google Play Billing and a repository-local backend
verification baseline are implemented. Backend deployment, RTDN configuration,
and Play tester lifecycle evidence remain pending.

Current production decision document:

```text
docs/SHORTS_BLOCKER_BILLING_BACKEND_DECISION.md
```

## Goals

- Use Google Play Billing for Play-distributed app purchases.
- Keep purchase flow parent-controlled.
- Verify entitlement reliably.
- Support restore purchases.
- Support subscription lifecycle changes.
- Avoid collecting child viewing history or app usage details.
- Keep backend data limited to billing technical data.

## Non-Goals

- No Stripe inside the Play-distributed app.
- No alternate payment links inside the Play-distributed app.
- No manual license key activation.
- No ads.
- No analytics unless explicitly approved later.
- No backend storage of short-video detections, blocked events, YouTube/TikTok/
  Instagram/Facebook history, Accessibility trees, URLs, video titles, account
  names, messages, screen recordings, audio, location, contacts, or browsing
  history.

## Google Play Billing Architecture

App-side components:

- Billing client lifecycle. Implemented locally.
- Product details loading. Implemented locally.
- Parent-controlled purchase screen. Implemented locally.
- Purchase flow. Implemented locally.
- Purchase acknowledgement. Implemented locally.
- Restore purchases. Implemented locally.
- Manage subscription entry to Google Play. Implemented locally.
- Local entitlement cache. Implemented locally.
- Play Console license-tester lifecycle verification. Pending.

Backend components:

- Purchase token verification with Google Play Developer API. Implemented in
  `billing-backend`.
- Entitlement state service. Implemented in `billing-backend`.
- Real-time Developer Notifications receiver. Implemented in `billing-backend`.
- Subscription lifecycle processor.
- Minimal billing database.

Play Console components:

- Subscription product.
- Base plan.
- Offer configuration if used.
- License testers.
- Real-time Developer Notifications topic.
- Internal and closed test tracks.

## Backend Role

The backend should be added only when explicitly approved for the billing phase.

Backend responsibilities:

- Receive purchase token and app package/product identifiers from app.
- Verify purchase token with Google Play Developer API.
- Store minimal entitlement record.
- Process Real-time Developer Notifications.
- Return current entitlement state to the app.
- Provide restore/refresh endpoint.

Backend must not:

- Receive child viewing history.
- Receive supported-app usage events.
- Receive Accessibility tree dumps.
- Receive video titles, URLs, comments, captions, account names, messages,
  screenshots, audio, location, contacts, or browsing history.
- Run analytics on child behavior.

## Minimal Backend Data Model

Suggested entities:

```text
Installation:
  install_id: random app-generated identifier
  created_at
  last_entitlement_check_at

Purchase:
  install_id
  package_name
  product_id
  purchase_token_hash
  purchase_token_encrypted
  subscription_state
  expires_at
  auto_renewing
  acknowledged
  last_verified_at
  last_rtdn_at

Entitlement:
  install_id
  state
  active_until
  grace_until
  source
  updated_at
```

Privacy rule:

- `install_id` must be random and app-scoped.
- Do not use hardware identifiers, advertising ID, phone number, contact data,
  account names, or child identifiers.

## Entitlement Model

Target states:

- `ACTIVE`: paid entitlement currently active.
- `CANCELED_ACTIVE`: user canceled but access remains until paid period end.
- `PENDING`: payment pending; product decision required before enabling paid
  protection.
- `IN_GRACE`: payment problem but Google subscription is in grace period.
- `ON_HOLD`: payment issue beyond grace; paid protection disabled or locked
  according to product decision.
- `EXPIRED`: subscription expired.
- `REVOKED`: refunded, charged back, or revoked.
- `UNKNOWN`: app cannot verify state.

Access rules:

- Active and canceled-active states enable paid protection.
- Expired, revoked, and on-hold states do not enable paid protection.
- Unknown may use offline grace only if there was a recently verified active
  entitlement.
- Parent settings must remain accessible even if entitlement is expired.

## Subscription Lifecycle

Required flows:

- First purchase.
- Purchase acknowledgement.
- Renewal.
- User cancellation.
- Resubscribe/restore.
- Payment pending.
- Grace period.
- Account hold.
- Expiration.
- Refund/revocation.
- Product/base plan changes.

The app must handle every state without trapping the parent away from settings.

## Real-Time Developer Notifications

RTDN target flow:

1. Google Play sends subscription notification to Pub/Sub.
2. Backend validates the notification source.
3. Backend uses purchase token to query Google Play Developer API.
4. Backend updates subscription and entitlement records.
5. App receives updated entitlement on next refresh or restore.

RTDN rules:

- RTDN is a signal, not the source of truth.
- Backend must query Google Play before changing entitlement.
- Duplicate notifications must be idempotent.
- Missing notifications must be corrected by periodic verification or app
  refresh.

## Restore Purchase

Restore requirements:

- App must call Google Play Billing purchase query APIs.
- App sends current purchase token to backend if backend verification is active.
- Backend verifies token before returning active entitlement.
- Restore must work after reinstall.
- Restore must not require manual license keys.
- Manage subscription opens Google Play subscription management.

## Offline Grace Behavior

Offline behavior must be conservative and parent-friendly.

Suggested policy:

- If app has a recently verified active entitlement, allow short offline grace.
- If local cache is expired and backend cannot be reached, show entitlement
  unknown and keep parent settings accessible.
- Do not start a new paid entitlement offline.
- Do not extend protection indefinitely from stale local state.

Example values to decide during implementation:

- Online verification interval: 24 hours.
- Offline grace after active verification: 72 hours.
- Hard stale limit: 7 days.

These values are product decisions and must be finalized before implementation.

## App-Side Billing States

UI states:

- Product loading.
- Product unavailable.
- Billing unavailable.
- Purchase in progress.
- Purchase canceled.
- Purchase pending.
- Purchase successful, waiting for acknowledgement.
- Entitlement active.
- Entitlement expired.
- Restore successful.
- Restore not found.
- Backend verification unavailable.

Parent copy must avoid blaming the child and must explain what parent action is
needed.

## Privacy Limits

App-to-backend payloads may include:

- app package name;
- app version;
- random install ID;
- Play product ID;
- Play purchase token;
- device-local entitlement cache timestamp;
- billing debug correlation ID without child data.

App-to-backend payloads must not include:

- child name;
- child profile;
- watched videos;
- blocked app event history;
- YouTube/TikTok/Instagram/Facebook account name;
- URLs;
- video titles;
- comments;
- captions;
- messages;
- contacts;
- location;
- browsing history;
- screen recordings;
- audio;
- raw Accessibility tree dumps.

## Security Requirements

- Use HTTPS only.
- Store purchase tokens encrypted or hash tokens when raw token is not needed.
- Validate request schema.
- Rate-limit entitlement endpoints.
- Do not log full purchase tokens.
- Do not log personal or sensitive data.
- Validate RTDN message source.
- Make entitlement updates idempotent.
- Separate production and test Play Console credentials.
- Keep Google Play Developer API credentials out of the app.

## Implementation Phases

1. Billing policy lock:
   - final subscription product type;
   - final product ID;
   - entitlement state rules;
   - offline grace rules.
2. App-side Play Billing:
   - product details: implemented locally;
   - purchase: implemented locally;
   - acknowledgement: implemented locally;
   - restore: implemented locally;
   - manage subscription: implemented locally;
   - license tester lifecycle QA: pending.
3. Backend skeleton:
   - health endpoint: implemented;
   - purchase verification endpoint: implemented;
   - entitlement endpoint: implemented;
   - secure config: implemented through runtime environment variables.
4. RTDN:
   - Pub/Sub setup;
   - notification validation: implemented through runtime shared secret;
   - lifecycle processor: implemented for verified purchase-token updates.
5. QA:
   - license testers;
   - test purchase;
   - renew/cancel/expire;
   - restore after reinstall;
   - offline grace;
   - backend failure.

## Policy References

- Google Play Billing subscriptions:
  https://developer.android.com/google/play/billing/subscriptions
- Google Play Data Safety form guidance:
  https://support.google.com/googleplay/android-developer/answer/10787469
- Google Play User Data policy:
  https://support.google.com/googleplay/android-developer/answer/10144311
