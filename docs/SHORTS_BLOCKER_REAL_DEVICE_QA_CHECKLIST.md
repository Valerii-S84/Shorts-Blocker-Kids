# Shorts Blocker Kids Real-Device QA Checklist

Status: FAIL / incomplete current-candidate verification. This checklist defines acceptance; scenario descriptions and historical reports are not current pass results.

## Scope

This checklist verifies the production-hardening state on real Android phones
with the actual social apps. It does not replace the automated unit and emulator
E2E gates. It does not claim support for packages listed as unsupported.

## Evidence To Capture

Capture evidence for each device and each app run:

- Video recording of the full scenario from opening the target app to the
  expected block or non-block result.
- Screenshot of the blocking overlay when a screen is expected to block.
- Screenshot of the target screen when it is expected not to block.
- Shorts Blocker Kids build variant, version name, and version code.
- Phone manufacturer and model.
- Android version.
- Target app package name and app version.
- Protection state before the run: Protection ON, AccessibilityService enabled,
  temporary allow inactive.
- Result: pass, fail, or blocked by environment.

## Supported Packages To Test

| Platform | Package | Expected supported surface |
|---|---|---|
| YouTube | `com.google.android.youtube` | Shorts |
| TikTok short-video feed | `com.zhiliaoapp.musically` | For You |
| Instagram | `com.instagram.android` | Reels |
| Facebook | `com.facebook.katana` | Reels |

## Known Unsupported Packages

Do not claim these as supported unless a future code change and QA pass
explicitly enable them:

- TikTok regional package: `com.ss.android.ugc.trill`
- Facebook Lite: `com.facebook.lite`
- Instagram Lite: `com.instagram.lite`
- YouTube TV: `com.google.android.youtube.tv`
- YouTube Music: `com.google.android.apps.youtube.music`

## Device Matrix

Record every internal testing device before scenario execution.

| Device ID | OEM / model | Android version | Install source | App version | Tester account | Result |
|---|---|---|---|---|---|---|
| DEV-01 | TBD | TBD | Play internal testing | 0.1.0 (1) | TBD | Pending |
| DEV-02 | TBD | TBD | Play internal testing | 0.1.0 (1) | TBD | Pending |
| DEV-03 | TBD | TBD | Play internal testing | 0.1.0 (1) | TBD | Pending |
| DEV-04 | TBD | TBD | Play internal testing | 0.1.0 (1) | TBD | Pending |

Required coverage:

- Pixel or close-to-AOSP device.
- Samsung device.
- Xiaomi, Redmi, or another OEM with aggressive background restrictions.
- Low-end Android device if available.
- Android 12, 13, 14, 15, and current available Android version where available.

- Pending: Include available Android 10 and Android 11 devices alongside the
  existing Android 12–15/current-version matrix. Record exact device, OS, app and
  build metadata; unavailable coverage remains explicitly untested.

## Common Setup

1. Fresh install or clear app data.
2. Open Shorts Blocker Kids.
3. Create parent PIN.
4. Select protected apps: YouTube, TikTok, Instagram, and Facebook.
5. Review the Accessibility disclosure.
6. Decline once and verify Android Accessibility settings do not open.
7. Reopen setup, accept disclosure, and enable the AccessibilityService.
8. Confirm dashboard shows a green active checklist for parent PIN, protected
   apps, disclosure, and Accessibility Service.
9. Optionally open Tamper Protection, read the separate disclosure, and enable
   Device Admin from the Android confirmation screen.
10. Confirm temporary allow is inactive before each app scenario.

## YouTube

Expected to block:

- Open Shorts from bottom tab.
- Open Shorts from home recommendation.
- Open Shorts from search result if available.
- Pending: Open Shorts from a channel page and record the detection result.
- Pending: Open a direct Shorts link and record the supported-screen transition
  and blocking result; this is a QA requirement, not verified support evidence.
- Swipe between Shorts; overlay must not duplicate or flicker.

Expected not to block:

- Open a normal YouTube video.
- Pending: Open and navigate a normal-video playlist; it must remain unblocked.
- Open Home.
- Open Search.
- Open comments on a normal video.
- Open Profile or account page.

Evidence:

