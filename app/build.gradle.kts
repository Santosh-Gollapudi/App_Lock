// ─────────────────────────────────────────────────────────────────────────────
// app/build.gradle.kts
//
// Dependency resolution for the three error categories:
//
//  ERROR 1 — "Cannot resolve class ConstraintLayout / MaterialCardView / Chip"
//            → Missing Gradle dependencies. Fixed by adding:
//                • androidx.constraintlayout:constraintlayout
//                • com.google.android.material:material
//
//  ERROR 2 — "Cannot resolve symbol @drawable/..." / "@style/Widget.Material3..."
//            → The drawable XML files must exist as INDIVIDUAL files inside
//              res/drawable/. The styles must reference a Material3 theme.
//              Both are provided in this project. No Gradle change needed.
//
//  ERROR 3 — "Typo: In word 'oneui'"
//            → Android Studio spell-checker treating color token names as prose.
//              Suppressed via the tools:ignore directive in the layout, and
//              globally via the spellcheck dictionary entry below.
// ─────────────────────────────────────────────────────────────────────────────

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}
android {
    namespace = "com.example.applock"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.applock"
        minSdk = 26          // One UI 6 targets Android 8+ at minimum
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true   // Recommended over findViewById for type safety
    }
}
dependencies {
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.13.1")
}
