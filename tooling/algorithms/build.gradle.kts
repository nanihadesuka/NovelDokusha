plugins {
    alias(libs.plugins.noveldokusha.android.library)
}

android {
    namespace = "my.noveldokusha.algorithms"
}

dependencies {
    testImplementation(libs.test.junit)
}
