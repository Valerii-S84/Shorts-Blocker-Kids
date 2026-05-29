# Shorts Blocker Kids

Shorts Blocker Kids is a local-only Android app for blocking supported
short-video surfaces on a child's phone with a parent PIN and Android
Accessibility Service. Current platform support is intentionally narrow:

| Platform | Status |
|---|---|
| YouTube Shorts | supported |
| TikTok main `com.zhiliaoapp.musically` | supported by code; needs real-device QA |
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

Release artifacts require `SBK_BILLING_BACKEND_BASE_URL` at build time and the
value must be an HTTPS URL. Debug builds can still omit that value and use the
client-only Google Play Billing path for internal testing.

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
./gradlew :app:assembleRelease
./gradlew :app:bundleRelease
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
```

Full local foundation check:

```bash
./gradlew :app:ktlintCheck :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:jacocoDebugUnitTestCoverageVerification :app:lintDebug
```

Full local release readiness check:

```bash
./gradlew :app:ktlintCheck :app:assembleDebug :app:assembleRelease :app:bundleRelease :app:testDebugUnitTest :app:jacocoDebugUnitTestCoverageVerification :app:lintDebug :app:lintRelease
```

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
- No aggressive anti-uninstall behavior

## Billing Backend

Runtime configuration:

```bash
GOOGLE_APPLICATION_CREDENTIALS=/secure/path/service-account.json
SBK_BACKEND_PORT=8080
SBK_PACKAGE_NAME=com.shortsblockerkids
SBK_PLAY_SUBSCRIPTION_PRODUCT_ID=shorts_blocker_kids_monthly
SBK_BACKEND_STORE_FILE=/secure/path/billing-backend-data.json
SBK_RTDN_SHARED_SECRET=...
```

Android release builds can opt into backend verification with:

```bash
SBK_BILLING_BACKEND_BASE_URL=https://billing.example.com \
./gradlew :app:bundleRelease
```

`assembleRelease` and `bundleRelease` fail closed when the backend URL is blank
or not HTTPS. This prevents a Play-bound build from shipping with subscription
purchase UI but no backend entitlement verification.

## Known Limitation

Short-video blocking works when Protection is ON and Android Accessibility
Service is active. If the system permission is turned off or the app is removed,
blocking stops.