- Video showing Shorts blocked.
- Video or screenshots showing normal video, Home, Search, and Profile are not
  blocked.
- Toggle YouTube off in Shorts Blocker Kids and verify YouTube Shorts no longer
  blocks while other selected platforms remain protected. Toggle YouTube back
  on before continuing.
- Pending: Reboot the device, record Accessibility/protection and entitlement
  state, then repeat Shorts and normal-video/playlist regressions. Record any
  required service re-enablement rather than assuming reboot recovery passed.

## TikTok

Expected to block:

- Open For You feed in `com.zhiliaoapp.musically`.
- Swipe to the next For You item; overlay must not duplicate or flicker.

Expected not to block:

- Open Profile.
- Open Search.
- Open Settings.
- Open Inbox.
- Open comments on a non-target screen where available.

Unsupported:

- `com.ss.android.ugc.trill` must remain documented as unsupported.

Evidence:

- Video showing For You blocked.
- Video or screenshots showing Profile, Search, and Settings are not blocked.
- Target package and app version.
- Toggle TikTok off in Shorts Blocker Kids and verify the For You feed no
  longer blocks while other selected platforms remain protected. Toggle TikTok
  back on before continuing.

## Instagram

Expected to block:

- Open Reels tab.
- Open a Reel from feed if available.
- Swipe between Reels; overlay must not duplicate or flicker.

Expected not to block:

- Open Feed.
- Open Profile.
- Open Comments on a normal post where available.
- Open Direct messages or inbox.
- Open Search.
- Open Settings.
- Open Story.

Evidence:

- Video showing Reels blocked.
- Video or screenshots showing Feed, Profile, and Story are not blocked.
- Target package and app version.
- Toggle Instagram off in Shorts Blocker Kids and verify Reels no longer blocks
  while other selected platforms remain protected. Toggle Instagram back on
  before continuing.

## Facebook

Expected to block:

- Open Reels surface.
- Open a Reel from feed if available.
- Swipe between Reels; overlay must not duplicate or flicker.

Expected not to block:

- Open Feed.
- Open Profile.
- Open Groups.
- Open Comments on a normal post or video where available.
- Open Search.
- Open Settings.
- Open Messages where available.

Unsupported:

- `com.facebook.lite` must remain documented as unsupported.

Evidence:

- Video showing Reels blocked.
- Video or screenshots showing Feed, Profile, and Groups are not blocked.
- Target package and app version.
- Toggle Facebook off in Shorts Blocker Kids and verify Reels no longer blocks
  while other selected platforms remain protected. Toggle Facebook back on
  before continuing.

## Cross-App Scenarios

Run these at least once per device:

- Back from overlay: no loop and no duplicate overlay.
- Home from overlay: returns to launcher and overlay clears.
- Reopen blocked app after Home: expected blocked screen blocks again.
- Enter wrong PIN: temporary allow is not granted.
- Enter correct PIN: temporary allow choices appear.
- Choose 15 minute temporary allow: blocked surfaces are allowed until expiry.
- After temporary allow expiry: blocked surfaces block again.
- Disable global Protection: supported short-video screens do not block.
- Re-enable global Protection: supported short-video screens block again.
- Disable AccessibilityService: no blocking occurs and dashboard reports
  inactive protection.
- If Device Admin tamper protection is active, request admin disable in Android
  settings and verify the warning appears before admin is turned off.

## Billing Scenarios

Use [Billing Internal Testing](SHORTS_BLOCKER_PLAY_BILLING_INTERNAL_TEST_RUNBOOK.md) BIL-01…BIL-12 on a Play-installed fresh AAB. Record purchased/pending/none and failed-query recovery, offline cached state, refund/revocation/expiry refresh, persistence-before-acknowledgement and account changes. No own server or 72-hour grace test exists. Fake apps and a sideloaded APK do not verify Play purchases.

## Pass Criteria

The real-device QA pass is complete only when:

- Every supported app has one positive blocking video.
- Every supported app has non-blocking evidence for the listed normal screens.
- Each supported app's enable/disable toggle has evidence that disabling the
  app prevents blocking only for that platform.
