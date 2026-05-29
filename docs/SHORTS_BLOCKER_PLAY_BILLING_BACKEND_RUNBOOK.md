# Shorts Blocker Kids Play Billing Backend Runbook

Status: Repository production flow prepared. Android release builds now require
an HTTPS billing backend URL. Live deployment, Play Console RTDN setup, and
license-tester evidence remain pending.

Date: May 28, 2026

## Scope

This runbook covers the repository-local `billing-backend` module used for
Google Play purchase verification, entitlement status, and Real-time Developer
Notifications.

It does not create Play Console credentials, Pub/Sub topics, DNS, TLS
certificates, or hosting. Those remain runtime deployment tasks outside the
repository.

Official references checked for this backend:

- Google Play backend integration:
  <https://developer.android.com/google/play/billing/backend>
- RTDN reference:
  <https://developer.android.com/google/play/billing/rtdn-reference>
- Pub/Sub authenticated push:
  <https://docs.cloud.google.com/pubsub/docs/authenticate-push-subscriptions>
- Subscriptions v2 API:
  <https://developers.google.com/android-publisher/api-ref/rest/v3/purchases.subscriptionsv2>
- Subscription acknowledge API:
  <https://developers.google.com/android-publisher/api-ref/rest/v3/purchases.subscriptions/acknowledge>

## Runtime Inputs

Required outside the repository:

```text
SBK_ENV=production
SBK_BACKEND_PORT=8080
SBK_PUBLIC_BASE_URL=https://billing.example.com
SBK_REQUIRE_HTTPS=true
SBK_PACKAGE_NAME=com.shortsblockerkids
SBK_PLAY_SUBSCRIPTION_PRODUCT_ID=shorts_blocker_kids_monthly
SBK_DATABASE_URL=jdbc:postgresql://billing-db:5432/shorts_blocker_kids
SBK_DATABASE_USER=<runtime database user>
SBK_DATABASE_PASSWORD=<runtime database password>
GOOGLE_APPLICATION_CREDENTIALS=/run/secrets/google-service-account.json
SBK_RTDN_PUBSUB_AUDIENCE=https://billing.example.com/billing/play/rtdn
SBK_RTDN_PUBSUB_SERVICE_ACCOUNT_EMAIL=<pubsub push service account email>
```

Do not commit service-account JSON, runtime secrets, keystores, database
passwords, or production environment files.

## Local Commands

```bash
./gradlew :billing-backend:test
./gradlew :billing-backend:run
```

Production-like Docker Compose flow:

```bash
./scripts/backend_deploy_compose.sh
curl -fsS http://127.0.0.1:8080/health
```

`docker-compose.yml` runs PostgreSQL with a named volume, runs migrations at
backend startup, mounts Google credentials through a Docker Compose secret, and
uses `GET /health` for container healthchecks.

Production-configured Android build:

```bash
SBK_BILLING_BACKEND_BASE_URL=https://billing.example.com \
ANDROID_HOME=/home/serputko/Android/Sdk ./gradlew :app:bundleRelease
```

Release builds fail closed when `SBK_BILLING_BACKEND_BASE_URL` is blank or is
not an HTTPS URL. Debug builds may omit the backend URL for internal client-only
Billing Library testing.

Do not remove `android.permission.INTERNET` or
`android.permission.ACCESS_NETWORK_STATE` from the release manifest until there
is separate proof that Google Play Billing and production backend verification
continue to work without them.

## Endpoints

```http
GET /health
```

```http
POST /billing/play/verify
X-Forwarded-Proto: https
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
X-Forwarded-Proto: https
```

```http
POST /billing/play/rtdn
Authorization: Bearer <pubsub-signed-jwt>
X-Forwarded-Proto: https
Content-Type: application/json
```

The RTDN endpoint expects a Google Pub/Sub push body. RTDN is treated only as a
signal: the backend queries the Google Play Developer API before changing
entitlement state. Duplicate Pub/Sub `messageId` values are persisted and
processed idempotently.

For local development only, RTDN can use `SBK_RTDN_SHARED_SECRET` and
`X-SBK-RTDN-Secret` when Pub/Sub JWT config is absent. Production config rejects
missing Pub/Sub JWT settings.

## Storage And Migrations

Production storage:

- PostgreSQL through `SBK_DATABASE_URL`;
- migration runner applies `billing-backend/migrations/*.sql` in filename order;
- applied migration versions are tracked in `schema_migrations`;
- `entitlements.purchase_token_hash` is unique;
- raw purchase tokens are used only for Google Play verification and
  acknowledgement, then discarded;
- RTDN message IDs are persisted for idempotency.

Local development fallback:

```text
SBK_BACKEND_STORE_FILE=/secure/path/billing-backend-data.json
```

Production config rejects file storage.

Migration files:

