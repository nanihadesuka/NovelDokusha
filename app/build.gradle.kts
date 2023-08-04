import org.jetbrains.kotlin.konan.properties.hasProperty
import java.util.Properties

plugins {
    alias(libs.plugins.android.app)
    alias(libs.plugins.android.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.ksp)
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
        kotlinCompilerExtensionVersion = libs.versions.compose.core.orNull
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
                fullImplementation(libs.kotlinx.coroutines.playServices)

                // Android ML Translation Kit
                fullImplementation(libs.translate)
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
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.script)

    // Room components
    implementation(libs.androidx.room.core)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.testing)

    // Lifecycle components
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.common)

    // Preferences
    implementation(libs.androidx.preference.ktx)

    // Local storage directory access
    implementation(libs.androidx.documentfile)

    // UI
    implementation(libs.android.material)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.fragment.ktx)


    // Media session controls
    implementation(libs.androidx.media)

    // Test
    testImplementation(libs.tests.junit)
    testImplementation(libs.tests.mockito)
    androidTestImplementation(libs.tests.androidx.core)
    androidTestImplementation(libs.tests.androidx.junit)
    androidTestImplementation(libs.tests.androidx.espresso)
    androidTestImplementation(libs.tests.androidx.compose)
    androidTestImplementation(libs.tests.androidx.rules)
    androidTestImplementation(libs.tests.androidx.runner)
    androidTestUtil(libs.tests.androidx.util)

    // Serialization
    implementation(libs.serialization.kotlin.json)
    implementation(libs.serialization.gson)
    implementation(libs.serialization.moshi)
    implementation(libs.serialization.moshi.kotlin)

    // Dependency injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // HTML text extractor
    implementation(libs.html.crux)
    implementation(libs.html.readability4j)
    implementation(libs.html.jsoup)

    // Jetpack compose
    implementation(libs.compose.activity)
    implementation(libs.compose.material3)
    implementation(libs.compose.animation)
    implementation(libs.compose.tooling)
    implementation(libs.compose.livedata)
    implementation(libs.compose.viewmodel)
    implementation(libs.compose.contrainLayout)
    implementation(libs.compose.materiaIconsExtended)
    implementation(libs.compose.systemuicontroller)
    implementation(libs.compose.swiperefresh)
    implementation(libs.compose.insets)
    implementation(libs.compose.pager)
    implementation(libs.compose.pagerIndicator)
    implementation(libs.compose.coil)
    implementation(libs.compose.glide)
    implementation(libs.compose.lazyColumnScrollbar)

    // Networking
    implementation(libs.networking.okhttp)
    implementation(libs.networking.okhttp.brotli)
    implementation(libs.networking.okhttp.interceptor)
    implementation(libs.networking.okhttp.glideIntegration)
    implementation(libs.networking.retrofit)

    // Logging
    implementation(libs.logging.timber)
}

hilt {
    enableAggregatingTask = true
}
