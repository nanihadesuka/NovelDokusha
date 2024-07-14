package my.noveldoksuha.convention.plugin

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal fun Project.configureAndroid(
    commonExtension: CommonExtension<*, *, *, *, *>
) {
    commonExtension.apply {
        compileSdk = appConfig.COMPILE_SDK

        defaultConfig {
            minSdk = appConfig.MIN_SDK

            testInstrumentationRunnerArguments["clearPackageData"] = "true"
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        compileOptions {
            sourceCompatibility = appConfig.javaVersion
            targetCompatibility = appConfig.javaVersion
        }

        testOptions {
            execution = "ANDROIDX_TEST_ORCHESTRATOR"
        }
    }

    configureKotlin()
}

private fun Project.configureKotlin() {
    // Use withType to workaround https://youtrack.jetbrains.com/issue/KT-55947
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            // Set JVM target to 17
            jvmTarget = appConfig.JAVA_VERSION_STRING
            freeCompilerArgs = freeCompilerArgs + listOf(
                "-opt-in=kotlin.RequiresOptIn",
                "-Xjvm-default=all-compatibility",
            )
        }
    }
}
