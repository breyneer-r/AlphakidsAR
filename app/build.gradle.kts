plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // No usamos Compose, solo Views + Fragments
}

android {
    namespace = "com.example.alphakidsar"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.alphakidsar"
        minSdk = 24 // mínimo para ARCore
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        // Compose deshabilitado, usamos ArFragment (basado en Views)
        // compose = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // === DEPENDENCIAS BÁSICAS DE ANDROID ===
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.fragment:fragment-ktx:1.7.1")

    // === REALIDAD AUMENTADA (ARCore + Sceneform) ===
    implementation("com.google.ar:core:1.42.0")

    // Sceneform actualizado (fork de Thomas Gorisse, compatible con SDK 34)
    implementation("com.gorisse.thomas.sceneform:sceneform:1.23.0")
    implementation("com.gorisse.thomas.sceneform:sceneform-base:1.23.0")
    implementation("com.gorisse.thomas.sceneform:sceneform-ux:1.23.0")
    implementation("com.gorisse.thomas.sceneform:sceneform-assets:1.23.0")

    // === RECONOCIMIENTO DE TEXTO (ML Kit) ===
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.mlkit:vision-common:17.3.0")

    // === TESTING ===
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
