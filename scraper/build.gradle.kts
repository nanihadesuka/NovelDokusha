plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "my.noveldokusha.scraper"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xjvm-default=all-compatibility",
            "-opt-in=androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi",
        )
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.kotlin.compose.compilerVersion.get()
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(project(":strings"))
    implementation(project(":core"))
    implementation(project(":networking"))

    // Dependency injection
    implementation(libs.hilt.android)
    implementation(libs.hilt.workmanager)
    ksp(libs.hilt.compiler)
    ksp(libs.hilt.androidx.compiler)

    implementation(libs.compose.androidx.ui)

    implementation(libs.androidx.core.ktx)
    implementation(libs.jsoup)
    implementation(libs.gson)
    implementation(libs.okhttp)
    androidTestImplementation(libs.test.androidx.espresso.core)
}