package my.noveldokusha.scraper

import my.noveldokusha.core.appPreferences.AppPreferences
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.scraper.databases.BakaUpdates
import my.noveldokusha.scraper.databases.NovelUpdates
import my.noveldokusha.scraper.sources.BacaLightnovel
import my.noveldokusha.scraper.sources.BoxNovel
import my.noveldokusha.scraper.sources.IndoWebnovel
import my.noveldokusha.scraper.sources.LightNovelWorld
import my.noveldokusha.scraper.sources.LightNovelsTranslations
import my.noveldokusha.scraper.sources.Lnori
import my.noveldokusha.scraper.sources.LocalSource
import my.noveldokusha.scraper.sources.MeioNovel
import my.noveldokusha.scraper.sources.MoreNovel
import my.noveldokusha.scraper.sources.NovelCool
import my.noveldokusha.scraper.sources.NovelFire
import my.noveldokusha.scraper.sources.NovelHall
import my.noveldokusha.scraper.sources.NovelPhoenix
import my.noveldokusha.scraper.sources.Novelku
import my.noveldokusha.scraper.sources.ReadNovelFull
import my.noveldokusha.scraper.sources.Reddit
import my.noveldokusha.scraper.sources.RemoteSourceLoader
import my.noveldokusha.scraper.sources.RoyalRoad
import my.noveldokusha.scraper.sources.Saikai
import my.noveldokusha.scraper.sources.SakuraNovel
import my.noveldokusha.scraper.sources.Sousetsuka
import my.noveldokusha.scraper.sources.TimoTxt
import my.noveldokusha.scraper.sources.TimoTxtGemini
import my.noveldokusha.scraper.sources.TimoTxtTranslate
import my.noveldokusha.scraper.sources.WbNovel
import my.noveldokusha.scraper.sources.Wuxia
import my.noveldokusha.scraper.sources.WuxiaBox
import my.noveldokusha.scraper.sources.WuxiaWorld
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Scraper @Inject constructor(
    networkClient: NetworkClient,
    localSource: LocalSource,
    appPreferences: AppPreferences,
    private val remoteSourceLoader: RemoteSourceLoader,
) {
    val databasesList = setOf(
        NovelUpdates(networkClient),
        BakaUpdates(networkClient)
    )

    // Mutable StateFlow so that ScraperRepository can reactively observe
    // changes when external Lua sources are loaded asynchronously.
    // The flow emits the current set of sources whenever addAll is called.
    private val _sourcesList = kotlinx.coroutines.flow.MutableStateFlow<Set<SourceInterface>>(emptySet())

    val sourcesList: Set<SourceInterface> get() = _sourcesList.value

    /**
     * Reactive flow of the source list. Emits whenever sources are added
     * (including async external Lua source loading).
     */
    val sourcesListFlow: kotlinx.coroutines.flow.StateFlow<Set<SourceInterface>> = _sourcesList

    init {
        _sourcesList.value = setOf(
            localSource,
            // ---- English ----
            LightNovelsTranslations(networkClient),
            ReadNovelFull(networkClient),
            RoyalRoad(networkClient),
            my.noveldokusha.scraper.sources.NovelUpdates(networkClient),
            Reddit(),
            Wuxia(networkClient),
            NovelFire(networkClient),
            NovelPhoenix(networkClient),
            NovelCool(networkClient),
            Lnori(networkClient),
            WuxiaBox(networkClient),
            Sousetsuka(),
            Saikai(networkClient),
            BoxNovel(networkClient),
            LightNovelWorld(networkClient),
            NovelHall(networkClient),
            WuxiaWorld(networkClient),
            MeioNovel(networkClient),
            MoreNovel(networkClient),
            Novelku(networkClient),
            WbNovel(networkClient),
            // ---- Indonesian ----
            IndoWebnovel(networkClient),
            BacaLightnovel(networkClient),
            SakuraNovel(networkClient),
            // ---- Chinese ----
            TimoTxt(networkClient),
            TimoTxtTranslate(networkClient),
            TimoTxtGemini(
                networkClient = networkClient,
                geminiApiKey = appPreferences.GEMINI_API_KEY.value,
                geminiModel = appPreferences.GEMINI_MODEL.value,
            ),
            // ---- MTL (Machine Translation) ----
            my.noveldokusha.scraper.sources.Wtrlab(networkClient),
        )
    }

    // Computed properties — re-evaluate each time so they pick up
    // dynamically-added external Lua sources.
    val sourcesCatalogsList: List<SourceInterface.Catalog>
        get() = sourcesList.filterIsInstance<SourceInterface.Catalog>()

    val sourcesCatalogsLanguagesList: Set<my.noveldokusha.core.LanguageCode>
        get() = sourcesCatalogsList.mapNotNull { it.language }.toSet()

    private fun String.isCompatibleWithBaseUrl(baseUrl: String): Boolean {
        val normalizedUrl = if (this.endsWith("/")) this else "$this/"
        val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return normalizedUrl.startsWith(normalizedBaseUrl)
    }

    fun getCompatibleSource(url: String): SourceInterface? =
        sourcesList.find { url.isCompatibleWithBaseUrl(it.baseUrl) }

    fun getCompatibleSourceCatalog(url: String): SourceInterface.Catalog? =
        sourcesCatalogsList.find { url.isCompatibleWithBaseUrl(it.baseUrl) }

    fun getCompatibleDatabase(url: String): DatabaseInterface? =
        databasesList.find { url.isCompatibleWithBaseUrl(it.baseUrl) }

    /**
     * Load external Lua source plugins from HnDK0's GitHub repository.
     *
     * This is async — call from a background coroutine (e.g. App.onCreate).
     * Sources are added to [sourcesList] as they load, so the catalog
     * explorer picks them up on its next recomputation.
     *
     * Languages loaded: en, zh, mtl (excluding wtrlab from mtl — the user
     * has a custom native Kotlin implementation for that site).
     *
     * On network failure, cached plugins from a previous successful load
     * are still used.
     */
    suspend fun loadExternalSources() {
        try {
            val externalSources = remoteSourceLoader.loadAllSources()
            // Atomically update the StateFlow so that all observers
            // (ScraperRepository → CatalogExplorerViewModel → UI) re-emit.
            _sourcesList.value = _sourcesList.value + externalSources
            Timber.i("Scraper: loaded ${externalSources.size} external HnDK0 sources (total: ${_sourcesList.value.size})")
        } catch (e: Exception) {
            Timber.e(e, "Scraper: failed to load external sources: ${e.message}")
        }
    }

    /**
     * Find alternative sources that can serve the same book as [url].
     *
     * This enables the "switch source" feature: a novel favorited on one
     * TimoTxt source (e.g. TimoTxt) can be opened in either of the other
     * two TimoTxt variants (TimoTxtTranslate, TimoTxtGemini) because they
     * all share the same path structure on timotxt.com
     * (`/{novelId}/` and `/{novelId}/{chNum}.html`).
     */
    fun getAlternativeSources(url: String): List<Pair<SourceInterface.Catalog, String>> {
        val timotxtSourceIds = setOf("timotxt", "timotxt_translate", "timotxt_gemini")

        val currentSource = getCompatibleSourceCatalog(url) ?: return emptyList()
        if (currentSource.id !in timotxtSourceIds) return emptyList()

        val path = runCatching {
            val uri = android.net.Uri.parse(url)
            val p = uri.path ?: return emptyList()
            if (p.isEmpty()) return emptyList()
            p
        }.getOrNull() ?: return emptyList()

        return sourcesCatalogsList
            .filter { it.id in timotxtSourceIds && it.id != currentSource.id }
            .map { source ->
                val convertedUrl = source.baseUrl.trimEnd('/') + path
                source to convertedUrl
            }
    }
}
