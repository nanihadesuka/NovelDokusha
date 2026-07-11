package my.noveldokusha.scraper.sources

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Standalone Gemini API client for translating Chinese novel text to English.
 *
 * Uses the Gemini generativelanguage API with structured JSON output,
 * professional light novel translation system prompt, and smart batching
 * to translate paragraph lists efficiently.
 */
class GeminiApiClient(
    private val apiKey: String,
    private val model: String = "gemini-2.5-flash"
) {
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Track request timestamps for rate limiting */
    private val requestTimestamps = mutableListOf<Long>()
    private val lock = Any()

    /** Rate limit configuration per model */
    private data class RateLimit(val rpm: Int, val rpd: Int)

    private val rateLimits = mapOf(
        "gemini-2.5-flash" to RateLimit(rpm = 10, rpd = 250),
        "gemini-2.5-flash-lite" to RateLimit(rpm = 30, rpd = 1000)
    )

    /** JSON media type for OkHttp */
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Paragraph data class for input/output JSON serialization.
     */
    data class ParagraphItem(
        val id: Int,
        val text: String
    )

    /**
     * Translate a list of Chinese paragraphs to English using Gemini.
     *
     * Splits into batches of 2-3 chapters worth of paragraphs,
     * translates each batch, and returns the translated paragraphs
     * in the same order. On failure, returns the original text.
     */
    suspend fun translateParagraphs(paragraphs: List<ParagraphItem>): List<ParagraphItem> =
        withContext(Dispatchers.IO) {
            if (paragraphs.isEmpty()) return@withContext emptyList()

            val batches = smartBatch(paragraphs)
            val results = mutableListOf<ParagraphItem>()

            for (batch in batches) {
                val translated = translateBatch(batch)
                results.addAll(translated)
            }

            results
        }

    /**
     * Translate a single text string.
     */
    suspend fun translateText(text: String): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext text

        val paragraphs = listOf(ParagraphItem(id = 0, text = text))
        val result = translateParagraphs(paragraphs)
        result.firstOrNull()?.text ?: text
    }

    // ─── Internal translation logic ───────────────────────────────────

    /**
     * Translate a single batch of paragraphs.
     * On failure, returns the original paragraphs unchanged.
     */
    private suspend fun translateBatch(batch: List<ParagraphItem>): List<ParagraphItem> {
        return retryWithBackoff(maxRetries = 3, initialDelayMs = 2000L) {
            enforceRateLimit()

            val inputJson = gson.toJson(batch)
            val requestBody = buildRequestBody(inputJson)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody.toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            // Use a local val so we can deterministically close it even if
            // parsing throws — OkHttp response bodies must be closed to allow
            // connection reuse, otherwise connections leak from the pool.
            response.use {
                val body = it.body?.string()
                if (!it.isSuccessful) {
                    if (it.code == 429) {
                        throw RateLimitException("Rate limited by Gemini API")
                    }
                    throw GeminiApiException("API error ${it.code}: $body")
                }
                parseBatchResponse(body ?: "", batch)
            }
        } ?: batch // Fallback to original on failure
    }

    /**
     * Build the Gemini API request body JSON with the professional translator
     * system prompt.
     */
    private fun buildRequestBody(inputJson: String): String {
        val systemPrompt = """
You are a professional light novel translator with 20 years of experience. You specialize in Chinese-to-English translation of web novels and light novels.

TRANSLATION PROCESS:
1. First pass: Produce a literal, faithful translation preserving the original meaning.
2. Second pass: Polish the translation for natural English flow while maintaining the author's voice, tone, and style.

OUTPUT FORMAT:
You MUST output valid JSON matching this exact structure:
[{"id": 0, "text": "translated paragraph"}, {"id": 1, "text": "translated paragraph"}, ...]

CRITICAL RULES:
- Output ONLY the JSON array. No intro, no outro, no notes, no commentary.
- Translate 100% of the text. Leave NO Chinese characters or pinyin in the output.
- Preserve the "id" field exactly as provided in the input.
- Each paragraph must be translated as a complete, coherent unit.

STYLE GUIDELINES:
- Punctuation: Convert …… (Chinese ellipsis) to — (em dash) or ... (English ellipsis) based on context.
- Sound effects (SFX): Translate contextually. E.g., "嗖" → "whoosh", "砰" → "BANG".
- Gender pronouns: Maintain consistency throughout. Infer from context when ambiguous.
- Idioms: Localize to natural English equivalents. E.g., "畫蛇添足" → "gilding the lily" or "overdoing it".
- Paragraph pacing: Follow Western publishing conventions — vary sentence length, use paragraph breaks for emphasis.
- Number scaling: 万 → 10,000; 亿 → 100 million; 千 → 1,000. Write out numbers naturally.
- Brackets: Preserve 【】, 《》, 「」 as [ ], < >, " " respectively, or adapt to context.
- Honorifics: Keep if culturally relevant (e.g., Shifu, Da-shixiong) with brief context on first use.
- Dialogue: Use proper English dialogue formatting with quotation marks.
- Tone: Match the original tone — humorous, dramatic, tense, casual, etc.
        """.trimIndent()

        // Build JSON manually to avoid complex nested Gson serialization
        val escapedSystemPrompt = escapeJson(systemPrompt.trim())
        val escapedInput = escapeJson(inputJson)

        return """
{
  "system_instruction": {"parts": [{"text": "$escapedSystemPrompt"}]},
  "contents": [{"parts": [{"text": "$escapedInput"}]}],
  "generationConfig": {
    "temperature": 0.55,
    "responseMimeType": "application/json"
  },
  "safetySettings": [
    {"category": "HARM_CATEGORY_HARASSMENT", "threshold": "BLOCK_ONLY_HIGH"},
    {"category": "HARM_CATEGORY_HATE_SPEECH", "threshold": "BLOCK_ONLY_HIGH"},
    {"category": "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold": "BLOCK_ONLY_HIGH"},
    {"category": "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold": "BLOCK_ONLY_HIGH"}
  ]
}
        """.trimIndent()
    }

    /**
     * Parse the Gemini API response and extract translated paragraphs.
     */
    private fun parseBatchResponse(
        responseBody: String,
        originalBatch: List<ParagraphItem>
    ): List<ParagraphItem> {
        try {
            val root = JsonParser.parseString(responseBody).asJsonObject
            val candidates = root.getAsJsonArray("candidates")
            if (candidates.size() == 0) return originalBatch

            val content = candidates[0].asJsonObject
                .getAsJsonObject("content")
            val parts = content.getAsJsonArray("parts")
            if (parts.size() == 0) return originalBatch

            val text = parts[0].asJsonObject.get("text").asString

            // Parse the JSON array of translated paragraphs
            val type = object : TypeToken<List<ParagraphItem>>() {}.type
            val translated: List<ParagraphItem> = gson.fromJson(text, type)

            // Verify we got the same number of items back
            if (translated.size != originalBatch.size) {
                return originalBatch
            }

            // Verify IDs match
            val idMismatch = translated.zip(originalBatch).any { (t, o) -> t.id != o.id }
            if (idMismatch) {
                return originalBatch
            }

            return translated
        } catch (e: Exception) {
            return originalBatch
        }
    }

    // ─── Smart batching ───────────────────────────────────────────────

    /**
     * Split paragraphs into batches suitable for Gemini API.
     * Each batch represents 2-3 chapters worth of text,
     * respecting approximate token/character limits.
     */
    private fun smartBatch(
        paragraphs: List<ParagraphItem>,
        maxCharsPerBatch: Int = 12000
    ): List<List<ParagraphItem>> {
        val batches = mutableListOf<List<ParagraphItem>>()
        var currentBatch = mutableListOf<ParagraphItem>()
        var currentSize = 0
        var chapterBreakCount = 0

        for (para in paragraphs) {
            val paraSize = para.text.length

            // Detect chapter breaks (heuristic: short lines containing "第" or "Chapter")
            val isChapterHeading = para.text.length < 100 &&
                (para.text.contains(Regex("第.+[章回節卷]")) ||
                 para.text.contains(Regex("(?i)chapter\\s+\\d")))

            if (isChapterHeading) {
                chapterBreakCount++
                // Start a new batch every 2-3 chapter breaks
                if (chapterBreakCount >= 3 && currentBatch.isNotEmpty()) {
                    batches.add(currentBatch.toList())
                    currentBatch = mutableListOf()
                    currentSize = 0
                    chapterBreakCount = 0
                }
            }

            // Also break on size limit
            if (currentBatch.isNotEmpty() && currentSize + paraSize > maxCharsPerBatch) {
                batches.add(currentBatch.toList())
                currentBatch = mutableListOf()
                currentSize = 0
                chapterBreakCount = 0
            }

            currentBatch.add(para)
            currentSize += paraSize
        }

        if (currentBatch.isNotEmpty()) {
            batches.add(currentBatch.toList())
        }

        return batches
    }

    // ─── Rate limiting ────────────────────────────────────────────────

    /**
     * Enforce rate limits by tracking request timestamps and waiting
     * if necessary.
     *
     * This is a suspend function and uses [delay] rather than [Thread.sleep]
     * so that the calling coroutine (running on Dispatchers.IO) yields its
     * thread while waiting, instead of blocking a precious IO thread for
     * potentially up to a full day on the daily-limit path.
     */
    private suspend fun enforceRateLimit() {
        val limit = rateLimits[model] ?: RateLimit(rpm = 10, rpd = 250)
        val now = System.currentTimeMillis()

        var waitMs = 0L
        synchronized(lock) {
            // Clean up timestamps older than 1 day
            requestTimestamps.removeAll { now - it > 86_400_000 }

            // Check daily limit
            if (requestTimestamps.size >= limit.rpd) {
                val oldestToday = requestTimestamps.first()
                waitMs = maxOf(waitMs, 86_400_000 - (now - oldestToday) + 1000)
            }

            // Check per-minute limit
            val oneMinuteAgo = now - 60_000
            val recentRequests = requestTimestamps.count { it > oneMinuteAgo }
            if (recentRequests >= limit.rpm) {
                // lastOrNull() — `last { ... }` throws NoSuchElementException
                // if no element matches, which can happen if the cleanup above
                // removed all recent timestamps between the count() and now.
                val oldestRecent = requestTimestamps.lastOrNull { it > oneMinuteAgo }
                if (oldestRecent != null) {
                    waitMs = maxOf(waitMs, 60_000 - (now - oldestRecent) + 1000)
                }
            }

            // Record this request attempt even if we're about to wait —
            // otherwise concurrent callers could all pass the limit check.
            requestTimestamps.add(System.currentTimeMillis())
        }

        if (waitMs > 0) {
            delay(waitMs)
        }
    }

    // ─── Retry logic ──────────────────────────────────────────────────

    /**
     * Retry with exponential backoff. On 429 (rate limit) errors,
     * wait longer. Returns null if all retries exhausted.
     */
    private suspend fun <T> retryWithBackoff(
        maxRetries: Int,
        initialDelayMs: Long = 2000L,
        block: suspend () -> T
    ): T? {
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                return block()
            } catch (e: RateLimitException) {
                lastException = e
                // Longer backoff for rate limits
                val delayMs = (initialDelayMs * 2) * (1L shl attempt)
                delay(delayMs)
            } catch (e: Exception) {
                lastException = e
                val delayMs = initialDelayMs * (1L shl attempt)
                delay(delayMs)
            }
        }

        return null
    }

    // ─── Utility ──────────────────────────────────────────────────────

    /**
     * Escape a string for safe embedding in JSON.
     */
    private fun escapeJson(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    // ─── Custom exceptions ────────────────────────────────────────────

    class RateLimitException(message: String) : Exception(message)
    class GeminiApiException(message: String) : Exception(message)
}
