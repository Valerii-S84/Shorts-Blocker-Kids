# Short Video Blocker Detection Strategy

Status: Active app-side strategy. YouTube Shorts, TikTok short-video feed,
Instagram Reels, and Facebook Reels detectors are implemented with fixture unit
tests and are in the protected-platform release scope. Real-device QA evidence
is still required before Play submission.

## Detection Principles

- Use Android AccessibilityService only after prominent disclosure and
  affirmative consent.
- Listen only to supported package names.
- Scan only when relevant Accessibility events arrive.
- Prefer deterministic, rule-based detection.
- Block only high-confidence short-video detections in production.
- Keep debug output sanitized.
- Do not store child viewing history or raw Accessibility tree dumps.
- Update disclosure, Privacy Policy, Data Safety, and Play declaration when
  supported apps or data access change.

## Package Names

Primary target package names:

| Platform | Package name | Blocking target |
|---|---|---|
| YouTube | `com.google.android.youtube` | YouTube Shorts |
| TikTok | `com.zhiliaoapp.musically` | TikTok short-video feed |
| TikTok regional variant | `com.ss.android.ugc.trill` | Not enabled; requires separate regional QA |
| Instagram | `com.instagram.android` | Instagram Reels |
| Facebook | `com.facebook.katana` | Facebook Reels |
| Facebook Lite | `com.facebook.lite` | Not enabled; requires separate Lite QA |

The protected release target focuses on the primary package names. Any regional
or Lite package must be separately tested before being enabled.

## Event Strategy

Relevant Accessibility events:

- `TYPE_WINDOW_STATE_CHANGED`
- `TYPE_WINDOW_CONTENT_CHANGED`
- `TYPE_VIEW_SCROLLED`
- `TYPE_WINDOWS_CHANGED`

Event handling:

1. Ignore packages outside the supported package set.
2. If temporary allow is active, do not block.
3. Debounce high-volume content change events.
4. Build a sanitized snapshot from the active window.
5. Route snapshot to the detector registered for the current package.
6. Block only high-confidence results.
7. Dismiss overlay when package leaves the blocked surface or service stops.

## YouTube Shorts Detection Strategy

Current status:

- YouTube Shorts detection exists in the app today.
- Existing YouTube detector and fixtures are the regression baseline.
- Preserve current YouTube positive/false-positive regression behavior when changing detection.

Target package:

- `com.google.android.youtube`

Likely signals:

- View IDs containing `shorts`, `reel`, or Shorts-specific containers.
- Shorts tab state.
- Fullscreen vertical video structure.
- Shorts player action stack.
- Known Shorts accessibility labels and content descriptions.
- Repeated vertical-feed structure after swipe.

Fallback behavior:

- If high-confidence Shorts signals are absent, do not block.
- Do not block ordinary YouTube videos by default.
- Use medium/low confidence only for debug reports and fixture calibration.

Risks:

- YouTube may rename view IDs.
- Shorts entry points can differ by region, account, experiment, or app version.
- Accessibility tree timing can vary by device.

## TikTok Detection Strategy

Current status:

- Implemented for primary package `com.zhiliaoapp.musically`.
- Regional package `com.ss.android.ugc.trill` is not enabled.
- Real-device QA is pending.

Target packages:

- `com.zhiliaoapp.musically`
- `com.ss.android.ugc.trill` only after regional QA.

Preferred strategy:

- Detect TikTok foreground package.
- Identify the primary short-video feed using UI tree structure, bottom
  navigation, video player controls, and stable content descriptions.
- Do not use app-level TikTok blocking in the current implementation.

Likely signals to investigate:

- Bottom navigation labels such as Home, Friends, Inbox, Profile.
- Fullscreen vertical video player layout.
- Repeated video action stack: like, comments, share, profile/avatar.
- View IDs or class names that remain stable across app versions.
- Text or content descriptions that distinguish feed from settings, inbox, or
  profile.

Fallback behavior:

- If high-confidence feed-specific signals are absent, do not block.
- App-level TikTok blocking is not enabled and would require a separate product
  decision plus disclosure updates.

Risks:

- TikTok UI changes frequently.
- TikTok may use dynamic labels or obfuscated view IDs.
- Feed-specific false positives may block inbox, profile, or settings.
- App-level fallback is simpler but broader and must be clearly described to
  parents and Play review.

## Instagram Reels Detection Strategy

Current status:

- Implemented for package `com.instagram.android`.
- Real-device QA is pending.

Target package:

- `com.instagram.android`

Preferred strategy:

- Detect Instagram foreground package.
- Identify Reels tab/viewer state.
- Block only high-confidence Reels surfaces.

Likely signals to investigate:

- Reels tab label or selected navigation state.
- Reels viewer content descriptions.
- Fullscreen vertical video player structure.
- Action stack: like, comment, share, audio, more.
- View IDs or labels related to reels or clips.
- URL/deep-link entry into Reels if visible through app state, without storing
  browsing history.

Fallback behavior:

- If Reels cannot be distinguished from other Instagram surfaces, do not block
  Instagram broadly unless a later product decision explicitly approves and
  discloses wider Instagram blocking.
- Keep Instagram messages, profile, and normal feed unblocked unless detector QA
  proves the active surface is Reels.

Risks:

