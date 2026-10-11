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
`android.permission.ACCESS_NETWORK_STATE` through the Billing SDK. Google Play
Billing provides subscription purchase state.

Subscriptions are handled through Google Play Billing. There are no website,
Stripe, manual license key, or external payment flows in the Play-distributed
app.

The public product website source lives under `website/` and is intended for
`https://shortsblockerkids.de`, including the Play Console Privacy Policy URL
`https://shortsblockerkids.de/privacy` and support URL
`https://shortsblockerkids.de/support`. The website does not offer direct APK
download as the primary install action.

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
./gradlew :app:ktlintCheck :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:jacocoDebugUnitTestCoverageVerification :app:lintDebug
```

Full local release readiness check:

```bash
./gradlew :app:ktlintCheck :app:assembleDebug :app:assembleRelease :app:bundleRelease :app:testDebugUnitTest :app:jacocoDebugUnitTestCoverageVerification :app:lintDebug :app:lintRelease
```

## GitHub Readiness

The GitHub repository is intended to be public only after the local secret scan
and quality gate pass. Create or keep the repository without GitHub-generated
README, `.gitignore`, or license files so this repository remains the source of
truth.

Do not commit `.env`, `.env.*`, `keystore.properties`, keystores, Google service
account JSON, client secrets, or API tokens.
Local release signing belongs in environment variables, Gradle properties, or an
untracked `keystore.properties` file.

The GitHub Actions workflow is `.github/workflows/android.yml`. It runs Website,
Android App, Security Hygiene, and Git Hygiene jobs on pull
requests and pushes to `main`. CI does not require production secrets, sign
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

- Google Play Billing for subscription purchases and access state
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

## Known Limitation

Short-video blocking works when Protection is ON and Android Accessibility
Service is active. Optional Device Admin tamper protection can make uninstall
harder while active, but if Accessibility permission is turned off, blocking
stops.

## Current Product And Documentation

One parent PIN controls selected platforms. The local free test defaults to 20 days and starts once after successful protection activation: Accessibility enabled, protection enabled, disclosure accepted and PIN configured. App launch/PIN setup alone do not start it. After expiry, Google Play paid access is required. Temporary allow supports 5, 10 and 15 minutes. This local free test is separate from Play offers.

Current storage is Preferences DataStore; pure policies/use cases are separated from infrastructure, platform and presentation, with composition wiring adapters. The own backend is removed. Play purchases determine local access; server verification, Stripe and a server offline-grace timer are not current requirements.

| Current document | Purpose |
|---|---|
| [SHORT_VIDEO_BLOCKER_ARCHITECTURE.md](docs/SHORT_VIDEO_BLOCKER_ARCHITECTURE.md) | Architecture and behavioral contracts |
| [SHORT_VIDEO_BLOCKER_DETECTION_STRATEGY.md](docs/SHORT_VIDEO_BLOCKER_DETECTION_STRATEGY.md) | Signals, package scope and rescan |
| [SHORTS_BLOCKER_CLEAN_ARCHITECTURE_MIGRATION_PLAN.md](docs/SHORTS_BLOCKER_CLEAN_ARCHITECTURE_MIGRATION_PLAN.md) | Approved migration objectives, not full completion certification |
| [SHORTS_BLOCKER_AAB_RELEASE_READINESS.md](docs/SHORTS_BLOCKER_AAB_RELEASE_READINESS.md) | Technical release gate and provenance |
| [SHORTS_BLOCKER_REAL_DEVICE_QA_CHECKLIST.md](docs/SHORTS_BLOCKER_REAL_DEVICE_QA_CHECKLIST.md) | Device/emulator/evidence matrix |
| [SHORTS_BLOCKER_PLAY_BILLING_BACKEND_RUNBOOK.md](docs/SHORTS_BLOCKER_PLAY_BILLING_BACKEND_RUNBOOK.md) | Current Billing integration; legacy filename retained for tests |
| [SHORTS_BLOCKER_PLAY_BILLING_INTERNAL_TEST_RUNBOOK.md](docs/SHORTS_BLOCKER_PLAY_BILLING_INTERNAL_TEST_RUNBOOK.md) | Real Play tester scenarios |
| [SHORTS_BLOCKER_PLAY_CONSOLE_BILLING_CONFIG.md](docs/SHORTS_BLOCKER_PLAY_CONSOLE_BILLING_CONFIG.md) | Product/base-plan configuration |
| [SHORTS_BLOCKER_PLAY_CONSOLE_PREPARATION_PACKAGE.md](docs/SHORTS_BLOCKER_PLAY_CONSOLE_PREPARATION_PACKAGE.md) | Listing/forms/reviewer access |
| [SHORTS_BLOCKER_PLAY_POLICY_PACKAGE.md](docs/SHORTS_BLOCKER_PLAY_POLICY_PACKAGE.md) | Accessibility/Data Safety/demo |
| [SHORTS_BLOCKER_PRIVACY_POLICY_DRAFT.md](docs/SHORTS_BLOCKER_PRIVACY_POLICY_DRAFT.md) | Privacy text aligned with website |
| [SHORTS_BLOCKER_PRODUCTION_DEPLOYMENT_2026-05-31.md](docs/SHORTS_BLOCKER_PRODUCTION_DEPLOYMENT_2026-05-31.md) | Dated public URL observations, not a current live check |
| [Website workflow](website/README.md) | Static build, deployment checks and CTA. |

### Documentation Preparation Boundary

All 28 deletion candidates remain unchanged pending a separate user decision. Older Vision/Roadmap, phase/backend/Stripe plans and dated audit/checklist sources are retained history, not current execution authority. Their apparent active statuses, server requirements, old YouTube restrictions and verification results are superseded by the documents above. Do not execute them as new tasks or infer current readiness from them.

Information from these 20 sources is consolidated into existing destinations; originals remain for independent comparison:

| Retained source | Destination / preserved information |
|---|---|
| `Shorts_Blocker_Kids_Clean_Product_Vision.md` | README product boundaries and local parent control |
| `Shorts_Blocker_Kids_Implementation_Roadmap.md` | README navigation, AAB release gates and migration objectives |
| `docs/SHORT_VIDEO_BLOCKER_BILLING_BACKEND_PLAN.md` | Billing integration and internal testing: app-side flows |
| `docs/SHORTS_BLOCKER_BILLING_BACKEND_DECISION.md` | Billing integration: current no-own-backend decision |
| `docs/SHORT_VIDEO_BLOCKER_PLAY_STORE_READINESS_PLAN.md` | AAB/Play/Console packages: external release gates |
| `docs/SHORT_VIDEO_BLOCKER_PRODUCT_PLAN.md` | README boundaries, architecture and QA acceptance |
| `docs/SHORT_VIDEO_BLOCKER_TEST_PLAN.md` | Device QA scenarios and AAB quality gates |
| `docs/SHORTS_BLOCKER_ACCESSIBILITY_POLICY_READINESS.md` | Play Policy disclosure/declaration/demo and Device QA evidence |
| `docs/SHORTS_BLOCKER_EMULATOR_E2E_QA.md` | Device QA fixture packages, entrypoints, scenarios and limits |
| `docs/SHORTS_BLOCKER_EVENT_RESCAN_FIX_2026-09-14.md` | Detection rescan contract, QA limits and dated AAB coverage warning |
| `docs/SHORTS_BLOCKER_FINAL_RC_FREEZE_EVIDENCE.md` | AAB stale-artifact/release safeguards and deployment history |
| `docs/SHORTS_BLOCKER_INTERNAL_TEST_MATRIX.md` | Device QA release/device/app/scenario/evidence matrices |
| `docs/SHORTS_BLOCKER_PHASE_10_BILLING_CHECKLIST.md` | Billing runbooks/config: current app-side checks |
| `docs/SHORTS_BLOCKER_PHASE_5_SERVICE_MANUAL_CHECKLIST.md` | Device QA service/consent/logging checks |
| `docs/SHORTS_BLOCKER_PHASE_7_BLOCKING_LAYER_CHECKLIST.md` | Device QA overlay, rotation, small-screen and dismissal checks |
| `docs/SHORTS_BLOCKER_PHASE_9_GUARDRAILS_CHECKLIST.md` | Device QA honest status, Settings and tamper guardrails |
| `docs/SHORTS_BLOCKER_PRIVACY_PERMISSIONS_AUDIT.md` | AAB permissions/release checks and architecture privacy contract |
| `docs/SHORTS_BLOCKER_REAL_DEVICE_SMOKE_REPORT.md` | Device QA dated owner report and missing device/version evidence |
| `docs/SHORTS_BLOCKER_WEBSITE_DEPLOYMENT.md` | website/README static deployment, live checks, CTA and metrics |
| `docs/SHORTS_BLOCKER_WINDOWS_EMULATOR_FALLBACK.md` | Device QA portable Windows/WSL emulator and ADB workflow |

Preparation does not rebuild/approve an AAB, certify all migration slices, complete device/Billing QA, publish or authorize deletion. The historical dist bundle retains original metadata; current release QA uses the linked checklist.
