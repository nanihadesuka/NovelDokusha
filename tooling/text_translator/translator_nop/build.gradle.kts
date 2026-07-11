plugins {
    alias(libs.plugins.noveldokusha.android.library)
    alias(libs.plugins.noveldokusha.android.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "my.noveldokusha.tooling.text_translator"
}

dependencies {
    implementation(projects.core)
    implementation(projects.networking)
    implementation(projects.tooling.textTranslator.domain)

    // OkHttp for API calls (Gemini, OpenAI, Google PA)
    implementation(libs.okhttp)

    // Gson for JSON parsing (used by GooglePA)
    implementation(libs.gson)

    // kotlinx-serialization for GoogleFree's JSON response parsing
    implementation(libs.kotlinx.serialization.json)

    // Coroutines (used by all managers for withContext/Dispatchers.IO)
    implementation(libs.kotlinx.coroutines.core)
}
