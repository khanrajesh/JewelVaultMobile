import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.gms.google-services")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}

// Load keystore properties
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.velox.jewelvault"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.velox.jewelvault"
        minSdk = 29
        targetSdk = 35
        versionCode = 13
        versionName = "0.0.13-test"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/ASL2.0"
            excludes += "META-INF/*.kotlin_module"
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

//for room database
kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.material)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.junit.ktx)
    implementation(libs.androidx.room.testing.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Hilt + Compose integration
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation (libs.androidx.work.runtime.ktx)
    implementation (libs.androidx.hilt.work)
    kapt (libs.androidx.hilt.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation (libs.jsoup) // Use latest version


    // For OkHttp (network requests)
    implementation(libs.okhttp)

// For JSON handling
    implementation(libs.json)
    implementation(libs.gson)

// For Kotlin Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // CameraX
    implementation (libs.androidx.camera.core)
    implementation (libs.androidx.camera.camera2)
    implementation (libs.androidx.camera.lifecycle)
    implementation (libs.androidx.camera.view)

    
    // Apache POI for Excel processing
    implementation (libs.poi)
    implementation (libs.poi.ooxml)
    implementation (libs.poi.scratchpad)

    // Optional for Android compatibility
    implementation(libs.xmlbeans)
    implementation(libs.commons.collections4)


// ML Kit Barcode Scanning
    implementation (libs.barcode.scanning)

    // ZXing for QR code generation
    implementation (libs.core)
    implementation (libs.zxing.android.embedded)

    implementation (libs.exp4j)
    implementation (libs.androidx.lifecycle.extensions)

    //room
    implementation (libs.androidx.room.runtime)
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    // Testing
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.core)


    implementation( libs.accompanist.permissions) // For permission

    //firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.config)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.kotlinx.coroutines.play.services)
    
    // SafetyNet/Play Integrity API for reCAPTCHA verification
    implementation(libs.play.services.safetynet)
    implementation(libs.integrity)

    // Google Sheets API (kept for future use)
    implementation(libs.google.api.services.sheets)
    implementation(libs.google.auth.library.oauth2.http)
    implementation(libs.google.api.client.android)
    implementation(libs.google.api.client.gson)

    // Coil for image loading
    implementation(libs.coil.compose)

    // Biometric authentication
    implementation(libs.androidx.biometric)
    implementation(libs.kotlin.reflect)


}
