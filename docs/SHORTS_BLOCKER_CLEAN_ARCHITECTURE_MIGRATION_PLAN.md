# Shorts Blocker Kids Clean Architecture Migration Plan

Status: APPROVED / READY FOR EXECUTION

Priority: P0 / NEXT CODE-CHANGE TRACK

Approved: 2026-07-28

Baseline:

```text
branch: main
commit: f87f66e1411d31455fbfbe05b731cf884b3baa75
local main == local origin/main
```

This document is the canonical execution plan for the approved Clean
Architecture migration.

Execution rules:

1. Start with Slice 1 only.
2. Use one implementation slice per PR.
3. Do not start the next slice until the current slice passes its declared
   checks and is reviewed.
4. Preserve behavior, persisted data, resources, manifest semantics, and
   release safeguards unless a separate product decision explicitly changes
   them.
5. Do not add a DI framework, a new Gradle module, or a new dependency without
   a separately documented need.
6. Billing remains in the late migration slices.
7. Emergency production or security hotfixes may interrupt this sequence, but
   unrelated feature work may not be bundled into it.

## 1. Actual State

The project contains several well-isolated policies and controllers, but the
current `core/*` and `feature/*` package names do not enforce inward dependency
direction.

### App entrypoint

`app/src/main/java/com/shortsblockerkids/MainActivity.kt` is simultaneously:

- the Android entrypoint;
- the manual composition root;
- a lifecycle owner;
- the launcher for system settings;
- a settings observer;
- the Billing host;
- the owner of Compose navigation and parent-control state.

The private `ShortsBlockerKidsApp` Composable coordinates onboarding, PIN,
Accessibility disclosure, protection activation, temporary allow, and Billing
callbacks.

### Persistence and settings

`core/storage/SettingsRepository.kt` is a DataStore adapter, but also owns:

- protection and disclosure writes;
- free-test activation;
- Billing entitlement persistence;
- Billing installation UUID creation;
- temporary-allow timestamps;
- PIN hashing, verification, attempts, and lockout transitions;
- direct wall-clock reads.

`core/storage/AppSettings.kt` combines:

- the persisted settings record;
- protection configuration;
- Billing and free-test metadata;
- installation ID;
- PIN hash/salt/version and attempt state;
- eligibility policies such as `canProtect`.

The project context document still says SharedPreferences, while the
implementation uses Preferences DataStore. This is documentation drift, not a
reason to change the storage implementation.

### Accessibility and detection

`accessibility/ShortsBlockerAccessibilityService.kt` is an Android entrypoint
and a second manual composition root.

`accessibility/AccessibilityEventRouter.kt` combines:

- Android event routing;
- package and platform selection;
- protection eligibility;
- scan debounce;
- Accessibility tree scanning;
- detection;
- blocking decisions;
- overlay control;
- debug snapshot and runtime-state updates.

The following classes are already predominantly pure and should not be
rewritten:

- `BlockingDecisionController`;
- `ShortVideoDetectionEngine`;
- `YouTubeShortsDetector`;
- `TikTokShortVideoDetector`;
- `InstagramReelsDetector`;
- `FacebookReelsDetector`;
- detector heuristics and signal models.

Their remaining outer-layer coupling comes primarily from:

- `BuildConfig`-controlled debug fixture aliases;
- production registry construction inside the detection engine;
- pure snapshot/node models living in the Android scanner file.

### Billing and entitlement

`core/billing/PlayBillingRepository.kt` currently combines:

- BillingClient lifecycle;
- product and offer queries;
- purchase launch;
- Manage Subscription navigation;
- purchase processing and acknowledgement;
- backend verification and refresh;
- fail-closed entitlement decisions;
- client-only internal-test behavior;
- UI state and message mapping.

The pure Billing entitlement state, snapshot, and most verification rules are
suitable for the domain layer. BillingClient, HTTP, serialization, `Activity`,
`Intent`, and `Context` remain outer adapter concerns.

### Presentation

Most callback-only Compose screens are already suitable presentation
renderers. The principal remaining violations are:

- `DashboardScreen` computes entitlement/protection state and reads system
  time;
