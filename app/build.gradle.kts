plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

kotlin {
    jvmToolchain(21)
}

android {
    namespace = "com.akil.privacyshield"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.akil.privacyshield"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // ═══════════════════════════════════════════════════════════
    // CORE ANDROID
    // ═══════════════════════════════════════════════════════════
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // ═══════════════════════════════════════════════════════════
    // JETPACK COMPOSE
    // ═══════════════════════════════════════════════════════════
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.compose.material3.window.size)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // ═══════════════════════════════════════════════════════════
    // ACTIVITY & LIFECYCLE
    // ═══════════════════════════════════════════════════════════
    implementation(libs.activity.compose)
    implementation(libs.bundles.lifecycle)

    // ═══════════════════════════════════════════════════════════
    // NAVIGATION
    // ═══════════════════════════════════════════════════════════
    implementation(libs.navigation.compose)

    // ═══════════════════════════════════════════════════════════
    // YUKIHOOKAPI (Modern Kotlin Hook Framework)
    // ═══════════════════════════════════════════════════════════
    implementation(libs.yukihookapi.api)
    ksp(libs.yukihookapi.ksp.xposed)
    compileOnly(libs.xposed.api) // Provided at runtime by Xposed framework

    // ═══════════════════════════════════════════════════════════
    // DATA STORAGE
    // ═══════════════════════════════════════════════════════════
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)

    // ═══════════════════════════════════════════════════════════
    // COROUTINES
    // ═══════════════════════════════════════════════════════════
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // ═══════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════
    implementation(libs.timber)
    implementation(libs.coil.compose)

    // ═══════════════════════════════════════════════════════════
    // TESTING
    // ═══════════════════════════════════════════════════════════
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}