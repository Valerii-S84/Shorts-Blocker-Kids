# Shorts Blocker Kids - Windows Emulator Fallback

Use this fallback if the Android Emulator cannot run reliably inside WSL2.

## 1. Build APK In WSL2

From the repo root in WSL2:

```bash
ANDROID_HOME=/path/to/Android/Sdk ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
mkdir -p dist
cp app/build/outputs/apk/debug/app-debug.apk dist/ShortsBlockerKids-debug.apk
sha256sum dist/ShortsBlockerKids-debug.apk > dist/ShortsBlockerKids-debug.apk.sha256
```

The APK path for Windows access is:

```text
\\wsl.localhost\Ubuntu\mnt\c\Users\User\Desktop\Shorts Blocker Kids\dist\ShortsBlockerKids-debug.apk
```

If the distro name is different, open File Explorer and browse:

```text
\\wsl.localhost\
```

## 2. Start Android Emulator In Windows

Preferred:

1. Open Android Studio on Windows.
2. Open Device Manager.
3. Create or start a Pixel-like emulator with Android 15 / API 35 or higher.
4. Prefer a Google Play image if available.
5. Wait until the emulator fully boots.

Alternative command-line start from Windows:

```powershell
cd "$env:LOCALAPPDATA\Android\Sdk\emulator"
.\emulator.exe -list-avds
.\emulator.exe -avd ShortsBlocker_Pixel_API35
```

## 3. Install APK With Windows ADB

In PowerShell:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb devices
& $adb install -r "\\wsl.localhost\Ubuntu\mnt\c\Users\User\Desktop\Shorts Blocker Kids\dist\ShortsBlockerKids-debug.apk"
& $adb shell pm list packages | findstr /i shorts
```

Expected package:

```text
package:com.shortsblockerkids
```

## 4. Manual Emulator Checklist

- [ ] App opens.
- [ ] Welcome screen appears.
- [ ] PIN setup works.
- [ ] Weak PIN is rejected.
- [ ] PIN confirmation works.
- [ ] Accessibility disclosure appears.
- [ ] Dashboard appears.
- [ ] Dashboard shows PIN created.
- [ ] Dashboard shows Accessibility Service enabled/disabled.
- [ ] Dashboard shows Protection ON/OFF.
- [ ] Dashboard shows Runtime active/inactive.
- [ ] Settings survive app restart.
- [ ] Detector Playground opens in debug build.
- [ ] Detector Playground scenarios show expected confidence.
- [ ] Test blocking overlay trigger is available.

## 5. Manual Phone Checklist

For real proof, use:

```text
dist/TEST_ON_PHONE_CHECKLIST.md
```

The emulator does not replace a real-phone YouTube proof.
