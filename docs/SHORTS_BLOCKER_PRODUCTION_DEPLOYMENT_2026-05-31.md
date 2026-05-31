# Shorts Blocker Kids Production Deployment - 2026-05-31

Status: Partial.

## Scope

Deployed on the production host reached through SSH alias `valerchik.de`
(`46.225.181.45`) in isolated Shorts Blocker Kids paths. Existing production
apps on the host were left running.

Public DNS is only partially aligned:

- `www.shortsblockerkids.de` resolves to `46.225.181.45` from the server.
- `billing.shortsblockerkids.de` resolves to `46.225.181.45` from the server.
- `shortsblockerkids.de` still resolves to `185.181.104.242`, so apex HTTPS is
  not live on the deployed host yet.

## Server Paths

- App root: `/opt/shorts-blocker-kids`
- Website root: `/var/www/shortsblockerkids.de`
- Runtime env: `/etc/shorts-blocker-kids/billing-backend.env`
- Log directory placeholder: `/var/log/shorts-blocker-kids`
- Reverse proxy config: `/opt/infra-caddy/Caddyfile`
- Backup directory: `/root/shorts-blocker-kids-backups/20260531T151405Z`

## Deployment Method

The server does not have Node.js, npm, Java, or Gradle installed. To avoid
installing toolchains on a shared production host, build artifacts were prepared
locally and copied to isolated server paths.

Local gates before deployment:

```bash
git status --short --branch
npm run check && npm run build && npm run smoke
./gradlew :billing-backend:test
ANDROID_HOME=/home/serputko/Android/Sdk ./gradlew :app:testDebugUnitTest
SBK_BILLING_BACKEND_BASE_URL=https://billing.shortsblockerkids.de ./gradlew :app:validateProductionReleaseConfig
./gradlew :billing-backend:test :billing-backend:installDist
```

Website files from `website/dist/` were copied to:

```text
/var/www/shortsblockerkids.de
```

The website is served by the Docker container:

```text
shorts-blocker-kids-website
```

The billing backend is served by Docker Compose:

```bash
cd /opt/shorts-blocker-kids
docker compose --env-file /etc/shorts-blocker-kids/billing-backend.env up -d --build
```

Compose project and containers:

```text
shorts-blocker-kids
shorts-blocker-kids-website
shorts-blocker-kids-billing-db
shorts-blocker-kids-billing-backend
```

Backend host binding:

```text
127.0.0.1:18081 -> container port 8080
```

The backend is also reachable from Caddy over the existing edge Docker network
`api-quiz-bank-edge`.

## Reverse Proxy

Existing edge proxy:

```text
Dockerized Caddy: infra_caddy_prod
Config: /opt/infra-caddy/Caddyfile
```

Added host routes:

```text
shortsblockerkids.de
www.shortsblockerkids.de
billing.shortsblockerkids.de
```

Validation and reload:

```bash
docker exec infra_caddy_prod caddy validate --config /tmp/Caddyfile.sbk-candidate --adapter caddyfile
docker exec infra_caddy_prod caddy reload --config /etc/caddy/Caddyfile
docker exec infra_caddy_prod caddy validate --config /etc/caddy/Caddyfile
```

TLS result:

- `www.shortsblockerkids.de`: certificate obtained.
- `billing.shortsblockerkids.de`: certificate obtained.
- `shortsblockerkids.de`: certificate blocked because DNS still points to
  `185.181.104.242`.

## Runtime Secrets

The server-only env file is stored at:

```text
/etc/shorts-blocker-kids/billing-backend.env
```

Permissions:

```text
600 root:root
```

The env file contains runtime database credentials and RTDN shared-secret
material. Do not commit or print it.

Google Play service-account JSON was not found on the server. Because production
backend config requires Google credentials and Pub/Sub RTDN settings, the
backend was deployed in a safe degraded runtime:

```text
SBK_ENV=development
SBK_REQUIRE_HTTPS=true
Postgres storage enabled
```

In this state health and entitlement status work, but purchase verification
fails closed and cannot activate entitlement without real Google Play
credentials.

Required before full production billing verification:

