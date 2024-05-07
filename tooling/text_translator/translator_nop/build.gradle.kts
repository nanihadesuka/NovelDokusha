plugins {
    alias(libs.plugins.noveldokusha.android.library)
    alias(libs.plugins.noveldokusha.android.compose)
}

android {
    namespace = "my.noveldokusha.tooling.text_translator"
}

dependencies {
    implementation(projects.core)
    implementation(projects.tooling.textTranslator.domain)
}