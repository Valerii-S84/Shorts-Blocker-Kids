# Shorts Blocker Kids Clean Architecture Migration Plan

Status: Approved objectives retained; completion of all 16 slices is NOT certified in this documentation preparation. No code-change track starts here.

Original approval: 2026-07-28. Planning baseline: `f87f66e1411d31455fbfbe05b731cf884b3baa75`. Its branch comparison and test results are historical, not the current checkout or a fresh remote check.

## Current State And Approval Boundary

Domain/application/infrastructure/platform/presentation/composition packages, Preferences DataStore and a pure activation predicate exist. The old core/feature map and instruction to start automatically with Slice 1 are superseded. Package presence alone does not prove completion.

Before a future code task, inspect each relevant objective's implementation and regression evidence, identify unclosed work and obtain that task's scope. Do not replay completed work or call an unknown checklist result a product defect.

The own Billing backend was removed. Former HTTP/verification ports, server-first policy, RTDN, release URL validation, UNKNOWN-on-server-failure and 72-hour grace are withdrawn obligations, not requirements to reimplement.

## Retained Approved Objectives

Every row is **not certified by this preparation**. Evidence locations/expectations are not pass results.

| Slice | Approved objective | Evidence required for closure |
|---|---|
| 1 | Extract activation predicate. | Pure ProtectionActivationPolicy, truth-table coverage and outer delegation without hidden canProtect dependency. |
| 2 | Application-owned activation and clock. | RecordSuccessfulProtectionActivationUseCase, clock/status ports, one-time atomic store operation; bare writes do not start the timer. |
| 3 | PIN application facade. | Create/verify use cases, UI delegation and unchanged validation/recovery. |
| 4 | PIN internals behind atomic ports. | PinStateStore, hashing/lockout clocks, concurrent/atomic attempts, corrupt metadata and lockout behavior. |
| 5 | Temporary-allow boundary. | Set/clear use cases, durations, cancel/no-write, wall/elapsed/boot timing and expiry cleanup. |
| 6 | Extract app coordinator. | Thin entrypoint, explicit use-case dependencies and unchanged flows. |
| 7 | Explicit time in blocking controller. | Injected time; PIN grace, expiry, cooldown and return-to-foreground behavior. |
| 8 | Pure detection cohort. | Domain snapshots/detectors preserve weights, exclusions, confidence and fixtures. |
| 9 | Router/overlay/diagnostics seams. | Platform ports, sanitized logging, cancellation and behavioral tests. |
| 10 | Platform package convergence. | Android runtime ownership plus manifest/XML/lifecycle/debug-release references. |
| 11 | Split AppSettings. | Stored models versus plain application/domain snapshots; no key/schema/data change. |
| 12 | Storage convergence and architecture guard. | Infrastructure stores/mappers, ports, layer tests and source-path/coverage exclusions. |
| 13 | Dashboard/Protected Apps UI models. | Plain UI models/use-case boundaries, selection/status and scroll/accessibility behavior. |
| 14 | Billing safety seam. | Pure purchased/pending/none mapping and failure tests under current Play contract; former server seam withdrawn. |
| 15 | Billing layer alignment. | SDK operations in adapter; access in application; persist-before-ack in coordinator; no UI/server types in application. |
| 16 | Presentation namespace convergence. | Screens/coordinators/imports/resource and source-path tests preserve existing behavior. |

## Behavioral Invariants

[Architecture](SHORT_VIDEO_BLOCKER_ARCHITECTURE.md), [Detection](SHORT_VIDEO_BLOCKER_DETECTION_STRATEGY.md) and [Billing](SHORTS_BLOCKER_PLAY_BILLING_BACKEND_RUNBOOK.md) maintain the current contracts. Preserve the original applicable §6 obligations:

- Exactly 20-day default local free test, full activation prerequisites, one start and exact expiry.
- 4–6 digit PIN, weak-value exclusions, PBKDF2 parameters, constant-time comparison, lockout schedule, corrupt metadata and atomic mutation.
- 15/30/60 minute allow, cancel/no grant, physical expiry cleanup and clock-rollback safety; retain current monotonic/boot-aware timing.
- Four production packages, HIGH-only blocking, debug-only fixture aliases and sanitized diagnostics; moves do not change detector weights/exclusions.
- Overlay dismissal, PIN grace, scanner bounds, rescan/cooldown scheduling and lifecycle cancellation.
- Current Play purchase-state/local-access policy, persistence before acknowledgement, pending denial and free-test precedence.
- Disclosure/consent, EN/DE/UK and API-specific resources, manifest/XML and transparent optional Device Admin semantics.

## Execution And Verification For A Separately Authorized Code Task

One bounded slice per PR. A dependent slice waits for declared checks and independent review. Preserve behavior/data/resources/manifest/release safeguards. No DI framework, extra production module or dependency solely for package moves. Emergency fixes have their own scope.

Original relevant quality, device and release profiles are retained as future code-task evidence, not results of this preparation:

~~~powershell
.\gradlew.bat :app:ktlintCheck :app:assembleDebug :app:testDebugUnitTest :app:jacocoDebugUnitTestCoverageVerification :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
.\gradlew.bat :app:lintRelease :app:assembleRelease :app:bundleRelease
~~~

Use focused behavior tests and relevant profiles; successful unrelated checks are not rerun without changed artifacts. The removed backend profile is not executable or required.

## Unresolved Evidence And Risks

Full slice completion and full quality/coverage are unverified here. Real social-app/OEM/final release QA and Play-installed purchase lifecycle remain external gates. PIN atomicity and timing boundaries require behavior evidence. Namespace work must account for hardcoded source-path tests and JaCoCo exclusions. Current merged manifest/SDK disclosures must be verified for the eventual release AAB; an old artifact is not proof.

This preserves approval context and unclosed objectives from the original plan and Roadmap. It changes no implementation approval, keys/schema, behavior, translations, permissions, detector thresholds, dependencies or release policy.
