plugins {
    alias(libs.plugins.noveldokusha.android.library)
}

android {
    namespace = "my.noveldokusha.lua_engine"
}

dependencies {
    implementation(projects.core)
    implementation(projects.networking)
    implementation(projects.strings)

    implementation(libs.jsoup)
    implementation(libs.gson)
    implementation(libs.luaj)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.timber)
}
