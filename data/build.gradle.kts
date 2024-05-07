plugins {
    alias(libs.plugins.noveldokusha.android.library)
    alias(libs.plugins.noveldokusha.android.compose)
}

android {
    namespace = "my.noveldoksuha.data"
}

dependencies {
    implementation(projects.core)
    implementation(projects.networking)
    implementation(projects.tooling.localDatabase)
    implementation(projects.tooling.epubParser)

    implementation(libs.androidx.core.ktx)
    testImplementation(libs.test.junit)
}