package my.noveldokusha.text_translator.domain

/**
 * All languages supported by Google Cloud Translation (NMT + Translation LLM).
 * Source: https://docs.cloud.google.com/translate/docs/languages
 *
 * Includes both base ISO-639 codes and BCP-47 regional/script variants
 * (e.g. pt-BR, zh-TW, es-MX, pa-Arab).
 *
 * Some codes use ISO 639-2 / legacy tags that Google accepts but are NOT valid
 * java.util.Locale BCP-47 tags (e.g. "ceb", "hmn", "jv", "jw", "haw").
 * Callers that rely on Locale(langCode) should catch IllegalArgumentException
 * or use a fallback.
 */
val GOOGLE_TRANSLATE_LANGUAGES: List<String> = listOf(
    // ── Base codes (ISO-639) ──────────────────────────────────────────────
    "ab",    // Abkhaz
    "ace",   // Acehnese
    "ach",   // Acholi
    "af",    // Afrikaans
    "ak",    // Akan (Twi)
    "alz",   // Alur
    "am",    // Amharic
    "ar",    // Arabic
    "as",    // Assamese
    "awa",   // Awadhi
    "ay",    // Aymara
    "az",    // Azerbaijani
    "ba",    // Bashkir
    "ban",   // Balinese
    "bbc",   // Batak Toba
    "be",    // Belarusian
    "bem",   // Bemba
    "bg",    // Bulgarian
    "bho",   // Bhojpuri
    "bik",   // Bikol
    "bm",    // Bambara
    "bn",    // Bengali
    "br",    // Breton
    "bs",    // Bosnian
    "bts",   // Batak Simalungun
    "btx",   // Batak Karo
    "bua",   // Buryat
    "ca",    // Catalan
    "ceb",   // Cebuano (ISO 639-2)
    "cgg",   // Kiga
    "chm",   // Meadow Mari
    "ckb",   // Kurdish (Sorani)
    "cnh",   // Hakha Chin
    "co",    // Corsican
    "crh",   // Crimean Tatar
    "crs",   // Seychellois Creole
    "cs",    // Czech
    "cv",    // Chuvash
    "cy",    // Welsh
    "da",    // Danish
    "de",    // German
    "din",   // Dinka
    "doi",   // Dogri
    "dov",   // Dombe
    "dv",    // Divehi
    "dz",    // Dzongkha
    "ee",    // Ewe
    "el",    // Greek
    "en",    // English
    "eo",    // Esperanto
    "es",    // Spanish
    "et",    // Estonian
    "eu",    // Basque
    "fa",    // Persian
    "ff",    // Fulfulde
    "fi",    // Finnish
    "fj",    // Fijian
    "fr",    // French
    "fy",    // Frisian
    "ga",    // Irish
    "gaa",   // Ga
    "gd",    // Scots Gaelic
    "gl",    // Galician
    "gn",    // Guarani
    "gom",   // Konkani
    "gu",    // Gujarati
    "ha",    // Hausa
    "haw",   // Hawaiian (ISO 639-2)
    "he",    // Hebrew
    "hi",    // Hindi
    "hil",   // Hiligaynon
    "hmn",   // Hmong (ISO 639-2)
    "hr",    // Croatian
    "hrx",   // Hunsrik
    "ht",    // Haitian Creole
    "hu",    // Hungarian
    "hy",    // Armenian
    "id",    // Indonesian
    "ig",    // Igbo
    "ilo",   // Iloko
    "is",    // Icelandic
    "iw",    // Hebrew (legacy)
    "ja",    // Japanese
    "jv",    // Javanese (ISO 639-2)
    "jw",    // Javanese (legacy)
    "ka",    // Georgian
    "kk",    // Kazakh
    "km",    // Khmer
    "kn",    // Kannada
    "ko",    // Korean
    "kri",   // Krio
    "ktu",   // Kituba
    "ku",    // Kurdish (Kurmanji)
    "ky",    // Kyrgyz
    "la",    // Latin
    "lb",    // Luxembourgish
    "lg",    // Ganda (Luganda)
    "li",    // Limburgan
    "lij",   // Ligurian
    "lmo",   // Lombard
    "ln",    // Lingala
    "lo",    // Lao
    "lt",    // Lithuanian
    "ltg",   // Latgalian
    "luo",   // Luo
    "lv",    // Latvian
    "mai",   // Maithili
    "mak",   // Makassar
    "mg",    // Malagasy
    "mi",    // Maori
    "min",   // Minang
    "ml",    // Malayalam
    "mn",    // Mongolian
    "mr",    // Marathi
    "ms",    // Malay
    "mt",    // Maltese
    "my",    // Myanmar (Burmese)
    "nb",    // Norwegian Bokmal
    "ne",    // Nepali
    "new",   // Nepalbhasa (Newari)
    "no",    // Norwegian
    "nr",    // Ndebele (South)
    "nso",   // Northern Sotho (Sepedi)
    "nus",   // Nuer
    "ny",    // Chichewa (Nyanja)
    "oc",    // Occitan
    "om",    // Oromo
    "or",    // Odia (Oriya)
    "pa",    // Punjabi
    "pag",   // Pangasinan
    "pam",   // Kapampangan
    "pap",   // Papiamento
    "pl",    // Polish
    "ps",    // Pashto
    "pt",    // Portuguese
    "qu",    // Quechua
    "rn",    // Rundi
    "ro",    // Romanian
    "rom",   // Romani
    "ru",    // Russian
    "rw",    // Kinyarwanda
    "sa",    // Sanskrit
    "sd",    // Sindhi
    "sg",    // Sango
    "shn",   // Shan
    "si",    // Sinhala (Sinhalese)
    "sk",    // Slovak
    "sl",    // Slovenian
    "sm",    // Samoan
    "sn",    // Shona
    "so",    // Somali
    "sr",    // Serbian
    "ss",    // Swati
    "st",    // Sesotho
    "su",    // Sundanese
    "sv",    // Swedish
    "sw",    // Swahili
    "szl",   // Silesian
    "ta",    // Tamil
    "te",    // Telugu
    "tg",    // Tajik
    "tet",   // Tetum
    "th",    // Thai
    "ti",    // Tigrinya
    "tk",    // Turkmen
    "tl",    // Filipino (Tagalog)
    "tn",    // Tswana
    "tr",    // Turkish
    "ts",    // Tsonga
    "tt",    // Tatar
    "ug",    // Uyghur
    "uk",    // Ukrainian
    "ur",    // Urdu
    "uz",    // Uzbek
    "vi",    // Vietnamese
    "xh",    // Xhosa
    "yi",    // Yiddish
    "yo",    // Yoruba
    "yua",   // Yucatec Maya
    "zu",    // Zulu

    // ── Regional / script variants (BCP-47) ───────────────────────────────
    "ar-SA",    // Arabic (Saudi Arabia)
    "bn-IN",    // Bengali (India)
    "bs-Cyrl",  // Bosnian (Cyrillic)
    "en-AU",    // English (Australia)
    "en-CA",    // English (Canada)
    "en-GB",    // English (United Kingdom)
    "en-NZ",    // English (New Zealand)
    "en-PH",    // English (Philippines)
    "en-US",    // English (United States)
    "en-ZA",    // English (South Africa)
    "es-419",   // Spanish (Latin America)
    "es-AR",    // Spanish (Argentina)
    "es-CL",    // Spanish (Chile)
    "es-CO",    // Spanish (Colombia)
    "es-CR",    // Spanish (Costa Rica)
    "es-EC",    // Spanish (Ecuador)
    "es-ES",    // Spanish (Spain)
    "es-GT",    // Spanish (Guatemala)
    "es-HN",    // Spanish (Honduras)
    "es-HT",    // Spanish (Haiti)
    "es-MX",    // Spanish (Mexico)
    "es-NI",    // Spanish (Nicaragua)
    "es-PA",    // Spanish (Panama)
    "es-PE",    // Spanish (Peru)
    "es-PR",    // Spanish (Puerto Rico)
    "es-PY",    // Spanish (Paraguay)
    "es-SV",    // Spanish (El Salvador)
    "es-US",    // Spanish (United States)
    "es-UY",    // Spanish (Uruguay)
    "es-VE",    // Spanish (Venezuela)
    "fr-CA",    // French (Canada)
    "fr-CH",    // French (Switzerland)
    "fr-FR",    // French (France)
    "mni-Mtei", // Meiteilon (Manipuri)
    "ms-Arab",  // Malay (Jawi)
    "nl-BE",    // Dutch (Belgium)
    "pa-Arab",  // Punjabi (Shahmukhi)
    "pa-PK",    // Punjabi (Pakistan)
    "pt-BR",    // Portuguese (Brazil)
    "pt-PT",    // Portuguese (Portugal)
    "zh-CN",    // Chinese (Simplified / China)
    "zh-Hans",  // Chinese (Simplified)
    "zh-Hant",  // Chinese (Traditional)
    "zh-HK",    // Chinese (Hong Kong)
    "zh-TW",    // Chinese (Traditional / Taiwan)
)