- PIN screens perform application/domain orchestration;
- `ProtectedAppsScreen` consumes storage and platform models;
- the private app coordinator lives in `MainActivity`.

### Build and dependencies

The existing modules remain:

```text
:app
:billing-backend
:test-fixtures:fake-social-apps
```

The current Compose, DataStore, coroutines/Flow, Billing KTX, serialization,
JUnit, and Android test dependencies are sufficient. No new dependency or
module is approved by this plan.

## 2. Responsibility Boundary Problems

1. `MainActivity` mixes app composition, presentation, application
   orchestration, domain decisions, storage, and Billing implementation.
2. `SettingsRepository` mixes persistence with PIN, free-test, entitlement,
   UUID, and clock rules.
3. `AppSettings` exposes PIN and installation metadata to consumers that only
   need protection state.
4. Classes under `core` import Android, DataStore, BillingClient, HTTP,
   resources, `BuildConfig`, and system time.
5. Presentation reads raw settings, platform state, and wall-clock time.
6. The Accessibility router depends on raw `AppSettings`, concrete overlay
   control, and global diagnostics state.
7. Pure detectors depend on debug build configuration through fixture package
   aliases.
8. Temporary-allow validation is enforced only by caller discipline.
9. The free-test activation gate is private to `MainActivity`, while the
   persistence method cannot prove that activation prerequisites succeeded.
10. PIN verification is correctly atomic today, but that invariant is not
    expressed as a storage port contract.
11. Billing fail-closed orchestration lacks a focused pure behavior test.
12. Hardcoded source paths in invariant tests and JaCoCo exclusions increase
    the risk of package moves.

## 3. Target Architecture

The migration uses logical packages inside `:app`:

```text
com.shortsblockerkids
├─ app
│  ├─ manual composition
│  ├─ lifecycle and BuildConfig
│  └─ Android system launchers
├─ domain
│  ├─ protection
│  ├─ entitlement
│  ├─ pin
│  ├─ blocking
│  └─ detection
├─ application
│  ├─ model
│  ├─ port
│  ├─ protection
│  ├─ pin
│  └─ billing
├─ infrastructure
│  ├─ storage
│  ├─ security
│  ├─ time
│  └─ billing
├─ platform
│  ├─ accessibility
│  │  ├─ routing
│  │  ├─ scanning
│  │  ├─ overlay
│  │  └─ diagnostics
│  └─ tamper
└─ presentation
   ├─ app
   ├─ dashboard
   ├─ onboarding
   ├─ pin
   ├─ billing
   └─ privacy
```

Composition roots:

- `MainActivity` owns the UI/app graph.
- `ShortsBlockerAccessibilityService` owns the Accessibility service graph.
- `TamperProtectionReceiver` remains a thin framework entrypoint.

Manifest-bound entrypoint FQCNs should initially remain stable as compatibility
shells. Their implementation moves behind them into `platform/*`, avoiding a
manifest/XML change in the same slice as architectural extraction.

## 4. Dependency Rules

### Domain

May import only Kotlin/std and domain types.

Must not import:

- Android or AndroidX;
- `Context`;
- DataStore;
- Compose or `R`;
- `BuildConfig`;
- BillingClient;
- HTTP or serialization;
- coroutines `Flow`;
- a concrete repository;
- direct system time.

Time-dependent domain rules receive `nowMillis` explicitly.

### Application

May import:

- domain types;
- application models and ports;
- coroutines/Flow where needed.

Must not import:

- Android framework types;
- Compose;
- DataStore;
- BillingClient;
- `AccessibilityEvent`;
- HTTP implementations.

### Infrastructure

Infrastructure adapters implement application ports:

- DataStore and `Context` belong to `infrastructure/storage`;
- PBKDF2 and secure random belong to `infrastructure/security`;
- BillingClient, HTTP, serialization, Billing configuration, and Android
  purchase launch belong to `infrastructure/billing`;
- the system clock belongs to `infrastructure/time`.

Infrastructure must not import presentation or platform orchestration.

### Platform

`platform/accessibility` is the only implementation layer that receives:

