# CODE_STYLE

Project-specific language and framework conventions for Shorts Blocker Kids.

primary_language: Kotlin
active_sections: Kotlin / Android, Gradle Kotlin DSL, Android XML, Markdown, Tests and fixtures
fallback: if a language or framework is introduced later, follow the existing local style and add a project-specific section before making broad style changes.

## Active languages

- Languages in scope: Kotlin, Gradle Kotlin DSL, Android XML, Markdown

## Kotlin / Android

- Use the existing package root `com.shortsblockerkids`.
- Keep Android framework entrypoints thin; place detector, storage, and security behavior in focused classes under the existing feature/core/accessibility packages.
- Keep user-facing strings in `app/src/main/res/values/strings.xml` when they are reused or belong to Android system declarations.
- Accessibility logging must stay sanitized and debug-only; release builds must not log raw accessibility trees or private YouTube UI text.
- Prefer immutable values and small data classes for detector results and settings state.
- Do not add backend, account, cloud, analytics, ads, billing, language analysis, video analysis, or broad parental-control behavior unless explicitly scoped.

## Gradle Kotlin DSL

- Keep the single-module structure unless a task explicitly requires module changes.
- Do not update dependency versions or add plugins without explicit scope.
- Keep Android SDK settings aligned with `app/build.gradle.kts` unless the task explicitly changes platform support.

## Android XML

- Keep manifest permissions minimal.
- Do not add `INTERNET` permission unless explicitly justified by the task.
- Keep AccessibilityService package filtering narrow to YouTube unless the task explicitly expands supported apps.

## Markdown

- Keep reports factual and evidence-based.
- Mark device-dependent results as `Partial` when no real Android device verification was performed.

## Python

- Not used in this repo.

## JavaScript / TypeScript

- Not used in this repo.

## Go

- Not used in this repo.

## SQL

- Not used in this repo.

## Shell / CLI

- No project shell scripts are currently defined.
- Prefer documented Gradle and adb commands over adding scripts.

## Tests and fixtures

- Test frameworks: JUnit 4 JVM unit tests under `app/src/test/java`
- Fixture / mock conventions: keep detector and security fixtures small, explicit, and local to the test file unless reused by multiple tests
- Required test suites before close-out: for Android code changes run `ANDROID_HOME=/home/serputko/Android/Sdk gradle :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`; for `.agent/project`-only changes reread changed files and verify no template placeholders remain

## Framework or repo-specific exceptions

- None.
