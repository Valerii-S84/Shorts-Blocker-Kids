# Shorts Blocker Kids Phone Test Checklist

Use this checklist on a real Android phone with YouTube installed.

- [ ] 1. App opens.
- [ ] 2. PIN setup works.
- [ ] 3. Weak PIN is rejected.
- [ ] 4. Dashboard shows PIN created.
- [ ] 5. Accessibility Service can be enabled.
- [ ] 6. Dashboard shows service enabled.
- [ ] 7. Protection ON.
- [ ] 8. YouTube Home is not blocked.
- [ ] 9. Normal YouTube video is not blocked.
- [ ] 10. YouTube Search is not blocked.
- [ ] 11. YouTube Shorts tab is blocked.
- [ ] 12. Shorts from Home is blocked.
- [ ] 13. Swipe between Shorts remains blocked.
- [ ] 14. Exit Shorts button works.
- [ ] 15. Enter PIN opens parent PIN flow and temporary allow options.
- [ ] 16. Temporary allow works for 5 minutes.
- [ ] 17. Temporary allow works for 10 minutes.
- [ ] 18. Temporary allow works for 15 minutes.
- [ ] 19. Temporary allow closes the overlay while active.
- [ ] 20. Expired temporary allow does not reactivate after setting the system time backward.
- [ ] 21. Protection OFF asks for parent PIN.
- [ ] 22. Protection OFF disables block and closes the overlay after correct PIN.
- [ ] 23. Leaving YouTube closes the overlay.
- [ ] 24. Accessibility OFF shows inactive.
- [ ] 25. Dashboard explains blocking works only when Protection is ON and Accessibility Service is active.
- [ ] 26. Dashboard explains disabling permission or removing the app stops blocking.
- [ ] 27. App does not hide itself or pretend to be an Android system service.
- [ ] 28. App does not block Android Settings or uninstall.
- [ ] 29. Dashboard shows subscription state.
- [ ] 30. Subscribe button opens Stripe Checkout in browser/custom tab.
- [ ] 31. Successful Stripe test payment activates the app automatically without license key.
- [ ] 32. Active Stripe entitlement allows protection.
- [ ] 33. Expired or unpaid Stripe entitlement pauses blocking while settings remain accessible.
- [ ] 34. Overlay is readable in portrait.
- [ ] 35. Overlay is readable in landscape.
- [ ] 36. Overlay is readable on a small screen.
- [ ] 37. Overlay is branded as Shorts Blocker Kids, not an Android system warning.
- [ ] 38. App restart keeps settings.
- [ ] 39. No crash.
- [ ] 40. No overlay loop.

Record device details:

```bash
adb shell getprop ro.product.manufacturer
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
```

Record YouTube version if available:

```bash
adb shell dumpsys package com.google.android.youtube | grep versionName
```

Record website APK and Stripe test details for items 29-33:

- APK build version.
- Website APK download URL.
- Stripe mode: test or production.
- Stripe product ID.
- Stripe price ID for `€2.20/month`.
- Checkout session result.
- Automatic activation result.
- Customer Portal management result.
- Cancel-until-period-end result.
- Expiry result.
- Offline grace result.