- `AccessibilityService`;
- `AccessibilityEvent`;
- `AccessibilityNodeInfo`;
- Accessibility windows;
- overlay/window APIs.

`platform/tamper` owns Device Admin and `DevicePolicyManager`.

Platform imports application/domain contracts, not DataStore details or
presentation implementations.

### Presentation

Presentation may use Compose, resources, and presentation/application UI
models.

Presentation must not receive:

- a concrete settings repository;
- DataStore types;
- BillingClient types;
- Accessibility events or nodes;
- PIN hash/salt;
- Billing installation ID;
- direct system time.

### App

The app layer performs manual wiring and lifecycle integration. It may know
outer implementations, but must not make business decisions that belong to
domain/application policies.

### Approved application ports

- `TimeProvider`
- `SettingsStatePort`
- `ProtectionActivationStore`
- `TemporaryAllowStore`
- transitional `PinAccessPort`
- final `PinStateStore`
- `PinHashingPort`
- `BillingEntitlementStore`
- `BillingInstallationIdProvider`
- `BillingVerificationPort`
- framework-free `PlayBillingGateway`
- `AccessibilityDiagnosticsPort`, if the debug UI remains connected to
  service runtime state

Navigation from an Accessibility overlay to an Activity is a local platform
contract, not an application port.

## 5. Current-to-Target Mapping

| Current | Target | Action |
|---|---|---|
| `MainActivity` | app composition root | Keep and thin |
| `ShortsBlockerKidsApp`, `AppScreen` | `presentation.app` | Extract and move |
| `SettingsRepository` | `infrastructure.storage.DataStoreSettingsStore` | Wrap first; split/rename late |
| `AppSettings` | `StoredAppSettings` + `AppSettingsSnapshot` + domain policies | Split |
| `FreeTestPolicy` | `domain.entitlement` | Keep and move |
| `LocalEntitlementResolver` | `application.protection` | Wrap neutral input and move |
| `EntitlementState` | `application.model` | Move; split only if later justified |
| `PinPolicy`, `PinRateLimiter` | `domain.pin` | Keep and move |
| `PinHasher` | `infrastructure.security.Pbkdf2PinHasher` | Wrap and move/rename |
| `PinVerificationResult` | `application.pin` | Keep and move |
| `TemporaryAllowFlowController` | application use cases + presentation navigation wrapper | Split |
| `BillingEntitlementState/Snapshot` | `domain.entitlement` | Keep and move |
| `BillingVerificationPolicy` | domain policy + presentation message mapper | Split |
| `BillingAvailability` | domain grace policy + `PlayBillingConfig` | Split |
| `BillingBackendClient` | `application.billing.BillingVerificationPort` | Move/rename |
| `HttpBillingBackendClient` | `infrastructure.billing.HttpBillingVerificationAdapter` | Move/rename |
| `PlayBillingRepository` | `GooglePlayBillingGateway` + `BillingCoordinator` | Facade first, then split |
| `BillingUiState`, `BillingPresentation` | `presentation.billing` | Keep and move |
| `BlockingDecisionController` | `domain.blocking` | Keep and move; explicit time |
| `ShortVideoDetectionEngine` | `domain.detection` | Keep and move; factory outside |
| Four detectors and heuristics | `domain.detection` | Keep algorithms and move |
| Snapshot/node models in scanner file | `domain.detection` | Split from Android scanner |
| `AccessibilityTreeScanner` | `platform.accessibility.scanning` | Keep and move |
| Router and event policy | `platform.accessibility.routing` | Keep and wrap |
| `SupportedPlatform` | domain IDs + platform registry + presentation resource catalog | Split |
| Overlay, phone-home, status, debug/runtime | `platform.accessibility.*` | Keep and move |
| Accessibility service | stable shell + `AccessibilityServiceRuntime` | Split |
| Tamper status | `platform.tamper` | Move |
| Tamper receiver | stable thin framework shell | Keep |
| Compose screens | `presentation.*` | Keep behavior; move in a move-only PR |

## 6. Non-Negotiable Behavioral Invariants

### Free test and protection eligibility

