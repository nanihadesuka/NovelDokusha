plugins {
    alias(libs.plugins.noveldokusha.android.library)
    alias(libs.plugins.noveldokusha.android.compose)
}

android {
    namespace = "my.noveldokusha.tooling.backup_restore"
}

dependencies {
    implementation(projects.core)
    implementation(projects.coreui)
    implementation(projects.strings)
    implementation(projects.data)
    implementation(projects.tooling.localDatabase)

    implementation(libs.timber)
    implementation(libs.compose.androidx.activity)
    implementation(libs.okhttp)
}