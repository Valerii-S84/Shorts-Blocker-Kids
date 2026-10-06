# Shorts Blocker Kids

Shorts Blocker Kids is a local-only Android app for blocking supported
short-video surfaces on a child's phone with a parent PIN and Android
Accessibility Service. Current platform support:

| Platform | Status |
|---|---|
| YouTube Shorts | supported |
| TikTok short-video feed `com.zhiliaoapp.musically` | supported by code; needs real-device QA |
| TikTok regional `com.ss.android.ugc.trill` | not supported |
| Instagram Reels | supported by code; needs real-device QA |
| Facebook Reels | supported by code; needs real-device QA |
| Facebook Lite `com.facebook.lite` | not supported |

The app has no account system and no analytics. Google Play Billing adds
`com.android.vending.BILLING`, `android.permission.INTERNET`, and
`android.permission.ACCESS_NETWORK_STATE` through the Billing SDK. A
repository-local `billing-backend` module provides the Play purchase
verification and RTDN baseline for production hardening.

Subscriptions are handled through Google Play Billing. There are no website,
Stripe, manual license key, or external payment flows in the Play-distributed
app.

The public product website source lives under `website/` and is intended for
`https://shortsblockerkids.de`, including the Play Console Privacy Policy URL
`https://shortsblockerkids.de/privacy` and support URL
`https://shortsblockerkids.de/support`. The website does not offer direct APK
download as the primary install action.

Release artifacts require `SBK_BILLING_BACKEND_BASE_URL` at build time. The
canonical production value is `https://billing.shortsblockerkids.de`. Debug
builds can still omit that value and use the client-only Google Play Billing
path for internal testing.

## Requirements

- JDK 17
- Android SDK with platform `android-36`
- Android build tools `36.0.0`
- Gradle wrapper from this repository

Local commands use:

```bash
export ANDROID_HOME=/path/to/Android/Sdk
```

`ANDROID_SDK_ROOT` can be used instead when `ANDROID_HOME` is not set.

## Build

```bash
./gradlew :app:assembleDebug
SBK_BILLING_BACKEND_BASE_URL=https://billing.shortsblockerkids.de ./gradlew :app:assembleRelease
SBK_BILLING_BACKEND_BASE_URL=https://billing.shortsblockerkids.de ./gradlew :app:bundleRelease
./gradlew :billing-backend:test
./gradlew :billing-backend:run
```

The debug APK is written under `app/build/outputs/apk/debug/`.
The release AAB is written under `app/build/outputs/bundle/release/`.

The release APK is unsigned unless release signing values are provided.

Release signing can be configured through environment variables, Gradle properties, or an untracked root `keystore.properties` file:

```properties
SBK_RELEASE_STORE_FILE=/absolute/path/to/release.jks
SBK_RELEASE_STORE_PASSWORD=...
SBK_RELEASE_KEY_ALIAS=...
SBK_RELEASE_KEY_PASSWORD=...
```

Do not commit keystores or `keystore.properties`.

Current release decisions:

- `versionName = 0.1.0` and `versionCode = 1` are intentionally kept for the first controlled release candidate line.
- `release` builds use R8 minification and resource shrinking.
- Google Play publishing remains out of scope for this repository state.
- Production billing backend credentials must be supplied through runtime
  environment variables and must not be committed.

