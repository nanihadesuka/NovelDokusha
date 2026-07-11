package my.noveldokusha.text_translator

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.core.appPreferences.AppPreferences
import my.noveldokusha.text_translator.domain.TranslationManager
import my.noveldokusha.text_translator.domain.TranslationModelState
import my.noveldokusha.text_translator.domain.TranslatorState

/**
 * Composite translation manager.
 * Active backend is determined by appPreferences.TRANSLATION_PROVIDER:
 *   "GOOGLE_PA"   — translate-pa.googleapis.com (HTML chunks, best quality, default)
 *   "GOOGLE_FREE" — translate.googleapis.com/translate_a/single (plain text)
 *   "GEMINI"      — Google Gemini API (requires user API key; throws if key absent)
 *   "OPENAI"      — Any OpenAI-compatible API (OpenAI, OpenRouter, Mistral, DeepSeek, etc.)
 *
 * No silent fallback for any provider — errors are thrown with descriptive messages
 * so the user always knows what went wrong.
 */
class TranslationManagerComposite(
    private val geminiManager: TranslationManagerGemini,
    private val googleFreeManager: TranslationManagerGoogleFree,
    private val googlePAManager: TranslationManagerGooglePA,
    private val openAiManager: TranslationManagerOpenAI,
    private val appPreferences: AppPreferences
) : TranslationManager {

    override val available: Boolean = true

    override val models = mutableStateListOf<TranslationModelState>()

    init {
        val allLanguages = mutableSetOf<String>()
        allLanguages.addAll(geminiManager.models.map { it.language })
        allLanguages.addAll(googleFreeManager.models.map { it.language })
        allLanguages.addAll(googlePAManager.models.map { it.language })
        allLanguages.addAll(openAiManager.models.map { it.language })
        models.addAll(allLanguages.map { lang ->
            TranslationModelState(
                language = lang,
                available = true,
                downloading = false,
                downloadingFailed = false
            )
        })
    }

    private fun activeProvider(): String = appPreferences.TRANSLATION_PROVIDER.value

    override fun getTranslator(source: String, target: String, systemPromptOverride: String?): TranslatorState {
        val provider = activeProvider()
        Log.d(TAG, "getTranslator: source=$source, target=$target, provider=$provider, override=${systemPromptOverride != null}")
        return when {
            provider == "OPENAI"      -> openAiManager.getTranslator(source, target, systemPromptOverride)
            provider == "GEMINI"      -> buildGeminiTranslator(source, target, systemPromptOverride)
            provider == "GOOGLE_FREE" -> googleFreeManager.getTranslator(source, target)
            else                      -> googlePAManager.getTranslator(source, target)
        }
    }

    /**
     * Gemini translator with retry — no fallback to other providers.
     * If Gemini fails (no key, rate limit, API error), the exception propagates
     * to ReaderChaptersLoader which shows it as a ReaderItem.Error.
     */
    private fun buildGeminiTranslator(source: String, target: String, systemPromptOverride: String? = null): TranslatorState {
        val geminiTranslator = geminiManager.getTranslator(source, target, systemPromptOverride)
        return TranslatorState(
            source = source,
            target = target,
            translate = { input -> geminiTranslator.translate(input) }
        )
    }

    override suspend fun translateBatch(
        texts: List<String>,
        sourceLanguage: String,
        targetLanguage: String,
        systemPromptOverride: String?,
    ): Map<String, String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "translateBatch: $sourceLanguage → $targetLanguage, override='${systemPromptOverride?.take(200)}', texts=${texts.size}")
        if (texts.isEmpty()) return@withContext emptyMap()

        val resolvedSource = if (sourceLanguage == "auto") {
            val sample = texts.firstOrNull { it.isNotBlank() }?.take(200) ?: ""
            val detected = googleFreeManager.detectLanguage(sample)
            Log.d(TAG, "translateBatch: detected language=$detected")
            detected ?: sourceLanguage
        } else {
            sourceLanguage
        }

        when (activeProvider()) {
            "OPENAI" -> {
                Log.d(TAG, "translateBatch: using OpenAI-compatible API")
                openAiManager.translateBatch(texts, resolvedSource, targetLanguage, systemPromptOverride)
            }
            "GEMINI" -> {
                Log.d(TAG, "translateBatch: using Gemini, passing override='${systemPromptOverride?.take(200)}'")
                // No fallback — let exception propagate with descriptive message
                geminiManager.translateBatch(texts, resolvedSource, targetLanguage, systemPromptOverride)
            }
            "GOOGLE_FREE" -> {
                Log.d(TAG, "translateBatch: using Google Free")
                googleFreeManager.translateBatch(texts, resolvedSource, targetLanguage)
            }
            else -> {
                Log.d(TAG, "translateBatch: using Google PA")
                googlePAManager.translateBatch(texts, resolvedSource, targetLanguage)
            }
        }
    }

    override suspend fun detectLanguage(text: String): String? {
        return googleFreeManager.detectLanguage(text)
    }

    companion object {
        private const val TAG = "TranslationComposite"
    }
}