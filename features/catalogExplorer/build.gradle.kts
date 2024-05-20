plugins {
    alias(libs.plugins.noveldokusha.android.library)
    alias(libs.plugins.noveldokusha.android.compose)
}

android {
    namespace = "my.noveldokusha.catalogexplorer"
}

dependencies {
    implementation(projects.core)
    implementation(projects.coreui)
    implementation(projects.strings)
    implementation(projects.data)
    implementation(projects.scraper)
    implementation(projects.navigation)
    implementation(projects.tooling.localDatabase)

    implementation(libs.compose.androidx.activity)
    implementation(libs.compose.material3.android)
    implementation(libs.compose.androidx.lifecycle.viewmodel)
    implementation(libs.compose.androidx.material.icons.extended)
    implementation(libs.compose.accompanist.systemuicontroller)
    implementation(libs.compose.accompanist.insets)
    implementation(libs.compose.landscapist.glide)
    implementation(libs.compose.coil)
}