- Unsupported packages are not claimed as supported.
- No duplicate overlay, overlay flicker, or Back/Home loop is observed.
- Device model, Android version, Shorts Blocker Kids build, target package, and
  target app version are recorded for every run.

## Remaining Limits

Real-device QA validates the observed app versions and devices only. Social app
UI changes, locale changes, account-specific UI, OEM background restrictions,
and future Android platform behavior can still require follow-up calibration.

## Release And Scenario Evidence Matrix

Record package/versionName/versionCode, source commit, install source, exact APK/AAB path/size/SHA-256, test date and tester identity domain (no raw account/token secrets). Bind evidence to that artifact, not an earlier smoke build.

## App Matrix

| App | Package | Version tested | Target surface | Non-target surfaces | Result |
|---|---|---|---|---|---|
| YouTube | `com.google.android.youtube` | TBD | Shorts | normal video, Home, Search, comments, profile/account | Pending |
| TikTok short-video feed | `com.zhiliaoapp.musically` | TBD | For You | Profile, Search, Settings, Inbox, comments where available | Pending |
| Instagram | `com.instagram.android` | TBD | Reels | Feed, Profile, Story, Comments, Direct/messages, Search, Settings | Pending |
| Facebook | `com.facebook.katana` | TBD | Reels | Feed, Profile, Groups, Comments, Search, Settings, Messages | Pending |

Unsupported packages must not be claimed as supported:

- `com.ss.android.ugc.trill`
- `com.facebook.lite`
- `com.instagram.lite`
- `com.google.android.youtube.tv`
- `com.google.android.apps.youtube.music`

## Scenario Matrix

| ID | Scenario | Expected result | Evidence |
|---|---|---|---|
| DET-01 | Block target surface for each supported app. | Overlay appears once without flicker or duplicate launch. | Screen recording per app. |
| DET-02 | Do not block false-positive surfaces for each supported app. | No overlay on listed non-target screens. | Screenshot or recording per app/surface. |
| DET-03 | Unsupported package lookalike. | No overlay and no supported-platform claim. | Recording and package name. |
| OVR-01 | Repeated high-confidence events. | Single stable overlay. | Recording or emulator E2E output. |
| OVR-02 | Exit to phone home. | Overlay clears and launcher opens. | Recording. |
| PIN-01 | Wrong PIN. | Temporary allow is not granted. | Recording. |
| PIN-02 | Correct PIN. | Temporary allow choices appear. | Recording. |
| TMP-01 | Temporary allow 15 minutes. | Supported short-video surfaces are allowed until expiry. | Recording and timestamp. |
| TMP-02 | Temporary allow 30 minutes. | Supported short-video surfaces are allowed until expiry. | Recording and timestamp. |
| TMP-03 | Temporary allow 60 minutes. | Supported short-video surfaces are allowed until expiry. | Recording and timestamp. |
| SUB-01 | Subscription active. | Paid protection entitlement is active. | Dashboard status and Play tester evidence. |
| SUB-02 | Subscription pending. | Paid protection is not unlocked until approval. | Dashboard status and Play tester evidence. |
| SUB-03 | Subscription canceled but still active. | Access remains active until Play reports expiry. | Dashboard status and Play tester evidence. |
| SUB-04 | Grace period / account hold. | App reflects Google Play purchase state after refresh. | Dashboard status and Play tester evidence. |
| SUB-05 | Subscription expired. | Paid protection locks after active entitlement ends. | Dashboard status and Play tester evidence. |
| BILL-01 | Restore purchase. | Active subscription is restored after reinstall or clear data. | Dashboard status and Play tester evidence. |

## Unit And Integration Regression Scope

### Unit Tests

Detector tests:

- YouTube Shorts high-confidence positive fixtures.
- YouTube normal video/home/search negative fixtures.
- TikTok short-video feed positive fixtures.
- TikTok non-feed/settings/profile/inbox negative fixtures.
- Instagram Reels positive fixtures.
- Instagram feed/profile/messages negative fixtures.
- Facebook Reels positive fixtures.
- Facebook feed/groups/profile/marketplace negative fixtures.
- Regional and locale fixture variants where labels are used.
- Misleading text where Shorts/Reels-like labels appear outside short-video
  mode.
