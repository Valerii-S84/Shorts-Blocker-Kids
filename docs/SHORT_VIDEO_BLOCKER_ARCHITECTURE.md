# Short Video Blocker Architecture

Status: Current repository architecture. Device, Play and live deployment acceptance are separate evidence gates.

## Ownership And Dependencies

| Layer | Responsibility |
|---|---|
| `domain` | Pure detection, PIN, free-test, entitlement and protection policies. |
| `application` | Use cases, plain models and storage/clock/Accessibility/Billing ports. |
| `infrastructure` | Preferences DataStore, PIN hashing, Android clocks and Play SDK adapters. |
| `platform` | Accessibility runtime, routing/scanning, overlay and Android navigation. |
| `presentation` | Compose screens, UI models and coordinators calling use cases. |
| `composition` | Wires concrete implementations to application and UI contracts. |

`app` is the production Gradle module; `test-fixtures:fake-social-apps` is test-only. Domain/application policy stays independent of Android, SDK and DataStore implementation types. Infrastructure must not own presentation policy. Existing compatibility components are not places for new business logic. Layer guards: `app/src/test/java/com/shortsblockerkids/architecture/LayerDependencyTest.kt`.

## Protection And Local Free Test

`ProtectionActivationPolicy` starts the free test only when Accessibility is enabled, protection is enabled, disclosure accepted, PIN configured and the test not already started. `RecordSuccessfulProtectionActivationUseCase` uses a clock/status port and the atomic storage activation operation; the original start is recorded once. App opening, PIN creation or disclosure acceptance alone does not start it.

`FreeTestPolicy.DEFAULT_DURATION_DAYS` is 20. Active means `start <= now < expiry`; exact expiry is inactive. This local free test is not a Play introductory offer or automatic purchase.

`ProtectionEligibilityPolicy.canProtect` requires protection enabled, accepted disclosure, `BLOCK_SHORTS`, at least one enabled platform, free-test or paid access, configured PIN and no active temporary allow. Accessibility permission is an additional runtime prerequisite; disabling it stops blocking.

## Parent PIN

- Input is 4–6 digits with matching confirmation. `0000`, `1111`, `1234`, `123456` are rejected.
- `Pbkdf2PinHasher`: PBKDF2-HMAC-SHA256, 120,000 iterations, 256-bit output, fresh 16-byte salt, Base64 metadata and constant-time byte comparison.
- Plaintext PIN is never persisted. Preserve atomic verification/attempt state through PinStateStore.
- Failure 5 locks for 30 seconds; failure 6 for 60 seconds; failure 7+ for 5 minutes. Preserve active-lockout rejection, success reset and corrupt-metadata behavior in regression tests.
- `pinHashVersion` is metadata; do not introduce a new algorithm migration/dispatch implicitly.

## Temporary Allow

Only 15, 30 and 60 minutes are supported, using the shared parent PIN across enabled platforms. Cancel grants nothing. Active allow makes blocking ineligible.

Current configuration stores wall-clock and elapsed-realtime deadlines plus boot metadata. `TemporaryAllowPolicy` prefers an elapsed deadline when present: an available elapsed clock and `deadline > elapsedNow` are required. Without it, wall-clock fallback is active only for `until > now`. Expired timing is cleared through the use-case/store boundary. Preserve restart/reboot/rollback behavior; do not revert to the historical wall-clock-only model.

## Detection And Shared Overlay

| Platform ID | Production package | Protected surface |
|---|---|
| `youtube_shorts` | `com.google.android.youtube` | Shorts; preserve normal videos and non-target screens. |
| `tiktok` | `com.zhiliaoapp.musically` | Detected short-video feed; no blanket whole-app blocking. |
| `instagram_reels` | `com.instagram.android` | Reels; preserve normal feed, stories, profiles and messages. |
| `facebook_reels` | `com.facebook.katana` | Reels; preserve ordinary non-target screens. |

`com.ss.android.ugc.trill` and `com.facebook.lite` are unsupported. Fixture aliases are debug-only. Block only a positive short-video result with HIGH confidence; LOW/MEDIUM do not block.

One branded overlay serves all platforms. It must dismiss when eligibility is lost, the target surface/package is left or the service stops. Exit returns to phone home. PIN entry/temporary allow must not duplicate or trap the overlay. [Detection Strategy](SHORT_VIDEO_BLOCKER_DETECTION_STRATEGY.md) owns per-platform signals, scanner bounds and rescan scheduling.

## DataStore And Billing

`DataStoreSettingsStore` uses Preferences DataStore and maps stored settings to application/domain snapshots. Keep protection/platform choices, disclosure, PIN hash/attempt metadata, free-test and temporary-allow timing, and local paid-access state. This preparation changes no keys/schema/data/resources.

`GooglePlayBillingGateway` queries purchases of `shorts_blocker_kids_monthly`. `SyncBillingEntitlementUseCase` maps purchased/pending/none to ACTIVE/PENDING/INACTIVE. `BillingCoordinator` persists before acknowledgement. DataStore stores the resulting active-access flag; transient UI Billing states are not a server lifecycle database.

The own backend and Stripe are removed. Current release has no server verification, RTDN, Developer API credentials, backend URL or 72-hour server-grace requirement. A failed SDK query is not proof of expiry. See [Billing Integration](SHORTS_BLOCKER_PLAY_BILLING_BACKEND_RUNBOOK.md) for cached state/recovery limits and [Billing Testing](SHORTS_BLOCKER_PLAY_BILLING_INTERNAL_TEST_RUNBOOK.md) for Play evidence.

## Privacy And Optional Tamper Protection

Protection processing is local. Do not store/transmit child activity, watch history, video titles, URLs, account names, comments/messages, raw trees, screenshots, audio, location, contacts or browsing history. Release diagnostics stay sanitized/debug tools disabled. Billing/network permissions from the existing SDK do not authorize analytics/cloud/child-data flows.

Optional Device Admin makes uninstall harder while active; it does not replace Accessibility or block Settings. No wipe, lock, password reset, hidden settings or other-app management. Consent/Android confirmation remain explicit, with `<uses-policies />` adding no destructive admin policies.

## Failure Modes And Evidence

Disabled Accessibility stops blocking. Third-party UI changes require fixture/device calibration, not broader scanning or weaker confidence. Unsupported packages are not supported claims. Billing unavailable/pending/storage failure need recoverable parent-facing handling. No absolute anti-bypass promise.

[Device QA](SHORTS_BLOCKER_REAL_DEVICE_QA_CHECKLIST.md) separates fake-app automation from real app/device evidence; TikTok/Reels detector/unit coverage is not production certification. [Migration objectives](SHORTS_BLOCKER_CLEAN_ARCHITECTURE_MIGRATION_PLAN.md) retain 16 approved slices without certifying full completion.

Consolidated contracts from the product plans, Roadmap and migration invariants. Historical backend conditions are superseded by the present Play/use-case/store path.
