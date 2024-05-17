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
    implementation(projects.scraper)
    implementation(projects.tooling.localDatabase)
    implementation(projects.tooling.epubParser)

    implementation(libs.jsoup)
    implementation(libs.readability4j)
    implementation(libs.gson)
    implementation(libs.okhttp)

    implementation(libs.androidx.core.ktx)
    testImplementation(libs.test.junit)
}