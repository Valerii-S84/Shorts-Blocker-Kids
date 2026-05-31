# PROJECT_CONTEXT

Project-specific facts for Shorts Blocker Kids.

## 1. Stack

- Project name: Shorts Blocker Kids
- Primary languages: Kotlin, Gradle Kotlin DSL, Android XML, Markdown, HTML, CSS, JavaScript
- Runtime / platform: Android app, minSdk 26, targetSdk 36, compileSdk 36; static public website for `shortsblockerkids.de`
- Main frameworks / libraries: Android Gradle Plugin, Jetpack Compose, Material 3, Activity Compose, Android AccessibilityService
- Data stores: Android SharedPreferences for local settings and PIN hash metadata
- Default user-facing language: English

## 2. Project structure

- Root entrypoints: `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`
- Source directories: `app/src/main/java/com/shortsblockerkids`, `app/src/main/res`, `website/src`, `website/public`
- Test directories: `app/src/test/java`
- Config / infra directories: `.agent`, `app/src/main/res/xml`, Gradle project files, `website/scripts`
- Read-only or protected paths: `.agent/core` unless the task explicitly changes the rule bundle core

## 3. Key commands

| Purpose | Command | Notes |
|---|---|---|
| Test | `gradle :app:testDebugUnitTest` | JVM unit tests; requires `ANDROID_HOME` or `ANDROID_SDK_ROOT` |
| Lint | `gradle :app:lintDebug` | Android lint for debug variant; requires `ANDROID_HOME` or `ANDROID_SDK_ROOT` |
| Build | `gradle :app:assembleDebug` | Produces debug APK under `app/build/outputs/apk/debug/`; requires `ANDROID_HOME` or `ANDROID_SDK_ROOT` |
| Dev / Run | `gradle :app:assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk` | Requires a real Android device or emulator visible through `adb devices` |
| Website check | `cd website && npm run check` | Dependency-free JavaScript syntax check for static website scripts |
| Website build | `cd website && npm run build` | Generates static files under `website/dist/` |
| Website smoke | `cd website && npm run smoke` | Builds and verifies root/privacy/support/terms routes and policy invariants |

## 4. External dependencies

| System / service | Purpose | Access mode | Notes |
|---|---|---|---|
| Google Maven repository | Android and Compose dependencies | Gradle build-time dependency resolution | Declared in `settings.gradle.kts` |
| Maven Central | JUnit and library dependencies | Gradle build-time dependency resolution | Declared in `settings.gradle.kts` |
| Android SDK | Build, lint, APK install, adb tooling | Local filesystem and USB device tooling | Set `ANDROID_HOME` or `ANDROID_SDK_ROOT` before Android commands |
| YouTube Android app | Manual AccessibilityService detector calibration target | Installed app on local test device | No backend integration with YouTube |
| Node.js | Static website build and smoke checks | Local command-line runtime | No npm package dependencies are required for the website |

## 5. Project constraints

- Protected paths: `.agent/core` is protected unless explicitly requested; production publishing, CI/CD, and dependency policy files are protected unless explicitly requested
- Secrets / credentials locations: no project secret files are expected; do not add credentials, keystores, API tokens, or account secrets
- Deploy / production boundaries: no production deployment is configured; Google Play publishing and release signing are out of scope unless explicitly requested
- Approval-required operations: dependency updates, adding network permissions, backend/cloud/account/analytics integrations, release signing, publishing, destructive filesystem actions, git history rewriting
- Restricted hosts / environments: local-only app work; no backend, account, cloud, analytics, ads, billing, language analysis, video analysis, or full parental-control expansion without explicit scope
- Project-specific forbidden actions: do not add `INTERNET` permission unless explicitly justified; do not store watch history; do not log raw accessibility trees in release builds; do not collect video titles, comments, account names, URLs, audio, or screen recordings
- Website constraints: public website must not expose backend admin/debug information, secrets, fake metrics, fake testimonials, direct APK download as the primary install action, or non-Play payment CTAs for the Play-distributed app

## 6. Git settings

- Default / protected branch: not configured in this local directory
- Branching strategy: not configured; do not create, push, merge, rebase, or rewrite branches without explicit user request
- Merge strategy: not configured
- PR title format: not configured
- PR requirements: not configured
