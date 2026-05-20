# Shorts Blocker Kids Phone Test Checklist

Use this checklist on a real Android phone with YouTube installed.

- [ ] 1. Clean install the APK.
- [ ] 2. First launch shows the welcome screen.
- [ ] 3. Parent PIN setup works.
- [ ] 4. Weak PIN is rejected.
- [ ] 5. Accessibility permission screen opens from the app.
- [ ] 6. Enable Shorts Blocker Kids Accessibility Service.
- [ ] 7. Return to the app and verify protection activation.
- [ ] 8. First successful protection activation starts the 20-day Free Test Mode.
- [ ] 9. Dashboard shows "Free test active".
- [ ] 10. Dashboard shows days remaining.
- [ ] 11. Dashboard shows "Protection active".
- [ ] 12. App restart keeps the same Free Test Mode start time and days remaining.
- [ ] 13. Phone restart keeps protection active.
- [ ] 14. YouTube Home is not blocked.
- [ ] 15. Normal YouTube video is not blocked.
- [ ] 16. YouTube Search is not blocked.
- [ ] 17. YouTube Shorts tab is blocked.
- [ ] 18. Shorts from Home is blocked.
- [ ] 19. Swipe between Shorts remains blocked.
- [ ] 20. Exit Shorts button works.
- [ ] 21. Enter PIN opens parent PIN flow and temporary allow options.
- [ ] 22. Wrong PIN is rejected.
- [ ] 23. Temporary allow works for 5 minutes.
- [ ] 24. Temporary allow works for 10 minutes.
- [ ] 25. Temporary allow works for 15 minutes.
- [ ] 26. Temporary allow closes the overlay while active.
- [ ] 27. Expired temporary allow does not reactivate after setting the system time backward.
- [ ] 28. Protection OFF asks for parent PIN.
- [ ] 29. Protection OFF disables block and closes the overlay after correct PIN.
- [ ] 30. Leaving YouTube closes the overlay.
- [ ] 31. Disable Accessibility Service manually.
- [ ] 32. Dashboard shows "Protection permission missing".
- [ ] 33. Dashboard shows a clear warning that protection is disabled.
- [ ] 34. Re-enable Accessibility Service and confirm protection still works.
- [ ] 35. Simulate expired test period.
- [ ] 36. Dashboard shows "Free test expired".
- [ ] 37. Protection is locked after expiry.
- [ ] 38. Expired screen/message says "Free test period ended. Subscription will be available soon."
- [ ] 39. APK update over existing install keeps the original Free Test Mode timer.
- [ ] 40. App does not show a payment screen.
- [ ] 41. App does not ask for an account.
- [ ] 42. App does not ask for a license key.
- [ ] 43. Overlay is readable in portrait.
- [ ] 44. Overlay is readable in landscape.
- [ ] 45. Overlay is readable on a small screen.
- [ ] 46. Overlay is branded as Shorts Blocker Kids, not an Android system warning.
- [ ] 47. App does not hide itself or pretend to be an Android system service.
- [ ] 48. App does not block Android Settings or uninstall.
- [ ] 49. No crash.
- [ ] 50. No overlay loop.

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

Record APK test details:

- APK build version.
- APK build path.
- Clean install result.
- First launch result.
- PIN setup result.
- Permission enable result.
- First protection activation timestamp.
- Free test days remaining shown.
- App restart result.
- Phone restart result.
- Manual permission disable result.
- Wrong PIN result.
- Expired test simulation method and result.
- APK update over existing install result.