- Empty Accessibility tree, null package, unsupported aliases, action rail,
  vertical fullscreen, and repeated-feed branches.

Shared blocking tests:

- High-confidence result shows overlay.
- Medium/low/no confidence does not block in production mode.
- Unsupported package never blocks.
- Temporary allow suppresses blocking.
- Protection disabled suppresses blocking.
- Overlay cooldown prevents flicker loops.
- Service restart starts from clean blocker state.
- First block, duplicate event, overlay already visible, short-to-normal,
  normal-to-short, unsupported package, global protection ON/OFF, and rapid
  repeated events are covered by `BlockingDecisionControllerTest`.

PIN and temporary allow tests:

- PIN creation validates 4-6 digits.
- Weak PIN is rejected.
- PIN hash and salt are stored, not plain text.
- Correct PIN unlocks parent action.
- Wrong PIN increments rate limit.
- Lockout expires.
- Temporary allow stores 15, 30, and 60 minute durations.
- Temporary allow expiry resumes protection.

Storage tests:

- Settings defaults are safe.
- Disclosure accepted flag is required before protection can become active.
- Global protection state and selected protection mode persist locally.
- Entitlement cache reads/writes only billing state.
- Active, expired, and missing temporary allow states are covered.

### Integration Tests

Accessibility event routing:

- Events from unsupported packages are ignored.
- Events from supported packages route to the correct detector.
- Scanner is not called for unsupported packages.
- Detector result maps to shared overlay request.
- Non-YouTube app support does not regress YouTube routing.

Overlay integration:

- Overlay receives platform name and blocked surface.
- `Exit to phone home` performs expected exit behavior.
- `Enter PIN` opens parent PIN flow.
- Overlay dismisses on temporary allow.
- Overlay dismisses when leaving blocked app/surface.
- Overlay dismisses when AccessibilityService is interrupted or destroyed.

Entitlement integration:

- Active entitlement enables protection.
- A successful Play query with no purchased subscription clears paid access
  without blocking parent settings or revoking a valid local free test.
- A failed Play query retains the persisted paid-access cache; a successful
  purchase-state query refreshes ACTIVE/PENDING/INACTIVE according to the
  [current Billing contract](SHORTS_BLOCKER_PLAY_BILLING_BACKEND_RUNBOOK.md).
  There is no server-side expiry or offline-grace timer in this implementation.
- Restore purchase refreshes entitlement.

### Accessibility Flows

Required scenarios:

- Disclosure is shown before opening Android Accessibility settings.
- User can decline disclosure.
- Decline does not open Android Accessibility settings.
- Android Accessibility settings open only after affirmative consent.
- Re-triggering setup after decline shows disclosure again.
- Dashboard request shows disclosure if consent is missing.
- Disclosure text names every supported app/surface in the shipped build.
- Disclosure states what data is accessed, how it is used, and what is not
  collected or shared.
- Back/home/navigation away from disclosure is not treated as consent.
- `isAccessibilityTool=true` is not set unless product positioning changes to a
  true disability-assistance tool.
- Optional Device Admin tamper protection has a separate parent-facing
  disclosure before Android Device Admin settings open.

## Emulator Fixture Automation

### Fake Target Apps

Module:

- `test-fixtures/fake-social-apps`

Gradle flavors:

| Flavor | Package | Simulated screens |
|---|---|---|
| `youtube` | `com.shortsblockerkids.fixture.youtube` | Home, Search, Normal Video, Shorts |
| `tiktok` | `com.shortsblockerkids.fixture.tiktok` | For You, Profile, Search, Settings |
| `instagram` | `com.shortsblockerkids.fixture.instagram` | Feed, Profile, Reels, Story |
| `facebook` | `com.shortsblockerkids.fixture.facebook` | Feed, Profile, Reels, Groups/Page |
| `unsupported` | `com.example.shortvideo.fixture` | Reels-like lookalike, Feed |

The fake apps use native Android views and resource IDs/content descriptions
that exercise the same AccessibilityService scanner and detector path as the
product app. The supported fake app packages are debug-only detector aliases so
they can be installed on a Play Store emulator without replacing preinstalled
apps such as YouTube. Release builds keep the real supported package names.