- Default free test duration is exactly 20 days.
- It is active only for `start <= now < start + duration`.
- Exact expiry is already inactive.
- App open, PIN creation, disclosure acceptance, or a bare settings write does
  not start the timer.
- The activation gate remains exactly:
  - Accessibility enabled;
  - protection enabled;
  - Accessibility disclosure accepted;
  - parent PIN configured;
  - free test not previously started.
- Activation timestamp is written only once.
- `canProtect` still requires:
  - protection enabled;
  - disclosure accepted;
  - mode `BLOCK_SHORTS`;
  - at least one enabled platform;
  - active free test or valid paid entitlement;
  - configured PIN;
  - no active temporary allow.
- Accessibility permission remains a separate outer prerequisite and is not
  added to `canProtect`.

### PIN

- PIN length remains 4–6 digits.
- Weak values remain `0000`, `1111`, `1234`, and `123456`.
- Hashing remains PBKDF2-HMAC-SHA256 with:
  - 120,000 iterations;
  - 256-bit output;
  - 16-byte random salt;
  - Base64 storage;
  - constant-time comparison.
- Plaintext PIN is never persisted.
- Attempt 5 locks for 30 seconds.
- Attempt 6 locks for 60 seconds.
- Attempt 7 and later lock for 5 minutes.
- Active lockout rejects even a correct PIN without increasing attempts.
- Success after expiry clears attempts and lockout.
- Corrupt hash metadata returns `NotConfigured`.
- `pinHashVersion` storage and its current non-dispatch behavior remain
  unchanged.
- Read, verify, and update of attempt state remain one atomic DataStore
  mutation.

### Temporary allow

- Only 5, 10, and 15 minutes are accepted.
- Active means `until > now`.
- Expired means `until <= now`.
- Expired timestamps are physically cleared so a clock rollback cannot
  reactivate access.
- Selection and cancel keep the current return-to-foreground behavior.
- Cancel performs no persistence write.
- Active temporary allow keeps `canProtect` false.

### Detection and Accessibility

- Stable IDs remain:
  - `youtube_shorts`;
  - `tiktok`;
  - `instagram_reels`;
  - `facebook_reels`.
- Production packages remain:
  - `com.google.android.youtube`;
  - `com.zhiliaoapp.musically`;
  - `com.instagram.android`;
  - `com.facebook.katana`.
- `com.ss.android.ugc.trill` and `com.facebook.lite` remain unsupported.
- Fixture aliases remain debug-only.
- Blocking occurs only for `isShorts && confidence == HIGH`.
- Scanner limits remain depth `12` and nodes `180`.
- Scan debounce remains `500 ms`.
- Blocking detection debounce remains `500 ms`.
- Block cooldown remains `1,200 ms`.
- PIN-entry grace remains `5,000 ms`.
- Post-grace recheck remains `100 ms`.
- HOME dismiss delay remains `700 ms`.
- Accessibility XML notification timeout remains `120 ms`.
- Existing detector weights, thresholds, HIGH branch expressions, exclusions,
  and fixture expectations are not changed by package moves.
- Release Accessibility diagnostics remain sanitized and disabled by
  `ACCESSIBILITY_DEBUG_TOOLS_ENABLED=false`.
- No raw Accessibility tree or user text may be logged in release.

### Billing

- Paid access remains limited to `ACTIVE`, `CANCELED_ACTIVE`, and `IN_GRACE`.
- `PENDING`, `ON_HOLD`, `EXPIRED`, `REVOKED`, and `UNKNOWN` do not grant
  protection.
- Verification timestamps in the future are rejected.
- Active-until and 72-hour offline windows remain unchanged.
- Persisted `billingSubscriptionActive` is not treated as authoritative.
- A configured backend always has priority over client-only mode.
- Missing installation ID, verify failure, or refresh failure stores
  fail-closed `UNKNOWN`.
- An observed backend failure immediately overwrites a previous active local
  snapshot; this plan does not introduce stale-cache allow.
- Client-only entitlement requires both:
  - explicit client-only request;
  - internal/debug build.
