# Shorts Blocker Kids AAB Release Readiness

Status: PARTIAL for release freeze 1.0.0. Local release gates and targeted instrumented regressions passed; the applicable external Play gates still need confirmation. The signed AAB is prepared for Play review, not certified as accepted by Play.

## Release Freeze 1.0.0 — 16 October 2026

This section owns the current freeze result. Earlier candidate versions, hashes and dated test results below remain historical evidence.

- Candidate: `com.shortsblockerkids`, versionName `1.0.0`, versionCode `1`, minSdk `26`, targetSdk `36`; release is non-debuggable.
- Fresh AAB: `app/build/outputs/bundle/release/app-release.aab`.
- AAB SHA-256: `9D330C386C687DE9D17F42E1A26938C815216948572737266DC17F4D3363CD88`.
- Artifact gate passed: Bundletool validation and manifest/config dump, JAR signature, matching AAB/APK signer, 64-bit ELF PT_LOAD alignment, APK 16 KB ZIP alignment and BundleConfig `PAGE_ALIGNMENT_16K`.
- Public signer SHA-256: `6F2FF6FFAF3D67F5A0AB0BF71D10A2035BA0EECE5767A5A548916910DA8205C4`. Local validation does not establish Play upload-key acceptance.
- Unit result: **596 tests, 0 failures, 0 errors, 0 skipped**. Coverage: instructions **97.2614%**, lines **99.0642%**, branches **944/993 = 95.0655%**; thresholds/exclusions retained.
- Final static/build checks passed. Android lint: debug **22 warnings**, release **55 warnings**, **0 errors**.
- Instrumented history: full run **25/32 passed**; authorized seven-method follow-up **5/7 passed**. Final two-method follow-up: **2/2 passed**, failures=0, skipped=0. Together these cover all 32 distinct methods; the last source change was verified by its two targeted regressions and all JVM tests. A repeated full-suite run on the final APK is not claimed.
- Current evidence: `app/build/release-freeze-20261016/final-freeze-summary.json`, `final-two-summary.json`, `local-summary.json`, `aab-validation.json`; prior full/seven-method results remain archived separately.
- External Play gates remain pending: Accessibility declaration/demo video URL, Console forms/listing/reviewer access, configured subscription and Play-installed Billing tests, and fresh AAB upload-key/Play App Signing acceptance. Account-specific testing/production-access conditions remain unverified.

Post-release backlog: assess the remaining non-error Android lint warnings, including `AppBundleLocaleChanges` and language-split delivery behavior. No installed-Play locale defect is established; these warnings do not independently establish a publication blocker.

The release remains **Partial** until all remaining acceptance items and the applicable external Play gates are closed. No new general audit or optional refactoring is required by this freeze record.

## Current Release Contract

Package `com.shortsblockerkids`; current source versionName `1.0.0`, versionCode `1`, minSdk 26 and target/compileSdk 36. Recheck these against the exact proposed release. Release uses R8 minification and resource shrinking. Signing uses existing SBK_RELEASE_STORE_FILE/STORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD values outside Git; never publish signing material.

The own billing backend is removed. Release does not require a backend URL, server tests, credentials, PostgreSQL, RTDN, backup/restore or a server grace policy. Google Play Billing and its local-access contract are in [Billing Integration](SHORTS_BLOCKER_PLAY_BILLING_BACKEND_RUNBOOK.md).

## Historical Artifact Warning

The July 17, 2026 signed AAB and older APK/AAB-derived emulator evidence predate reviewed source changes. They are retained history and MUST NOT be uploaded as the current release. Old backend BuildConfig values and historical pass counts are not current release requirements or current verification. Rebuild from reviewed source and bind all evidence to that exact artifact.

## Historical 0.1.0 Provenance — Superseded by Release Freeze 1.0.0

