plugins {
    alias(libs.plugins.noveldokusha.android.library)
    alias(libs.plugins.noveldokusha.android.compose)
}

android {
    namespace = "my.noveldoksuha.coreui"
    androidResources {
        resourcePrefix = ""
    }
}

dependencies {
    implementation(projects.strings)
    implementation(projects.core)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.compose.landscapist.glide)
    implementation(libs.compose.coil)
    implementation(libs.compose.androidx.activity)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.compose.material3.android)
    implementation(libs.compose.accompanist.systemuicontroller)
    implementation(libs.compose.androidx.material.icons.extended)
    implementation(libs.compose.accompanist.swiperefresh)
}