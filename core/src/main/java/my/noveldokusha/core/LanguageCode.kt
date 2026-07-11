package my.noveldokusha.core

import androidx.annotation.StringRes

/**
 * ISO 639-1 codes
 * https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes
 */
enum class LanguageCode(
    @Suppress("PropertyName") val iso639_1: String,
    @StringRes val nameResId: Int
) {
    ENGLISH(iso639_1 = "en", nameResId = R.string.language_english),
    PORTUGUESE(iso639_1 = "pt", nameResId = R.string.language_portuguese),
    @Suppress("unused")
    SPANISH(iso639_1 = "es", nameResId = R.string.language_spanish),
    @Suppress("unused")
    FRENCH(iso639_1 = "fr", nameResId = R.string.language_french),
    INDONESIAN(iso639_1 = "id", nameResId = R.string.language_indonesian),
    @Suppress("unused")
    CHINESE(iso639_1 = "zh", nameResId = R.string.language_chinese),
    // HnDK0 external sources — loaded from GitHub at runtime via Lua engine.
    // These appear as separate filter options in the catalog explorer so
    // users can browse community-maintained sources independently of the
    // built-in ones.
    ENGLISH_HNDK0(iso639_1 = "en_hndk0", nameResId = R.string.language_english_hndk0),
    CHINESE_HNDK0(iso639_1 = "zh_hndk0", nameResId = R.string.language_chinese_hndk0),
    MTL_HNDK0(iso639_1 = "mtl_hndk0", nameResId = R.string.language_mtl_hndk0),
}