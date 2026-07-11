package my.noveldokusha.text_translator

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import my.noveldokusha.core.appPreferences.AppPreferences
import my.noveldokusha.network.ScraperNetworkClient
import my.noveldokusha.text_translator.domain.GOOGLE_TRANSLATE_LANGUAGES
import my.noveldokusha.text_translator.domain.TranslationManager
import my.noveldokusha.text_translator.domain.TranslationModelState
import my.noveldokusha.text_translator.domain.TranslatorState
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Translation manager using translate-pa.googleapis.com/v1/translateHtml —
 * the same API used by WtrLab plugin. Sends HTML-wrapped paragraphs which gives
 * significantly better quality than the plain-text translate.googleapis.com endpoint.
 */
class TranslationManagerGooglePA(
    private val appPreferences: AppPreferences,
    private val networkClient: ScraperNetworkClient
) : TranslationManager {

    private val client get() = networkClient.client

    override val available = true

    private val translateUrl = "https://translate-pa.googleapis.com/v1/translateHtml"
    private val KEY_CACHE_DURATION_MS = 24 * 60 * 60 * 1000L
    private val keyHeaderRegex = Regex(""""X-Goog-API-Key"\s*:\s*"([^"]+)"""")

    private val keyFetchMutex = Mutex()
    private var keyFetchJob: Deferred<String>? = null

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
        Log.d(TAG, "getTranslator: source=$source, target=$target")
        return TranslatorState(
            source = source,
            target = target,
            translate = { input -> translateSingle(input, source, target) }
        )
    }

    // ─── Key management ────────────────────────────────────────────────────────

    private suspend fun getApiKey(): String = coroutineScope {
        val cachedKey = appPreferences.TRANSLATION_GOOGLE_PA_CACHED_KEY.value
        val lastChecked = appPreferences.TRANSLATION_GOOGLE_PA_KEY_LAST_CHECKED.value.toLongOrNull() ?: 0L
        val now = System.currentTimeMillis()
        if (cachedKey.isNotBlank() && (now - lastChecked) < KEY_CACHE_DURATION_MS) {
            Log.d(TAG, "getApiKey: using cached key (age=${(now - lastChecked) / 1000}s)")
            return@coroutineScope cachedKey
        }

        val deferred: Deferred<String> = keyFetchMutex.withLock {
            val freshKey = appPreferences.TRANSLATION_GOOGLE_PA_CACHED_KEY.value
            val freshChecked = appPreferences.TRANSLATION_GOOGLE_PA_KEY_LAST_CHECKED.value.toLongOrNull() ?: 0L
            if (freshKey.isNotBlank() && (System.currentTimeMillis() - freshChecked) < KEY_CACHE_DURATION_MS) {
                Log.d(TAG, "getApiKey: cache refreshed while waiting for mutex")
                return@coroutineScope freshKey
            }

            keyFetchJob?.let { existing ->
                Log.d(TAG, "getApiKey: joining existing fetch job")
                return@withLock existing
            }

            Log.d(TAG, "getApiKey: starting new fetch job")
            val job = async(Dispatchers.IO) { fetchAndCacheKey() }
            keyFetchJob = job
            job
        }

        try {
            deferred.await()
        } finally {
            keyFetchMutex.withLock {
                if (keyFetchJob === deferred) keyFetchJob = null
            }
        }
    }

    private suspend fun fetchAndCacheKey(): String {
        val now = System.currentTimeMillis()
        val keys = appPreferences.TRANSLATION_GOOGLE_PA_API_KEYS.value
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        for (key in keys) {
            if (checkKey(key)) {
                Log.d(TAG, "fetchAndCacheKey: found working key from preferences")
                cacheKey(key, now)
                return key
            }
        }

        Log.d(TAG, "fetchAndCacheKey: no working key found, fetching from wtr-lab")
        val fetchedKey = fetchKeyFromWtrLab()
        if (fetchedKey != null) {
            Log.d(TAG, "fetchAndCacheKey: got key from wtr-lab, adding to list")
            addKeyToPreferences(fetchedKey)
            cacheKey(fetchedKey, now)
            return fetchedKey
        }

        throw IllegalStateException("Google PA: No working API key found. Check your keys in Settings or try again later.")
    }

    private suspend fun checkKey(key: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = listOf(listOf("<p>test</p>", "en", "en"), "wt_lib")
            val body = Gson().toJson(payload).toRequestBody("application/json+protobuf".toMediaType())
            val request = Request.Builder()
                .url(translateUrl)
                .addHeader("X-Goog-Api-Key", key)
                .addHeader("Origin", "https://translate.google.com")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            val ok = response.isSuccessful
            response.body.close()
            Log.d(TAG, "checkKey: ${key.take(12)}… → HTTP ${response.code}")
            ok
        } catch (e: Exception) {
            Log.w(TAG, "checkKey failed: ${e.message}")
            false
        }
    }

    private fun cacheKey(key: String, timestamp: Long) {
        appPreferences.TRANSLATION_GOOGLE_PA_CACHED_KEY.value = key
        appPreferences.TRANSLATION_GOOGLE_PA_KEY_LAST_CHECKED.value = timestamp.toString()
    }

    private fun addKeyToPreferences(key: String) {
        val existing = appPreferences.TRANSLATION_GOOGLE_PA_API_KEYS.value
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() && it != key }
        appPreferences.TRANSLATION_GOOGLE_PA_API_KEYS.value =
            (listOf(key) + existing).joinToString("\n")
    }

    private suspend fun fetchKeyFromWtrLab(): String? = withContext(Dispatchers.IO) {
        try {
            val rankingHtml = client.newCall(
                Request.Builder()
                    .url("https://wtr-lab.com/en/ranking/monthly")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    .build()
            ).execute().body.string().ifBlank {
                Log.w(TAG, "fetchKeyFromWtrLab: ranking page returned null body")
                return@withContext null
            }

            Log.d(TAG, "fetchKeyFromWtrLab: ranking page length=${rankingHtml.length}")
            val novelMatches = Regex("""href=["']([^"']*/novel/[^"']+)["']""").findAll(rankingHtml).toList()
            Log.d(TAG, "fetchKeyFromWtrLab: /novel/ matches found=${novelMatches.size}")
            if (novelMatches.isEmpty()) {
                val allHrefs = Regex("""href=["']([^"']+)["']""").findAll(rankingHtml)
                    .map { it.groupValues[1] }
                    .take(20)
                    .toList()
                Log.w(TAG, "fetchKeyFromWtrLab: no /novel/ links, all hrefs sample=$allHrefs")
            }

            val novelUrl = novelMatches.map {
                if (it.groupValues[1].startsWith("http")) it.groupValues[1]
                else "https://wtr-lab.com${it.groupValues[1]}"
            }.firstOrNull() ?: run {
                Log.w(TAG, "fetchKeyFromWtrLab: no novel link found on ranking page")
                return@withContext null
            }

            val chapterUrl = novelUrl.trimEnd('/') + "/chapter-1"
            Log.d(TAG, "fetchKeyFromWtrLab: loading chapter page: $chapterUrl")

            val chapterHtml = client.newCall(
                Request.Builder()
                    .url(chapterUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    .build()
            ).execute().body.string().ifBlank { return@withContext null }

            keyHeaderRegex.find(chapterHtml)?.groupValues?.get(1)?.let { key ->
                Log.d(TAG, "fetchKeyFromWtrLab: found key inline in chapter HTML")
                return@withContext key
            }

            val scriptUrls = Regex("""<script[^>]+src=["']([^"']*/_next/[^"']+\.js[^"']*)["']""")
                .findAll(chapterHtml)
                .map { it.groupValues[1] }
                .map { if (it.startsWith("http")) it else "https://wtr-lab.com$it" }
                .filter { !it.contains("_buildManifest") && !it.contains("_ssgManifest") }
                .distinct()
                .toList()

            Log.d(TAG, "fetchKeyFromWtrLab: found ${scriptUrls.size} _next scripts on chapter page")

            if (scriptUrls.isEmpty()) {
                Log.w(TAG, "fetchKeyFromWtrLab: no _next scripts on chapter page")
                return@withContext null
            }

            searchKeyInScripts(scriptUrls)
        } catch (e: Exception) {
            Log.e(TAG, "fetchKeyFromWtrLab failed: ${e.message}")
            null
        }
    }

    private suspend fun searchKeyInScripts(urls: List<String>): String? = withContext(Dispatchers.IO) {
        Log.d(TAG, "searchKeyInScripts: searching ${urls.size} scripts (sequential)")
        for (url in urls) {
            try {
                val js = client.newCall(
                    Request.Builder().url(url).build()
                ).execute().body.string()
                if (js.isBlank()) continue
                val key = keyHeaderRegex.find(js)?.groupValues?.get(1) ?: continue
                Log.d(TAG, "searchKeyInScripts: found key in $url")
                return@withContext key
            } catch (e: Exception) {
                Log.w(TAG, "searchKeyInScripts: failed $url: ${e.message}")
            }
        }
        Log.w(TAG, "searchKeyInScripts: key not found in any script")
        null
    }

    // ─── Translation ────────────────────────────────────────────────────────────

    private suspend fun translateSingle(
        text: String,
        sourceLanguage: String,
        targetLanguage: String,
    ): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext text
        val paragraphs = text.split("\n").filter { it.isNotBlank() }
        if (paragraphs.isEmpty()) return@withContext text
        val sourceLang = if (sourceLanguage == "auto") "auto" else sourceLanguage
        // Let exceptions propagate — caller (ReaderChaptersLoader) will show error to user
        translateChunks(paragraphs, sourceLang, targetLanguage).joinToString("\n")
    }

    private fun unescapeHtmlEntities(text: String): String {
        return text
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace(Regex("&#(\\d+);")) {
                it.groupValues[1].toIntOrNull()
                    ?.toChar()
                    ?.toString()
                    ?: it.value
            }
    }

    private suspend fun translateChunks(
        paragraphs: List<String>,
        sourceLang: String,
        targetLang: String
    ): List<String> {
        val maxChunkChars = 8_000
        val result = paragraphs.toMutableList()

        data class Chunk(val indices: List<Int>, val html: String)

        val chunks = mutableListOf<Chunk>()
        val currentIndices = mutableListOf<Int>()
        val currentParts = mutableListOf<String>()
        var currentLen = 0

        for ((i, para) in paragraphs.withIndex()) {
            if (currentLen > 0 && currentLen + para.length + 4 > maxChunkChars) {
                chunks.add(Chunk(currentIndices.toList(), currentParts.joinToString("<br>")))
                currentIndices.clear()
                currentParts.clear()
                currentLen = 0
            }
            currentIndices.add(i)
            currentParts.add(para)
            currentLen += para.length + 4
        }
        if (currentParts.isNotEmpty()) {
            chunks.add(Chunk(currentIndices.toList(), currentParts.joinToString("<br>")))
        }

        Log.d(TAG, "translateChunks: ${paragraphs.size} paragraphs → ${chunks.size} chunks, $sourceLang→$targetLang")

        val apiKey = getApiKey()
        var failedChunks = 0

        for ((idx, chunk) in chunks.withIndex()) {
            if (idx > 0) delay(400L)

            val translated = try {
                translateHtml(chunk.html, sourceLang, targetLang, apiKey)
            } catch (e: Exception) {
                Log.e(TAG, "Chunk ${idx + 1}/${chunks.size} failed: ${e.message}")
                failedChunks++
                continue
            }

            if (translated == chunk.html) {
                Log.w(TAG, "Chunk ${idx + 1}: translated == original, skipping update")
                continue
            }

            val translatedParas = translated
                .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
                .split("\n")
                .map { unescapeHtmlEntities(it.trim()) }
                .filter { it.isNotBlank() }

            val minSize = minOf(translatedParas.size, chunk.indices.size)
            for (pos in 0 until minSize) {
                result[chunk.indices[pos]] = translatedParas[pos]
            }

            if (translatedParas.size != chunk.indices.size) {
                Log.w(TAG, "Chunk ${idx + 1}: expected ${chunk.indices.size} paragraphs, got ${translatedParas.size}")
            }
        }

        // If ALL chunks failed — throw so the caller can show an error to the user
        if (failedChunks == chunks.size && chunks.isNotEmpty()) {
            throw IllegalStateException("Google PA: All translation chunks failed. Check your internet connection.")
        }

        return result
    }

    private suspend fun translateHtml(
        html: String,
        sourceLang: String,
        targetLang: String,
        apiKey: String
    ): String = withContext(Dispatchers.IO) {
        val payload = listOf(listOf(html, sourceLang, targetLang), "wt_lib")
        val requestBody = Gson().toJson(payload)
            .toRequestBody("application/json+protobuf".toMediaType())

        val request = Request.Builder()
            .url(translateUrl)
            .addHeader("X-Goog-API-Key", apiKey)
            .addHeader("Origin", "https://translate.google.com")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val code = response.code
            response.body.close()
            Log.e(TAG, "translateHtml: HTTP $code")
            throw IllegalStateException("Google PA: HTTP error $code")
        }

        val body = readBodyOrThrow(response, "Google PA")
        try {
            val arr = JsonParser.parseString(body).asJsonArray
            arr.get(0).asJsonArray.get(0).asString
        } catch (e: Exception) {
            Log.e(TAG, "translateHtml: parse error — ${e.message}")
            throw IllegalStateException("Google PA: Failed to parse response — ${e.message}")
        }
    }

    override suspend fun translateBatch(
        texts: List<String>,
        sourceLanguage: String,
        targetLanguage: String,
        systemPromptOverride: String?,
    ): Map<String, String> = withContext(Dispatchers.IO) {
        if (texts.isEmpty()) return@withContext emptyMap()

        val sourceLang = if (sourceLanguage == "auto") "auto" else sourceLanguage

        // PA chunks by character count (8 000-char limit) inside translateChunks — no item-count limit needed.
        val normalizedTexts = texts.filter { it.isNotBlank() }
        if (normalizedTexts.isEmpty()) return@withContext emptyMap()

        val boundaries = mutableListOf<IntRange>()
        val allParagraphs = mutableListOf<String>()
        for (text in normalizedTexts) {
            val lines = text.split("\n").filter { it.isNotBlank() }
            val start = allParagraphs.size
            allParagraphs.addAll(lines)
            boundaries.add(start until start + lines.size)
        }

        // Let exceptions propagate — caller (ReaderChaptersLoader) will show error to user
        val translatedAll = translateChunks(allParagraphs, sourceLang, targetLanguage)

        val result = mutableMapOf<String, String>()
        for ((i, text) in normalizedTexts.withIndex()) {
            val range = boundaries[i]
            if (range.isEmpty()) {
                result[text] = text
                continue
            }
            val safeEnd = range.last.coerceAtMost(translatedAll.size - 1)
            if (safeEnd < range.first) {
                result[text] = text
                continue
            }
            val translatedLines = translatedAll.subList(range.first, safeEnd + 1)
            result[text] = if (translatedLines.isNotEmpty()) translatedLines.joinToString("\n") else text
        }

        Log.d(TAG, "translateBatch: total=${normalizedTexts.size}, translated=${result.size}")
        result
    }

    companion object {
        private const val TAG = "TranslationGooglePA"
    }
}