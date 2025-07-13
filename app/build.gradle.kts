plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.velox.jewelvault"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.velox.jewelvault"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
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


    implementation(libs.androidx.datastore.preferences)
    implementation (libs.jsoup) // Use latest version


    // For OkHttp (network requests)
    implementation(libs.okhttp)

// For JSON handling
    implementation(libs.json)

// For Kotlin Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // CameraX
    implementation (libs.androidx.camera.core)
    implementation (libs.androidx.camera.camera2)
    implementation (libs.androidx.camera.lifecycle)
    implementation (libs.androidx.camera.view)
    implementation ("androidx.work:work-runtime-ktx:2.10.2")


// ML Kit Barcode Scanning
    implementation (libs.barcode.scanning)

    implementation (libs.exp4j)
    implementation ("androidx.lifecycle:lifecycle-extensions:2.2.0")

    //room
    implementation ("androidx.room:room-runtime:2.7.1")
    kapt("androidx.room:room-compiler:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")


    // Testing
    androidTestImplementation("androidx.room:room-testing:2.7.1")
    androidTestImplementation("androidx.test:core:1.5.0")

    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3")
    // Optional for Android compatibility
    implementation("org.apache.xmlbeans:xmlbeans:5.1.1")
    implementation("org.apache.commons:commons-collections4:4.4")

    implementation("com.itextpdf:itext7-core:7.2.3") // For PDF generation

    implementation ("org.apache.poi:poi:5.2.3") // For .xls
    implementation ("org.apache.poi:poi-ooxml:5.2.3")    // For .xlsx

    implementation( "com.google.accompanist:accompanist-permissions:0.33.0-alpha") // For permission

    //firebase
    implementation(platform("com.google.firebase:firebase-bom:33.16.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation(libs.firebase.config)
    implementation(libs.firebase.firestore.ktx)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Coil for image loading
    implementation(libs.coil.compose)

    // Biometric authentication
    implementation("androidx.biometric:biometric:1.1.0")

}
