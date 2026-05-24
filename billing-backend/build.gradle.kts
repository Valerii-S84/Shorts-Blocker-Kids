plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
    application
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        allWarningsAsErrors.set(false)
    }
}

ktlint {
    ignoreFailures.set(false)
    outputToConsole.set(true)
    filter {
        exclude("**/build/**")
    }
}

application {
    mainClass.set("com.shortsblockerkids.billingbackend.BillingBackendMainKt")
}

dependencies {
    implementation(libs.google.auth.oauth2.http)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
