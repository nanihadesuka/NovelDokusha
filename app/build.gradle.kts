import org.jetbrains.kotlin.konan.properties.hasProperty
import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
    kotlin("plugin.serialization") version ("1.8.10")

}

inner class CLICustomSettings {
    val splitByAbi = propExist(key = "splitByAbi")
    val splitByAbiDoUniversal = splitByAbi && propExist(key = "splitByAbiDoUniversal")
    val localPropertiesFilePath = propString(
        key = "localPropertiesFilePath",
        default = "local.properties"
    )

    private fun propExist(key: String) = project.hasProperty(key)
    private fun propString(key: String, default: String) =
        project.properties[key]?.toString()?.ifBlank { default } ?: default
}

val cliCustomSettings = CLICustomSettings()

android {

    val localPropertiesFile = rootProject.file(cliCustomSettings.localPropertiesFilePath)
    println("localPropertiesFilePath: ${cliCustomSettings.localPropertiesFilePath}")

    val defaultSigningConfigData = Properties().apply {
        if (localPropertiesFile.exists())
            load(localPropertiesFile.inputStream())
    }
    val hasDefaultSigningConfigData = defaultSigningConfigData.hasProperty("storeFile")
    println("hasDefaultSigningConfigData: $hasDefaultSigningConfigData")

    compileSdk = 33

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xjvm-default=enable",
            "-opt-in=androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi",
        )
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.4"
    }

    if (cliCustomSettings.splitByAbi) splits {
        abi {
            isEnable = true
            isUniversalApk = cliCustomSettings.splitByAbiDoUniversal
        }
    }

    defaultConfig {
        applicationId = "my.noveldokusha"
        minSdk = 26
        targetSdk = 33
        versionCode = 13
        versionName = "2.0.1"
        setProperty("archivesBaseName", "NovelDokusha_v$versionName")

        testInstrumentationRunnerArguments["clearPackageData"] = "true"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
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
            // Having the dependencies here the same in the main scope, visually separated
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("org.jetbrains.kotlin:kotlin-script-runtime:1.8.10")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.20")

    // Room components
    implementation("androidx.room:room-runtime:2.5.2")
    implementation("androidx.room:room-ktx:2.5.2")
    ksp("androidx.room:room-compiler:2.5.2")
    androidTestImplementation("androidx.room:room-testing:2.5.2")

    // Lifecycle components
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.6.1")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")

    // Preferences
    implementation("androidx.preference:preference-ktx:1.2.0")

    // Local storage directory access
    implementation("androidx.documentfile:documentfile:1.0.1")

    // UI
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.navigation:navigation-fragment-ktx:2.6.0")
    implementation("androidx.navigation:navigation-ui-ktx:2.6.0")
    implementation("androidx.activity:activity-ktx:1.7.2")
    implementation("androidx.fragment:fragment-ktx:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.9.0")


    // Media session controls
    implementation("androidx.media:media:1.6.0")

    // Test
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")

    // e2e test
    androidTestImplementation("androidx.test:core-ktx:1.5.0")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.4.3")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test:runner:1.5.2")

    androidTestUtil("androidx.test:orchestrator:1.4.2")


    // Serialization
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.moshi:moshi:1.14.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.14.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")

    // Dependency injection
    implementation("com.google.dagger:hilt-android:2.45")
    ksp("com.google.dagger:hilt-compiler:2.45")

    // HTML text extractor
    implementation("com.chimbori.crux:crux:3.12.0")
    implementation("net.dankito.readability4j:readability4j:1.0.8")
    implementation("org.jsoup:jsoup:1.15.4")

    // Memory leak detector
    //debugImplementation("com.squareup.leakcanary:leakcanary-android:2.7")

    // Jetpack compose
    implementation("androidx.activity:activity-compose:1.7.2")
    implementation("androidx.compose.material3:material3:1.1.1")
    implementation("androidx.compose.animation:animation:1.4.3")
    implementation("androidx.compose.ui:ui-tooling:1.4.3")
    implementation("androidx.compose.runtime:runtime-livedata:1.4.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1")
    implementation("androidx.compose.material:material-icons-extended:1.4.3")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.30.0")
    implementation("com.google.accompanist:accompanist-swiperefresh:0.30.0")
    implementation("com.google.accompanist:accompanist-insets:0.30.0")
    implementation("com.google.accompanist:accompanist-pager:0.30.0")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.30.0")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.11")
    implementation("com.squareup.okhttp3:okhttp-brotli:5.0.0-alpha.11")
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.11")

    // Coil for jetpack compose
    implementation("io.coil-kt:coil-compose:2.2.2")

    // Glide for jetpack compose (has more compatible formats)
    implementation("com.github.skydoves:landscapist-glide:2.1.9")
    implementation("com.github.bumptech.glide:okhttp3-integration:4.15.1")

    // Compose collapsing toolbar
    implementation("me.onebone:toolbar-compose:2.3.5")

    // Compose scroll bar
    implementation("com.github.nanihadesuka:LazyColumnScrollbar:1.6.3")

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")
}

hilt {
    enableAggregatingTask = true
}
