plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.moonpaw"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.moonpaw"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Navigation Component
    implementation("androidx.navigation:navigation-fragment:2.8.3")
// Hoặc phiên bản mới nhất
    implementation("androidx.navigation:navigation-ui:2.8.3")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.airbnb.android:lottie:6.1.0")
}