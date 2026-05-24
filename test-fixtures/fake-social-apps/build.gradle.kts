plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.shortsblockerkids.fixtureapps"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        resValues = true
    }

    flavorDimensions += "platform"

    productFlavors {
        create("youtube") {
            dimension = "platform"
            applicationId = "com.shortsblockerkids.fixture.youtube"
            resValue("string", "app_name", "Fake YouTube")
        }
        create("tiktok") {
            dimension = "platform"
            applicationId = "com.shortsblockerkids.fixture.tiktok"
            resValue("string", "app_name", "Fake TikTok")
        }
        create("instagram") {
            dimension = "platform"
            applicationId = "com.shortsblockerkids.fixture.instagram"
            resValue("string", "app_name", "Fake Instagram")
        }
        create("facebook") {
            dimension = "platform"
            applicationId = "com.shortsblockerkids.fixture.facebook"
            resValue("string", "app_name", "Fake Facebook")
        }
        create("unsupported") {
            dimension = "platform"
            applicationId = "com.example.shortvideo.fixture"
            resValue("string", "app_name", "Fake Unsupported")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
