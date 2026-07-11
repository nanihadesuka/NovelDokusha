package my.noveldokusha.scraper.sources

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.core.LanguageCode
import my.noveldokusha.lua_engine.LuaEngine
import my.noveldokusha.network.NetworkClient
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads external Lua source plugins from HnDK0's GitHub repository at
 * runtime (not copied into the app — always uses the latest version from
 * https://github.com/HnDK0/external-sources).
 *
 * Supported languages: en, zh, mtl (machine translation).
 * The wtrlab source from mtl is EXCLUDED (the user has a custom native
 * Kotlin implementation instead).
 *
 * Plugins are cached in the app's cache directory so they work offline
 * after the first load. The cache is refreshed on each app start (if
 * network is available).
 *
 * The loaded sources use LanguageCode variants (ENGLISH_HNDK0,
 * CHINESE_HNDK0, MTL_HNDK0) so they appear as separate filter options
 * in the catalog explorer: "English (HnDK0)", "Chinese (HnDK0)",
 * "MTL (HnDK0)".
 */
@Singleton
class RemoteSourceLoader @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val networkClient: NetworkClient,
    private val luaEngine: LuaEngine,
) {
    companion object {
        private const val INDEX_BASE_URL =
            "https://raw.githubusercontent.com/HnDK0/external-sources/refs/heads/main"

        // Languages to load from HnDK0 (user requested en, zh, mtl only).
        // The wtrlab source is excluded from mtl.
        private val SUPPORTED_LANGS = setOf("en", "zh", "mtl")
        private val EXCLUDED_SOURCES = setOf("wtrlab")

        private const val CACHE_DIR = "lua_plugins"
    }

    private val cacheDir = File(appContext.cacheDir, CACHE_DIR).also { it.mkdirs() }

    /**
     * Fetch the index.yaml for a given language, download all .lua plugins
     * (excluding wtrlab), and return a list of source adapters.
     *
     * For zh (Chinese) sources, TWO adapters are created per plugin:
     *   1. A raw [LuaSourceAdapter] under the "Chinese (HnDK0)" language
     *      (shows original Chinese text)
     *   2. A [LuaTranslatedSourceAdapter] under the "MTL (HnDK0)" language
     *      with "(Translate)" suffix (translates Chinese → English via
     *      Google Translate API, same engine as TimoTxtTranslate)
     *
     * This is a suspend function — call from a background coroutine.
     * Returns an empty list on network failure (cached plugins from a
     * previous successful load are still returned).
     */
    suspend fun loadAllSources(): List<my.noveldokusha.scraper.SourceInterface> = withContext(Dispatchers.IO) {
        val results = mutableListOf<my.noveldokusha.scraper.SourceInterface>()

        for (lang in SUPPORTED_LANGS) {
            try {
                val sources = loadSourcesForLanguage(lang)
                results.addAll(sources)
                Timber.i("RemoteSourceLoader: loaded ${sources.size} sources for lang=$lang")
            } catch (e: Exception) {
                Timber.e(e, "RemoteSourceLoader: failed to load lang=$lang: ${e.message}")
            }
        }

        Timber.i("RemoteSourceLoader: total loaded ${results.size} external sources")
        results
    }

    private suspend fun loadSourcesForLanguage(lang: String): List<my.noveldokusha.scraper.SourceInterface> {
        val indexUrl = "$INDEX_BASE_URL/$lang/index.yaml"

        // Fetch the index
        val indexText = fetchText(indexUrl) ?: run {
            Timber.w("RemoteSourceLoader: failed to fetch index for lang=$lang, trying cache")
            return loadFromCache(lang)
        }

        // Parse the index
        val sources = parseIndexYaml(indexText, lang)

        // Filter out excluded sources (wtrlab)
        val filtered = sources.filter { it.id !in EXCLUDED_SOURCES }
        Timber.i("RemoteSourceLoader: ${filtered.size} sources for lang=$lang (after excluding ${sources.size - filtered.size})")

        // Download and cache each plugin's .lua file
        val adapters = mutableListOf<my.noveldokusha.scraper.SourceInterface>()
        for (src in filtered) {
            try {
                val luaScript = fetchText(src.url) ?: loadFromCacheFile(src.id, lang)
                if (luaScript != null) {
                    saveToCacheFile(src.id, lang, luaScript)

                    // Create the standard adapter (raw, no translation)
                    val adapter = createAdapter(src, luaScript, lang)
                    if (adapter != null) adapters.add(adapter)

                    // For zh sources, also create a translated variant
                    // that translates Chinese → English via Google Translate API
                    if (lang == "zh") {
                        val translatedAdapter = createTranslatedAdapter(src, luaScript)
                        if (translatedAdapter != null) adapters.add(translatedAdapter)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "RemoteSourceLoader: failed to load plugin '${src.id}': ${e.message}")
            }
        }

        return adapters
    }

    private suspend fun fetchText(url: String): String? = try {
        val response = networkClient.get(url)
        val body = response.body?.string()
        response.close()
        if (body != null && body.isNotEmpty()) body else null
    } catch (e: Exception) {
        Timber.w("RemoteSourceLoader: fetch failed for $url: ${e.message}")
        null
    }

    private fun parseIndexYaml(yamlText: String, lang: String): List<HnDK0Source> {
        // Simple YAML parser for the predictable index.yaml format.
        // Each source entry has: id, name, version, url, icon, language.
        val sources = mutableListOf<HnDK0Source>()
        var currentSource: MutableMap<String, String>? = null

        for (line in yamlText.lines()) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("- id:") -> {
                    currentSource?.let { sources.add(HnDK0Source.fromMap(it)) }
                    currentSource = mutableMapOf()
                    currentSource["id"] = trimmed.substringAfter("id:").trim().trim('"')
                }
                currentSource != null && trimmed.contains(":") -> {
                    val key = trimmed.substringBefore(":").trim()
                    val value = trimmed.substringAfter(":").trim().trim('"')
                    currentSource[key] = value
                }
            }
        }
        currentSource?.let { sources.add(HnDK0Source.fromMap(it)) }

        return sources
    }

    private fun createAdapter(src: HnDK0Source, luaScript: String, lang: String): LuaSourceAdapter? {
        val languageCode = when (lang) {
            "en" -> LanguageCode.ENGLISH_HNDK0
            "zh" -> LanguageCode.CHINESE_HNDK0
            "mtl" -> LanguageCode.MTL_HNDK0
            else -> null
        } ?: return null

        val displayName = "${src.name} (HnDK0)"

        // Parse baseUrl from the Lua script — it's a global variable
        // like: baseUrl = "https://www.example.com"
        // Without this, all sources get baseUrl="https://example.com/"
        // and getCompatibleSource() can never match them.
        val baseUrl = parseBaseUrlFromLua(luaScript)?.let {
            if (it.endsWith("/")) it else "$it/"
        } ?: "https://example.com/"

        return LuaSourceAdapter(
            luaEngine = luaEngine,
            pluginScript = luaScript,
            pluginId = src.id,
            _displayName = displayName,
            baseUrl = baseUrl,
            languageCode = languageCode,
            iconUrlStr = src.icon ?: "",
        )
    }

    /**
     * Create a translated variant of a zh source. Uses the same Lua plugin
     * for site structure but wraps all text output with Google Translate
     * API translation (same engine as TimoTxtTranslate).
     *
     * Display name: "SourceName (Translate)" — appears under "MTL (HnDK0)"
     * in the catalog language filter.
     */
    private fun createTranslatedAdapter(src: HnDK0Source, luaScript: String): LuaTranslatedSourceAdapter? {
        // Parse baseUrl from the Lua script (it's a global variable)
        val baseUrl = parseBaseUrlFromLua(luaScript) ?: "https://example.com/"
        val displayName = "${src.name} (Translate)"

        return LuaTranslatedSourceAdapter(
            luaEngine = luaEngine,
            networkClient = networkClient,
            pluginScript = luaScript,
            pluginId = src.id,
            _displayName = displayName,
            baseUrl = baseUrl,
            iconUrlStr = src.icon ?: "",
        )
    }

    /**
     * Extract the baseUrl global variable from a Lua plugin script.
     * Returns null if not found.
     */
    private fun parseBaseUrlFromLua(script: String): String? {
        // Match: baseUrl = "https://..." or baseUrl  =  "https://..."
        val regex = Regex("""(?m)^baseUrl\s*=\s*["']([^"']+)["']""")
        return regex.find(script)?.groupValues?.get(1)
    }

    // ─── Cache management ───────────────────────────────────────────

    private fun cacheFile(pluginId: String, lang: String) =
        File(cacheDir, "${lang}_${pluginId}.lua")

    private fun saveToCacheFile(pluginId: String, lang: String, content: String) {
        try {
            cacheFile(pluginId, lang).writeText(content)
        } catch (e: Exception) {
            Timber.w("RemoteSourceLoader: cache save failed for $pluginId: ${e.message}")
        }
    }

    private fun loadFromCacheFile(pluginId: String, lang: String): String? = try {
        val file = cacheFile(pluginId, lang)
        if (file.exists() && file.length() > 0) file.readText() else null
    } catch (e: Exception) {
        null
    }

    private fun loadFromCache(lang: String): List<my.noveldokusha.scraper.SourceInterface> {
        val adapters = mutableListOf<my.noveldokusha.scraper.SourceInterface>()
        val files = cacheDir.listFiles { f -> f.name.startsWith("${lang}_") && f.name.endsWith(".lua") }
            ?: return emptyList()

        for (file in files) {
            try {
                val luaScript = file.readText()
                val pluginId = file.name.removePrefix("${lang}_").removeSuffix(".lua")
                val src = HnDK0Source(id = pluginId, name = pluginId, url = "", icon = null)
                val adapter = createAdapter(src, luaScript, lang)
                if (adapter != null) adapters.add(adapter)

                // For zh sources, also create translated variant from cache
                if (lang == "zh") {
                    val translatedAdapter = createTranslatedAdapter(src, luaScript)
                    if (translatedAdapter != null) adapters.add(translatedAdapter)
                }
            } catch (_: Exception) {}
        }
        return adapters
    }

    // ─── Data class for parsed index entries ────────────────────────

    private data class HnDK0Source(
        val id: String,
        val name: String,
        val url: String,
        val icon: String?,
    ) {
        companion object {
            fun fromMap(map: Map<String, String>) = HnDK0Source(
                id = map["id"] ?: "",
                name = map["name"] ?: map["id"] ?: "",
                url = map["url"] ?: "",
                icon = map["icon"],
            )
        }
    }
}