- Release client-only flags remain false.
- Product ID, offer selection, backend endpoints, request/response contracts,
  and acknowledgement ownership remain unchanged.

### Resources, manifest, and tamper protection

- Accessibility disclosure and consent behavior remain unchanged.
- EN/DE/UK and API-specific resource variants remain unchanged.
- Accessibility XML event/package lists remain unchanged.
- Manifest component and permission semantics remain unchanged.
- Device Admin remains optional and does not replace Accessibility.
- Device Admin gains no wipe, lock, reset, app-management, hidden-control, or
  stronger anti-uninstall policy.
- `tamper_protection_device_admin.xml` remains `<uses-policies />`.

## 7. Implementation Slices

Each numbered slice is one PR.

### Shared verification profiles

```powershell
# Q — full debug quality gate
.\gradlew.bat :app:ktlintCheck :app:testDebugUnitTest :app:jacocoDebugUnitTestCoverageVerification :app:lintDebug :app:assembleDebug

# E — device/E2E, with configured emulator and fixtures
.\gradlew.bat :app:connectedDebugAndroidTest

# R — release gate
$env:SBK_BILLING_BACKEND_BASE_URL='https://billing.shortsblockerkids.de'
.\gradlew.bat :app:lintRelease :app:assembleRelease :app:bundleRelease

# Backend contract
.\gradlew.bat :billing-backend:test

git diff --check
```

### Slice 1 — Extract the activation predicate

Goal:

- place the free-test activation decision in a pure, directly tested domain
  policy;
- make no other architectural change.

Create:

- `app/src/main/java/com/shortsblockerkids/domain/protection/ProtectionActivationPolicy.kt`
- `app/src/test/java/com/shortsblockerkids/domain/protection/ProtectionActivationPolicyTest.kt`

Change:

- `app/src/main/java/com/shortsblockerkids/MainActivity.kt`

Move:

- only the current private activation predicate.

Do not change:

- activation call sites;
- DataStore;
- repository signatures;
- UI;
- Billing;
- Accessibility implementation;
- packages of existing classes;
- resources;
- manifest.

Required tests:

- each missing prerequisite independently returns false;
- an already-started free test returns false;
- all prerequisites with no start time return true;
- existing free-test and settings tests remain green.

Done:

- `MainActivity` has no private activation predicate;
- the truth table is behaviorally identical;
- targeted test, Q, and `git diff --check` pass;
- the PR stops without starting Slice 2.

Rollback:

- revert the two new files and the single caller edit.

### Slice 2 — Application-owned activation and clock

Create:

- `application/port/TimeProvider.kt`
- `application/port/ProtectionActivationStore.kt`
- `application/protection/RecordSuccessfulProtectionActivationUseCase.kt`
- `infrastructure/time/SystemTimeProvider.kt`
- `RecordSuccessfulProtectionActivationUseCaseTest.kt`

Change:

- the three activation call sites in `MainActivity`;
- the activation persistence signature in `SettingsRepository`;
- activation-related repository tests.

Invariants:

- exact activation gate;
- one exact timestamp;
- no reset after restart or repeated activation;
- default duration 20 only when absent;
- DataStore keys and record format unchanged.

Checks:

- use-case tests;
- activation portions of `SettingsRepositoryTest`;
- `AppSettingsTest`;
- Q.

Rollback:

- restore direct repository delegation; no data migration is required.

### Slice 3 — PIN application façade

Create:

- `application/port/PinAccessPort.kt`
- `application/pin/CreatePinUseCase.kt`
- `application/pin/VerifyPinUseCase.kt`
- `infrastructure/storage/SettingsPinAccessAdapter.kt`
- use-case tests.

Change:

- `MainActivity`;
- `PinSetupScreen`;
- `PinEntryScreen`.

Do not change:

- PBKDF2 constants;
- Preferences keys;
- repository atomic verification block;
- lockout thresholds.

Checks:

- all PIN policy/rate-limiter/hasher tests;
- PIN scenarios in `SettingsRepositoryTest`;
- new use-case tests;
- Q.

Rollback:

- return screen callbacks to direct repository delegation.

### Slice 4 — PIN internals behind atomic ports