### E2E Test Entry Point

Instrumentation tests:

- `app/src/androidTest/java/com/shortsblockerkids/e2e/ShortVideoBlockerEmulatorE2eTest.kt`

Run on a connected emulator:

```bash
ANDROID_HOME=/path/to/Android/Sdk ./gradlew :app:connectedDebugAndroidTest
```

The `:app:connectedDebugAndroidTest` task depends on `installFakeSocialApps`,
which builds and installs all fake target apps first.

Fixture-only install:

```bash
ANDROID_HOME=/path/to/Android/Sdk ./gradlew installFakeSocialApps
```

### Emulator E2E Scenarios

Covered by the Android instrumentation `UiAutomation` suite:

- Fake YouTube opens and non-Shorts Home/Search/Normal Video do not block.
- Fake YouTube Shorts blocks and records active package
  `com.shortsblockerkids.fixture.youtube`.
- Fake TikTok Profile/Search/Settings do not block.
- Fake TikTok For You blocks and records active package
  `com.shortsblockerkids.fixture.tiktok`.
- Fake Instagram Feed/Profile/Story do not block.
- Fake Instagram Reels blocks and records active package
  `com.shortsblockerkids.fixture.instagram`.
- Fake Facebook Feed/Profile/Groups do not block.
- Fake Facebook Reels blocks and records active package
  `com.shortsblockerkids.fixture.facebook`.
- Repeated short-video events keep one blocking overlay.
- Rapid repeated events keep the overlay visible without duplicate overlays.
- Normal screen to short-video screen blocks again.
- Short-video screen to normal screen is asserted to clear the overlay.
- `Enter PIN` opens the parent PIN flow.
- Wrong PIN does not unlock temporary allow.
- Correct PIN opens temporary allow choices.
- Temporary allow suppresses blocking.
- Expired temporary allow resumes blocking.
- `Exit to phone home`, Back, and Home recovery are exercised.
- Global Protection OFF suppresses blocking.
- Global Protection ON resumes blocking.
- Unsupported package with a Reels-like UI does not block.
- Rapid switching across fake YouTube, TikTok, Instagram, Facebook, and an
  unsupported lookalike blocks only after a confirmed short-video screen.
- YouTube still blocks after TikTok, Instagram, and Facebook detector events.

### Not Covered Automatically

- Per-platform settings already exist and have unit/manual coverage. Automated
  fixture-driven toggle scenarios remain pending where reliable; extend fixture
  coverage without treating these settings as a missing product feature.
- Real app UI trees, IDs, locales, account states, and app-version changes are
  not proven by fake apps.
- Release package filtering for the real social package names remains covered
  by unit tests and still requires real-device QA with the real apps.
- Fake apps passing does not prove real YouTube, TikTok, Instagram, or Facebook
  Accessibility trees.
- OEM AccessibilityService timing, overlay behavior, background restrictions,
  and launcher behavior are not fully represented by a single emulator.
- Google Play Accessibility review behavior and real-device policy evidence are
  not proven by emulator tests.

## Overlay, Service And Honest Guardrails

Accept disclosure before enabling service; declining must not open Android settings. Enable and return to the app, check status, then disable Accessibility and verify blocking stops/warning is honest; re-enable and retest. Non-allowlisted apps are not scanned/blocked. All four production packages have positive and normal-screen negative evidence.

Check one branded overlay without flicker/duplicates, exit to phone home, leaving the supported surface/app, protection OFF with PIN, allow expiry and permission/service loss. Repeat portrait, landscape, smallest screen and scrolling when height is insufficient. Wrong PIN grants nothing; 15/30/60 minute choices work; app/phone restart, update and clock rollback preserve the intended timing. Verify last-event-inside-debounce and unavailable-root transitions do not miss the final surface; service shutdown cancels pending scans.

Launcher stays visible; no system-service impersonation, Device Owner requirement, hidden Settings/uninstall blocking or absolute anti-bypass claim. Optional Device Admin needs separate disclosure/Android confirmation; inspect disable warning and recovery before uninstall. Verify release diagnostics have no raw trees/private UI data and debug tools are absent. Manifest/permission checks are in [AAB Readiness](SHORTS_BLOCKER_AAB_RELEASE_READINESS.md).

