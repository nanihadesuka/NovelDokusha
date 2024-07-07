plugins {
    alias(libs.plugins.noveldokusha.android.library)
    alias(libs.plugins.noveldokusha.android.compose)
}

android {
    namespace = "my.noveldokusha.tooling.application_workers"
}

dependencies {
    implementation(projects.core)
    implementation(projects.coreui)
    implementation(projects.strings)
    implementation(projects.data)
    implementation(projects.navigation)
    implementation(projects.tooling.localDatabase)

    implementation(libs.timber)
    implementation(libs.androidx.workmanager)
    implementation(libs.hilt.workmanager)

    implementation(libs.compose.androidx.activity)
    implementation(libs.compose.material3.android)
    implementation(libs.compose.landscapist.glide)
}
