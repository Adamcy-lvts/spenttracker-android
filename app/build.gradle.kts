plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.spenttracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.spenttracker"
        minSdk = 24
        targetSdk = 36
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
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Core Android libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    
    // UI libraries
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")
    
    // Splash Screen API
    implementation("androidx.core:core-splashscreen:1.0.1")
    
    // Room database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    
    // Hilt dependency injection
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    
    // Network dependencies
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    
    // DataStore for secure preferences storage
    implementation(libs.datastore.preferences)
    
    // Security for encrypted preferences
    implementation(libs.androidx.security.crypto)
    
    // WorkManager for background tasks
    implementation(libs.workmanager)
    implementation(libs.workmanager.hilt)
    
    // Google Play Services Location & Maps
    implementation(libs.gms.location)
    implementation(libs.gms.maps)
    
    // Google Places API (New SDK with SearchNearby support)
    implementation(libs.gms.places)
    
    // Coroutines support for Play Services
    implementation(libs.coroutines.play.services)
    
    // Font Awesome Icons for beautiful UI
    implementation(libs.compose.icons.font.awesome)
    
    // Testing libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// KSP configuration for Room
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}