## Pending Performance Acceptance

The Roadmap's performance requirement remains unverified. For each supported app,
measure the time from a relevant transition into its supported short-video screen
until the blocking overlay first becomes visible. The normal-case target is
**less than 1 second**. Record the transition and overlay timestamps, measurement
method, exact app version, Android version, device/OEM and tested build/artifact.
Mark the result Pending until measured; a configured debounce or a passing unit
test does not establish this real-device latency result.

Repeat entry, exit, app switching and temporary-allow expiry while checking for
overlay flicker, back loops, ANRs and crashes. Preserve the existing zero-blocking
acceptance on core non-target screens. Store dated pass/fail evidence with the
same privacy restrictions as the other QA scenarios.

## Windows Or WSL Emulator Workflow

Use the repository wrapper and configured Android SDK to build a debug APK in its build-output path. Start a Windows AVD in Android Studio Device Manager; prefer a Play-enabled image for Billing testing. WSL may build while Windows owns emulator/USB; use Windows adb and the actual Windows-accessible APK path. Do not overwrite the historical dist bundle or its checksum. Replace placeholders with your local paths; no specific old distro/KVM fix is a prerequisite.

~~~powershell
& "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -list-avds
& "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -avd <your-avd-name>
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r <actual-apk-path>
~~~

The expected production package is com.shortsblockerkids. Check unauthorized devices by unlocking and accepting USB debugging. Fake-app automation is not proof against actual social app UIs, OEM behavior, Billing or policy review.

## Historical Owner-Reported Smoke Evidence

The owner reported smoke success across five devices on May 22, 2026: install, PIN, Accessibility enablement, normal YouTube videos unblocked, Shorts blocked, exit and 5/10/15 minute allow. Device/model/Android/app-version/build metadata and detailed recordings were not supplied. This is prior user-reported evidence, not fresh or four-platform production certification.

On May 28, Facebook Reels did not block in an owner smoke run; code-level detector hardening followed. A successful real-device retest after that change was not established by that report. Record the missing device/Android/social-app/build/rotation/OEM matrix and final signed Play/Billing regression separately.

The 2026-09-14 event-rescan report proved a code-level scheduling defect/fix with unit tests. It did not independently confirm the original installed-app incident or exercise coroutine/window-manager behavior on a device. Do not claim those integrations passed based on compilation/lint.

## Evidence Status And Consolidation

### Current Remediation Acceptance (2026-10-16 local)

**Local preparation: FAIL. Overall publication readiness: FAIL.** The reserved final frozen-source phase started 2026-10-15T22:53:35Z (16 October in Europe/Berlin). No automatic retry or source change followed. Ktlint failed; fresh unit XML is **594 / 1 / 0 / 0** (tests/failures/errors/skipped). Debug/release builds and both lint variants completed. Coverage verification/report did not execute after the failing unit prerequisite; no new achieved ratios are claimed. The entire 32-test instrumentation task started once without a filter, but terminated with only **13 completed XML tests / 0 failures / 0 errors / 0 skipped**, leaving **19 without results**. This is incomplete, never a whole-suite pass. Both Pevisor reviews remain unavailable and unfulfilled.

Completed XML suites: LocaleMatrix 3/0/0/0, ParentalSettings 9/0/0/0 and Runtime past-wall method 1/0/0/0. Other two Runtime and all 17 E2E results are missing after UiAutomation disconnect/process death. Unit failure is the dashboard elapsed-clock regression at line 35; source diagnosis identifies missing elapsed argument in DashboardStateFactory, pending owner approval. Style fails Runtime lines 89/90/114/115; both lint lanes pass. Fresh coverage is blocked; old branch 89.86961% is historical, required 95% unchanged.

