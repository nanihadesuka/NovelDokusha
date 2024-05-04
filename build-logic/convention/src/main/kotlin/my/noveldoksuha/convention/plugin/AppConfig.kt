package my.noveldoksuha.convention.plugin

import org.gradle.api.JavaVersion

internal object appConfig {
    val javaVersion = JavaVersion.VERSION_17
    const val javaVersionString = "17"
    const val compileSdk = 34
    const val targetSdk = compileSdk
    const val minSdk = 26
}