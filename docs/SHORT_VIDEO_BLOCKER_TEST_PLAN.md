# Short Video Blocker Test Plan

Status: Planning. This document defines required tests for the full Short Video
Blocker product. It does not run tests or implement new app support.

## Test Goals

- Preserve current YouTube Shorts blocker behavior.
- Prove each new platform detector before enabling it.
- Prove shared overlay, parent PIN, and temporary allow across supported apps.
- Prove Accessibility disclosure and consent cannot be bypassed.
- Prove billing and backend entitlement states before production rollout.
- Prove no child viewing history or unsupported data is collected.

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

Shared blocking tests:

- High-confidence result shows overlay.
- Medium/low/no confidence does not block in production mode.
- Unsupported package never blocks.
- Temporary allow suppresses blocking.
- Protection disabled suppresses blocking.
- Overlay cooldown prevents flicker loops.
- Service restart starts from clean blocker state.

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
- Supported platform toggles persist locally.
- Entitlement cache reads/writes only billing state.

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

- No `INTERNET` permission before backend/billing phase unless explicitly
  approved.
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

## Billing Test Matrix

Play Console setup:

- License tester configured.
- Subscription product configured.
- Base plan configured.
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

Endpoint tests:

- Verify valid purchase token.
- Reject invalid token.
- Reject malformed request.
- Return active entitlement.
- Return expired entitlement.
- Return unknown entitlement on Google API failure.
- Do not log full purchase token.
- Do not accept child activity payload fields.

RTDN tests:

- Validate message source.
- Process renewal.
- Process cancellation.
- Process expiration.
- Process revoke/refund.
- Deduplicate repeated notification.
- Recover by querying Google Play API.

Privacy/security tests:

- Backend database contains only billing technical data.
- Backend rejects unsupported fields such as video title, URL, account name,
  location, contact, message, or Accessibility tree dump.
- Logs redact purchase tokens and credentials.

## Release Gates

Code gates:

- `gradle clean :app:assembleRelease`
- `:app:lintRelease`
- `:app:ktlintCheck`
- `:app:assembleDebug`
- `:app:testDebugUnitTest`
- `:app:lintDebug`

Manual gates:

- Full Accessibility disclosure video recorded.
- Real-device QA completed for every supported app.
- Billing test matrix completed.
- Backend verification test matrix completed if backend is included.
- Privacy permissions audit completed for release merged manifest.
- No unsupported permissions.
- No raw Accessibility logs in release.

Policy gates:

- Privacy Policy URL hosted and accurate.
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
- Backend, if included, passes privacy and entitlement tests.
- Play Store readiness checklist is complete.