| Field | Current release evidence |
|---|---|
| Source commit and approved working-tree scope | 9b4f486e5994b8062513de2bef4d3d95c6dfbbd7 on main; 31 approved task files uncommitted; two external website changes excluded. |
| Version name/code and package | `com.shortsblockerkids`, `0.1.0` / `1`; verified from current release APK manifest. |
| Signed AAB path/size/SHA-256 | Fresh built, not accepted: 3,749,699 bytes; 9B029B6ECAC34F233B49C68F4C8158E75623BDAC71F879CCE98382CDEBB09C8F. |
| Signing certificate/upload key and Play App Signing acceptance | Local JAR signature verified; upload-key identity and Play acceptance pending. Secrets not inspected. |
| Build/check commands and dated results | Final phase: unit 594/1/0/0; ktlint fails; lint passes; fresh coverage blocked; instrumentation only 13 completed/0/0/0 of 32, incomplete. |
| Merged manifest/SDK/dependency versions | Current release APK manifest decoded below; minSdk 26 / targetSdk 36; release non-debuggable. |
| Play track/release/install source | Pending upload and install |

Do not reuse an old hash, substitute a debug APK or claim a local signed file is Play-accepted. Both Billing Config and tester runbook refer here for artifact provenance.

## Technical Quality Gate For A Separately Authorized Release

~~~powershell
.\gradlew.bat :app:ktlintCheck :app:assembleDebug :app:assembleRelease :app:bundleRelease :app:testDebugUnitTest :app:jacocoDebugUnitTestCoverageVerification :app:lintDebug :app:lintRelease
~~~

Run relevant checks once after final changes. The current `app/build.gradle.kts` thresholds are instructions 94.05%, lines 95.39% and branches 95%; do not lower thresholds or change exclusions to declare success. The 2026-09-14 rescan report records a historical coverage failure, not the current candidate result. Latest results distinguish completed, failed and blocked lanes; full acceptance did not pass.

Run the complete instrumentation lane separately:

~~~powershell
.\gradlew.bat :app:connectedDebugAndroidTest
~~~

This task depends on `installFakeSocialApps`, which builds and installs all five fixture flavors. Record the selected emulator/device, full XML tests/failures/errors/skipped counts and fixture installation outcome. A selected test or old XML does not establish a full-suite pass. Fake applications exercise local Accessibility integration; real supported-app UI and Play purchases require separate evidence.

## Current Local Remediation Evidence

**Local preparation: FAIL. Overall publication readiness: FAIL.** The reserved final frozen-source phase started 2026-10-15T22:53:35Z (16 October in Europe/Berlin). No automatic retry or source change followed. Ktlint failed; fresh unit XML is **594 / 1 / 0 / 0** (tests/failures/errors/skipped). Debug/release builds and both lint variants completed. Coverage verification/report did not execute after the failing unit prerequisite; no new achieved ratios are claimed. The entire 32-test instrumentation task started once without a filter, but terminated with only **13 completed XML tests / 0 failures / 0 errors / 0 skipped**, leaving **19 without results**. This is incomplete, never a whole-suite pass. Both Pevisor reviews remain unavailable and unfulfilled.

Frozen source: main, 9b4f486e5994b8062513de2bef4d3d95c6dfbbd7; **31 approved task files uncommitted**, including new MonotonicTemporaryAllowStorageTest.kt. Existing website/README.md modification and untracked website/Dockerfile remain outside this work. No commit, push, PR, upload or production change occurred.

The exact eight-task command above ran once. It failed :app:ktlintAndroidTestSourceSetCheck in 41 seconds: RuntimeProtectionLifecycleInstrumentedTest.kt lines **89, 90, 114, 115**, 27 findings (18 argument-list-wrapping, five chain-method-continuation, four max-line-length). The six not-yet-reached tasks ran with --continue; after 3 minutes 54 seconds :app:testDebugUnitTest failed. Failure: DashboardUiStateAssemblerTest.injectedElapsedClockControlsDashboardAllowanceDespitePastWallDeadline, line 35: expected canProtect=false, observed true while injected elapsed=1999 before deadline=2000 despite a past wall deadline. Independent source diagnosis: DashboardStateFactory's canProtect call omits the injected elapsed argument and therefore uses null. That production file has not been changed; personal owner approval is required. Unit XML: **594/1/0/0**, 77 files. Billing suites are 4/0/0/0 and 11/0/0/0; AutomaticProtectionWiring 20/0/0/0; BlockingProtectionExecutor 21/0/0/0; PhoneHomeExitController 13/0/0/0.

assembleDebug, assembleRelease, bundleRelease, lintDebug and lintRelease completed. Lint XML: debug zero errors / 25 warnings; release zero errors / 55 warnings. JaCoCo verification and report were blocked by the failing unit prerequisite. Thresholds remain instructions **94.05%**, lines **95.39%**, branches **95%**, with exclusions unchanged. Old XML dated 15 October 14:41Z is historical: instructions 12009 covered / 565 missed = 95.50660%; lines 2325/28 = 98.81003%; branches 896/101 = 89.86961%. These are **not** final-source achieved ratios.

