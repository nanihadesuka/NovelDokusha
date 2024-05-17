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

    implementation(libs.material)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.common.java8)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    implementation(libs.compose.androidx.activity)
    implementation(libs.compose.material3.android)
    implementation(libs.compose.androidx.lifecycle.viewmodel)

    implementation(libs.compose.androidx.material.icons.extended)
    implementation(libs.compose.accompanist.systemuicontroller)
    implementation(libs.compose.accompanist.insets)
    implementation(libs.compose.accompanist.pager)
    implementation(libs.compose.accompanist.pager.indicators)
    implementation(libs.compose.landscapist.glide)
    implementation(libs.compose.coil)
}