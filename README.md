# Shorts Blocker Kids

Shorts Blocker Kids is a local-only Android app for blocking YouTube Shorts on a child's phone with a parent PIN and Android Accessibility Service.

The app has no account system, no app backend, no analytics, and no `INTERNET` permission in the current Android manifest.

## Requirements

- JDK 17
- Android SDK with platform `android-36`
- Android build tools `36.0.0`
- Gradle wrapper from this repository

Local commands use:

```bash
ANDROID_HOME=/home/serputko/Android/Sdk
```

Adjust `ANDROID_HOME` if the SDK is installed elsewhere.

## Build

```bash
ANDROID_HOME=/home/serputko/Android/Sdk ./gradlew :app:assembleDebug
ANDROID_HOME=/home/serputko/Android/Sdk ./gradlew :app:assembleRelease
ANDROID_HOME=/home/serputko/Android/Sdk ./gradlew :app:bundleRelease
```

The debug APK is written under `app/build/outputs/apk/debug/`.
The release AAB is written under `app/build/outputs/bundle/release/`.

The release APK is unsigned unless release signing values are provided.

Release signing can be configured through environment variables, Gradle properties, or an untracked root `keystore.properties` file:

```properties
SBK_RELEASE_STORE_FILE=/absolute/path/to/release.jks
SBK_RELEASE_STORE_PASSWORD=...
SBK_RELEASE_KEY_ALIAS=...
SBK_RELEASE_KEY_PASSWORD=...
```

Do not commit keystores or `keystore.properties`.

Current release decisions:

- `versionName = 0.1.0` and `versionCode = 1` are intentionally kept for the first controlled release candidate line.
- `isMinifyEnabled = false` is intentionally kept until release-device regression coverage is complete for the AccessibilityService detector path.
- Google Play publishing remains out of scope for this repository state.

## Test And Lint

```bash
ANDROID_HOME=/home/serputko/Android/Sdk ./gradlew :app:testDebugUnitTest
ANDROID_HOME=/home/serputko/Android/Sdk ./gradlew :app:lintDebug
ANDROID_HOME=/home/serputko/Android/Sdk ./gradlew :app:ktlintCheck
```

Full local foundation check:

```bash
ANDROID_HOME=/home/serputko/Android/Sdk ./gradlew :app:ktlintCheck :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:lintDebug
```

Full local release readiness check:

```bash
ANDROID_HOME=/home/serputko/Android/Sdk ./gradlew :app:ktlintCheck :app:assembleDebug :app:assembleRelease :app:bundleRelease :app:testDebugUnitTest :app:lintDebug :app:lintRelease
```

## Install On A Device

Connect a device or emulator visible in `adb devices`, then run:

```bash
ANDROID_HOME=/home/serputko/Android/Sdk ./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

After installing, enable the Accessibility Service manually in Android settings.

## Product Boundaries

- No app server
- No account
- No cloud sync
- No analytics
- No watch history storage
- No screen recording
- No microphone, camera, location, contacts, or SMS permissions
- No aggressive anti-uninstall behavior

## Known Limitation

Shorts blocking works when Protection is ON and Android Accessibility Service is active.
If the system permission is turned off or the app is removed, blocking stops.
