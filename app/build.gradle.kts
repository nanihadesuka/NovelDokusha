import org.jetbrains.kotlin.konan.properties.hasProperty
import java.util.*

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
    kotlin("plugin.serialization") version("1.7.10")

}

android {

    val localPropertiesFile = file("../local.properties")

    val defaultSigningConfigData = Properties().apply {
        if (localPropertiesFile.exists())
            load(localPropertiesFile.inputStream())
    }
    val hasDefaultSigningConfigData = defaultSigningConfigData.hasProperty("storeFile")
    println("hasDefaultSigningConfigData: $hasDefaultSigningConfigData")

    compileSdk = 33

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xjvm-default=enable",
            "-opt-in=androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi",
        )

    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.2.0"
    }

    splits {
        abi {
            isEnable = true
            isUniversalApk = true
        }
    }

    defaultConfig {
        applicationId = "my.noveldokusha"
        minSdk = 26
        targetSdk = 33
        versionCode = 9
        versionName = "1.7.0"
        setProperty("archivesBaseName", "NovelDokusha_v$versionName")
    }

    signingConfigs {
        if (hasDefaultSigningConfigData) create("default") {
            storeFile = file(defaultSigningConfigData.getProperty("storeFile"))
            storePassword = defaultSigningConfigData.getProperty("storePassword")
            keyAlias = defaultSigningConfigData.getProperty("keyAlias")
            keyPassword = defaultSigningConfigData.getProperty("keyPassword")
        }
    }

    buildTypes {

        signingConfigs.asMap["default"]?.let {
            all {
                signingConfig = it
            }
        }

        named("debug") {
            postprocessing {
                isRemoveUnusedCode = false
                isObfuscate = false
                isOptimizeCode = false
                isRemoveUnusedResources = false
            }
        }

        named("release") {
            postprocessing {
                proguardFile("proguard-rules.pro")
                isRemoveUnusedCode = true
                isObfuscate = false
                isOptimizeCode = true
                isRemoveUnusedResources = true
            }
        }
    }

    productFlavors {
        flavorDimensions.add("dependencies")

        create("full") {
            dimension = "dependencies"
            dependencies {
                // Needed to have the Task -> await extension.
                fullImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4")

                // Android ML Translation Kit
                fullImplementation("com.google.mlkit:translate:17.0.1")
            }
        }

        create("foss") {
            dimension = "dependencies"
        }
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }
    namespace = "my.noveldokusha"
}

fun DependencyHandler.fullImplementation(dependencyNotation: Any): Dependency? =
    add("fullImplementation", dependencyNotation)

fun DependencyHandler.fossImplementation(dependencyNotation: Any): Dependency? =
    add("fossImplementation", dependencyNotation)

dependencies {

    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlin:kotlin-script-runtime:1.7.10")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.10")

    // Room components
    implementation("androidx.room:room-runtime:2.4.3")
    implementation("androidx.room:room-ktx:2.4.3")
    kapt("androidx.room:room-compiler:2.4.3")
    androidTestImplementation("androidx.room:room-testing:2.4.3")

    // Lifecycle components
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.5.1")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.5.1")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")

    // Preferences
    implementation("androidx.preference:preference-ktx:1.2.0")

    // UI
    implementation("androidx.appcompat:appcompat:1.5.0")
    implementation("androidx.core:core-ktx:1.9.0-rc01")
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.1")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.1")
    implementation("androidx.activity:activity-ktx:1.5.1")
    implementation("androidx.fragment:fragment-ktx:1.5.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.google.android.material:material:1.8.0-alpha01")
    implementation("com.l4digital.fastscroll:fastscroll:2.0.1")
    implementation("com.afollestad.material-dialogs:core:3.3.0")

    // Test
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")

    // Serialization
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")

    // Dependency injection
    implementation("com.google.dagger:hilt-android:2.43.1")
    kapt("com.google.dagger:hilt-compiler:2.43.1")

    // HTML text extractor
    implementation("com.chimbori.crux:crux:3.8.1")
    implementation("net.dankito.readability4j:readability4j:1.0.8")
    implementation("org.jsoup:jsoup:1.15.2")

    // Memory leak detector
    //debugImplementation("com.squareup.leakcanary:leakcanary-android:2.7")

    // Jetpack compose
    implementation("androidx.activity:activity-compose:1.5.1")
    implementation("androidx.compose.material:material:1.2.1")
    implementation("androidx.compose.animation:animation:1.2.1")
    implementation("androidx.compose.ui:ui-tooling:1.2.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.5.1")
    implementation("androidx.compose.runtime:runtime-livedata:1.2.1")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1")
    implementation("androidx.compose.material:material-icons-extended:1.2.1")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.25.0")
    implementation("com.google.accompanist:accompanist-swiperefresh:0.25.0")
    implementation("com.google.accompanist:accompanist-insets:0.25.0")
    implementation("com.google.accompanist:accompanist-pager:0.25.0")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.25.0")

    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.2.1")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.squareup.okhttp3:okhttp-brotli:4.10.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")

    // Coil for jetpack compose
    implementation("io.coil-kt:coil-compose:2.1.0")

    // Glide for jetpack compose (has more compatible formats)
    implementation("com.github.skydoves:landscapist-glide:1.6.0")
    implementation("com.github.bumptech.glide:okhttp3-integration:4.11.0")

    // Compose collapsing toolbar
    implementation("me.onebone:toolbar-compose:2.3.4")

    // Compose scroll bar
    implementation("com.github.nanihadesuka:LazyColumnScrollbar:1.5.1")

    // Compose customizable slider
    implementation("com.github.SmartToolFactory:Compose-Colorful-Sliders:1.0.2")

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")
}

kapt {
    correctErrorTypes = true
}

hilt {
    enableAggregatingTask = true
}