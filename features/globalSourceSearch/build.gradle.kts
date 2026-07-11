plugins {
    alias(libs.plugins.noveldokusha.android.library)
    alias(libs.plugins.noveldokusha.android.compose)
}

android {
    namespace = "my.noveldokusha.globalsourcesearch"
}

dependencies {
    implementation(projects.core)
    implementation(projects.coreui)
    implementation(projects.strings)
    implementation(projects.data)
    implementation(projects.scraper)
    implementation(projects.navigation)
    implementation(projects.networking)
    implementation(projects.tooling.localDatabase)

    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    implementation(libs.compose.androidx.activity)
    implementation(libs.compose.material3.android)
}