| Previous failing scenario | Evidence, correction and remaining uncertainty |
|---|---|
| Interrupt/automatic exit | The old assertion required automatic exit while the overlay was visible. Under the expressly approved stable-overlay contract, successful blocking waits for user action; only failed overlay display uses bounded fallback. The revised interrupt test checks the stable barrier and same fixture. Its final-run result is missing. |
| Ukrainian PIN | Successful overlay display immediately initiated HOME, racing a PIN tap; the old helper retained nodes and eagerly scanned diagnostics. Stable overlay and bounded node reacquisition address this common static race. Exclusive timing of this old failure was not proven; its final-run result is missing. |
| German PIN | Same established common race and causal correction as Ukrainian PIN. The localized assertion remains. Exclusive per-method timing is unproven; final result missing. |
| Cancel PIN / repeat block | Same common automatic-navigation/node race. PIN cancellation resets the blocking episode; assertions retain actual PIN navigation and same-fixture reentry. Final result missing. |
| Past-wall deadline | Corrected original-APK diagnostic failed 151 ms before the elapsed deadline with matching stored elapsed/boot metadata, but SettingsLoadingProtectionConservative then SettingsUnavailableProtectionDegraded and launcher active. This proves a conservative loading/unavailable barrier, not a clock mismatch. Underlying readiness failure remains unresolved. This method passed in the final phase; natural allowance completion is still unverified. |
| Future-wall deadline | Original expiry-recheck failure remains without an exclusive proven cause. First diagnostic failed earlier in quiet-window setup with a fixture ANR in the same run. Corrected original-APK diagnostic passed. Clock predicate, deadline and assertions were not weakened. Final-run result missing. |

The 15 October actual YouTube splash/permission/ANR observations below are historical and do not verify this final source. Valid real-app Protection, ordinary screens/short video, Exit and reentry remain unexecuted. Real TikTok, Instagram and Facebook are unavailable. Accelerated clocks do not prove natural 15/30/60-minute expiry. Device Admin consent/disable, rotation/truncation/small-screen behavior and the full actual-device matrix remain unverified. Artifact and exact evidence: [AAB Readiness](SHORTS_BLOCKER_AAB_RELEASE_READINESS.md).

### Historical Remediation Acceptance (2026-10-15 daytime)

Baseline reproduction: 1 test / 1 failure / 0 errors / 0 skipped; `exitBackAndHomeInteractionsRecoverWithoutDuplicateOverlay` reported `Blocking overlay was still visible` before exit-code changes. That named scenario passed in that historical full run, while six other device scenarios failed.

Historical unit XML: **568 tests / 0 failures / 0 errors / 0 skipped**. Both Android lint variants pass. Historical full fixture instrumentation, with no class filter: **32 tests / 6 failures / 0 errors / 0 skipped** in 13 minutes 25 seconds. AutomaticProtectionWiring has 20 passing executable tests replacing two source-text tests; BlockingProtectionExecutor has 20 passing tests and PhoneHomeExitController 13. Whole local acceptance still fails branch coverage: 896 covered / 101 missed = 89.86961% versus unchanged 95%. Instructions 95.50660% and lines 98.81003% pass.

| Instrumented suite | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| LocaleMatrixInstrumentedTest | 3 | 0 | 0 | 0 |
| ParentalSettingsSessionInstrumentedTest | 9 | 0 | 0 | 0 |
| RuntimeProtectionLifecycleInstrumentedTest | 3 | 3 | 0 | 0 |
| ShortVideoBlockerEmulatorE2eTest | 17 | 3 | 0 | 0 |

- `RuntimeProtectionLifecycleInstrumentedTest.interruptRetainsBarrierWhileAutomaticExitIsUnconfirmed`: Automatic exit did not start.
- `RuntimeProtectionLifecycleInstrumentedTest.elapsedDeadlineSurvivesReconnectDespitePastWallDeadline`: Allowance ended before elapsed deadline.
- `RuntimeProtectionLifecycleInstrumentedTest.elapsedDeadlineExpiresAfterInterruptDespiteFutureWallDeadline`: Expiry failed to recheck the quiet active window.
- `ShortVideoBlockerEmulatorE2eTest.ukrainianLocaleOverlayAndPinSmoke`: Unable to click UI text: Ввести PIN-код.
- `ShortVideoBlockerEmulatorE2eTest.germanLocaleOverlayAndPinSmoke`: Unable to click UI text: PIN eingeben.
- `ShortVideoBlockerEmulatorE2eTest.cancelingPinDuringAutomaticExitAllowsSameShortToBlockAgain`: Unable to click UI text: Enter PIN.

