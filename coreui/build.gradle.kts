plugins {
    alias(libs.plugins.noveldokusha.android.library)
    alias(libs.plugins.noveldokusha.android.compose)
    alias(libs.plugins.kotlin.parcelize)
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
    implementation(projects.tooling.localDatabase)

    implementation(libs.androidx.core.ktx)
    api(libs.androidx.appcompat)
    api(libs.material)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    implementation(libs.compose.androidx.activity)
    implementation(libs.compose.androidx.animation)
    implementation(libs.compose.androidx.material.icons.extended)
    api(libs.compose.androidx.material)
    implementation(libs.compose.material3.android)
    implementation(libs.compose.accompanist.systemuicontroller)
    implementation(libs.compose.landscapist.glide)
    implementation(libs.compose.coil)
}