Whole connectedDebugAndroidTest started 22:58:15Z, without a class filter, with all five fixture installation dependencies. Environment setup force-stopped fixture YouTube and the own debug app, then opened Home; no data/account change. Last progress: 12/32 at 23:01:41Z; XML at 23:01:55Z contains LocaleMatrix 3/0/0/0, ParentalSettings 9/0/0/0 and only Runtime's past-wall method 1/0/0/0. Monitor session 40799 disappeared. At 23:03:12Z no Gradle wrapper, active instrumentation or blocker test process remained. Event 23:02:39Z records debug-app DeadObjectException disconnecting UiAutomation and process death. Its relation to the user's interruption is not established. **19 results are absent**: other two Runtime tests and all 17 E2E tests. No restart or retry.

Historical baseline: one exitBackAndHomeInteractionsRecoverWithoutDuplicateOverlay test failed 'Blocking overlay was still visible'; the 15 October daytime full suite later passed that method while six others failed. These dated results are not latest-source passes.

| Previous failing scenario | Evidence, correction and remaining uncertainty |
|---|---|
| Interrupt/automatic exit | The old assertion required automatic exit while the overlay was visible. Under the expressly approved stable-overlay contract, successful blocking waits for user action; only failed overlay display uses bounded fallback. The revised interrupt test checks the stable barrier and same fixture. Its final-run result is missing. |
| Ukrainian PIN | Successful overlay display immediately initiated HOME, racing a PIN tap; the old helper retained nodes and eagerly scanned diagnostics. Stable overlay and bounded node reacquisition address this common static race. Exclusive timing of this old failure was not proven; its final-run result is missing. |
| German PIN | Same established common race and causal correction as Ukrainian PIN. The localized assertion remains. Exclusive per-method timing is unproven; final result missing. |
| Cancel PIN / repeat block | Same common automatic-navigation/node race. PIN cancellation resets the blocking episode; assertions retain actual PIN navigation and same-fixture reentry. Final result missing. |
| Past-wall deadline | Corrected original-APK diagnostic failed 151 ms before the elapsed deadline with matching stored elapsed/boot metadata, but SettingsLoadingProtectionConservative then SettingsUnavailableProtectionDegraded and launcher active. This proves a conservative loading/unavailable barrier, not a clock mismatch. Underlying readiness failure remains unresolved. This method passed in the final phase; natural allowance completion is still unverified. |
| Future-wall deadline | Original expiry-recheck failure remains without an exclusive proven cause. First diagnostic failed earlier in quiet-window setup with a fixture ANR in the same run. Corrected original-APK diagnostic passed. Clock predicate, deadline and assertions were not weakened. Final-run result missing. |

Fresh built AAB: C:\Users\User\Desktop\Shorts Blocker Kids\app\build\outputs\bundle\release\app-release.aab; **3,749,699 bytes**, modified 2026-10-15T22:57:34.9525658Z; SHA-256 **9B029B6ECAC34F233B49C68F4C8158E75623BDAC71F879CCE98382CDEBB09C8F**. Jarsigner -verify returned 0 and 'jar verified'. Existing signing mechanism was used without inspecting secrets. This fresh artifact **fails local acceptance**; upload-key identity and Play App Signing acceptance remain external.

Exact public jarsigner warning summaries:

- This jar contains entries whose certificate chain is invalid. Reason: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
- This jar contains entries whose signer certificate is self-signed.
- This jar contains signatures that do not include a timestamp. Without a timestamp, users may not be able to validate this jar after any of the signer certificates expire (as early as 2053-10-06).
- POSIX file permission and/or symlink attributes detected. These attributes are ignored when signing and are not protected by the signature.
- This JAR file contains internal inconsistencies that may result in different contents when reading via JarFile and JarInputStream:
- - Manifest is missing when reading via JarInputStream
- Re-run with the -verbose and -certs options for more details.

There are 128 additional per-entry JarFile/JarInputStream warnings in the generated signature log. Local verification does not establish Play acceptance.

