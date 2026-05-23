# Short Video Blocker Architecture

Status: Active app-side architecture. The local detector registry now includes
YouTube Shorts, TikTok primary package, Instagram Reels, and Facebook Reels.
Backend/cloud/analytics integrations remain out of scope.

## Architecture Goals

- Preserve current working YouTube Shorts blocker behavior.
- Add support for multiple short-video platforms through detector abstraction.
- Reuse one overlay, one parent PIN flow, one temporary allow model, and one
  local settings model.
- Keep package scanning narrow.
- Keep release logging sanitized.
- Avoid backend, billing, analytics, or network dependencies until their
  implementation phase is explicitly approved.

## Current YouTube Logic Preservation Plan

The existing YouTube blocker logic is the baseline and must not be rewritten as
part of multi-app planning.

Preservation rules:

- Keep current YouTube detector behavior intact until replacement tests prove
  parity or improvement.
- Add abstraction around the current detector instead of moving core matching
  rules without tests.
- Keep current YouTube package handling for `com.google.android.youtube`.
- Keep release builds free of raw Accessibility tree logs.
- Keep current overlay behavior as the shared overlay foundation.
- Add regression tests before changing detector thresholds, signals, debounce,
  overlay cooldown, or temporary allow behavior.

## Target Component Model

Target modules:

```text
accessibility/
  ShortsBlockerAccessibilityService
  AccessibilityEventRouter
  SupportedAppPolicy
  ShortVideoDetectorRegistry
  ShortVideoDetector
  PlatformDetectionResult
  AccessibilityTreeScanner
  BlockOverlayController
  BlockingDecisionController

detectors/
  YouTubeShortsDetector
  TikTokShortVideoDetector
  InstagramReelsDetector
  FacebookReelsDetector

feature/blocking/
  shared blocker overlay
  temporary allow controller

feature/pin/
  parent PIN setup and entry

core/storage/
  local protection settings
  supported-app enablement
  entitlement cache
```

The current package structure may stay as-is. The important design requirement
is the boundary between platform-specific detection and shared blocking
behavior.

## Detector Abstraction

Target detector interface:

```text
ShortVideoDetector:
  packageNames: Set<String>
  platform: ShortVideoPlatform
  detect(snapshot, eventContext): PlatformDetectionResult
```

Result fields:

- platform.
- package name.
- confidence: none, low, medium, high.
- matched signals.
- blocker reason.
- safe debug summary.

Only high-confidence results should trigger the blocking overlay by default.
Medium-confidence results may be used for debug QA and detector calibration but
must not block in production unless explicitly approved.

## Platform-Specific Detectors

YouTube Shorts detector:

- Existing detector becomes the first implementation of `ShortVideoDetector`.
- Current fixture-based tests remain regression baseline.

TikTok detector:

- Detects primary TikTok app package `com.zhiliaoapp.musically`.
- Uses feed-specific detection from UI structure, stable labels, and action rail
  signals.
- Does not enable app-level TikTok blocking.
- Does not enable regional package `com.ss.android.ugc.trill` without separate
  QA and code change.

Instagram Reels detector:

- Detects Instagram package `com.instagram.android`.
- Matches Reels tab, Reels viewer, reel containers, player controls, and
  package-specific view IDs where stable.

Facebook Reels detector:

- Detects Facebook package `com.facebook.katana`.
- Matches Reels surfaces, viewer labels, player controls, and package-specific
  view IDs where stable.
- Does not enable Facebook Lite without separate QA and code change.

## Shared Overlay

One overlay must serve all platforms.

Overlay inputs:

- platform name.
- detected blocked surface.
- parent PIN action.
- exit action.
- current temporary allow state.

Overlay requirements:

- Full-screen blocking overlay.
- Clear product branding.
- `Exit to phone home`.
- `Enter PIN`.
- No platform-specific copy unless it clarifies which surface was blocked.
- No imitation of Android system warnings.
- Dismiss when protection is disabled, temporary allow starts, app leaves the
  blocked surface, or AccessibilityService stops.

## Shared PIN And Temporary Allow

PIN:

- One parent PIN for all supported apps.
- PIN hash and salt stored locally.
- PIN required for disabling protection.
- PIN required for temporary allow from overlay.

Temporary allow:

- Durations: 5, 10, 15 minutes.
- Applies globally to short-video blocking unless a later approved product
  decision adds per-platform allow.
- Stored locally.
- Automatically expires.
- Must not start billing entitlement or analytics events.

## Storage Model

Planned local settings:

```text
protectionEnabled: Boolean
accessibilityDisclosureAccepted: Boolean
supportedPlatforms:
  youtubeShortsEnabled: Boolean
  tiktokEnabled: Boolean
  instagramReelsEnabled: Boolean
  facebookReelsEnabled: Boolean
temporaryAllowUntil: Timestamp?
pinHash: String?
pinSalt: String?
pinHashVersion: Int
entitlement:
  source: localFreeTest | playBilling | backendVerified
  state: active | canceledActive | pending | expired | unpaid | unknown
  expiresAt: Timestamp?
  lastVerifiedAt: Timestamp?
```

Storage rules:

- Store only settings, entitlement state, PIN hash metadata, and temporary allow
  timestamps.
- Do not store watch history.
- Do not store URLs, video titles, account names, comments, captions, messages,
  raw Accessibility trees, screenshots, audio, location, contacts, or browsing
  history.

## Failure Modes

AccessibilityService disabled:

- Dashboard shows protection inactive.
- Blocking stops.
- Parent is instructed to enable AccessibilityService again.

Detector false negative:

- Short-video surface may not be blocked.
- QA fixtures and real-device calibration must be updated.

Detector false positive:

- Non-short-video surface may be blocked.
- Production threshold must stay conservative.

Third-party UI change:

- Detector may lose signals.
- Use remote config only if explicitly approved later; default product has no
  remote config.

Overlay lifecycle failure:

- Overlay must dismiss on service interrupt/destroy.
- Overlay must avoid flicker loops.
- Overlay must not trap parent access to PIN or settings.

Billing unknown:

- Parent access remains available.
- Protection entitlement follows the billing/backend plan.

## No-Regression Rules

- Do not weaken current YouTube high-confidence detection without tests.
- Do not broaden package scanning without policy and privacy updates.
- Do not add `INTERNET` permission until backend/billing phase explicitly
  requires it.
- Do not add analytics SDKs during detector implementation.
- Do not add ads.
- Do not mark `isAccessibilityTool=true` unless the product is truly
  repositioned as an accessibility tool for people with disabilities.
- Do not claim platform support in UI, store listing, Privacy Policy, or Play
  declaration until that platform is implemented and verified.
- Every new supported app requires:
  - detector unit tests;
  - real-device QA;
  - disclosure update;
  - privacy review;
  - Play declaration update if AccessibilityService use changes.
