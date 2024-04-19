import com.android.build.gradle.internal.tasks.ValidateSigningTask

plugins {
    kotlin("multiplatform")
//    alias(libs.plugins.androidApplication)
    id("com.android.application")
    id("io.github.kmbisset89.azurekeystore.plugin")
}

kotlin {

    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }
    jvm()
    sourceSets {

        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("androidx.core:core-ktx:1.6.0")
                implementation("androidx.appcompat:appcompat:1.3.1")
                implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.3.1")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2")
            }
        }
    }
}

android {
    namespace = "com.example"

    compileSdk = 30
    defaultConfig {
        applicationId = "com.example"
        minSdk = 21
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.create("config").apply {
                storeFile = rootProject.ext.get("keystoreFile") as File
                storePassword = rootProject.ext.get("storePassword") as String
                keyAlias = rootProject.ext.get("keyAlias") as String
                keyPassword = rootProject.ext.get("keyPassword") as String
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}





