import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
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
        versionCode = 2
        versionName = "0.0.2-test"

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
    implementation("androidx.compose.material:material-icons-extended:1.6.3")
    implementation(libs.androidx.navigation.compose)
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
    implementation ("androidx.hilt:hilt-work:1.1.0")
    kapt ("androidx.hilt:hilt-compiler:1.1.0")

    implementation(libs.androidx.datastore.preferences)
    implementation (libs.jsoup) // Use latest version


    // For OkHttp (network requests)
    implementation(libs.okhttp)

// For JSON handling
    implementation(libs.json)
    implementation("com.google.code.gson:gson:2.10.1")

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
    implementation ("com.google.zxing:core:3.5.3")
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
    implementation("com.google.android.gms:play-services-safetynet:18.0.1")
    implementation("com.google.android.play:integrity:1.3.0")

    // Google Sheets API (kept for future use)
    implementation("com.google.apis:google-api-services-sheets:v4-rev20220927-2.0.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")
    implementation("com.google.api-client:google-api-client-android:2.2.0")
    implementation("com.google.api-client:google-api-client-gson:2.2.0")

    // Coil for image loading
    implementation(libs.coil.compose)

    // Biometric authentication
    implementation(libs.androidx.biometric)
    implementation("org.jetbrains.kotlin:kotlin-reflect")


}
