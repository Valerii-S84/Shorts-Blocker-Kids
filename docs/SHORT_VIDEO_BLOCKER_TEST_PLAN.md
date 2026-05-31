# Short Video Blocker Test Plan

Status: Active test plan. The current support matrix is:

| Platform | Status |
|---|---|
| YouTube Shorts | supported |
| TikTok main `com.zhiliaoapp.musically` | supported by code; needs real-device QA |
| TikTok regional `com.ss.android.ugc.trill` | not supported |
| Instagram Reels | supported by code; needs real-device QA |
| Facebook Reels | supported by code; needs real-device QA |
| Facebook Lite `com.facebook.lite` | not supported |

Fixture unit tests exist for the supported and code-supported surfaces, and
real-device QA is still required before production for TikTok main, Instagram
Reels, and Facebook Reels.

## Test Goals

- Preserve current YouTube Shorts blocker behavior.
- Prove each new platform detector before enabling it.
- Prove shared overlay, parent PIN, and temporary allow across supported apps.
- Prove Accessibility disclosure and consent cannot be bypassed.
- Prove local billing entitlement states before production rollout.
- Prove no child viewing history or unsupported data is collected.
- Keep backend/cloud/analytics/ads out of scope unless a future product task
  explicitly adds them.

## Unit Tests

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
- Temporary allow stores 5, 10, and 15 minute durations.
- Temporary allow expiry resumes protection.

Storage tests:

- Settings defaults are safe.
- Disclosure accepted flag is required before protection can become active.
- Global protection state and selected protection mode persist locally.
- Entitlement cache reads/writes only billing state.
- Active, expired, and missing temporary allow states are covered.

## Integration Tests

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
- Expired entitlement disables paid protection without blocking parent settings.
- Offline grace follows finalized rules.
- Restore purchase refreshes entitlement.

## Emulator E2E QA

Automated emulator E2E coverage is defined in
`docs/SHORTS_BLOCKER_EMULATOR_E2E_QA.md`.

It uses local fake target apps with debug-only fixture package aliases:

- `com.shortsblockerkids.fixture.youtube`
- `com.shortsblockerkids.fixture.tiktok`
- `com.shortsblockerkids.fixture.instagram`
- `com.shortsblockerkids.fixture.facebook`

The emulator suite verifies blocking behavior through the real
AccessibilityService and Android instrumentation `UiAutomation`. The latest
local run on 2026-05-24 passed 12/12 emulator tests, including rapid switching
between fake apps. It is still a controlled fixture
environment and does not replace final real-device QA against the real YouTube,
TikTok, Instagram, and Facebook apps.

Per-platform enable/disable is not currently an automated E2E scenario because
the shipped product has a global protection toggle, not per-platform settings.

## Accessibility Flow Tests

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

## Manual Real-Device QA

Execution checklist:

```text
docs/SHORTS_BLOCKER_REAL_DEVICE_QA_CHECKLIST.md
```

Device matrix:

- At least one Pixel device.
- At least one Samsung device.
- At least one Xiaomi/Redmi or other OEM with aggressive background behavior.
- Android 12, 13, 14, 15, and current target Android version where available.
- At least one low-end device.

Manual setup:

1. Fresh install.
2. Create parent PIN.
3. Review disclosure.
4. Decline and verify settings do not open.
5. Reopen setup, accept disclosure.
6. Enable AccessibilityService.
7. Confirm dashboard status.
8. Test each supported app.

Manual privacy checks:

- `INTERNET` and `ACCESS_NETWORK_STATE` are allowed only for the explicitly
  approved Google Play Billing SDK integration unless a later backend/network
  scope is approved.
- No camera permission.
- No microphone permission.
- No location permission.
- No contacts permission.
- No SMS permission.
- No raw Accessibility tree logs in release.
- No stored child viewing history.

## Per-App Test Matrix

YouTube:

- Open Shorts from bottom tab.
- Open Shorts from home recommendation.
- Open Shorts from search.
- Open normal YouTube video.
- Open YouTube home.
- Swipe between Shorts.
- Exit overlay to phone home.
- Parent temporary allow works.

TikTok:

