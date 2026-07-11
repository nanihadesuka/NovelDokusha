plugins {
    alias(libs.plugins.noveldokusha.android.library)
    alias(libs.plugins.noveldokusha.android.compose)
}

android {
    namespace = "my.noveldokusha.chapterslist"
}

dependencies {
    implementation(projects.core)
    implementation(projects.coreui)
    implementation(projects.strings)
    implementation(projects.data)
    implementation(projects.scraper)
    implementation(projects.navigation)
    implementation(projects.tooling.localDatabase)

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    implementation(libs.compose.androidx.activity)
    implementation(libs.compose.material3.android)
    implementation(libs.compose.androidx.material.icons.extended)
    implementation(libs.compose.androidx.material)
    implementation(libs.compose.lazyColumnScrollbar)

}