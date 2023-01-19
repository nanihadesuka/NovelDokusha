plugins {
    id("java-library")
    id("kotlin")
    id("org.jetbrains.kotlin.kapt")
    id("com.android.lint")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val lintVersion = "${7+23}.2.2" // AGPluginVersionPlusSeven
val autoServiceVersion = "1.0-rc7"

dependencies {
    compileOnly("com.android.tools.lint:lint-api:$lintVersion")
    compileOnly("com.android.tools.lint:lint-checks:$lintVersion")
    compileOnly("com.google.auto.service:auto-service-annotations:$autoServiceVersion")
    kapt("com.google.auto.service:auto-service:1.0-rc7")
}