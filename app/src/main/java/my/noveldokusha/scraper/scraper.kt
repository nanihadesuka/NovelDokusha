package my.noveldokusha.scraper

import BoxNovel
import my.noveldokusha.scraper.databases.BakaUpdates
import my.noveldokusha.scraper.databases.NovelUpdates
import my.noveldokusha.scraper.sources.*

object scraper
{
    val databasesList = setOf<DatabaseInterface>(
        NovelUpdates(),
        BakaUpdates()
    )

    val sourcesList = setOf<SourceInterface>(
        LightNovelsTranslations(),
        ReadLightNovel(),
        ReadNovelFull(),
        my.noveldokusha.scraper.sources.NovelUpdates(),
        Reddit(),
        AT(),
        Wuxia(),
        BestLightNovel(),
        _1stKissNovel(),
        Sousetsuka(),
        Saikai(),
        BoxNovel(),
        LightNovelWorld(),
        NovelHall(),
        MTLNovel(),
        WuxiaWorld(),
        Aixdzs(),
    )

    val sourcesListCatalog = sourcesList.filterIsInstance<SourceInterface.Catalog>().toSet()
    val sourcesLanguages = sourcesList.filterIsInstance<SourceInterface.Catalog>().map { it.language }.toSortedSet()

    private fun String.isCompatibleWithBaseUrl(baseUrl: String): Boolean
    {
        val normalizedUrl = if (this.endsWith("/")) this else "$this/"
        val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return normalizedUrl.startsWith(normalizedBaseUrl)
    }

    fun getCompatibleSource(url: String): SourceInterface? = sourcesList.find { url.isCompatibleWithBaseUrl(it.baseUrl) }
    fun getCompatibleSourceCatalog(url: String): SourceInterface.Catalog? = sourcesListCatalog.find { url.isCompatibleWithBaseUrl(it.baseUrl) }
    fun getCompatibleDatabase(url: String): DatabaseInterface? = databasesList.find { url.isCompatibleWithBaseUrl(it.baseUrl)}
}
