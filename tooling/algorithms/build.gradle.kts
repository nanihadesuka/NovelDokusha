plugins {
    alias(libs.plugins.noveldokusha.android.library)
}

android {
    namespace = "my.noveldokusha.algorithms"
}

dependencies {
    implementation(libs.test.junit)
}