- Google Play service-account JSON on the server.
- `GOOGLE_APPLICATION_CREDENTIALS` pointing to that server-only file.
- `SBK_ENV=production`.
- `SBK_RTDN_PUBSUB_AUDIENCE=https://billing.shortsblockerkids.de/billing/play/rtdn`.
- `SBK_RTDN_PUBSUB_SERVICE_ACCOUNT_EMAIL` for authenticated Pub/Sub push.

## Smoke Results

Local backend health on target:

```bash
curl -fsS http://127.0.0.1:18081/health
```

Result:

```json
{"status":"ok"}
```

Negative billing smoke on target:

```bash
curl -X POST http://127.0.0.1:18081/billing/play/verify \
  -H "X-Forwarded-Proto: https" \
  -H "Content-Type: application/json" \
  --data '{"install_id":"smoke-install-20260531","package_name":"com.shortsblockerkids","product_id":"shorts_blocker_kids_monthly","purchase_token":"invalid-smoke-token-20260531","app_version":"0.1.0"}'
```

Result:

```text
HTTP 500, fail-closed because Google Play credentials are unavailable.
```

Entitlement after invalid purchase:

```bash
curl "http://127.0.0.1:18081/entitlement/status?install_id=smoke-install-20260531" \
  -H "X-Forwarded-Proto: https"
```

Result:

```json
{"state":"UNKNOWN","is_active":false,"active_until_millis":null,"checked_at_millis":null,"source":"google_play_backend"}
```

RTDN without auth:

```text
HTTP 400, invalid RTDN source.
```

HTTPS from the target server:

```text
https://www.shortsblockerkids.de             200
https://www.shortsblockerkids.de/privacy     200
https://www.shortsblockerkids.de/support     200
https://www.shortsblockerkids.de/terms       200
https://billing.shortsblockerkids.de/health  200 {"status":"ok"}
https://shortsblockerkids.de                 failed: apex DNS points to 185.181.104.242
```

Existing production checks after Caddy reload:

```text
https://deutschmit.de        200
https://www.deutschmit.de    301
https://deutchquizarena.de   301
https://api.valerchik.de     401 without API key, expected
```

Existing production containers remained running after reload.

## Backup And Restore

Backups taken before Caddy changes:

```text
/root/shorts-blocker-kids-backups/20260531T151405Z/Caddyfile
/root/shorts-blocker-kids-backups/20260531T151405Z/infra-caddy-docker-compose.yml
/root/shorts-blocker-kids-backups/20260531T151405Z/.env.caddy
```

Database backup command:

```bash
docker exec shorts-blocker-kids-billing-db \
  pg_dump -U shorts_blocker_kids shorts_blocker_kids \
  > /root/shorts-blocker-kids-backups/$(date -u +%Y%m%dT%H%M%SZ)-shorts_blocker_kids.sql
```

Restore database command:

```bash
docker exec -i shorts-blocker-kids-billing-db \
  psql -U shorts_blocker_kids shorts_blocker_kids \
  < /root/shorts-blocker-kids-backups/<backup>.sql
```

## Rollback

Rollback only Shorts Blocker Kids and Caddy route changes:

```bash
cp -a /root/shorts-blocker-kids-backups/20260531T151405Z/Caddyfile /opt/infra-caddy/Caddyfile
docker exec infra_caddy_prod caddy validate --config /etc/caddy/Caddyfile
docker exec infra_caddy_prod caddy reload --config /etc/caddy/Caddyfile
cd /opt/shorts-blocker-kids
docker compose --env-file /etc/shorts-blocker-kids/billing-backend.env stop
```

Do not stop other compose projects and do not prune Docker volumes or networks.

## Remaining Blockers

- Apex `shortsblockerkids.de` DNS must point to `46.225.181.45`.
- Local resolver used during deployment still resolved all three SBK domains to
  `185.181.104.242`; server resolver already resolves `www` and `billing` to
  `46.225.181.45`.
- Google Play service-account credentials are missing.
- Pub/Sub authenticated RTDN config is missing.
- Real Google Play purchase/license-tester flow was not verified.
- Real-device QA was not verified in this deployment.
