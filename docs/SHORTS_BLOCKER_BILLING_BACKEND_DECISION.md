# Shorts Blocker Kids Billing Backend Decision

Status: Open for production decision.

Date: May 23, 2026

## Decision Needed

Before broad production rollout, decide whether Shorts Blocker Kids ships with:

1. app-side Google Play Billing only; or
2. app-side Google Play Billing plus secure backend verification and Real-time
   Developer Notifications.

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

The current app implements app-side Billing only:

- product details loading;
- purchase flow;
- purchase update handling;
- pending/cancel/error handling;
- app-side `acknowledgePurchase()` after purchased state;
- restore purchases through `queryPurchasesAsync`;
- local entitlement cache with 72-hour offline grace;
- no purchase token is stored locally;
- no app backend exists.

This is enough for internal tester validation of the app flow, but it is not the
strongest production-grade subscription lifecycle architecture.

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
- no backend infrastructure exists in this repository;
- the app stores only local entitlement status;
- internal testing can validate purchase, restore, pending, cancel, and expire
  UI behavior before backend scope is opened.

## Production Decision Gate

Do not mark Billing `Done` for production until one of these is explicitly
decided:

### Option A — Add backend before production

Required work:

- backend service;
- Google Play Developer API credentials;
- purchase verification endpoint;
- entitlement endpoint;
- RTDN Pub/Sub receiver;
- subscription lifecycle processor;
- secure logging rules excluding child/app-usage data;
- integration tests and Play tester lifecycle QA.

### Option B — Defer backend for v1 production

Required documentation:

- residual fraud risk accepted;
- lifecycle accuracy relies on app startup/restore queries;
- no server-side RTDN;
- no server-side acknowledgement;
- local entitlement cache remains conservative;
- support process exists for refund/revoke edge cases through Play Console.

Until Option A or Option B is chosen, backend verification remains an open
production decision.
