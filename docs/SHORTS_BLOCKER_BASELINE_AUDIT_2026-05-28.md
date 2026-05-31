# Shorts Blocker Kids Baseline Audit Evidence

Status: Done. Current local baseline was audited from `main` at the commit
listed below. No product code, build configuration, dependencies, manifest, or
release settings were changed for this audit.

Date: May 28, 2026

## Source State

Pre-audit working tree state:

```text
git status --short --branch
## main
```

Commit:

```text
a9ff08feac48d16b354c6a44dd89455da691f1bf
```

Version metadata from `app/build.gradle.kts`:

```text
versionCode = 1
versionName = "0.1.0"
debug versionNameSuffix = "-debug"
```

Toolchain:

```text
Gradle 9.4.1
Kotlin 2.3.0
Launcher JVM 17.0.19
Android SDK / ANDROID_HOME=/path/to/Android/Sdk
```

## Build Artifacts

Command:

```bash
ANDROID_HOME=/path/to/Android/Sdk \
  ./gradlew --rerun-tasks :app:assembleDebug :app:assembleRelease :app:bundleRelease
```

Result:

```text
BUILD SUCCESSFUL in 3m 34s
97 actionable tasks: 97 executed
```

Artifacts:

```text
app/build/outputs/apk/debug/app-debug.apk             13519231 bytes
app/build/outputs/apk/release/app-release.apk          9649190 bytes
app/build/outputs/bundle/release/app-release.aab       9150308 bytes
```

Release SHA-256:

```text
4611a3ac2dc415ad79beef759e0a05998c487238eb0de6e17202c2f26c5c71af  app/build/outputs/apk/release/app-release.apk
6cd05d175b7495c8ea8d5c0e1b5cac96072a4df2d904d77b5e1b55ad83c1a30e  app/build/outputs/bundle/release/app-release.aab
```

Build note:

```text
stripDebugDebugSymbols / stripReleaseDebugSymbols reported that
libandroidx.graphics.path.so and libdatastore_shared_counter.so could not be
stripped and were packaged as-is. The build completed successfully.
```

## Gates

All requested gates were run with `--rerun-tasks` where applicable, so Gradle
executed the checks instead of only returning cached/up-to-date task results.

| Gate | Command | Result |
| --- | --- | --- |
| App unit tests | `ANDROID_HOME=/path/to/Android/Sdk ./gradlew --rerun-tasks :app:testDebugUnitTest` | `BUILD SUCCESSFUL in 2m 28s`; 26 tasks executed |
| Backend tests | `ANDROID_HOME=/path/to/Android/Sdk ./gradlew --rerun-tasks :billing-backend:test` | `BUILD SUCCESSFUL in 45s`; 4 tasks executed |
| Lint debug | `ANDROID_HOME=/path/to/Android/Sdk ./gradlew --rerun-tasks :app:lintDebug` | `BUILD SUCCESSFUL in 2m 33s`; 29 tasks executed |
| Lint release | `ANDROID_HOME=/path/to/Android/Sdk ./gradlew --rerun-tasks :app:lintRelease` | `BUILD SUCCESSFUL in 1m 41s`; 16 tasks executed |
| ktlint | `ANDROID_HOME=/path/to/Android/Sdk ./gradlew --rerun-tasks :app:ktlintCheck :billing-backend:ktlintCheck` | `BUILD SUCCESSFUL in 27s`; 20 tasks executed |
| Coverage verification | `ANDROID_HOME=/path/to/Android/Sdk ./gradlew --rerun-tasks :app:jacocoDebugUnitTestCoverageVerification` | `BUILD SUCCESSFUL in 2m 36s`; 28 tasks executed |
| Build release bundle | included in artifact build command above as `:app:bundleRelease` | `BUILD SUCCESSFUL in 3m 34s`; `:app:bundleRelease` executed |

Test result summaries:

```text
app testDebugUnitTest: files=26 tests=193 skipped=0 failures=0 errors=0
billing-backend test: files=2 tests=8 skipped=0 failures=0 errors=0
```

Lint reports:

```text
app/build/reports/lint-results-debug.html    149357 bytes
app/build/reports/lint-results-debug.xml      20701 bytes
app/build/reports/lint-results-release.html  151708 bytes
app/build/reports/lint-results-release.xml    20701 bytes
```

Coverage verification result:

```text
INSTRUCTION missed=319 covered=7558 ratio=95.95%  threshold=94.05%
LINE        missed=36  covered=1401 ratio=97.49%  threshold=95.39%
BRANCH      missed=71  covered=687  ratio=90.63%  threshold=90.00%
```

Coverage report:

```text
app/build/reports/jacoco/jacocoDebugUnitTestReport/jacocoDebugUnitTestReport.xml
app/build/reports/jacoco/jacocoDebugUnitTestReport/html/index.html
```

## Not Run

The requested baseline audit did not include connected Android instrumentation
tests, emulator tests, real-device install/smoke verification, Play Console
submission, or external backend deployment checks.
