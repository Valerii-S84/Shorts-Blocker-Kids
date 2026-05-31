# Shorts Blocker Kids - Emulator E2E QA

Status: Automated emulator QA environment is active for local fake-app
regression testing. It is the app-controlled QA layer before real-device
verification with the actual social apps.

## Scope

This environment validates Short Video Blocker behavior against local fake
target apps in a controlled emulator. It does not add product features, does
not upload to Play Console, and does not replace final real-device QA.

## Fake Target Apps

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

## E2E Test Entry Point

Instrumentation tests:

- `app/src/androidTest/java/com/shortsblockerkids/e2e/ShortVideoBlockerEmulatorE2eTest.kt`

Run on a connected emulator:

```bash
ANDROID_HOME=/path/to/Android/Sdk gradle :app:connectedDebugAndroidTest
```

The `:app:connectedDebugAndroidTest` task depends on `installFakeSocialApps`,
which builds and installs all fake target apps first.

Fixture-only install:

```bash
ANDROID_HOME=/path/to/Android/Sdk gradle installFakeSocialApps
```

## Emulator E2E Scenarios

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

## Unit Test Coverage

Automatically proven by JVM unit tests:

- detector confidence for YouTube Shorts, TikTok For You, Instagram Reels, and
  Facebook Reels fixtures;
- non-short screens, profile/search/settings/feed/groups/story screens,
  misleading short-video text, empty trees, locale-like labels, null packages,
  and unsupported aliases;
- detector match/no-match structural branches including action rail, vertical
  fullscreen, repeated feed, and platform-specific high-confidence formulas;
- blocking decision cooldown/debounce behavior;
- temporary allow and expired temporary allow decision behavior;
- PIN validation, hashing, verification, rate limiting, and lockout;
- settings defaults and persistence;
- global protection ON/OFF state, active/expired/missing temporary allow,
  wrong/correct PIN, and not-configured/locked PIN verification;
- billing copy/state fallbacks and free-test policy edge cases;
- disclosure and release-copy invariants.

Coverage analysis from the previous report showed branch coverage was held down
mostly by compound detector branches, storage temporary allow/PIN branches, and
small null/default fallback branches. The largest missed-branch sources were
`YouTubeShortsDetector`, `InstagramReelsDetector`, `FacebookReelsDetector`,
`TikTokShortVideoDetector`, `SettingsRepository`, and `AppSettings`.

Latest local result on 2026-05-24:

- Unit tests: 164 passed, 0 failed, 0 skipped.
- Jacoco unit coverage: 95.77% instruction, 97.06% line, 91.00% branch.
- Branch coverage gate: `BRANCH` covered ratio must be at least `0.90`.

## Latest Emulator Result

Latest local result on 2026-05-24:

- Emulator: `ShortsBlocker_Pixel_API35(AVD) - 15`.
- Emulator E2E tests: 12 passed, 0 failed, 0 skipped.
- Fake apps were installed automatically through `installFakeSocialApps`.
- The AccessibilityService was enabled through secure settings for the test run
  and disabled during teardown.

## Not Covered Automatically

- Per-platform enable/disable toggles are not covered because the product does
  not currently expose per-platform settings. Adding them would be a product
  feature and is outside this QA-only scope.
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

## Required Real-Device QA

Before claiming production readiness, repeat the supported scenarios on real
devices with the actual apps:

- YouTube Shorts/Home/Search/normal video on at least one Pixel and one major
  OEM device.
- TikTok main package For You/Profile/Search/Settings.
- Instagram Reels/Feed/Profile/Story.
- Facebook Reels/Feed/Profile/Groups/Page.
- PIN unlock and temporary allow against real app foreground transitions.
- Overlay behavior across Back, Home, app switcher, orientation changes, and
  quick repeated accessibility events.
- AccessibilityService enable/disable and dashboard status refresh.

Emulator E2E is a regression guard for the app-controlled side of the product.
It is not a substitute for final QA on a real phone with real social apps.

## Full Local Verification Command Set

```bash
ANDROID_HOME=/path/to/Android/Sdk gradle ktlintCheck
ANDROID_HOME=/path/to/Android/Sdk gradle :app:testDebugUnitTest
ANDROID_HOME=/path/to/Android/Sdk gradle :app:connectedDebugAndroidTest
ANDROID_HOME=/path/to/Android/Sdk gradle :app:lintDebug
ANDROID_HOME=/path/to/Android/Sdk gradle :app:lintRelease
ANDROID_HOME=/path/to/Android/Sdk gradle :app:assembleDebug
ANDROID_HOME=/path/to/Android/Sdk gradle :app:assembleRelease
ANDROID_HOME=/path/to/Android/Sdk gradle :app:bundleRelease
ANDROID_HOME=/path/to/Android/Sdk gradle :app:jacocoDebugUnitTestReport
ANDROID_HOME=/path/to/Android/Sdk gradle :app:jacocoDebugUnitTestCoverageVerification
```

The aggregate local gate is also available:

```bash
ANDROID_HOME=/path/to/Android/Sdk gradle localQualityGate
```

It depends on ktlint, unit tests, emulator E2E, debug/release lint,
debug/release builds, release bundle, and JaCoCo coverage verification.

Latest full local verification on 2026-05-24 passed:

- `ktlintCheck`
- `:app:testDebugUnitTest`
- `:app:connectedDebugAndroidTest`
- `:app:lintDebug`
- `:app:lintRelease`
- `:app:assembleDebug`
- `:app:assembleRelease`
- `:app:bundleRelease`
- `:app:jacocoDebugUnitTestReport`
- `:app:jacocoDebugUnitTestCoverageVerification`