## Test And Lint

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:jacocoDebugUnitTestReport :app:jacocoDebugUnitTestCoverageVerification
./gradlew :app:lintDebug
./gradlew :app:ktlintCheck
cd website && npm run check && npm run build && npm run smoke
```

Full local foundation check:

```bash
SBK_BILLING_BACKEND_BASE_URL=https://billing.shortsblockerkids.de \
./gradlew :app:ktlintCheck :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:jacocoDebugUnitTestCoverageVerification :app:lintDebug
```

Full local release readiness check:

```bash
SBK_BILLING_BACKEND_BASE_URL=https://billing.shortsblockerkids.de \
./gradlew :app:ktlintCheck :app:assembleDebug :app:assembleRelease :app:bundleRelease :app:testDebugUnitTest :app:jacocoDebugUnitTestCoverageVerification :app:lintDebug :app:lintRelease
```

## GitHub Readiness

The GitHub repository is intended to be public only after the local secret scan
and quality gate pass. Create or keep the repository without GitHub-generated
README, `.gitignore`, or license files so this repository remains the source of
truth.

Do not commit `.env`, `.env.*`, `keystore.properties`, keystores, Google service
account JSON, client secrets, API tokens, or production billing credentials.
Local release signing belongs in environment variables, Gradle properties, or an
untracked `keystore.properties` file.

The GitHub Actions workflow is `.github/workflows/android.yml`. It runs Website,
Android App, Billing Backend, Security Hygiene, and Git Hygiene jobs on pull
requests and pushes to `main`. CI uses the canonical production backend URL only
for release config validation; it does not require production secrets, sign
release artifacts, upload Play artifacts, or publish deploy artifacts.

Tracked website preview screenshots under `website/preview/` and the small
phone-install evidence bundle under `dist/` are intentional review artifacts.
Generated website output under `website/dist/` and Gradle/Android build outputs
remain untracked.

## Install On A Device

Connect a device or emulator visible in `adb devices`, then run:

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

After installing, enable the Accessibility Service manually in Android settings.

## Product Boundaries

- Billing backend only for Google Play subscription verification
- No account
- No cloud sync
- No analytics
- No watch history storage
- No screen recording
- No microphone, camera, location, contacts, or SMS permissions
- Optional Device Admin tamper protection is transparent, parent-facing, and
  used only to make uninstall harder while admin is active. It does not replace
  Accessibility protection, block Android Settings, or perform hidden system
  actions.

## Billing Backend

Development runtime configuration:

```bash
GOOGLE_APPLICATION_CREDENTIALS=/secure/path/service-account.json
SBK_BACKEND_PORT=8080
SBK_PACKAGE_NAME=com.shortsblockerkids
SBK_PLAY_SUBSCRIPTION_PRODUCT_ID=shorts_blocker_kids_monthly
SBK_BACKEND_STORE_FILE=/secure/path/billing-backend-data.json
SBK_RTDN_SHARED_SECRET=...
```

Production requires `SBK_ENV=production`, PostgreSQL (`SBK_DATABASE_URL`),
`SBK_PUBLIC_BASE_URL` with HTTPS, runtime Google credentials, and authenticated
Pub/Sub push (`SBK_RTDN_PUBSUB_AUDIENCE` and
`SBK_RTDN_PUBSUB_SERVICE_ACCOUNT_EMAIL`). The shared RTDN secret above is a
development option; production RTDN still requires Google OIDC authentication.

The supported production topology is one trusted TLS reverse proxy on the
Docker host, forwarding to the loopback origin port. Compose publishes the
origin as `127.0.0.1:${SBK_BACKEND_PORT:-8080}:8080`; expose the proxy's HTTPS
listener to clients, and keep the origin inaccessible to untrusted callers.
Configure the required `SBK_TRUSTED_PROXY_IPS` with the exact numeric socket
peer addresses observed by the backend for that proxy. Docker forwarding can
change that address; do not assume loopback or a bridge gateway is trusted.
Hostnames, CIDRs, address ranges, and implicit proxy trust are unsupported.
Each configured peer must belong exclusively to the trusted proxy or protected
local host. Shared NAT ingress reachable by untrusted callers is unsupported.

The proxy must overwrite `X-Forwarded-For` with its actual client socket IP
(exactly one IPv4 or IPv6 address) and overwrite `X-Forwarded-Proto` with the
actual ingress scheme. It must remove `Forwarded` and other `X-Forwarded-*`
headers. For example, the relevant Nginx location directives are:

```nginx
proxy_pass http://127.0.0.1:8080;
proxy_set_header X-Forwarded-For $remote_addr;
proxy_set_header X-Forwarded-Proto $scheme;
proxy_set_header Forwarded "";
```

Configure stripping of other forwarded headers as well. The backend rejects
unsupported, duplicate, malformed, or chained forwarded headers; it accepts
these headers only from explicitly trusted peers. Direct HTTP from an
untrusted peer cannot satisfy the production HTTPS gate by adding a header.
Client rate limits use the validated proxy client address before Google or
database work. Clients sharing a public IP still share that IP's quota.
The local `/health` probe is exempt from the forwarding/HTTPS gate.

Installation authorization begins with `POST /billing/installations` and an
empty JSON object (`{}`). The server creates a fresh installation ID and a
256-bit random bearer credential; clients persist both and send
`Authorization: Bearer <credential>` on verify and status requests for that
installation. A UUID or an APK-wide secret does not establish ownership.
Unauthenticated callers cannot enroll a chosen or pre-existing installation
ID. Legacy installations without a credential fail closed; restoring their
ownership requires a separate trusted recovery process, which is not
implemented here. Creating a fresh installation never grants access to a
legacy installation or its existing purchase-token binding.

Android release builds can opt into backend verification with:

```bash
SBK_BILLING_BACKEND_BASE_URL=https://billing.shortsblockerkids.de \
./gradlew :app:bundleRelease
```

`assembleRelease` and `bundleRelease` fail closed when the backend URL is blank,
non-HTTPS, invalid, or not `https://billing.shortsblockerkids.de`. This prevents
a Play-bound build from shipping with subscription purchase UI but no backend
entitlement verification. Placeholder domains are not production backend URLs
for this app.

## Known Limitation

Short-video blocking works when Protection is ON and Android Accessibility
Service is active. Optional Device Admin tamper protection can make uninstall
harder while active, but if Accessibility permission is turned off, blocking
stops.
