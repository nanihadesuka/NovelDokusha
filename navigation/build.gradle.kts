plugins {
    alias(libs.plugins.noveldokusha.android.library)
}

android {
    namespace = "my.noveldokusha.navigation"
}

dependencies {
    implementation(projects.core)
    implementation(projects.tooling.localDatabase)
}