/**
 * English display names for every code in [GOOGLE_TRANSLATE_LANGUAGES].
 *
 * java.util.Locale only knows ISO 639-1 two-letter codes. Most of the codes
 * above are ISO 639-3 or legacy tags that Locale does NOT recognise, so
 * displayLanguage / getDisplayCountry return empty strings.
 *
 * This map is the single source of truth for UI display and LLM prompt
 * language-name substitution. Keep it in sync with the list above.
 */
val LANGUAGE_DISPLAY_NAMES: Map<String, String> = mapOf(
    // ── Base codes ────────────────────────────────────────────────────────
    "ab" to "Abkhaz",
    "ace" to "Acehnese",
    "ach" to "Acholi",
    "af" to "Afrikaans",
    "ak" to "Akan",
    "alz" to "Alur",
    "am" to "Amharic",
    "ar" to "Arabic",
    "as" to "Assamese",
    "awa" to "Awadhi",
    "ay" to "Aymara",
    "az" to "Azerbaijani",
    "ba" to "Bashkir",
    "ban" to "Balinese",
    "bbc" to "Batak Toba",
    "be" to "Belarusian",
    "bem" to "Bemba",
    "bg" to "Bulgarian",
    "bho" to "Bhojpuri",
    "bik" to "Bikol",
    "bm" to "Bambara",
    "bn" to "Bengali",
    "br" to "Breton",
    "bs" to "Bosnian",
    "bts" to "Batak Simalungun",
    "btx" to "Batak Karo",
    "bua" to "Buryat",
    "ca" to "Catalan",
    "ceb" to "Cebuano",
    "cgg" to "Kiga",
    "chm" to "Meadow Mari",
    "ckb" to "Kurdish (Sorani)",
    "cnh" to "Hakha Chin",
    "co" to "Corsican",
    "crh" to "Crimean Tatar",
    "crs" to "Seychellois Creole",
    "cs" to "Czech",
    "cv" to "Chuvash",
    "cy" to "Welsh",
    "da" to "Danish",
    "de" to "German",
    "din" to "Dinka",
    "doi" to "Dogri",
    "dov" to "Dombe",
    "dv" to "Divehi",
    "dz" to "Dzongkha",
    "ee" to "Ewe",
    "el" to "Greek",
    "en" to "English",
    "eo" to "Esperanto",
    "es" to "Spanish",
    "et" to "Estonian",
    "eu" to "Basque",
    "fa" to "Persian",
    "ff" to "Fulfulde",
    "fi" to "Finnish",
    "fj" to "Fijian",
    "fr" to "French",
    "fy" to "Frisian",
    "ga" to "Irish",
    "gaa" to "Ga",
    "gd" to "Scots Gaelic",
    "gl" to "Galician",
    "gn" to "Guarani",
    "gom" to "Konkani",
    "gu" to "Gujarati",
    "ha" to "Hausa",
    "haw" to "Hawaiian",
    "he" to "Hebrew",
    "hi" to "Hindi",
    "hil" to "Hiligaynon",
    "hmn" to "Hmong",
    "hr" to "Croatian",
    "hrx" to "Hunsrik",
    "ht" to "Haitian Creole",
    "hu" to "Hungarian",
    "hy" to "Armenian",
    "id" to "Indonesian",
    "ig" to "Igbo",
    "ilo" to "Iloko",
    "is" to "Icelandic",
    "iw" to "Hebrew",
    "ja" to "Japanese",
    "jv" to "Javanese",
    "jw" to "Javanese",
    "ka" to "Georgian",
    "kk" to "Kazakh",
    "km" to "Khmer",
    "kn" to "Kannada",
    "ko" to "Korean",
    "kri" to "Krio",
    "ktu" to "Kituba",
    "ku" to "Kurdish (Kurmanji)",
    "ky" to "Kyrgyz",
    "la" to "Latin",
    "lb" to "Luxembourgish",
    "lg" to "Ganda",
    "li" to "Limburgan",
    "lij" to "Ligurian",
    "lmo" to "Lombard",
    "ln" to "Lingala",
    "lo" to "Lao",
    "lt" to "Lithuanian",
    "ltg" to "Latgalian",
    "luo" to "Luo",
    "lv" to "Latvian",
    "mai" to "Maithili",
    "mak" to "Makassar",
    "mg" to "Malagasy",
    "mi" to "Maori",
    "min" to "Minang",
    "ml" to "Malayalam",
    "mn" to "Mongolian",
    "mr" to "Marathi",
    "ms" to "Malay",
    "mt" to "Maltese",
    "my" to "Myanmar (Burmese)",
    "nb" to "Norwegian Bokmal",
    "ne" to "Nepali",
    "new" to "Nepalbhasa (Newari)",
    "no" to "Norwegian",
    "nr" to "Ndebele (South)",
    "nso" to "Northern Sotho (Sepedi)",
    "nus" to "Nuer",
    "ny" to "Chichewa (Nyanja)",
    "oc" to "Occitan",
    "om" to "Oromo",
    "or" to "Odia (Oriya)",
    "pa" to "Punjabi",
    "pag" to "Pangasinan",
    "pam" to "Kapampangan",
    "pap" to "Papiamento",
    "pl" to "Polish",
    "ps" to "Pashto",
    "pt" to "Portuguese",
    "qu" to "Quechua",
    "rn" to "Rundi",
    "ro" to "Romanian",
    "rom" to "Romani",
    "ru" to "Russian",
    "rw" to "Kinyarwanda",
    "sa" to "Sanskrit",
    "sd" to "Sindhi",
    "sg" to "Sango",
    "shn" to "Shan",
    "si" to "Sinhala (Sinhalese)",
    "sk" to "Slovak",
    "sl" to "Slovenian",
    "sm" to "Samoan",
    "sn" to "Shona",
    "so" to "Somali",
    "sr" to "Serbian",
    "ss" to "Swati",
    "st" to "Sesotho",
    "su" to "Sundanese",
    "sv" to "Swedish",
    "sw" to "Swahili",
    "szl" to "Silesian",
    "ta" to "Tamil",
    "te" to "Telugu",
    "tg" to "Tajik",
    "tet" to "Tetum",
    "th" to "Thai",
    "ti" to "Tigrinya",
    "tk" to "Turkmen",
    "tl" to "Filipino (Tagalog)",
    "tn" to "Tswana",
    "tr" to "Turkish",
    "ts" to "Tsonga",
    "tt" to "Tatar",
    "ug" to "Uyghur",
    "uk" to "Ukrainian",
    "ur" to "Urdu",
    "uz" to "Uzbek",
    "vi" to "Vietnamese",
    "xh" to "Xhosa",
    "yi" to "Yiddish",
    "yo" to "Yoruba",
    "yua" to "Yucatec Maya",
    "zu" to "Zulu",

    // ── Regional / script variants ────────────────────────────────────────
    "ar-SA" to "Arabic (Saudi Arabia)",
    "bn-IN" to "Bengali (India)",
    "bs-Cyrl" to "Bosnian (Cyrillic)",
    "en-AU" to "English (Australia)",
    "en-CA" to "English (Canada)",
    "en-GB" to "English (United Kingdom)",
    "en-NZ" to "English (New Zealand)",
    "en-PH" to "English (Philippines)",
    "en-US" to "English (United States)",
    "en-ZA" to "English (South Africa)",
    "es-419" to "Spanish (Latin America)",
    "es-AR" to "Spanish (Argentina)",
    "es-CL" to "Spanish (Chile)",
    "es-CO" to "Spanish (Colombia)",
    "es-CR" to "Spanish (Costa Rica)",
    "es-EC" to "Spanish (Ecuador)",
    "es-ES" to "Spanish (Spain)",
    "es-GT" to "Spanish (Guatemala)",
    "es-HN" to "Spanish (Honduras)",
    "es-HT" to "Spanish (Haiti)",
    "es-MX" to "Spanish (Mexico)",
    "es-NI" to "Spanish (Nicaragua)",
    "es-PA" to "Spanish (Panama)",
    "es-PE" to "Spanish (Peru)",
    "es-PR" to "Spanish (Puerto Rico)",
    "es-PY" to "Spanish (Paraguay)",
    "es-SV" to "Spanish (El Salvador)",
    "es-US" to "Spanish (United States)",
    "es-UY" to "Spanish (Uruguay)",
    "es-VE" to "Spanish (Venezuela)",
    "fr-CA" to "French (Canada)",
    "fr-CH" to "French (Switzerland)",
    "fr-FR" to "French (France)",
    "mni-Mtei" to "Meiteilon (Manipuri)",
    "ms-Arab" to "Malay (Jawi)",
    "nl-BE" to "Dutch (Belgium)",
    "pa-Arab" to "Punjabi (Shahmukhi)",
    "pa-PK" to "Punjabi (Pakistan)",
    "pt-BR" to "Portuguese (Brazil)",
    "pt-PT" to "Portuguese (Portugal)",
    "zh-CN" to "Chinese (Simplified)",
    "zh-Hans" to "Chinese (Simplified)",
    "zh-Hant" to "Chinese (Traditional)",
    "zh-HK" to "Chinese (Hong Kong)",
    "zh-TW" to "Chinese (Traditional)",
)
