plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.mediconnect_android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.mediconnect_android"
        minSdk = 30
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
    }
    dataBinding {
        enable = true
    }
}

dependencies {
    implementation(libs.jackson.databind)
    implementation (libs.glide)
    annotationProcessor (libs.compiler)
    implementation (libs.unirest.java)
    implementation (libs.gson)
    implementation(libs.okhttpclient)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}