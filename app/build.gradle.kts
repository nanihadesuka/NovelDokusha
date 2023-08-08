import org.jetbrains.kotlin.konan.properties.hasProperty
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.scripting)
    alias(libs.plugins.kotlin.serialization)

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
            "-Xjvm-default=all-compatibility",
            "-opt-in=androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi",
        )
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.kotlin.compilerVersion.get()
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
    implementation(libs.kotlin.script.runtime)
    implementation(libs.kotlin.stdlib)

    // Room components
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    androidTestImplementation(libs.androidx.room.testing)

    // Lifecycle components
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.common.java8)
    implementation(libs.androidx.coordinatorlayout)

    // Preferences
    implementation(libs.androidx.preference.ktx)

    // Local storage directory access
    implementation(libs.androidx.documentfile)

    // UI
    implementation(libs.androidx.appcompat)
    implementation(libs.test.androidx.core.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)

    // Media session controls
    implementation(libs.androidx.media)

    // Test
    testImplementation(libs.test.junit)
    testImplementation(libs.test.mockito.kotlin)

    // e2e test
    androidTestImplementation(libs.test.androidx.core.ktx)
    androidTestImplementation(libs.test.androidx.junit.ktx)
    androidTestImplementation(libs.test.androidx.espresso.core)
    androidTestImplementation(libs.compose.androidx.ui.test.junit4)
    androidTestImplementation(libs.test.androidx.rules)
    androidTestImplementation(libs.test.androidx.runner)
    androidTestUtil(libs.test.androidx.orchestrator)

    // Serialization
    implementation(libs.gson)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)

    // Dependency injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // HTML text extractor
    implementation(libs.crux)
    implementation(libs.readability4j)
    implementation(libs.jsoup)

    // Jetpack compose
    implementation(libs.compose.androidx.activity)
    implementation(libs.compose.androidx.material3)
    implementation(libs.compose.androidx.animation)
    implementation(libs.compose.androidx.ui.tooling)
    implementation(libs.compose.androidx.runtime.livedata)
    implementation(libs.compose.androidx.lifecycle.viewmodel)
    implementation(libs.compose.androidx.constraintlayout)
    implementation(libs.compose.androidx.material.icons.extended)
    implementation(libs.compose.accompanist.systemuicontroller)
    implementation(libs.compose.accompanist.swiperefresh)
    implementation(libs.compose.accompanist.insets)
    implementation(libs.compose.accompanist.pager)
    implementation(libs.compose.accompanist.pager.indicators)
    implementation(libs.compose.landscapist.glide)
    implementation(libs.compose.coil)
    implementation(libs.compose.lazyColumnScrollbar)

    // Networking
    implementation(libs.okhttp)
    implementation(libs.okhttp.interceptor.brotli)
    implementation(libs.okhttp.interceptor.logging)
    implementation(libs.okhttp.glideIntegration)

    // Logging
    implementation(libs.timber)
}

hilt {
    enableAggregatingTask = true
}