Fresh release APK: C:\Users\User\Desktop\Shorts Blocker Kids\app\build\outputs\apk\release\app-release.apk; **1,860,943 bytes**, modified 2026-10-15T22:57:31.2657566Z; SHA-256 **BD565A220FB79413CCC233B7F581FBA054ACDA34CEDD8C5779144180C07707B9**. Final merged APK manifest: app/build/rc-verification-20261016/final-control-release-manifest.xml, batch echo stripped. Package com.shortsblockerkids; versionName 0.1.0, versionCode 1; minSdk 26, targetSdk 36; debuggable absent (Android default false). Version unchanged. Fresh debug APK SHA-256 8C200AA88916F88D7A9A0B79DE4645D3922852B5B98F5EC161D87F3E763E2D1D, modified 2026-10-15T22:53:55.9948948Z.

Manifest permissions: com.android.vending.BILLING, com.shortsblockerkids.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION, android.permission.ACCESS_NETWORK_STATE, android.permission.INTERNET.

Release components:

- com.shortsblockerkids.MainActivity (activity, exported=true).
- com.shortsblockerkids.accessibility.ShortsBlockerAccessibilityService (service, exported=true; permission android.permission.BIND_ACCESSIBILITY_SERVICE).
- com.shortsblockerkids.core.tamper.TamperProtectionReceiver (receiver, exported=true; permission android.permission.BIND_DEVICE_ADMIN).
- com.android.billingclient.api.ProxyBillingActivity (activity, exported=false).
- com.android.billingclient.api.ProxyBillingActivityV2 (activity, exported=false).
- com.google.android.gms.common.api.GoogleApiActivity (activity, exported=false).
- androidx.startup.InitializationProvider (provider, exported=false).
- androidx.profileinstaller.ProfileInstallReceiver (receiver, exported=true; permission android.permission.DUMP).
- com.google.android.datatransport.runtime.backends.TransportBackendDiscovery (service, exported=false).
- com.google.android.datatransport.runtime.scheduling.jobscheduling.JobInfoSchedulerService (service, exported=false; permission android.permission.BIND_JOB_SERVICE).
- com.google.android.datatransport.runtime.scheduling.jobscheduling.AlarmManagerSchedulerBroadcastReceiver (receiver, exported=false).

Release BuildConfig has DEBUG=false and ACCESSIBILITY_DEBUG_TOOLS_ENABLED=false; no app debug component is exported by the manifest. Current minifyReleaseWithR8 completed and mapping.txt exists; usage.txt is absent, so earlier specific removed-class evidence was not regenerated. Source guards disable debug diagnostics. A current release runtime privacy trace was not executed; do not claim empirical private-content logging acceptance.

Generated evidence: app/build/rc-verification-20261016/final-control-local.log, final-control-remaining.log, final-control-instrumented.log, final-control-summary.json, final-control-release-metadata.json, final-control-jarsigner.log and decoded manifest. Ordinary independent source reviews completed; neither required Pevisor review ran. Current real-app behavior, complete instrumentation, fresh coverage and both review gates remain unfulfilled.

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

1. Local coverage/instrumentation acceptance, both Pevisor reviews and Play App Signing/upload acceptance of the fresh signed candidate.
2. Public Privacy/Support/root routes and current policy text rechecked; old deployment observations are dated evidence only.
3. Store listing, screenshots/feature graphic, Data Safety, Accessibility declaration/demo, target audience/content rating and reviewer access submitted/accepted.
4. Every supported real app/device/OEM scope verified against the final release; no production claim based solely on fixtures.
5. Play-installed license-tester BIL-01…BIL-12 evidence and non-license sanity recorded.
6. Required internal/closed testing and production-access conditions checked for the actual developer account and current Console, rather than hardcoding an old duration.
7. Staged rollout plan: observe crashes/detection/Billing/policy issues, pause on a material failure and use a separately scoped hotfix/release with new provenance.

[Console Package](SHORTS_BLOCKER_PLAY_CONSOLE_PREPARATION_PACKAGE.md) owns form/listing/access details. [Deployment Evidence](SHORTS_BLOCKER_PRODUCTION_DEPLOYMENT_2026-05-31.md) retains public-safe history. No live checks, full quality gate, device tests, signing or publication were performed by this preparation.

Consolidated from Roadmap, Play Store Readiness, Test Plan, Final RC Freeze and privacy/permissions checklists; originals remain unchanged pending deletion approval.