- Open primary TikTok feed.
- Open profile.
- Open inbox/messages.
- Open settings.
- Open search.
- Verify feed-specific detection if implemented.
- Verify app-level fallback only if explicitly approved and disclosed.
- Parent temporary allow works.

Instagram:

- Open Reels tab.
- Open Reels from feed.
- Open normal feed.
- Open profile.
- Open messages.
- Open stories.
- Parent temporary allow works.

Facebook:

- Open Reels surface.
- Open Reels embedded in feed.
- Open normal feed.
- Open groups.
- Open marketplace.
- Open profile.
- Parent temporary allow works.

For every app:

- Confirm no false positive on non-target surfaces.
- Confirm no overlay flicker.
- Confirm overlay dismisses after leaving app.
- Confirm AccessibilityService disable stops blocking and dashboard explains
  inactive protection.

## TikTok / Reels Manual QA Checklist

Record before each run:

- Device model and Android version.
- Shorts Blocker Kids build variant and version code.
- Target app package and version name.
- Whether Protection is ON, AccessibilityService is enabled, and temporary
  allow is inactive before the scenario starts.

TikTok primary package `com.zhiliaoapp.musically`:

- Open the primary For You feed; expected result: blocking overlay appears.
- Swipe to the next feed item; expected result: blocking remains active without
  stacked overlays or flicker.
- Tap `Exit to phone home`; expected result: phone home opens and overlay clears.
- Reopen TikTok feed, enter parent PIN, choose 5 minute temporary allow; expected
  result: feed remains allowed until expiry.
- After temporary allow expires, reopen the primary feed; expected result:
  blocking overlay appears again.
- Open Profile; expected result: no blocking overlay.
- Open Inbox/messages; expected result: no blocking overlay.
- Open Search; expected result: no blocking overlay.
- Open Settings; expected result: no blocking overlay.
- Confirm regional package `com.ss.android.ugc.trill` is not claimed as covered
  unless a separate regional QA run and code change explicitly enable it.

Instagram package `com.instagram.android`:

- Open Reels tab; expected result: blocking overlay appears.
- Open a Reel from feed; expected result: blocking overlay appears.
- Swipe between Reels; expected result: blocking remains active without stacked
  overlays or flicker.
- Tap `Exit to phone home`; expected result: phone home opens and overlay clears.
- Reopen Reels, enter parent PIN, choose 10 minute temporary allow; expected
  result: Reels remains allowed until expiry.
- Open normal feed; expected result: no blocking overlay.
- Open Profile; expected result: no blocking overlay.
- Open Messages; expected result: no blocking overlay.
- Open Stories; expected result: no blocking overlay.

Facebook package `com.facebook.katana`:

- Open Reels surface; expected result: blocking overlay appears.
- Open Reels embedded from feed; expected result: blocking overlay appears.
- Swipe between Reels; expected result: blocking remains active without stacked
  overlays or flicker.
- Tap `Exit to phone home`; expected result: phone home opens and overlay clears.
- Reopen Reels, enter parent PIN, choose 15 minute temporary allow; expected
  result: Reels remains allowed until expiry.
- Open normal feed; expected result: no blocking overlay.
- Open Groups; expected result: no blocking overlay.
- Open Marketplace; expected result: no blocking overlay.
- Open Profile; expected result: no blocking overlay.
- Confirm Facebook Lite `com.facebook.lite` is not claimed as covered unless a
  separate Lite QA run and code change explicitly enable it.

Cross-platform checks:

- Dashboard lists exactly the protected platforms shipped in the build.
- Accessibility disclosure names YouTube Shorts as supported, marks TikTok main,
  Instagram Reels, and Facebook Reels as code-supported pending real-device QA,
  and states that TikTok regional and Facebook Lite are not supported.
- Disabling AccessibilityService stops blocking in every target app.
- No raw Accessibility tree, video title, URL, account name, message, or watch
  history is stored or exported.

## Billing Test Matrix

Detailed execution runbook:

```text
docs/SHORTS_BLOCKER_PLAY_BILLING_INTERNAL_TEST_RUNBOOK.md
```

Play Console setup:

