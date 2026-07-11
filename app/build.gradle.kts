import org.jetbrains.kotlin.konan.properties.hasProperty
import java.util.Properties

plugins {
    alias(libs.plugins.noveldokusha.android.application)
    alias(libs.plugins.noveldokusha.android.compose)
    alias(libs.plugins.kotlin.parcelize)
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

    if (cliCustomSettings.splitByAbi) splits {
        abi {
            isEnable = true
            isUniversalApk = cliCustomSettings.splitByAbiDoUniversal
        }
    }

    defaultConfig {
        // IMPORTANT: applicationId MUST stay "com.paras.noveldokusha".
        // Do NOT revert to "my.noveldokusha" — the v2.2.x release line is published
        // under this ID and changing it breaks update compatibility and forces a
        // fresh install on every device. See BUILD.md §Precautions.
        applicationId = "com.paras.noveldokusha"
        versionCode = 31
        versionName = "3.0.1"
        setProperty("archivesBaseName", "ParasDokusha_v$versionName")
    }

    signingConfigs {
        if (hasDefaultSigningConfigData) create("default") {
            storeFile = file(defaultSigningConfigData.getProperty("storeFile"))
            storePassword = defaultSigningConfigData.getProperty("storePassword")
            keyAlias = defaultSigningConfigData.getProperty("keyAlias")
            keyPassword = defaultSigningConfigData.getProperty("keyPassword")
            // AGP 8.2 ignores these flags when minSdk >= 24 — release APKs MUST be
            // re-signed manually with apksigner (v1+v2) to keep TV/OEM package
            // installers working. See BUILD.md §Signing. The flags are kept here
            // as documentation; they do not override AGP's default behavior.
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = false
            enableV4Signing = false
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
        }

        create("foss") {
            dimension = "dependencies"
        }
    }

    namespace = "my.noveldokusha"

    // Disable lint on release builds to avoid OOM on memory-constrained
    // build environments. Lint is still run in CI via the dedicated
    // lint workflows.
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {

    implementation(projects.tooling.localDatabase)
    implementation(projects.tooling.epubParser)
    implementation(projects.tooling.textTranslator.domain)
    implementation(projects.tooling.textToSpeech)
    implementation(projects.tooling.epubImporter)
    implementation(projects.tooling.applicationWorkers)
    implementation(projects.tooling.localSource)

    implementation(projects.features.reader)
    implementation(projects.features.chaptersList)
    implementation(projects.features.globalSourceSearch)
    implementation(projects.features.databaseExplorer)
    implementation(projects.features.sourceExplorer)
    implementation(projects.features.catalogExplorer)
    implementation(projects.features.libraryExplorer)
    implementation(projects.features.settings)
    implementation(projects.features.webview)

    implementation(projects.data)
    implementation(projects.core)
    implementation(projects.coreui)
    implementation(projects.navigation)
    implementation(projects.networking)
    implementation(projects.strings)
    implementation(projects.scraper)

    // Translation feature — both flavors use the cloud translator (no MLKit)
    implementation(projects.tooling.textTranslator.translatorNop)

    // Kotlin
    implementation(libs.kotlinx.coroutines.core)

    // Android SDK
    implementation(libs.androidx.workmanager)
    implementation(libs.androidx.startup)

    // UI
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

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

    // Jetpack compose
    implementation(libs.compose.androidx.activity)
    implementation(libs.compose.androidx.animation)
    implementation(libs.compose.material3.android)
    implementation(libs.compose.coil)

    // Networking
    implementation(libs.okhttp.glideIntegration)

    // Logging
    implementation(libs.timber)
}

hilt {
    enableAggregatingTask = true
}

