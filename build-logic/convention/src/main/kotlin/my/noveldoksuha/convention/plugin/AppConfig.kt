package my.noveldoksuha.convention.plugin

import org.gradle.api.JavaVersion

internal object appConfig {
    val javaVersion = JavaVersion.VERSION_17
    const val JAVA_VERSION_STRING = "17"
    const val COMPILE_SDK = 34
    const val TARGET_SDK = COMPILE_SDK
    const val MIN_SDK = 26
}