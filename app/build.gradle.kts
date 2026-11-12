// Declares plugins applied to the Android app.
plugins {
    // Android application build support.
    alias(libs.plugins.android.application)
    // Firebase Google Services configuration.
    alias(libs.plugins.google.services)
    alias(libs.plugins.ksp)

}

val marketplaceApiBaseUrl = providers.gradleProperty("MARKETPLACE_API_BASE_URL")
    .orElse(providers.environmentVariable("MARKETPLACE_API_BASE_URL"))
    .orElse("http://192.168.0.244:3000/")

android {
    namespace = "com.studentmarketplace"
    compileSdk {
        version = release(37)
    }
    defaultConfig {
        applicationId = "com.studentmarketplace"
        minSdk = 24
        buildConfigField("String", "MARKETPLACE_API_BASE_URL", "\"${marketplaceApiBaseUrl.get()}\"")
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures{
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    // Navigation Components
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    // Adds Firebase Authentication
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    // Testing Components
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}