package my.noveldokusha.text_translator

import my.noveldokusha.text_translator.buildSystemPrompt
import my.noveldokusha.text_translator.resolvePresetPrompt

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.core.appPreferences.AppPreferences
import my.noveldokusha.text_translator.domain.GOOGLE_TRANSLATE_LANGUAGES
import my.noveldokusha.text_translator.domain.TranslationManager
import my.noveldokusha.text_translator.domain.TranslationModelState
import my.noveldokusha.text_translator.domain.TranslatorState
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Translation manager using any OpenAI-compatible API.
 * Supports custom base URL (OpenAI, OpenRouter, Mistral, DeepSeek, local Ollama, etc.)
 *
 * Key rotation strategy:
 *   - Keys are split by newline/semicolon/comma from TRANSLATION_OPENAI_API_KEYS
 *   - Round-robin via atomic counter — each request picks the next key in sequence
 *   - On 401: tries all remaining keys before throwing
 *   - On 429: tries next key, if all exhausted throws with clear message
 *   - On 5xx: throws with HTTP code in message
 *   - On timeout/network error: rethrows IOException as-is
 *
 * No silent fallback — all errors are thrown so the caller and UI can report them.
 */
class TranslationManagerOpenAI(
    private val appPreferences: AppPreferences
) : TranslationManager {

    // Temperature is user-configurable via GEMINI_TEMPERATURE preference (shared with Gemini).
    private val defaultTemperature
        get() = appPreferences.GEMINI_TEMPERATURE.value.toDouble()

    /** 0 = let the model decide (no max_tokens in request). */
    private val maxOutputTokens: Int
        get() = appPreferences.TRANSLATION_MAX_OUTPUT_TOKENS.value

    /** Paragraphs per OpenAI batch request. */
    private val maxBatchItemsPerRequest: Int
        get() = appPreferences.TRANSLATION_BATCH_SIZE.value.coerceAtLeast(1)

    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    // Round-robin counter shared across all calls
    private val keyIndex = AtomicInteger(0)

    private val apiKeys: List<String>
        get() = appPreferences.TRANSLATION_OPENAI_API_KEYS.value
            .split("\n", ";", ",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

    private val baseUrl: String
        get() = appPreferences.TRANSLATION_OPENAI_BASE_URL.value
            .trimEnd('/')
            .ifBlank { "https://api.openai.com" }

    private val model: String
        get() = appPreferences.TRANSLATION_OPENAI_MODEL.value
            .ifBlank { "gpt-4o-mini" }

    private val useEnglishLocale: Boolean
        get() = appPreferences.TRANSLATION_PROMPT_USE_ENGLISH_LOCALE.value

    private fun resolveTemplatePrompt(systemPromptOverride: String?): String {
        if (systemPromptOverride != null && systemPromptOverride.isNotBlank()) {
            Log.d(TAG, "resolveTemplatePrompt: using override '${systemPromptOverride.take(200)}'")
            return systemPromptOverride
        }
        // Priority: custom system prompt → preset → default
        val customPrompt = appPreferences.TRANSLATION_ACTIVE_SYSTEM_PROMPT.value
        if (customPrompt.isNotBlank()) {
            Log.d(TAG, "resolveTemplatePrompt: using custom prompt '${customPrompt.take(200)}'")
            return customPrompt
        }
        val presetName = appPreferences.TRANSLATION_PROMPT_PRESET.value
        val presetPrompt = resolvePresetPrompt(presetName)
        Log.d(TAG, "resolveTemplatePrompt: using preset '$presetName' -> '${presetPrompt.take(200)}'")
        return presetPrompt
    }

    override val available = true

    override val models = mutableStateListOf<TranslationModelState>().apply {
        addAll(GOOGLE_TRANSLATE_LANGUAGES.map { lang ->
            TranslationModelState(
                language = lang,
                available = true,
                downloading = false,
                downloadingFailed = false
            )
        })
    }

    override fun getTranslator(source: String, target: String, systemPromptOverride: String?): TranslatorState {
        Log.d(TAG, "getTranslator: source=$source, target=$target, override=${systemPromptOverride != null}")
        return TranslatorState(
            source = source,
            target = target,
            translate = { input -> translateSingle(input, source, target, systemPromptOverride) }
        )
    }

    // ─── Single text translation ───────────────────────────────────────────────

    private suspend fun translateSingle(
        text: String,
        sourceLanguage: String,
        targetLanguage: String,
        systemPromptOverride: String? = null,
    ): String = withContext(Dispatchers.IO) {
        val systemPrompt = buildPrompt(sourceLanguage, targetLanguage, systemPromptOverride)
        val responseText = sendWithKeyRotation(systemPrompt, text)
        responseText.trim().ifEmpty { text }
    }

    // ─── Batch translation ─────────────────────────────────────────────────────

    override suspend fun translateBatch(
        texts: List<String>,
        sourceLanguage: String,
        targetLanguage: String,
        systemPromptOverride: String?,
    ): Map<String, String> = withContext(Dispatchers.IO) {
        if (texts.isEmpty()) return@withContext emptyMap()

        // Chunk large batches so the prompt stays compact and the model does not waste output tokens.
        val normalizedTexts = texts.filter { it.isNotBlank() }
        if (normalizedTexts.isEmpty()) return@withContext emptyMap()
        if (normalizedTexts.size > maxBatchItemsPerRequest) {
            val merged = mutableMapOf<String, String>()
            normalizedTexts.chunked(maxBatchItemsPerRequest).forEach { chunk ->
                merged.putAll(translateBatch(chunk, sourceLanguage, targetLanguage, systemPromptOverride))
            }
            return@withContext merged
        }

        Log.d(TAG, "translateBatch: ${normalizedTexts.size} paragraphs, $sourceLanguage→$targetLanguage, override='${systemPromptOverride?.take(200)}'")

        val systemPrompt = buildPrompt(sourceLanguage, targetLanguage, systemPromptOverride)
        Log.d(TAG, "translateBatch: systemPrompt='${systemPrompt.take(200)}'")

        // All format instructions are in the system prompt.
        // User message contains only the numbered text — clean and simple.
        val userMessage = normalizedTexts.mapIndexed { i, t -> "${i + 1}. $t" }.joinToString("\n")

        val responseText = sendWithKeyRotation(systemPrompt, userMessage)
        parseNumberedTranslations(responseText, normalizedTexts)
    }

    // ─── Key rotation + HTTP ───────────────────────────────────────────────────

    /**
     * Sends a chat completion request, rotating through all available keys on
     * retriable errors (429). Throws a descriptive exception on permanent failures.
     */
    private suspend fun sendWithKeyRotation(
        systemPrompt: String,
        userMessage: String
    ): String = withContext(Dispatchers.IO) {
        val keys = apiKeys

        if (keys.isEmpty()) {
            throw IllegalStateException("OpenAI: No API keys configured. Please add your API key in Settings → Translation.")
        }

        val startIndex = keyIndex.getAndIncrement() % keys.size
        var lastException: Exception? = null

        val retryPolicy = RetryPolicy(maxAttempts = keys.size, baseDelayMs = 250L, maxDelayMs = 1500L)
        Log.d(TAG, "sendWithKeyRotation: systemPrompt='${systemPrompt.take(200)}'")

        for (attempt in 0 until retryPolicy.maxAttempts) {
            val currentKey = keys[(startIndex + attempt) % keys.size]
            val keyLabel = "key #${(startIndex + attempt) % keys.size + 1}"

            try {
                val response = sendRequest(systemPrompt, userMessage, currentKey)
                val code = response.code

                when {
                    code == 401 -> {
                        Log.w(TAG, "sendWithKeyRotation: 401 on $keyLabel, trying next")
                        response.close()
                        lastException = IllegalStateException("OpenAI: Invalid API key ($keyLabel). Check your key in Settings.")
                        retryPolicy.backoff(attempt)
                        continue
                    }
                    code == 429 -> {
                        Log.w(TAG, "sendWithKeyRotation: 429 on $keyLabel, trying next")
                        response.close()
                        lastException = IllegalStateException("OpenAI: Rate limit exceeded ($keyLabel).")
                        retryPolicy.backoff(attempt)
                        continue
                    }
                    code in 500..599 -> {
                        response.close()
                        throw IOException("OpenAI: Server error ($code). Try again later.")
                    }
                    !response.isSuccessful -> {
                        val errorBody = response.body.string().take(200)
                        throw IllegalStateException("OpenAI: Unexpected error ($code): $errorBody")
                    }
                    else -> {
                        keyIndex.set((startIndex + attempt + 1) % keys.size)
                        val body = readBodyOrThrow(response, "OpenAI")
                        return@withContext parseResponse(body)
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "sendWithKeyRotation: network error — ${e.message}")
                throw e
            }
        }

        throw lastException
            ?: IllegalStateException("OpenAI: All API keys failed. Check your keys in Settings.")
    }

    private fun sendRequest(
        systemPrompt: String,
        userMessage: String,
        apiKey: String
    ): okhttp3.Response {
        val requestBody = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userMessage)
                })
            })
            put("temperature", defaultTemperature)
            // 0 = let the model decide; only send the field when the user set a cap.
            val cap = maxOutputTokens
            if (cap > 0) put("max_tokens", cap)
            put("top_p", 1.0)
            put("stream", false)
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$baseUrl/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        return client.newCall(request).execute()
    }

    private fun parseResponse(responseBody: String): String {
        return try {
            val json = JSONObject(responseBody)
            val choices = json.getJSONArray("choices")
            if (choices.length() > 0) {
                choices.getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()
            } else {
                throw IllegalStateException("OpenAI: No choices in response")
            }
        } catch (e: Exception) {
            Log.e(TAG, "parseResponse: failed to parse — ${e.message}")
            throw IllegalStateException("OpenAI: Failed to parse response — ${e.message}")
        }
    }

    // ─── Prompt building ───────────────────────────────────────────────────────

    private fun buildPrompt(sourceLanguage: String, targetLanguage: String, systemPromptOverride: String? = null): String =
        buildSystemPrompt(resolveTemplatePrompt(systemPromptOverride), sourceLanguage, targetLanguage, useEnglishLocale)

    // ─── Response parsing ──────────────────────────────────────────────────────

    /**
     * Parses a numbered translation response back to a map of original → translated.
     * Uses index-based matching to correctly handle duplicate paragraphs.
     *
     * Tolerates:
     *  - Preamble before the first numbered item (silently discarded)
     *  - Alternate numbering formats: "1)", "**1.**", "№1.", "1 ."
     *  - Missing items (falls back to original text)
     */
    private fun parseNumberedTranslations(
        translatedText: String,
        originalTexts: List<String>
    ): Map<String, String> {
        // Index-based map: key = 0-based index, value = translated text
        val byIndex = mutableMapOf<Int, String>()

        // Matches: "1.", "1)", "**1.**", "№1.", "#1.", "1 ." at start of line
        val numberPattern = Regex("""^\*{0,2}[№#]?\s*(\d+)\s*[.)]\*{0,2}\s*""")

        val lines = translatedText.split("\n")
        var currentIndex = -1  // -1 = before first numbered item (preamble)
        var currentText = StringBuilder()

        fun flush() {
            if (currentIndex >= 0 && currentText.isNotBlank()) {
                byIndex[currentIndex] = currentText.toString().trim()
            }
            currentText.clear()
        }

        for (line in lines) {
            val match = numberPattern.find(line)
            if (match != null) {
                flush()
                val num = match.groupValues[1].toIntOrNull() ?: continue
                currentIndex = num - 1  // convert to 0-based
                val rest = line.substring(match.value.length)
                if (rest.isNotBlank()) currentText.append(rest)
            } else {
                if (currentIndex == -1) continue  // preamble before "1." — discard
                val trimmed = line.trim()
                if (currentText.isNotEmpty()) currentText.append("\n")
                currentText.append(trimmed)
            }
        }
        flush()

        // Build final result by index — handles duplicate original texts correctly
        val result = mutableMapOf<String, String>()
        originalTexts.forEachIndexed { index, originalText ->
            val translation = byIndex[index]
            if (translation != null) {
                result[originalText] = translation
            } else {
                Log.w(TAG, "parseNumberedTranslations: missing index $index, using original")
                result[originalText] = originalText
            }
        }

        Log.d(TAG, "parseNumberedTranslations: ${byIndex.size}/${originalTexts.size} parsed")
        return result
    }

    companion object {
        private const val TAG = "TranslationOpenAI"
    }
}