```text
billing-backend/migrations/001_create_entitlements.sql
billing-backend/migrations/002_create_processed_rtdn_messages.sql
```

## Backup And Restore

PostgreSQL backup:

```bash
SBK_DATABASE_URL=jdbc:postgresql://host:5432/shorts_blocker_kids \
SBK_DATABASE_USER=<runtime database user> \
SBK_DATABASE_PASSWORD=<runtime database password> \
SBK_BACKUP_DIR=/secure/path/backups \
./scripts/backend_backup.sh
```

Docker Compose managed PostgreSQL backup:

```bash
SBK_BACKUP_WITH_DOCKER_COMPOSE=true \
SBK_BACKUP_DIR=/secure/path/backups \
./scripts/backend_backup.sh
```

PostgreSQL restore:

```bash
SBK_DATABASE_URL=jdbc:postgresql://host:5432/shorts_blocker_kids \
SBK_DATABASE_USER=<runtime database user> \
SBK_DATABASE_PASSWORD=<runtime database password> \
./scripts/backend_restore.sh /secure/path/backups/postgres-YYYYMMDDTHHMMSSZ.dump
```

Docker Compose managed PostgreSQL restore:

```bash
SBK_BACKUP_WITH_DOCKER_COMPOSE=true \
./scripts/backend_restore.sh /secure/path/backups/postgres-YYYYMMDDTHHMMSSZ.dump
```

The restore script first writes a pre-restore dump, then runs `pg_restore
--clean --if-exists --no-owner`.

File-store backup/restore remains supported only for local development by
omitting `SBK_DATABASE_URL`.

## Logging And Audit

The backend writes JSON-line structured logs to stdout:

- `http.request.completed` for request status and latency;
- `billing.verify.completed` for successful purchase verification;
- `billing.rtdn.processed` and `billing.rtdn.duplicate` for RTDN handling;
- `billing.request.failed` for billing endpoint failures.

Audit logs include request ID, endpoint, install ID where applicable, product
ID, entitlement state, acknowledgement status, and a short purchase-token hash
prefix. They must not include raw purchase tokens, credentials, request bodies,
child data, Accessibility trees, URLs, video titles, account names, or supported
app activity.

## Rate Limiting And Edge

Backend controls:

- per-remote-address rate limits for verify/status/RTDN endpoints;
- request body size limit through `SBK_MAX_REQUEST_BYTES`;
- `X-Forwarded-Proto: https` required for billing endpoints when
  `SBK_REQUIRE_HTTPS=true`;
- exact endpoint path checks.

Required edge controls:

- TLS termination before the backend process;
- reverse proxy sets `X-Forwarded-Proto`;
- RTDN endpoint uses authenticated Pub/Sub push;
- optional IP allowlisting where hosting supports it;
- healthcheck access limited to deployment or monitoring systems.

## Privacy Boundary

Allowed backend data:

- random app installation ID;
- package name;
- app version;
- subscription product ID;
- purchase token only during request processing;
- hashed purchase token at rest;
- entitlement state and timestamps;
- RTDN Pub/Sub message IDs.

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

## Rollback Plan

Before deployment:

1. Capture the currently running backend image tag.
2. Run `./scripts/backend_backup.sh`.
3. Deploy the new image through `./scripts/backend_deploy_compose.sh`.
4. Verify `GET /health` and one Play license-tester verification flow.

Application rollback:

```bash
./scripts/backend_rollback_compose.sh <previous-backend-image>
curl -fsS http://127.0.0.1:8080/health
```

Data rollback:

1. Stop the backend container.
2. Restore the last known-good dump with `./scripts/backend_restore.sh`.
3. Start the previous backend image.
4. Verify `GET /health`, `GET /entitlement/status`, and one Play tester
   restore flow.

Only restore database state when the failed release wrote incompatible or
incorrect data. Prefer application rollback alone when schema and data remain
compatible.

## Production Gates

- Backend is deployed behind HTTPS.
- `SBK_PUBLIC_BASE_URL` and app `SBK_BILLING_BACKEND_BASE_URL` point to the
  same public HTTPS backend origin.
- Google Play Developer API credentials are mounted only from runtime secret
  storage.
- Play Console RTDN Pub/Sub push is configured with authenticated push.
- PostgreSQL storage is live, migrated, and backed up.
- Rate limiting, request body limits, structured logs, and audit logs are
  enabled.
- Backend logs are checked for raw token, credential, and child-data leakage.
- App build is generated with `SBK_BILLING_BACKEND_BASE_URL`.
- `./gradlew :billing-backend:test` passes before image build.
- `./gradlew :app:testDebugUnitTest :app:lintRelease :app:bundleRelease` passes
  with the production HTTPS backend URL supplied.
- Play license tester purchase, restore, pending, cancel, grace, account hold,
  expire, refund/revoke, and non-license sanity flows pass against the deployed
  backend.
