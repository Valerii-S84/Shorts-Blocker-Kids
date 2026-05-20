# Install Shorts Blocker Kids Debug APK On Phone

## Option A: Install APK File On The Phone

1. Copy `ShortsBlockerKids-debug.apk` to the Android phone.
2. Open the APK from the phone's file picker or downloads app.
3. If Android asks, allow install from this source.
4. Install the app.
5. Open Shorts Blocker Kids.
6. Create the parent PIN.
7. Accept the Accessibility disclosure.
8. Open Android Accessibility settings.
9. Enable Shorts Blocker Kids Accessibility Service.
10. Return to Shorts Blocker Kids.
11. Turn Protection ON.
12. Open YouTube Shorts and test blocking.

## Option B: Install With ADB

From the repo root:

```bash
ANDROID_HOME=/home/serputko/Android/Sdk /home/serputko/Android/Sdk/platform-tools/adb install -r dist/ShortsBlockerKids-debug.apk
```

If the phone appears as `unauthorized`:

1. Unlock the phone.
2. Accept the USB debugging prompt.
3. Run:

```bash
ANDROID_HOME=/home/serputko/Android/Sdk /home/serputko/Android/Sdk/platform-tools/adb devices
```

Expected:

```text
List of devices attached
<device_id>    device
```
