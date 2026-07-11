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
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class TranslationManagerGemini(
    private val appPreferences: AppPreferences
) : TranslationManager {

    // Ultra-minimal prompt used as fallback when the main prompt triggers a content block.
    // No genre keywords, no register/style hints — pure translation instruction only.
    private val fallbackSystemPrompt = "Translate each numbered item from {source_language} to {target_language}. Output \"N. Text\" only. No notes, no preamble."

    // Temperature is user-configurable via GEMINI_TEMPERATURE preference.
    private val defaultTemperature
        get() = appPreferences.GEMINI_TEMPERATURE.value.toDouble()
    private val defaultTopP = 0.9

    /** 0 = let the model decide (no maxOutputTokens in request). */
    private val maxOutputTokens: Int
        get() = appPreferences.TRANSLATION_MAX_OUTPUT_TOKENS.value

    /** Paragraphs per Gemini batch request. */
    private val maxBatchItemsPerRequest: Int
        get() = appPreferences.TRANSLATION_BATCH_SIZE.value.coerceAtLeast(1)

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val keyIndex = java.util.concurrent.atomic.AtomicInteger(0)

    private val apiKeys: List<String>
        get() = appPreferences.TRANSLATION_GEMINI_API_KEY.value
            .split("\n", ";", ",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

    private fun getApiEndpoint(key: String): String {
        val model = appPreferences.TRANSLATION_GEMINI_MODEL.value.ifBlank { "gemini-2.5-flash" }
        return "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key"
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
        Log.d(TAG, "getTranslator: source=$source, target=$target, apiKeysConfigured=${apiKeys.size}, override=${systemPromptOverride != null}")
        return TranslatorState(
            source = source,
            target = target,
            translate = { input -> translateWithGemini(input, source, target, systemPromptOverride = systemPromptOverride) }
        )
    }

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

    private suspend fun translateWithGemini(
        text: String,
        sourceLanguage: String,
        targetLanguage: String,
        retryCount: Int = 3,
        systemPromptOverride: String? = null,
    ): String = withContext(Dispatchers.IO) {
        val keys = apiKeys
        if (keys.isEmpty()) throw IllegalStateException("Gemini: No API keys configured.")

        val useEnglish = appPreferences.TRANSLATION_PROMPT_USE_ENGLISH_LOCALE.value
        val templatePrompt = resolveTemplatePrompt(systemPromptOverride)
        val systemPrompt = buildSystemPrompt(templatePrompt, sourceLanguage, targetLanguage, useEnglish)
        val builtFallbackPrompt = buildSystemPrompt(fallbackSystemPrompt, sourceLanguage, targetLanguage, useEnglish)

        val startIndex = keyIndex.getAndIncrement() % keys.size
        var lastException: Exception? = null
        var usesFallback = false
        val totalAttempts = retryCount * keys.size

        for (attempt in 0 until totalAttempts) {
            val currentKey = keys[(startIndex + attempt) % keys.size]
            val keyLabel = "key #${(startIndex + attempt) % keys.size + 1}"
            val activePrompt = if (usesFallback) builtFallbackPrompt else systemPrompt

            try {
                val startTime = System.currentTimeMillis()
                Log.d(TAG, "🚀 Request start: attempt=${attempt + 1}, textLen=${text.length}, key=$keyLabel, fallback=$usesFallback")

                val response = sendGeminiRequest(activePrompt, text, currentKey)

                when (response.code) {
                    200 -> {
                        val responseBody = readBodyOrThrow(response, "Gemini")
                        val result = parseGeminiResponse(responseBody)
                        val totalTime = System.currentTimeMillis() - startTime
                        if (result == BLOCKED_MARKER) {
                            if (!usesFallback) {
                                Log.w(TAG, "translateWithGemini: blocked — switching to fallback prompt")
                                usesFallback = true
                            } else {
                                Log.w(TAG, "translateWithGemini: blocked even on fallback prompt, returning original")
                            }
                            lastException = IOException("Gemini: Content blocked")
                            continue
                        }
                        Log.d(TAG, "✅ Success: total=${totalTime}ms, resultLen=${result.length}")
                        if (result.isNotBlank()) return@withContext result
                        Log.w(TAG, "translateWithGemini: empty response after parsing, retrying...")
                        lastException = IOException("Gemini: Empty response after parsing")
                        continue
                    }
                    400 -> {
                        val errorBody = response.body.string()
                        Log.w(TAG, "translateWithGemini: 400 on $keyLabel: $errorBody")
                        lastException = IOException("Gemini: Bad request (400): $errorBody")
                        kotlinx.coroutines.delay(500L * (attempt / keys.size + 1))
                        continue
                    }
                    429 -> {
                        Log.w(TAG, "translateWithGemini: rate limit (429) on $keyLabel")
                        lastException = IOException("Gemini: Rate limit exceeded")
                        continue
                    }
                    in 500..599 -> {
                        val errorBody = response.body.string()
                        Log.w(TAG, "translateWithGemini: server error (${response.code}) on $keyLabel: $errorBody")
                        lastException = IOException("Gemini: Server error (${response.code})")
                        kotlinx.coroutines.delay(500L * (attempt / keys.size + 1))
                    }
                    else -> {
                        val errorBody = response.body.string()
                        Log.e(TAG, "translateWithGemini: API error ${response.code} on $keyLabel: $errorBody")
                        throw IOException("Gemini: API error ${response.code}: $errorBody")
                    }
                }
            } catch (e: Exception) {
                lastException = e
                if (e is IOException && attempt < totalAttempts - 1) continue
                throw e
            }
        }
        // All attempts exhausted — if blocked, return original text to avoid breaking the reader.
        if (lastException?.message?.contains("blocked", ignoreCase = true) == true) {
            Log.w(TAG, "translateWithGemini: all retries blocked, returning original text")
            return@withContext text
        }
        throw lastException ?: IOException("Gemini: All attempts failed")
    }

    override suspend fun translateBatch(
        texts: List<String>,
        sourceLanguage: String,
        targetLanguage: String,
        systemPromptOverride: String?,
    ): Map<String, String> = withContext(Dispatchers.IO) {
        if (texts.isEmpty()) return@withContext emptyMap()

        // Filter blank entries before sending — empty strings waste output tokens.
        val normalizedTexts = texts.filter { it.isNotBlank() }
        if (normalizedTexts.isEmpty()) return@withContext emptyMap()
        if (normalizedTexts.size > maxBatchItemsPerRequest) {
            val merged = mutableMapOf<String, String>()
            normalizedTexts.chunked(maxBatchItemsPerRequest).forEach { chunk ->
                    merged.putAll(translateBatch(chunk, sourceLanguage, targetLanguage, systemPromptOverride))
            }
            return@withContext merged
        }

        Log.d(TAG, "translateBatch: translating ${normalizedTexts.size} paragraphs, override='${systemPromptOverride?.take(200)}'")
        val availableKeys = apiKeys
        if (availableKeys.isEmpty()) throw IllegalStateException("Gemini: No API keys configured.")

        val useEnglish = appPreferences.TRANSLATION_PROMPT_USE_ENGLISH_LOCALE.value
        val templatePrompt = resolveTemplatePrompt(systemPromptOverride)
        val systemPrompt = buildSystemPrompt(templatePrompt, sourceLanguage, targetLanguage, useEnglish)
        Log.d(TAG, "translateBatch: systemPrompt='${systemPrompt.take(200)}'")

        // Numbered input keeps Gemini aligned to the existing batch parser with minimal overhead.
        val userText = normalizedTexts.mapIndexed { index, text -> "${index + 1}. $text" }.joinToString("\n")

        // Iterate keys sequentially. Switch to next key only on 429 (rate limit) or 401/403 (dead key).
        // Server errors (5xx) retry the same key. Content blocks fail immediately — no retry,
        // no key switch: the content is the problem, not the key.
        var keyIdx = keyIndex.getAndIncrement() % availableKeys.size
        val retryCount = 3
        var lastException: Exception? = null

        for (keyAttempt in 0 until availableKeys.size) {
            val currentApiKey = availableKeys[(keyIdx + keyAttempt) % availableKeys.size]
            val keyLabel = "key #${(keyIdx + keyAttempt) % availableKeys.size + 1}"

            for (retry in 0 until retryCount) {
                try {
                    val startTime = System.currentTimeMillis()
                    Log.d(TAG, "🚀 Batch request: key=$keyLabel, retry=${retry + 1}, paragraphs=${normalizedTexts.size}")

                    val response = sendGeminiRequest(systemPrompt, userText, currentApiKey)
                    val code = response.code

                    when (code) {
                        429 -> {
                            // Rate limit — move to next key immediately, no point retrying this one.
                            Log.w(TAG, "translateBatch: rate limit (429) on $keyLabel, switching key")
                            lastException = IOException("Gemini: Rate limit exceeded on $keyLabel")
                            break
                        }
                        401, 403 -> {
                            // Dead/invalid key — move to next key.
                            val errorBody = response.body.string()
                            Log.w(TAG, "translateBatch: auth error ($code) on $keyLabel, switching key: $errorBody")
                            lastException = IOException("Gemini: Auth error ($code) on $keyLabel")
                            break
                        }
                        in 500..599 -> {
                            // Server error — retry same key with backoff.
                            val errorBody = response.body.string()
                            Log.w(TAG, "translateBatch: server error ($code) on $keyLabel, retry ${retry + 1}")
                            lastException = IOException("Gemini: Server error ($code)")
                            kotlinx.coroutines.delay(2000L * (retry + 1))
                            continue
                        }
                        400 -> {
                            val errorBody = response.body.string()
                            Log.w(TAG, "translateBatch: bad request (400) on $keyLabel: $errorBody")
                            lastException = IOException("Gemini: Bad request (400): $errorBody")
                            kotlinx.coroutines.delay(1000L * (retry + 1))
                            continue
                        }
                        !in 200..299 -> {
                            val errorBody = response.body.string()
                            Log.e(TAG, "translateBatch: API error $code on $keyLabel: $errorBody")
                            throw IOException("Gemini: API error $code")
                        }
                    }

                    val responseBody = readBodyOrThrow(response, "Gemini")
                    val translatedText = parseGeminiResponse(responseBody)
                    val totalTime = System.currentTimeMillis() - startTime

                    if (translatedText == BLOCKED_MARKER) {
                        // Content itself is blocked — no retry, no key switch, fail the chapter cleanly.
                        Log.w(TAG, "translateBatch: PROHIBITED_CONTENT — failing chapter immediately")
                        throw ContentBlockedException("Gemini: chapter blocked by content filter (PROHIBITED_CONTENT)")
                    }

                    if (translatedText.isNotEmpty()) {
                        Log.d(TAG, "✅ Batch success: total=${totalTime}ms, resultLen=${translatedText.length}")
                        val translations = parseNumberedTranslations(translatedText, normalizedTexts)
                        Log.d(TAG, "translateBatch: parsed ${translations.size}/${normalizedTexts.size} translations")
                        return@withContext translations
                    }

                    Log.w(TAG, "translateBatch: empty response, retry ${retry + 1}")
                    lastException = IOException("Gemini: Empty response after parsing")
                    kotlinx.coroutines.delay(500L * (retry + 1))

                } catch (e: ContentBlockedException) {
                    throw e  // Never swallow content blocks
                } catch (e: Exception) {
                    Log.e(TAG, "translateBatch: exception on $keyLabel retry ${retry + 1}: ${e.message}", e)
                    lastException = e
                    kotlinx.coroutines.delay(1000L * (retry + 1))
                }
            }
        }
        throw lastException ?: IOException("Gemini: Batch translation failed")
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun sendGeminiRequest(
        systemPrompt: String,
        userText: String,
        apiKey: String
    ): okhttp3.Response {
        val generationConfig = JSONObject().apply {
            put("temperature", defaultTemperature)
            put("topP", defaultTopP)
            put("responseMimeType", "text/plain")
            // 0 = let the model decide; only send the field when the user set a cap.
            val cap = maxOutputTokens
            if (cap > 0) put("maxOutputTokens", cap)
        }

        // ✅ safetySettings = BLOCK_NONE for all known categories, including CIVIC_INTEGRITY (added in Gemini 2.x)
        val safetySettings = JSONArray().apply {
            val categories = listOf(
                "HARM_CATEGORY_HARASSMENT",
                "HARM_CATEGORY_HATE_SPEECH",
                "HARM_CATEGORY_SEXUALLY_EXPLICIT",
                "HARM_CATEGORY_DANGEROUS_CONTENT",
                "HARM_CATEGORY_CIVIC_INTEGRITY"
            )
            for (cat in categories) {
                put(JSONObject().apply {
                    put("category", cat)
                    put("threshold", "BLOCK_NONE")
                })
            }
        }

        val jsonBody = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                })
            })
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", userText) })
                    })
                })
            })
            put("generationConfig", generationConfig)
            put("safetySettings", safetySettings)
            // Disable tool use so Gemini only returns translation text.
            put("tools", JSONArray())
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(getApiEndpoint(apiKey))
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        // Read the body once for lightweight diagnostics, then restore it for downstream parsing.
        val bodyString = response.body.string()
        Log.d(TAG, "sendGeminiRequest: status=${response.code}, bodyPreview=${bodyString.take(160)}")
        val newBody = bodyString.toResponseBody("application/json".toMediaType())
        return response.newBuilder().body(newBody).build()
    }

    private fun parseGeminiResponse(responseBody: String): String {
        val trimmed = responseBody.trim()
        // Gemini usually returns JSON, but the parser keeps a plain-text fallback for resilience.
        Log.d(TAG, "parseGeminiResponse: start length=${responseBody.length}")

        // Try the rare array form first.
        if (trimmed.startsWith("[")) {
            try {
                Log.d(TAG, "parseGeminiResponse: trying array format")
                val jsonArray = JSONArray(trimmed)
                return buildString {
                    for (i in 0 until jsonArray.length()) {
                        val chunk = jsonArray.getJSONObject(i)
                        val candidates = chunk.getJSONArray("candidates")
                        if (candidates.length() > 0) {
                            val candidate = candidates.getJSONObject(0)
                            if (candidate.optString("finishReason") == "SAFETY" ||
                                candidate.optString("finishReason") == "PROHIBITED_CONTENT") continue
                            val parts = candidate.getJSONObject("content").getJSONArray("parts")
                            if (parts.length() > 0) append(parts.getJSONObject(0).getString("text"))
                        }
                    }
                }.trim()
            } catch (e: Exception) {
                Log.w(TAG, "parseGeminiResponse: array parse failed", e)
            }
        }

        // Then try the standard object form.
        if (trimmed.startsWith("{")) {
            try {
                Log.d(TAG, "parseGeminiResponse: trying object format")
                val jsonResponse = JSONObject(trimmed)

                // Проверка блокировки промпта
                val promptFeedback = jsonResponse.optJSONObject("promptFeedback")
                val blockReason = promptFeedback?.optString("blockReason")
                if (!blockReason.isNullOrEmpty() && blockReason != "BLOCK_REASON_UNSPECIFIED") {
                    Log.w(TAG, "Prompt blocked: $blockReason")
                    return BLOCKED_MARKER
                }

                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates == null) {
                    Log.w(TAG, "parseGeminiResponse: no candidates array in response")
                    return ""
                }
                if (candidates.length() == 0) {
                    Log.w(TAG, "parseGeminiResponse: candidates array is empty")
                    return ""
                }

                val candidate = candidates.getJSONObject(0)
                val finishReason = candidate.optString("finishReason", "UNKNOWN")
                Log.d(TAG, "parseGeminiResponse: finishReason=$finishReason")
                if (finishReason == "SAFETY" || finishReason == "PROHIBITED_CONTENT") {
                    val finishMessage = candidate.optString("finishMessage", "")
                    Log.w(TAG, "Response blocked by content filter: $finishReason — $finishMessage")
                    return BLOCKED_MARKER
                }

                val content = candidate.optJSONObject("content")
                if (content == null) {
                    Log.w(TAG, "parseGeminiResponse: no content in candidate")
                    return ""
                }
                val parts = content.optJSONArray("parts")
                if (parts == null || parts.length() == 0) {
                    Log.w(TAG, "parseGeminiResponse: no parts in content")
                    return ""
                }

                val resultText = parts.getJSONObject(0).getString("text").trim()
                Log.d(TAG, "parseGeminiResponse: parsed from JSON, len=${resultText.length}")
                return resultText

            } catch (e: Exception) {
                Log.w(TAG, "parseGeminiResponse: object parse failed", e)
            }
        }

        // Fall back to the raw body if the API returned plain text.
        Log.d(TAG, "parseGeminiResponse: returning raw trimmed, length=${trimmed.length}, preview=${trimmed.take(200)}")
        return trimmed
    }

    private fun parseNumberedTranslations(translatedText: String, originalTexts: List<String>): Map<String, String> {
        val byIndex = mutableMapOf<Int, String>()
        val numberPattern = Regex("""^\*{0,2}[№#]?\s*(\d+)\s*[.)]\*{0,2}\s*""")
        val lines = translatedText.split("\n")
        var currentIndex = -1
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
                currentIndex = num - 1
                val rest = line.substring(match.value.length)
                if (rest.isNotBlank()) currentText.append(rest)
            } else {
                if (currentIndex == -1) continue
                if (currentText.isNotEmpty()) currentText.append("\n")
                currentText.append(line.trim())
            }
        }
        flush()

        return originalTexts.mapIndexedNotNull { index, originalText ->
            byIndex[index]?.let { originalText to it }
        }.toMap().also {
            Log.d(TAG, "parseNumberedTranslations: ${it.size}/${originalTexts.size} parsed")
        }
    }

    class ContentBlockedException(message: String) : IOException(message)

    companion object {
        private const val TAG = "TranslationGemini"
        private const val BLOCKED_MARKER = "__GEMINI_BLOCKED__"
    }
}