plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ktlint) apply false
}

val fakeSocialAppVariants =
    mapOf(
        "youtube" to
            "test-fixtures/fake-social-apps/build/outputs/apk/youtube/debug/" +
                "fake-social-apps-youtube-debug.apk",
        "tiktok" to
            "test-fixtures/fake-social-apps/build/outputs/apk/tiktok/debug/" +
                "fake-social-apps-tiktok-debug.apk",
        "instagram" to
            "test-fixtures/fake-social-apps/build/outputs/apk/instagram/debug/" +
                "fake-social-apps-instagram-debug.apk",
        "facebook" to
            "test-fixtures/fake-social-apps/build/outputs/apk/facebook/debug/" +
                "fake-social-apps-facebook-debug.apk",
        "unsupported" to
            "test-fixtures/fake-social-apps/build/outputs/apk/unsupported/debug/" +
                "fake-social-apps-unsupported-debug.apk",
    )

val assembleFakeSocialApps by tasks.registering {
    group = "verification"
    description = "Builds fake emulator target apps for short-video E2E tests."
    dependsOn(
        fakeSocialAppVariants.keys.map { flavor ->
            ":test-fixtures:fake-social-apps:assemble${flavor.replaceFirstChar(Char::titlecase)}Debug"
        },
    )
}

val installFakeSocialApps by tasks.registering {
    group = "verification"
    description = "Installs fake emulator target apps for short-video E2E tests."
    dependsOn(assembleFakeSocialApps)

    doLast {
        val androidHome =
            providers.environmentVariable("ANDROID_HOME")
                .orElse(providers.environmentVariable("ANDROID_SDK_ROOT"))
                .orNull
                ?: error("Set ANDROID_HOME or ANDROID_SDK_ROOT to install fake social apps.")
        val adb = file("$androidHome/platform-tools/adb")
        require(adb.isFile) { "adb not found at ${adb.absolutePath}" }

        fakeSocialAppVariants.values.forEach { relativeApkPath ->
            val apk = file(relativeApkPath)
            require(apk.isFile) { "Missing fake app APK: ${apk.absolutePath}" }
            providers.exec {
                commandLine(adb.absolutePath, "install", "-r", apk.absolutePath)
            }.result.get().assertNormalExitValue()
        }
    }
}

tasks.register("localQualityGate") {
    group = "verification"
    description = "Runs the full local QA gate before real-device verification."
    dependsOn(
        ":app:ktlintCheck",
        ":app:testDebugUnitTest",
        ":app:connectedDebugAndroidTest",
        ":app:lintDebug",
        ":app:lintRelease",
        ":app:assembleDebug",
        ":app:assembleRelease",
        ":app:bundleRelease",
        ":app:jacocoDebugUnitTestCoverageVerification",
    )
}

subprojects {
    if (path == ":app") {
        tasks.matching { it.name == "connectedDebugAndroidTest" }.configureEach {
            dependsOn(installFakeSocialApps)
        }
    }
}
