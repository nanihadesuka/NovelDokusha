package my.noveldoksuha.convention.plugin

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

internal fun Project.configureAndroid(
    commonExtension: CommonExtension
) {
    commonExtension.apply {
        compileSdk = appConfig.COMPILE_SDK

        defaultConfig.apply {
            minSdk = appConfig.MIN_SDK

            testInstrumentationRunnerArguments["clearPackageData"] = "true"
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        buildFeatures.buildConfig = true

        compileOptions.apply {
            sourceCompatibility = appConfig.javaVersion
            targetCompatibility = appConfig.javaVersion
        }

        lint.apply {
            showAll = true
            abortOnError = false
            lintConfig = rootProject.file("lint.xml")
        }

        testOptions.execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }

    configureKotlin()
}

private fun Project.configureKotlin() {
    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(appConfig.JAVA_VERSION_STRING))
            optIn.add("kotlin.RequiresOptIn")
            // Equivalent of the former -Xjvm-default=all-compatibility
            freeCompilerArgs.add("-jvm-default=enable")
        }
    }
}
