plugins {
    alias(libs.plugins.noveldokusha.android.library)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "my.noveldokusha.tooling.epub_parser"
}

dependencies {
    implementation(projects.core)

    implementation(libs.jsoup)
}