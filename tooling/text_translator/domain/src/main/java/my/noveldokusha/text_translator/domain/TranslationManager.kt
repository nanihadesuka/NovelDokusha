package my.noveldokusha.text_translator.domain

import androidx.compose.runtime.snapshots.SnapshotStateList
import java.util.Locale

data class TranslationModelState(
    val language: String,
    val available: Boolean = true,
    val downloading: Boolean = false,
    val downloadingFailed: Boolean = false,
) {
    val locale: Locale = try {
        Locale.forLanguageTag(language)
    } catch (_: Exception) {
        try { Locale(language) } catch (_: Exception) { Locale("en") }
    }

    val displayName: String
        get() {
            val full = locale.getDisplayName()
                .takeIf { it.isNotBlank() && it != language }
            if (full != null) return full
            return LANGUAGE_DISPLAY_NAMES[language] ?: language.uppercase()
        }
}

data class TranslatorState(
    val source: String,
    val target: String,
    val translate: suspend (input: String) -> String,
) {
    val sourceLocale = Locale(source)
    val targetLocale = Locale(target)
}

interface TranslationManager {

    val available: Boolean

    val models: SnapshotStateList<TranslationModelState>

    suspend fun hasModelDownloaded(language: String): TranslationModelState? =
        models.firstOrNull { it.language == language }

    fun getTranslator(source: String, target: String, systemPromptOverride: String? = null): TranslatorState

    suspend fun translateBatch(
        texts: List<String>,
        sourceLanguage: String,
        targetLanguage: String,
        systemPromptOverride: String? = null,
    ): Map<String, String>

    suspend fun detectLanguage(text: String): String? = null
}
