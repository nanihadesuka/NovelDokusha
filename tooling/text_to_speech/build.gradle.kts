plugins {
    alias(libs.plugins.noveldokusha.android.library)
    alias(libs.plugins.noveldokusha.android.compose)
}

android {
    namespace = "my.noveldokusha.tooling.texttospeech"
}

dependencies {
    implementation(projects.tooling.algorithms)

    implementation(libs.test.junit)
}