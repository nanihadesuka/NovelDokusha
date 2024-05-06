plugins {
    alias(libs.plugins.noveldokusha.android.library)
    alias(libs.plugins.noveldokusha.android.compose)
}

android {
    namespace = "my.noveldokusha.reader.texttospeech"
}

dependencies {
    implementation(projects.core)
    implementation(projects.features.reader.domain)
    implementation(projects.features.textToSpeech)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
}