package my.noveldokusha.scraper

import my.noveldokusha.network.NetworkClient
import my.noveldokusha.scraper.databases.BakaUpdates
import my.noveldokusha.scraper.databases.NovelUpdates
import my.noveldokusha.scraper.sources.*

class Scraper(
    networkClient: NetworkClient
) {
    val databasesList = setOf<DatabaseInterface>(
        NovelUpdates(networkClient),
        BakaUpdates(networkClient)
    )

    val sourcesList = setOf<SourceInterface>(
        LightNovelsTranslations(networkClient),
        ReadLightNovel(networkClient),
        ReadNovelFull(networkClient),
        my.noveldokusha.scraper.sources.NovelUpdates(networkClient),
        Reddit(networkClient),
        AT(networkClient),
        Wuxia(networkClient),
        BestLightNovel(networkClient),
        _1stKissNovel(networkClient),
        Sousetsuka(networkClient),
        Saikai(networkClient),
        BoxNovel(networkClient),
        LightNovelWorld(networkClient),
        NovelHall(networkClient),
        MTLNovel(networkClient),
        WuxiaWorld(networkClient),
        KoreanNovelsMTL(networkClient),
    )

    val sourcesListCatalog = sourcesList.filterIsInstance<SourceInterface.Catalog>().toSet()
    val sourcesLanguages =
        sourcesList.filterIsInstance<SourceInterface.Catalog>().map { it.language }.toSortedSet()

    private fun String.isCompatibleWithBaseUrl(baseUrl: String): Boolean {
        val normalizedUrl = if (this.endsWith("/")) this else "$this/"
        val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return normalizedUrl.startsWith(normalizedBaseUrl)
    }

    fun getCompatibleSource(url: String): SourceInterface? =
        sourcesList.find { url.isCompatibleWithBaseUrl(it.baseUrl) }

    fun getCompatibleSourceCatalog(url: String): SourceInterface.Catalog? =
        sourcesListCatalog.find { url.isCompatibleWithBaseUrl(it.baseUrl) }

    fun getCompatibleDatabase(url: String): DatabaseInterface? =
        databasesList.find { url.isCompatibleWithBaseUrl(it.baseUrl) }
}
