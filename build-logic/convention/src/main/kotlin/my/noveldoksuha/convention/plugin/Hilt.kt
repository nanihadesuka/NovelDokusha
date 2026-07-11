package my.noveldoksuha.convention.plugin

import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.applyKSP() {
    with(pluginManager) {
        apply("com.google.devtools.ksp")
    }
}

internal fun Project.applyHilt() {
    applyKSP()

    with(pluginManager) {
        apply("com.google.dagger.hilt.android")
    }
    dependencies {
        implementation(libs.findLibrary("hilt.android").get())
        "ksp"(libs.findLibrary("hilt-compiler").get())
        "ksp"(libs.findLibrary("hilt-androidx-compiler").get())
    }
}