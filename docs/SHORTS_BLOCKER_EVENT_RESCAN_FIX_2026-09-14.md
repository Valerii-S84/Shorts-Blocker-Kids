# Accessibility event rescan fix — 2026-09-14

Status: Code defect reproduced and fixed. The original installed-app incident is not independently confirmed.

## Proven defect
AccessibilityEventPolicy.shouldScan rejected relevant events occurring within 500 ms of the last scan. AccessibilityEventRouter returned immediately, with no deferred scan. It also consumed the scan slot before finding that the root was unavailable.
A window transition at t=1000 followed by the final content-change event at t=1120 could therefore leave the new Shorts screen unscanned until another event arrived. This locates the code defect before tree scanning and detection.

## Correction
- The event policy requests a deferred scan for the remaining debounce interval.
- Additional events retain the original scan deadline rather than postponing it.
- An unavailable root requests a follow-up scan.
- The service schedules a single cancellable job which rereads the current active window; it does not retain an old AccessibilityEvent or node.
- Interrupt/destroy cancels the pending scan. Detector confidence and protection eligibility rules were not changed.

## Regression evidence
Before the fix, three added behavioral tests failed while the old decision logic remained in place (an inert callback seam allowed observation):
- lastContentChangeInsideDebounceRequestsScanWithoutAnotherEvent
- continuousContentChangesDoNotPostponeDeferredScanDeadline
- unavailableRootRequestsFollowUpAfterWindowTransition
The fourth new test checks that irrelevant events do not request a scan.
After the fix, all 7 AccessibilityEventPolicyTest cases pass.
- Full app JVM suite: 450 tests, 0 failures, 0 errors, 0 skipped.

## Final verification
```powershell
.\gradlew.bat --offline --console=plain :app:ktlintCheck :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:lintRelease :app:assembleRelease
```
Result: BUILD SUCCESSFUL. All six requested tasks completed successfully.
Environment: Android SDK at the previously configured local SDK path; SBK_BILLING_BACKEND_BASE_URL=https://billing.shortsblockerkids.de.

## Release artifact
- app/build/outputs/apk/release/app-release.apk
- SHA-256: BBC3A4021B8A033E512FC93E32D792840E2B0DB70B9B7EB7F63766E9CDBF563F

## Scope and limits
- No emulator, ADB, installation, dependency updates, or publication was used for this fix.
- The unit tests prove deferred-scan requests; Android coroutine/window-manager integration was compiled and linted, not exercised on a device.
- This fix closes a reproducible missed-scan defect. It does not establish that the exact reported installed-app failure had this cause.
- The previously failing project-wide coverage thresholds were not changed or rerun; they remain an open readiness issue.

## Changed source files
- app/src/main/java/com/shortsblockerkids/platform/accessibility/routing/AccessibilityEventPolicy.kt
- app/src/main/java/com/shortsblockerkids/platform/accessibility/routing/AccessibilityEventRouter.kt
- app/src/main/java/com/shortsblockerkids/platform/accessibility/AccessibilityServiceRuntime.kt
- app/src/test/java/com/shortsblockerkids/accessibility/AccessibilityEventPolicyTest.kt

## Local logs
- Red: C:\Users\User\AppData\Local\Temp\sbk-event-rescan-red.log
- Green: C:\Users\User\AppData\Local\Temp\sbk-event-rescan-green.log
