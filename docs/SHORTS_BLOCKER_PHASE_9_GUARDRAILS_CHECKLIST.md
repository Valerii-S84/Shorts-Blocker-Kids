# Shorts Blocker Kids — Phase 9 Honest Guardrails Checklist

Status: manual checklist prepared; device execution required.

## Preconditions

- Install a debug or release build on a real Android device or emulator.
- Create a parent PIN.
- Accept the Accessibility disclosure.
- Enable `Shorts Blocker Kids Protection` in Android Accessibility settings.

## Honest Status Checks

1. Open the dashboard with Protection ON and Accessibility Service enabled.
2. Confirm parents can see the Accessibility Service status.
3. Turn Accessibility Service OFF in Android settings.
4. Return to the app and confirm the dashboard shows Protection inactive.
5. Confirm the app explains that blocking works only when Protection is ON and Accessibility Service is active.
6. Confirm the app explains that disabling the permission or removing the app stops blocking.
7. Turn Protection OFF from the dashboard and confirm parent PIN is required.

## Policy Guardrail Checks

1. Confirm the launcher icon remains visible.
2. Confirm the app does not pretend to be an Android system service.
3. Confirm Android Settings are not blocked.
4. Confirm uninstall is not blocked by hidden or deceptive behavior.
5. Confirm Device Owner is not required.
6. Confirm no persistent notification was added for this phase.
7. Confirm `INTERNET` and `ACCESS_NETWORK_STATE` are present only through the
   Google Play Billing SDK, and confirm the manifest does not request
   `SYSTEM_ALERT_WINDOW` or `POST_NOTIFICATIONS`.

## Result Recording

Record:

- Device manufacturer and model.
- Android version and SDK.
- Build type tested.
- Pass/fail for each checklist item.
