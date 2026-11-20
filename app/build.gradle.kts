plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val keystoreFile = providers.environmentVariable("KEYSTORE_FILE").orNull
val keystorePassword = providers.environmentVariable("KEYSTORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("KEY_PASSWORD").orNull

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

    val releaseSigning = if (!keystoreFile.isNullOrBlank() && !keystorePassword.isNullOrBlank()
        && !releaseKeyAlias.isNullOrBlank() && !releaseKeyPassword.isNullOrBlank()
    ) {
        signingConfigs.create("release") {
            storeFile = rootProject.file(keystoreFile)
            storePassword = keystorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
    } else {
        null
    }

    buildTypes {
        release {
            // Nothing to shrink: no Compose, no Room, no reflection, four source files. R8 would
            // spend a minute of CI to save a few kilobytes and add a class of failure that only
            // shows up on the phone.
            isMinifyEnabled = false
            signingConfig = releaseSigning
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
