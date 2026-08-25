plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.gios.brightjump"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.gios.brightjump"
        minSdk = 29
        targetSdk = 35
        // CI overwrites both from the workflow run number; see .github/workflows/build.yml
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("../keystore/brightjump.jks")
            storePassword = "brightjump"
            keyAlias = "brightjump"
            keyPassword = "brightjump"
        }
    }

    buildTypes {
        release {
            // Nothing to shrink: no Compose, no Room, no reflection, four source files. R8 would
            // spend a minute of CI to save a few kilobytes and add a class of failure that only
            // shows up on the phone.
            isMinifyEnabled = false
            // Same committed key as debug, so either APK upgrades over the other.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // No androidx at all. The activity extends android.app.Activity, finishes in onCreate, and
    // never inflates a layout, so AppCompat and core-ktx would be dead weight in a 20 kB APK.
    testImplementation("junit:junit:4.13.2")
}
