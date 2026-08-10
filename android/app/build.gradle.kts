plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "org.teww.tew.app"
    compileSdk = 36

    signingConfigs {
        // A debug key committed to the repo, deliberately.
        //
        // AGP otherwise generates a fresh debug keystore per machine and per
        // CI runner. Two builds from different runs are then signed by
        // different keys, and Android refuses to install one over the other
        // (INSTALL_FAILED_UPDATE_INCOMPATIBLE) — the tester has to uninstall
        // first, and the error does not say so. With one shared key every
        // build upgrades cleanly from any other.
        //
        // This is NOT a secret. The password is the well-known Android debug
        // password, the key signs nothing but test builds, and Android's own
        // default debug key is public knowledge. It must never sign a
        // release; release signing is still an open decision in STATE.md.
        getByName("debug") {
            storeFile = file("../debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"

            // v2 only, and that is correct rather than a gap: v1 (JAR)
            // signing exists for Android 6 and below, and minSdk here is 26.
            // AGP 9 drops v1 at minSdk >= 24 regardless of what is asked for,
            // which is why there is no enableV1Signing line — it would read
            // as a setting that works.
            enableV2Signing = true
        }
    }

    defaultConfig {
        applicationId = "org.teww.tew"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            // Release signing config intentionally not set up yet — see
            // STATE.md. Release builds aren't possible until that lands.
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":feature-radio"))
    implementation(project(":feature-carddeck"))
    implementation(project(":feature-account"))
    implementation(project(":feature-record"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}
