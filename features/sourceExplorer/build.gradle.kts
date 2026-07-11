plugins {
    alias(libs.plugins.noveldokusha.android.library)
    alias(libs.plugins.noveldokusha.android.compose)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "my.noveldokusha.sourceexplorer"
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.timber)

    implementation(libs.compose.androidx.activity)
    implementation(libs.compose.material3.android)

    implementation(libs.compose.androidx.material.icons.extended)
}