- License tester configured.
- Subscription product configured.
- Base plan configured.
- Privacy Policy URL configured as `https://shortsblockerkids.de/privacy`.
- RTDN webhook configured as
  `https://billing.shortsblockerkids.de/billing/play/rtdn`.
- Test card flows available.

App-side tests:

- Product details load.
- Billing unavailable state.
- Purchase success.
- Purchase cancel.
- Purchase pending.
- Purchase acknowledgement.
- Restore after reinstall.
- Manage subscription opens Google Play.

Subscription lifecycle tests:

- Active.
- Canceled but active.
- Renewed.
- Expired.
- Grace period.
- Account hold.
- Revoked/refunded.
- Unknown/backend unavailable.

## Backend Verification Tests

Applicable to the production billing scope. The repository now includes a
`billing-backend` module with Play purchase verification, entitlement status,
and RTDN endpoints.

Required backend tests:

- `:billing-backend:test` passes.
- `POST /billing/play/verify` verifies a Play purchase token and stores only
  billing technical data.
- `GET /entitlement/status` returns active, canceled-active, grace, on-hold,
  expired, revoked, and unknown states without exposing purchase tokens.
- `POST /billing/play/rtdn` rejects production requests without authenticated
  Pub/Sub push config.
- RTDN `messageId` handling is idempotent.
- Backend logs and stored records do not include child data, app usage history,
  accessibility tree dumps, URLs, video titles, account names, messages,
  screen recordings, audio, location, contacts, or browsing history.

## Release Gates

Code gates:

- `ANDROID_HOME=/path/to/Android/Sdk gradle ktlintCheck`
- `ANDROID_HOME=/path/to/Android/Sdk gradle :app:testDebugUnitTest`
- `ANDROID_HOME=/path/to/Android/Sdk gradle :app:connectedDebugAndroidTest`
- `ANDROID_HOME=/path/to/Android/Sdk gradle :app:lintDebug`
- `ANDROID_HOME=/path/to/Android/Sdk gradle :app:lintRelease`
- `ANDROID_HOME=/path/to/Android/Sdk gradle :app:assembleDebug`
- `SBK_BILLING_BACKEND_BASE_URL=https://billing.shortsblockerkids.de ANDROID_HOME=/path/to/Android/Sdk gradle :app:assembleRelease`
- `SBK_BILLING_BACKEND_BASE_URL=https://billing.shortsblockerkids.de ANDROID_HOME=/path/to/Android/Sdk gradle :app:bundleRelease`
- `ANDROID_HOME=/path/to/Android/Sdk gradle :app:jacocoDebugUnitTestCoverageVerification`
- JaCoCo branch coverage must be at least 90.00%; instruction and line coverage
  gates must not be lower than the previous baseline.
- `ANDROID_HOME=/path/to/Android/Sdk gradle localQualityGate` aggregates
  the local gate when an emulator is available.

Manual gates:

- Full Accessibility disclosure video recorded.
- Real-device QA completed for every supported app.
- Billing test matrix completed.
- Backend verification test matrix runs against
  `https://billing.shortsblockerkids.de` after live DNS/TLS deployment.
- Privacy permissions audit completed for release merged manifest.
- No unsupported permissions.
- No raw Accessibility logs in release.

Policy gates:

- Website static checks pass:
  `cd website && npm run check && npm run build && npm run smoke`.
- Privacy Policy URL `https://shortsblockerkids.de/privacy` hosted and accurate.
- Data Safety form complete.
- Accessibility declaration complete.
- Target Audience and Content complete.
- Store listing does not overclaim support.
- Demo video shows disclosure, consent, settings enablement, blocked surfaces,
  decline flow, and parent temporary allow.

## Exit Criteria

The full Short Video Blocker product is test-ready only when:

- YouTube regression suite passes.
- Each new platform detector has positive and negative fixtures.
- Each supported app passes real-device QA.
- Shared overlay works across all supported apps.
- Parent PIN and temporary allow work across all supported apps.
- Billing entitlement is verified.
- Backend/cloud/analytics/ads remain absent, or a future approved backend passes
  privacy and entitlement tests before any backend claims.
- Play Store readiness checklist is complete.
