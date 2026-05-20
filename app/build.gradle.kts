plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
}

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

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
            buildConfigField("boolean", "ACCESSIBILITY_DEBUG_TOOLS_ENABLED", "true")
        }

        release {
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

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.datastore.preferences)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