Create:

- `application/model/PinCredential.kt`
- `application/model/PinAttemptState.kt`
- `application/port/PinHashingPort.kt`
- `application/port/PinStateStore.kt`
- `infrastructure/security/Pbkdf2PinHasher.kt`
- `infrastructure/storage/DataStorePinStateStore.kt`

Move:

- `PinPolicy` and `PinRateLimiter` to `domain.pin`;
- `PinVerificationResult` to `application.pin`.

Change:

- `SettingsPinAccessAdapter`;
- transitional `SettingsRepository`;
- related tests.

Invariants:

- one atomic DataStore mutation;
- identical PBKDF2 and persisted representation;
- no plaintext persistence;
- identical lockout and corrupt-state behavior.

Checks:

- all PIN suites;
- storage recovery scenarios;
- Q.

Rollback:

- revert the PR as a unit; do not use dual-write or schema migration.

### Slice 5 — Temporary-allow boundary

Create:

- `domain/protection/TemporaryAllowDuration.kt`
- `application/port/TemporaryAllowStore.kt`
- `application/protection/SetTemporaryAllowUseCase.kt`
- `application/protection/ClearExpiredTemporaryAllowUseCase.kt`
- use-case tests.

Change:

- `TemporaryAllowFlowController`;
- `TemporaryAllowScreen`;
- `MainActivity`;
- `ShortsBlockerAccessibilityService`;
- the storage adapter and tests.

Invariants:

- only 5/10/15;
- injected clock;
- cleanup at `until <= now`;
- physical removal of expired value;
- cancel writes nothing and returns to foreground;
- PIN/navigation flow unchanged.

Checks:

- controller, repository, and `AppSettings` tests;
- relevant blocking lifecycle tests;
- Q and E.

Rollback:

- restore old caller paths; persisted timestamps and key remain compatible.

### Slice 6 — Extract the app coordinator

Create:

- `presentation/app/AppScreen.kt`
- `presentation/app/ShortsBlockerKidsCoordinator.kt`
- `presentation/app/ShortsBlockerKidsApp.kt`
- `ShortsBlockerKidsCoordinatorTest.kt`

Change:

- `MainActivity`;
- `AccessibilityPermissionFlow`.

Do not move:

- individual Compose screen files;
- resources/locales;
- Billing and Accessibility adapters;
- manifest components.

Checks:

- first-launch, disclosure, PIN, disable-protection, and temporary-allow route
  tests;
- Q and E.

Rollback:

- return coordinator/Composable code to `MainActivity`.

### Slice 7 — Explicit time in the blocking controller

Change:

- remove the default `System.currentTimeMillis()` from
  `BlockingDecisionController`;
- make every caller pass its captured timestamp explicitly.

Do not move packages or change constants.

Checks:

- `BlockingDecisionControllerTest`;
- `AccessibilityServiceLifecycleTest`;
- Q.

Rollback:

- atomic revert of the controller and callers.

### Slice 8 — Pure detection cohort

Move to `domain/detection`:

- tree snapshot and node signal models;
- confidence, result, and detector signals;
- detector interface;
- detection engine;
- heuristics and text signals;
- YouTube, TikTok, Instagram, and Facebook detectors.

Create:

- `platform/accessibility/detection/ProductionDetectorRegistry.kt`

Keep:

- `AccessibilityTreeScanner` as the Android adapter.

Change:

- service composition;
- debug playground imports;
- fixture, contract, branch-hardening, and source-invariant tests.

Invariants:

- no detector algorithm, weight, threshold, HIGH branch, package, or
  confidence change;
- debug aliases are injected by the outer registry and never read through
  `BuildConfig` in domain code.

Checks:

- every detector unit/fixture/contract suite;
- Q and E.

Rollback:

- revert the cohesive package-move PR.

### Slice 9 — Router, overlay, and diagnostics seams

Create:

- `application/model/AccessibilityProtectionState.kt`
- `application/port/ProtectionSettingsPort.kt`
- `application/port/AccessibilityDiagnosticsPort.kt`
- local platform `TemporaryAllowNavigator.kt`

Change:

