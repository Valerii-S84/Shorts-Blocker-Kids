# Shorts Blocker Kids Billing Backend Decision

Status: Option A selected. Backend verification is required before production rollout.

Date: May 24, 2026

## Decision Needed

Before broad production rollout, Shorts Blocker Kids ships with:

1. app-side Google Play Billing; plus
2. secure backend verification and Real-time
   Developer Notifications.

App-side-only Billing remains acceptable only for internal Play tester
validation before the backend deployment is live.

## Official Sources Checked

- Android Developers: integrate Google Play Billing:
  <https://developer.android.com/google/play/billing/integrate>
- Android Developers: integrate Google Play with your server backend:
  <https://developer.android.com/google/play/billing/backend>
- Android Developers: subscription lifecycle:
  <https://developer.android.com/google/play/billing/lifecycle/subscriptions>
- Android Developers: fight fraud and abuse:
  <https://developer.android.com/google/play/billing/security>

## Current Implementation

The current app implements:

- product details loading;
- purchase flow;
- purchase update handling;
- pending/cancel/error handling;
- restore purchases through `queryPurchasesAsync`;
- local entitlement cache with 72-hour offline grace;
- release backend verification through
  `SBK_BILLING_BACKEND_BASE_URL=https://billing.movashield.de`;
- backend `POST /billing/play/verify`;
- backend `GET /entitlement/status`;
- backend `POST /billing/play/rtdn`;
- Google Play Developer API verification through application-default
  credentials;
- backend acknowledgement after verified active entitlement;
- hashed purchase-token storage in the backend store.

Debug builds still support client-only Billing when no backend URL is
configured so internal development can continue before backend deployment.
Release builds fail closed without a valid HTTPS backend URL.

## Backend Benefits

A secure backend would improve:

- purchase authenticity verification through Google Play Developer API;
- server-side acknowledgement reliability;
- subscription lifecycle handling after app uninstall or non-use;
- cancellation, refund, revoke, grace-period, account-hold, and expired state
  accuracy;
- Real-time Developer Notifications processing;
- fraud and replay resistance.

## Backend Privacy Boundary

If backend verification is added, it must receive only billing technical data:

```text
package name
subscription product ID
purchase token
entitlement status
last verification timestamp
optional install identifier if explicitly approved
```

It must not receive:

```text
child data
YouTube watch history
video titles
URLs
comments
messages
screen recordings
audio
location
contacts
raw Accessibility tree dumps
blocked-event history
```

## Acceptable Internal Testing Decision

For Play internal testing, proceed with app-side Billing only.

Reason:

- Play Console product and license tester flows must be validated first;
- Play Console product and license tester flows must be validated first;
- the app stores only local entitlement status;
- internal testing can validate purchase, restore, pending, cancel, and expire
  UI behavior before backend scope is opened.

## Production Decision Gate

Do not mark Billing `Done` for production until backend deployment is verified:

### Required before production

- HTTPS deployment;
- DNS/TLS verified for `https://billing.movashield.de`;
- Google Play Developer API credentials stored outside the repository;
- `GOOGLE_APPLICATION_CREDENTIALS` configured on the backend runtime;
- Play Console RTDN Pub/Sub authenticated push configured to
  `https://billing.movashield.de/billing/play/rtdn`;
- durable entitlement storage and backup policy;
- rate limiting / edge protection;
- secure logging rules excluding child/app-usage data;
- integration tests and Play tester lifecycle QA.
