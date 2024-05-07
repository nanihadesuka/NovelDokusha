plugins {
    alias(libs.plugins.noveldokusha.android.library)
    alias(libs.plugins.noveldokusha.android.compose)
}

android {
    namespace = "my.noveldokusha.reader"
}

dependencies {
    implementation(projects.core)
    implementation(projects.tooling.textToSpeech)
    implementation(projects.tooling.textTranslator.domain)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    testImplementation(libs.test.junit)
}