The nine passing ParentalSettingsSession cases cover unauthorized writes, wrong/cancelled PIN, authorized platform mutation, background/recreation revocation and 15/30/60-minute grant paths. They do not establish natural-duration expiry. LocaleMatrix passes resource resolution, formatting and plurals; German/Ukrainian overlay PIN-click scenarios fail. All three runtime lifecycle/deadline scenarios fail. Causes are unproven; do not reclassify them as harmless fixture failures or weaken assertions. See AAB Readiness for exact paths and counters.

Before fixture instrumentation, the observed unresponsive YouTube was force-stopped and the device returned Home; both commands succeeded. No data/account change occurred. The task installed the existing five fixture apps; fixtures do not prove real social-app UI. No test retry or source edit occurred during this frozen-source phase.

For every remaining device criterion, record the actual observed scope; these fixture results do not close all of the following requirements: disclosure refusal/re-trigger/affirmative consent before Settings; service disable/re-enable and honest dashboard status; each platform/global toggle; PIN creation/wrong PIN/lockout/cancel/background/reopen; 15/30/60-minute allow with natural expiry, restart and system-clock changes; 20-day activation/one-time start/expiry; storage recovery; separate optional Device Admin consent/disable; en/uk/de formatting, small screen and rotation. Label injected clocks/shortened deadlines as accelerated tests and record natural device expiry separately.

Available emulator: Android 15 / API 35, Google `sdk_gphone64_x86_64`, 320 × 640 at density 160. Real YouTube 19.17.42 is installed; real TikTok, Instagram and Facebook are unavailable. No actual social-app acceptance run is recorded for this candidate, including the installed YouTube. Fixtures do not establish those UI results. Record unavailable devices/app versions and every unexecuted row rather than treating code support as a real-app pass. No video is recorded in this remediation phase.

The 13:34:41–13:35:44 UTC availability check launched actual YouTube 19.17.42 (versionCode 1545868784); startup reached an Android notification-permission dialog above Home/Shorts tabs. No permission/account decision was made. Accessibility services were not enabled at that observation. The earlier lookup of release package `com.shortsblockerkids` cannot establish whether the distinct debug package was installed and is not used as absence evidence.

The separately authorized own-APK installation subsequently completed successfully at 13:37:40 UTC. Correct package `com.shortsblockerkids.debug` reports `0.1.0-debug` / code 1, minSdk 26 / targetSdk 36. Installed APK evidence: 14,522,630 bytes, modified 2026-10-15 13:29:54 UTC, SHA-256 `0FC3419E049BCACF2FA56F8F1227ECE7BEFDD2DD31AA7D9FB3F69AE5E7091151`. During the correct debug-launcher attempt at 13:40:06 UTC, the screenshot showed the system dialog `YouTube isn't responding` with Close app / Wait, rather than the blocker setup UI. No restart, further wait, account action or ANR-cause diagnosis was performed.

Valid Protection/Accessibility setup, unobstructed YouTube Home, normal video, actual Shorts blocking, close-video exit and re-entry blocking all remain unexecuted. The YouTube ANR is an observed barrier; its cause is unknown and it is not classified as a Shorts Blocker Kids failure. Real TikTok, Instagram and Facebook remain unavailable. Generated screenshots under `app/build/rc-verification-20261015/`: `real-youtube-initial.png`, `real-youtube-after-startup.png`, `fresh-debug-initial.png`. These are availability/setup evidence, not post-fix behavior passes. No video or raw UI dump was recorded; the device is free.

All matrices/scenarios above require dated pass/fail and redacted screenshots/recordings, observed status/confidence and exact artifact/device/app metadata. Blank, TBD and Pending entries are not passes. Do not store child history, private messages, raw tree dumps, full purchase tokens or account credentials in evidence.

Consolidated from Test Plan, Emulator E2E QA, Internal Test Matrix, Phase 5/7/9 checks, Privacy Permissions Audit, Windows Fallback and Real Device Smoke Report. Sources remain unchanged pending separate deletion approval. No emulator/real device/Play lifecycle tests ran in this preparation.
