import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
    jacoco
}

val releaseSigningProperties = Properties()
val releaseSigningPropertiesFile = rootProject.file("keystore.properties")
if (releaseSigningPropertiesFile.isFile) {
    releaseSigningPropertiesFile.inputStream().use(releaseSigningProperties::load)
}

fun releaseSigningValue(name: String): String? =
    providers.environmentVariable(name).orNull
        ?: providers.gradleProperty(name).orNull
        ?: releaseSigningProperties.getProperty(name)

val releaseStoreFile = releaseSigningValue("SBK_RELEASE_STORE_FILE")
val releaseStorePassword = releaseSigningValue("SBK_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = releaseSigningValue("SBK_RELEASE_KEY_ALIAS")
val releaseKeyPassword = releaseSigningValue("SBK_RELEASE_KEY_PASSWORD")
val isReleaseSigningConfigured =
    listOf(
        releaseStoreFile,
        releaseStorePassword,
        releaseKeyAlias,
        releaseKeyPassword,
    ).all { !it.isNullOrBlank() }

android {
    namespace = "com.shortsblockerkids"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.shortsblockerkids"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        if (isReleaseSigningConfigured) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
            buildConfigField("boolean", "ACCESSIBILITY_DEBUG_TOOLS_ENABLED", "true")
        }

        release {
            if (isReleaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
            isDebuggable = false
            isMinifyEnabled = false
            buildConfigField("boolean", "ACCESSIBILITY_DEBUG_TOOLS_ENABLED", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = false
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        allWarningsAsErrors.set(false)
    }
}

ktlint {
    android.set(true)
    additionalEditorconfig.set(
        mapOf("ktlint_function_naming_ignore_when_annotated_with" to "Composable"),
    )
    ignoreFailures.set(false)
    outputToConsole.set(true)
    filter {
        exclude("**/build/**")
    }
}

jacoco {
    toolVersion = "0.8.13"
}

val unitTestCoverageExclusions =
    listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*ComposableSingletons*.*",
        "**/AppScreen*.*",
        "**/MainActivity*.*",
        "**/accessibility/AccessibilityEventRouter*.*",
        "**/accessibility/AccessibilityServiceStatus*.*",
        "**/accessibility/AccessibilityTreeScanner*.*",
        "**/accessibility/BlockOverlayController*.*",
        "**/accessibility/DebugAccessibilityLogger*.*",
        "**/accessibility/LastDetectorResult*.*",
        "**/accessibility/RuntimeProtectionState*.*",
        "**/accessibility/ShortsBlockerAccessibilityService*.*",
        "**/core/billing/PlayBillingRepository*.*",
        "**/feature/dashboard/**",
        "**/feature/debug/**",
        "**/feature/pin/**",
        "**/feature/privacy/**",
        "**/feature/onboarding/*Screen*.*",
        "**/feature/blocking/*Screen*.*",
        "**/ui/**",
    )

fun debugUnitTestCoverageClasses() =
    files(
        layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes"),
        layout.buildDirectory.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes"),
    ).asFileTree.matching {
        exclude(unitTestCoverageExclusions)
    }

fun debugUnitTestCoverageData() =
    fileTree(layout.buildDirectory) {
        include("jacoco/testDebugUnitTest.exec")
        include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
    }

tasks.withType<Test>().configureEach {
    extensions.configure(JacocoTaskExtension::class) {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

tasks.register<JacocoReport>("jacocoDebugUnitTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    sourceDirectories.setFrom(files("src/main/java"))
    classDirectories.setFrom(debugUnitTestCoverageClasses())
    executionData.setFrom(debugUnitTestCoverageData())
}

tasks.register<JacocoCoverageVerification>("jacocoDebugUnitTestCoverageVerification") {
    dependsOn("jacocoDebugUnitTestReport")

    sourceDirectories.setFrom(files("src/main/java"))
    classDirectories.setFrom(debugUnitTestCoverageClasses())
    executionData.setFrom(debugUnitTestCoverageData())

    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.92".toBigDecimal()
            }
        }
    }
}

tasks.named("check") {
    dependsOn("jacocoDebugUnitTestCoverageVerification")
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.google.play.billing.ktx)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.uiautomator)
}
