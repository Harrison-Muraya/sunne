plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.streaming.sunne"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.streaming.sunne"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
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
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.leanback)
    implementation(libs.glide)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Material Design
    implementation("com.google.android.material:material:1.12.0")

    // Kodein Dependency Injection
    implementation("org.kodein.di:kodein-di-framework-android-x:7.21.2")

    // Room Database
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    annotationProcessor("androidx.room:room-compiler:2.8.4")

    // Navigation for Compose
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // Networking - Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}






//plugins {
//    alias(libs.plugins.android.application)
//    alias(libs.plugins.kotlin.android)
//    alias(libs.plugins.kotlin.compose)
//    id("kotlin-kapt")
//    alias(libs.plugins.android.navigation.safeargs)
//}
//
//android {
//    namespace = "com.streaming.sunne"
//    compileSdk = 36
//
//    defaultConfig {
//        applicationId = "com.streaming.sunne"
//        minSdk = 23
//        targetSdk = 36
//        versionCode = 1
//        versionName = "1.0"
//    }
//
//    buildTypes {
//        release {
//            isMinifyEnabled = false
//            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro"
//            )
//        }
//    }
//
//    dataBinding {
//        enable = true
//    }
//
//    compileOptions {
//        sourceCompatibility = JavaVersion.VERSION_17
//        targetCompatibility = JavaVersion.VERSION_17
//    }
//
//    buildFeatures {
//        compose = true
//    }
//}
//
//kotlin {
//    jvmToolchain(17)
//}
//
//dependencies {
//    implementation(libs.androidx.core.ktx)
//    implementation(libs.androidx.appcompat)
//    implementation(platform(libs.androidx.compose.bom))
//    implementation(libs.androidx.compose.ui)
//    implementation(libs.androidx.compose.ui.graphics)
//    implementation(libs.androidx.compose.ui.tooling.preview)
//    implementation(libs.androidx.tv.foundation)
//    implementation(libs.androidx.tv.material)
//    implementation(libs.androidx.lifecycle.runtime.ktx)
//    implementation(libs.androidx.activity.compose)
//    androidTestImplementation(platform(libs.androidx.compose.bom))
//    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
//    debugImplementation(libs.androidx.compose.ui.tooling)
//    debugImplementation(libs.androidx.compose.ui.test.manifest)
//
//    // ViewModel
//    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
//    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
//
//    // Material Design
//    implementation("com.google.android.material:material:1.12.0")
//
//    // Kodein Dependency Injection
//    implementation("org.kodein.di:kodein-di-framework-android-x:7.21.2")
//
//    // Room Database
//    implementation("androidx.room:room-runtime:2.8.4")
//    implementation("androidx.room:room-ktx:2.8.4")
//    kapt("androidx.room:room-compiler:2.8.4")
//
//    // Navigation
//    implementation("androidx.navigation:navigation-fragment-ktx:2.8.4")
//    implementation("androidx.navigation:navigation-ui-ktx:2.8.4")
//
//    // Networking - Retrofit
//    implementation("com.squareup.retrofit2:retrofit:2.9.0")
//    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
//    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
//
//    // Coroutines
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
//}