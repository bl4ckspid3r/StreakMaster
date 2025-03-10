plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.streakmaster"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.streakmaster"
        minSdk = 30
        targetSdk = 35
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
        compose = true
    }
}

dependencies {
    // Core Android dependencies
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")

    // Jetpack Compose dependencies
    implementation("androidx.activity:activity-compose:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation(platform("androidx.compose:compose-bom:2024.02.00")) // Latest BOM

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.3.0-alpha01") // Latest version with TimePicker support
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1")

    // Jetpack Navigation for Compose
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // Vico Charts (for beautiful Compose charts)
    implementation("com.patrykandpatrick.vico:core:1.15.0")
    implementation("com.patrykandpatrick.vico:compose:1.15.0")

    // Testing dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // Debugging tools for Compose
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    // Vico Core
    implementation("com.patrykandpatrick.vico:core:1.12.0") // Update to latest version
    // Vico Compose
    implementation("com.patrykandpatrick.vico:compose:1.12.0") // Update to latest version

    // Material Icons for Compose
    implementation("androidx.compose.material:material-icons-extended:1.6.0")
}