- Reels and normal feed video UI can look similar.
- UI labels differ by language.
- Instagram experiments can change navigation and labels.
- False positives may block normal Instagram use, which is outside the product
  target unless explicitly approved later.

## Facebook Reels Detection Strategy

Current status:

- Implemented for package `com.facebook.katana`.
- Facebook Lite is not enabled.
- Real-device QA is pending.

Target package:

- `com.facebook.katana`

Optional later package:

- `com.facebook.lite`

Preferred strategy:

- Detect Facebook foreground package.
- Identify Reels viewer or Reels feed using labels, navigation state, and video
  player structure.
- Block only high-confidence Reels surfaces.

Likely signals to investigate:

- Reels labels in selected tab, viewer title, or content descriptions.
- Fullscreen vertical video layout.
- Action stack: reactions, comments, share.
- Reels-specific player controls.
- Stable view IDs related to reels if available.

Fallback behavior:

- If Facebook Reels cannot be reliably distinguished, do not block the whole
  Facebook app in the first full short-video release unless explicitly approved
  and disclosed.
- Facebook Lite support must remain disabled until separately tested.

Risks:

- Facebook app surfaces are broad and frequently personalized.
- Reels can appear embedded inside other feeds.
- Language and region may change labels.
- False positives may block groups, marketplace, profile, or normal feed.

## Confidence Model

Confidence contract (production blocks HIGH only):

- High: multiple platform-specific signals match and surface is clearly a
  target short-video feed/viewer.
- Medium: package and some short-video signals match, but surface may overlap
  with non-target video UI.
- Low: package is supported but signals are weak or generic.
- None: unsupported package or no target signals.

Production action:

- High: show blocker overlay.
- Medium: do not block by default; record sanitized debug signal in debug builds
  only.
- Low/None: do not block.

## Fixture And Calibration Requirements

Each platform requires fixtures for:

- Target blocked surface.
- Normal home/feed surface.
- Search surface.
- Profile surface.
- Settings surface.
- Messages/inbox surface where applicable.
- Post/video surface that is not the target short-video feed.
- Locale variations if labels are used.

Manual calibration must capture:

- App version.
- Android version.
- Device/OEM.
- Locale.
- Accessibility event sequence.
- Sanitized matched signals.
- False positives and false negatives.

## Third-Party UI Change Risk

All supported apps can change UI without notice.

Mitigations:

- Keep detectors conservative.
- Keep per-platform fixtures current.
- Run real-device QA before every release.
- Maintain kill-switch only if a future approved architecture adds remote config;
  otherwise use app update hotfixes.
- Do not make absolute claims that blocking can never be bypassed.
- Tell parents that protection depends on Android AccessibilityService and
  supported app UI behavior.

## Pending Maintenance Loop

The Roadmap's maintenance loop is an unclosed workflow requirement. Establish a
regular weekly smoke/review cycle covering the latest supported target-app
versions, target detection and non-target regressions, and subscription
entitlement using the [Billing QA runbook](SHORTS_BLOCKER_PLAY_BILLING_INTERNAL_TEST_RUNBOOK.md).
Review support reports and retain sanitized reproduction steps, affected
app/device/build versions and the observed false-positive or false-negative
behavior. This cadence is pending, not evidence of an executed maintenance cycle.

For each detection-rule change, record its reason and check the affected target,
non-target, ambiguous, localized and transition fixtures, followed by relevant
real-device regression checks. Triage reported regressions promptly, reproduce
them and prepare an approved app-update hotfix when needed. Before publishing,
complete the [device QA](SHORTS_BLOCKER_REAL_DEVICE_QA_CHECKLIST.md) and
[AAB release gates](SHORTS_BLOCKER_AAB_RELEASE_READINESS.md); retain the conservative
confidence policy and zero-blocking acceptance on core non-target screens.

## Scanner And Deferred Rescan Contract

AccessibilityTreeScanner bounds traversal at depth 12 and 180 nodes. AccessibilityEventPolicy has a 500 ms scan debounce. Production XML listens for window state/content, scroll and windows-changed events with a 120 ms notification timeout and four-platform package filtering; fixture aliases stay debug-only.

A final relevant event inside debounce requests a scan for the remaining interval. Further events keep the original deferred deadline rather than postponing it. Unavailable root requests a follow-up. The service has one cancellable job, rereads the current active window, retains no old AccessibilityEvent/node and cancels pending work on interrupt/destroy. Confidence and protection eligibility do not change.

Retained regression cases: lastContentChangeInsideDebounceRequestsScanWithoutAnotherEvent, continuousContentChangesDoNotPostponeDeferredScanDeadline, unavailableRootRequestsFollowUpAfterWindowTransition, plus irrelevant-event rejection. The 2026-09-14 historical report records seven policy tests and 450 JVM tests; these are not new results. Coroutine/window-manager integration was compiled/linted then, not device-tested. The original installed-app incident was not independently attributed to this defect.

Keep sanitized target/non-target/ambiguous/localized/transition fixtures. Capture device/OEM, Android/app version, locale, enabled platforms and false-positive/negative evidence without private content. See [Device QA](SHORTS_BLOCKER_REAL_DEVICE_QA_CHECKLIST.md). Consolidated from the retained event-rescan report.
