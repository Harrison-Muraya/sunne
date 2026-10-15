plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

/** API server; override with `-PSUNNE_BASE_URL=https://host/` or in gradle.properties. Must end with "/". */
val sunneBaseUrl: String = (findProperty("SUNNE_BASE_URL") as String?) ?: "http://197.248.201.233:30000/"

android {
    namespace = "com.streaming.sunne"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.streaming.sunne"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "BASE_URL", "\"$sunneBaseUrl\"")
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
        compose = true
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // ── Compose ────────────────────────────────────────────────────────────────
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended:1.6.0")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ── ViewModel ──────────────────────────────────────────────────────────────
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")

    // ── Navigation ─────────────────────────────────────────────────────────────
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // ── Networking ─────────────────────────────────────────────────────────────
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // ── Coroutines ─────────────────────────────────────────────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // ── Image Loading ──────────────────────────────────────────────────────────
    implementation("io.coil-kt:coil-compose:2.6.0")

    // ── System UI ──────────────────────────────────────────────────────────────
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.31.4-beta")

    // ── ExoPlayer ──────────────────────────────────────────────────────────────
    implementation("com.google.android.exoplayer:exoplayer:2.19.0")
}