- router receives narrow state, time, and diagnostics;
- overlay no longer imports `MainActivity`;
- service composition supplies adapters;
- runtime singleton is hidden behind the diagnostics facade.

Do not change:

- event ordering;
- overlay lifecycle;
- HOME/PIN timings;
- debug release gates.

Checks:

- event policy, service lifecycle, blocking, phone-home, overlay, and
  sanitized snapshot tests;
- Q and E.

Rollback:

- restore direct collaborators.

### Slice 10 — Platform package convergence

Create:

- `platform/accessibility/AccessibilityServiceRuntime.kt`

Move:

- router, scanner, overlay, status, and diagnostics implementations into
  `platform/accessibility/*`;
- tamper status into `platform/tamper`.

Keep:

- existing Accessibility service FQCN as a thin shell;
- existing Device Admin receiver FQCN as a thin shell.

Change:

- imports;
- JaCoCo exclusions;
- hardcoded source-path invariant tests.

Do not change:

- manifest;
- Accessibility XML;
- Device Admin XML;
- permissions;
- resources.

Checks:

- source invariants;
- full Q and E.

Rollback:

- revert the entire move commit.

### Slice 11 — Split `AppSettings`

Create:

- `application/model/AppSettingsSnapshot.kt`;
- narrow protection configuration and entitlement models;
- neutral inputs for application entitlement resolution.

Move:

- pure free-test and entitlement policies into `domain.entitlement`;
- `LocalEntitlementResolver` into `application.protection`.

Keep:

- PIN hash/salt/version, Billing installation ID, and persisted attempt state in
  the storage-only record.

Change:

- application and platform consumers to use the narrow snapshot.

Do not change:

- DataStore schema, keys, defaults, or enum fallback behavior.

Checks:

- `AppSettingsTest`;
- `FreeTestPolicyTest`;
- `LocalEntitlementResolverTest`;
- entitlement boundary tests;
- Q.

Rollback:

- restore the mapper to the old `AppSettings` consumer model.

### Slice 12 — Storage convergence and architecture guard

Create:

- `infrastructure/storage/StoredAppSettings.kt`
- `infrastructure/storage/DataStoreSettingsMapper.kt`
- `infrastructure/storage/DataStoreSettingsStore.kt`
- `architecture/LayerDependencyTest.kt`

Change:

- after all consumers use ports, remove or rename the transitional
  `SettingsRepository`;
- update storage and recovery tests.

The architecture guard uses existing JUnit/file APIs and rejects forbidden
imports in domain, application, and presentation packages. No dependency is
added.

Invariants:

- store name remains `shorts_blocker_settings`;
- every Preferences key remains byte-compatible;
- defaults, corrupt-enum recovery, and unknown-platform filtering remain
  unchanged.

Checks:

- storage repository/recovery tests;
- architecture guard;
- Q.

Rollback:

- revert the move; persisted data remains compatible.

### Slice 13 — Dashboard and Protected Apps UI models

Create:

- `presentation/dashboard/DashboardUiState.kt`
- `presentation/dashboard/DashboardStateFactory.kt`
- `presentation/dashboard/ProtectedPlatformItemUiModel.kt`

Change:

- Dashboard;
- Protected Apps;
- app coordinator;
- debug screen signatures.

Presentation must no longer consume raw settings, platform matrices, security
metadata, or system time.

Checks:

- state factory truth table for every `canProtect` prerequisite;
- resource and source-invariant tests;
- Q and E.

Rollback:

- restore old renderer inputs.

### Slice 14 — Billing safety seam

Create:

- `application/billing/BillingPurchaseSummary.kt`
- `application/billing/BillingSyncOutcome.kt`
- `application/billing/SyncBillingEntitlementUseCase.kt`
- `SyncBillingEntitlementUseCaseTest.kt`

Change:

- keep `PlayBillingRepository` as a facade;
- delegate verify, refresh, client-only, and fail-closed orchestration to the
  use case;
- adjust composition wiring only as required.

Do not change:

- BillingClient mapping;
- purchase or Manage Subscription UI;
- product ID;
- offer selection;
- backend endpoints/contracts;
- acknowledgement ownership;
- DataStore keys;
- release flags.

