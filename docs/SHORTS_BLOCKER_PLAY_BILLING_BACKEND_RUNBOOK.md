# Shorts Blocker Kids Play Billing Backend Runbook

Status: Prepared. Deployment and Play Console RTDN setup remain pending.

Date: May 24, 2026

## Scope

This runbook covers the repository-local `billing-backend` module used for
Google Play purchase verification, entitlement status, and Real-time Developer
Notifications.

It does not create Play Console credentials, Pub/Sub topics, DNS, TLS
certificates, hosting, or production storage.

## Runtime Inputs

Required outside the repository:

```text
GOOGLE_APPLICATION_CREDENTIALS=/secure/path/service-account.json
SBK_BACKEND_PORT=8080
SBK_PACKAGE_NAME=com.shortsblockerkids
SBK_PLAY_SUBSCRIPTION_PRODUCT_ID=shorts_blocker_kids_monthly
SBK_BACKEND_STORE_FILE=/secure/path/billing-backend-data.json
SBK_RTDN_SHARED_SECRET=<runtime secret>
```

Do not commit service-account JSON, runtime secrets, keystores, or production
environment files.

## Local Commands

```bash
./gradlew :billing-backend:test
./gradlew :billing-backend:run
```

Production-configured Android build:

```bash
SBK_BILLING_BACKEND_BASE_URL=https://billing.example.com \
ANDROID_HOME=/home/serputko/Android/Sdk ./gradlew :app:bundleRelease
```

## Endpoints

```http
GET /health
```

```http
POST /billing/play/verify
Content-Type: application/json

{
  "install_id": "random-app-install-id",
  "package_name": "com.shortsblockerkids",
  "product_id": "shorts_blocker_kids_monthly",
  "purchase_token": "play-purchase-token",
  "app_version": "0.1.0"
}
```

```http
GET /entitlement/status?install_id=random-app-install-id
```

```http
POST /billing/play/rtdn
X-SBK-RTDN-Secret: <runtime secret>
Content-Type: application/json
```

The RTDN endpoint expects a Google Pub/Sub push body and uses the decoded
subscription purchase token as a signal. The backend still queries the Google
Play Developer API before changing entitlement state.

## Privacy Boundary

Allowed backend data:

- random app installation ID;
- package name;
- app version;
- subscription product ID;
- purchase token for verification;
- hashed purchase token at rest;
- entitlement state and timestamps.

Forbidden backend data:

- child data;
- supported app activity;
- watch history;
- video titles;
- URLs;
- comments;
- account names;
- messages;
- screen recordings;
- audio;
- location;
- contacts;
- browsing history;
- raw Accessibility tree dumps.

## Production Gates

- Backend is deployed behind HTTPS.
- Google Play Developer API credentials are available only in runtime secret
  storage.
- RTDN Pub/Sub push is configured in Play Console.
- `SBK_RTDN_SHARED_SECRET` is configured at the backend edge.
- Entitlement storage is durable and backed up.
- Backend logs are checked for token and child-data leakage.
- App build is generated with `SBK_BILLING_BACKEND_BASE_URL`.
- Play license tester purchase, restore, pending, cancel, grace, account hold,
  expire, and non-license sanity flows pass against the deployed backend.
