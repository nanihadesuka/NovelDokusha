package my.noveldokusha.core

import androidx.annotation.StringRes

/**
 * ISO 639-1 codes
 * https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes
 */
enum class LanguageCode(val iso639_1: String, @StringRes val nameResId: Int) {
    ENGLISH(iso639_1 = "en", nameResId = R.string.language_english),
    PORTUGUESE(iso639_1 = "pt", nameResId = R.string.language_portuguese),
    SPANISH(iso639_1 = "es", nameResId = R.string.language_spanish),
    FRENCH(iso639_1 = "fr", nameResId = R.string.language_french),
}