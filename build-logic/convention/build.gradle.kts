import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "my.noveldokusha.convention.plugin"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.android.tools.common)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "noveldokusha.android.application"
            implementationClass =
                "NoveldokushaAndroidApplicationBestPracticesConventionPlugin" // :)
        }
        register("androidLibrary") {
            id = "noveldokusha.android.library"
            implementationClass =
                "NoveldokushaAndroidLibraryBestPracticesConventionPlugin" // ;)
        }
        register("androidCompose") {
            id = "noveldokusha.android.compose"
            implementationClass =
                "NoveldokushaAndroidComposeBestPracticesConventionPlugin" // :)
        }
    }
}
