# Shorts Blocker Kids Real-Device Smoke Report

Status: Partial. User-confirmed smoke QA passed on five real Android devices;
detailed device matrix remains pending.

## Scope

This report records manual real-device smoke evidence provided by the product
owner on May 22, 2026.

The report covers the current Shorts Blocker Kids Android app behavior:

- install;
- parent PIN;
- Accessibility Service enablement;
- normal YouTube video non-blocking;
- YouTube Shorts blocking;
- exit to phone home;
- parent PIN temporary allow for 5, 10, and 15 minutes.

## Result Summary

User-confirmed results across five devices:

- App installs successfully.
- Parent PIN setup and PIN entry work.
- Android Accessibility Service can be enabled.
- Normal YouTube videos are not blocked.
- YouTube Shorts are blocked immediately and reliably in the tested flow.
- Exit to phone home works from the blocking screen.
- Parent PIN temporary allow works.
- Temporary allow durations 5, 10, and 15 minutes work.
- No problems were observed in this smoke pass.

## Production Impact

This closes the main real-device blocker smoke risk that was still open after
the emulator-only reports.

For Play production readiness, this is enough to move forward with the policy
package and Play Console preparation. It is not a substitute for the full
device/version matrix required before final staged rollout.

## Still Pending

- Device manufacturer and model table.
- Android version per device.
- YouTube app version per device.
- Build type per device.
- Portrait and landscape layout pass per device.
- OEM-specific behavior notes.
- Regression pass after Play Billing integration.
- Regression pass on the final signed release candidate.
