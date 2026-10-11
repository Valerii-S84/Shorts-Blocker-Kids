# Shorts Blocker Kids AAB Release Readiness

Status: Partial. A fresh signed AAB from reviewed source, artifact provenance and Play acceptance are pending. No artifact was rebuilt or uploaded by this documentation preparation.

## Current Release Contract

Package `com.shortsblockerkids`; current source versionName `0.1.0`, versionCode `1`, minSdk 26 and target/compileSdk 36. Recheck these against the exact proposed release. Release uses R8 minification and resource shrinking. Signing uses existing SBK_RELEASE_STORE_FILE/STORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD values outside Git; never publish signing material.

The own billing backend is removed. Release does not require a backend URL, server tests, credentials, PostgreSQL, RTDN, backup/restore or a server grace policy. Google Play Billing and its local-access contract are in [Billing Integration](SHORTS_BLOCKER_PLAY_BILLING_BACKEND_RUNBOOK.md).

## Historical Artifact Warning

The July 17, 2026 signed AAB and older APK/AAB-derived emulator evidence predate reviewed source changes. They are retained history and MUST NOT be uploaded as the current release. Old backend BuildConfig values and historical pass counts are not current release requirements or current verification. Rebuild from reviewed source and bind all evidence to that exact artifact.

## Provenance To Record For A Fresh Artifact

| Field | Current release evidence |
|---|---|
| Source commit and approved working-tree scope | Pending |
| Version name/code and package | Pending artifact verification |
| Signed AAB path/size/SHA-256 | Pending rebuild |
| Signing certificate/upload key and Play App Signing acceptance | Pending, keep secret material private |
| Build/check commands and dated results | Pending relevant release run |
| Merged manifest/SDK/dependency versions | Pending fresh artifact inspection |
| Play track/release/install source | Pending upload and install |

Do not reuse an old hash, substitute a debug APK or claim a local signed file is Play-accepted. Both Billing Config and tester runbook refer here for artifact provenance.

## Technical Quality Gate For A Separately Authorized Release

~~~powershell
.\gradlew.bat :app:ktlintCheck :app:assembleDebug :app:assembleRelease :app:bundleRelease :app:testDebugUnitTest :app:jacocoDebugUnitTestCoverageVerification :app:lintDebug :app:lintRelease
~~~

Run relevant checks once after final changes. Preserve the configured JaCoCo branch threshold of at least 90% and instruction/line thresholds; do not lower coverage/exclusions to declare success. The 2026-09-14 rescan report records that project-wide coverage had previously failed and was not rerun by that fix. Its current result is NOT established here; this remains an evidence item for the release gate, not a newly reproduced code defect.

## Manifest, Diagnostics And Policy Checks

- Inspect the freshly merged release manifest and SDK permissions, not an old generated output. BILLING/INTERNET/ACCESS_NETWORK_STATE support the existing Play SDK.
- Verify no camera/microphone/location/contacts/SMS, SYSTEM_ALERT_WINDOW or POST_NOTIFICATIONS expansion is introduced.
- Verify Accessibility package/event filtering, service metadata and explicit disclosure/consent match the four protected platforms.
- Verify raw Accessibility trees/private UI text and debug playground/overlay tools are disabled in release; diagnostics remain sanitized.
- Verify no account, ads, analytics, child history, cloud sync, alternate payments or manual license activation.
- Device Admin is explicit/optional, has no destructive policies and never replaces Accessibility or blocks Settings.
- Verify normal screens remain unblocked, overlay dismissal/rotation/small-screen behavior and recovery after permission loss.

Use [Device QA](SHORTS_BLOCKER_REAL_DEVICE_QA_CHECKLIST.md), [Play Policy](SHORTS_BLOCKER_PLAY_POLICY_PACKAGE.md) and [Privacy](SHORTS_BLOCKER_PRIVACY_POLICY_DRAFT.md).

## External Release Gates Still To Complete

1. Fresh signed AAB provenance and Play App Signing/upload acceptance.
2. Public Privacy/Support/root routes and current policy text rechecked; old deployment observations are dated evidence only.
3. Store listing, screenshots/feature graphic, Data Safety, Accessibility declaration/demo, target audience/content rating and reviewer access submitted/accepted.
4. Every supported real app/device/OEM scope verified against the final release; no production claim based solely on fixtures.
5. Play-installed license-tester BIL-01…BIL-12 evidence and non-license sanity recorded.
6. Required internal/closed testing and production-access conditions checked for the actual developer account and current Console, rather than hardcoding an old duration.
7. Staged rollout plan: observe crashes/detection/Billing/policy issues, pause on a material failure and use a separately scoped hotfix/release with new provenance.

[Console Package](SHORTS_BLOCKER_PLAY_CONSOLE_PREPARATION_PACKAGE.md) owns form/listing/access details. [Deployment Evidence](SHORTS_BLOCKER_PRODUCTION_DEPLOYMENT_2026-05-31.md) retains public-safe history. No live checks, full quality gate, device tests, signing or publication were performed by this preparation.

Consolidated from Roadmap, Play Store Readiness, Test Plan, Final RC Freeze and privacy/permissions checklists; originals remain unchanged pending deletion approval.