Checks:

- configured backend always beats client-only;
- verify success stores the exact snapshot;
- missing install ID or verification failure stores `UNKNOWN`;
- refresh failure stores `UNKNOWN`;
- null refresh stores `EXPIRED`;
- only `(requested=true, internal=true)` permits client-only;
- pending/no purchase never grants protection;
- local acknowledgement is requested only in the allowed client-only path;
- Q, R, and `:billing-backend:test`.

Rollback:

- restore orchestration in the existing facade.

### Slice 15 — Billing layer alignment

Move/split:

- entitlement state/snapshot and pure verification policy to
  `domain.entitlement`;
- UI message mapping to `presentation.billing`;
- backend port to `application.billing.BillingVerificationPort`;
- HTTP implementation to
  `infrastructure.billing.HttpBillingVerificationAdapter`;
- product configuration to `infrastructure.billing.PlayBillingConfig`;
- `BillingUiState` and resource mapping to `presentation.billing`;
- facade into `GooglePlayBillingGateway` and `BillingCoordinator`.

Change:

- app composition;
- Dashboard/debug imports;
- JaCoCo exclusions;
- Billing presentation and release source-path invariant tests.

Checks:

- all Billing, free-test, entitlement, and `AppSettings` tests;
- Q;
- R;
- backend tests;
- manual Google Play license-tester/internal-track purchase, restore, pending,
  cancellation, expiry, and Manage Subscription QA.

Rollback:

- revert the complete split/move PR. Persisted keys, serialized enum names, and
  backend contracts remain compatible.

### Slice 16 — Presentation namespace convergence

Move:

- remaining `feature/*` presentation files into `presentation/*`.

Change only:

- packages/imports;
- source-path invariant tests;
- coverage exclusions.

Do not combine with:

- copy changes;
- resource changes;
- layouts;
- navigation changes;
- domain or storage refactoring.

Checks:

- source/resource invariants;
- Q and E.

Rollback:

- revert the move-only PR.

## 8. Priority Slice

Slice 1 is the only authorized starting scope for the first implementation PR.

The policy should receive plain values for:

- Accessibility enabled;
- protection enabled;
- disclosure accepted;
- PIN configured;
- free test already started.

It must not depend on `AppSettings` and must not call `canProtect`.

The first PR stops when:

- the new pure policy and truth-table test exist;
- `MainActivity` delegates the current predicate to it;
- behavior is unchanged;
- targeted tests, Q, and `git diff --check` pass.

Starting Slice 2 in the same PR is out of scope.

## 9. Risks and Unknowns

- The planning audit did not run Gradle, emulator, or device tasks because they
  would create artifacts.
- The baseline was verified against the local remote-tracking ref; no live
  remote fetch was performed.
- Real TikTok, Instagram, Facebook, overlay, and OEM Accessibility behavior
  requires device QA.
- Google Play purchase lifecycle behavior requires license-tester/internal
  track QA.
- The source manifest has no explicit permission declarations. A previously
  generated merged artifact showed Billing/Internet transitively, but it may be
  stale. Release slices must verify the merged manifest without changing the
  permission set.
- PIN atomicity is the highest-risk persistence boundary.
- `pinHashVersion` is stored but does not currently dispatch the hashing
  algorithm; this plan does not silently change that behavior.
- Package moves require simultaneous updates to JaCoCo exclusions and
  hardcoded source-path tests.
- Single-module boundaries are guarded by tests rather than Gradle/compiler
  module isolation.
- Key coordinators currently excluded from coverage require behavior seams and
  tests before mechanical moves.
- The SharedPreferences/DataStore documentation mismatch remains a separate
  documentation correction.

## 10. Explicit Non-Changes

Approval of this plan does not itself change:

- application behavior;
- Gradle modules or dependencies;
- manifest or permissions;
- Accessibility or Device Admin XML;
- resources or translations;
- DataStore keys/schema;
- detector algorithms;
- Billing product/backend contracts;
- release policy.

Implementation begins only with Slice 1 in a separate code-change PR.
