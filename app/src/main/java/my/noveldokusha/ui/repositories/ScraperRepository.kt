package my.noveldokusha.ui.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import my.noveldokusha.AppPreferences
import my.noveldokusha.scraper.DatabaseInterface
import my.noveldokusha.scraper.Scraper
import my.noveldokusha.scraper.SourceInterface

data class SourceLanguageItem(val language: String, val active: Boolean)
data class SourceCatalogItem(val catalog: SourceInterface.Catalog, val pinned: Boolean)

class ScraperRepository(
    private val appPreferences: AppPreferences,
    private val scraper: Scraper,
) {

    fun databaseList(): List<DatabaseInterface> {
        return scraper.databasesList.toList()
    }

    fun sourcesCatalogListFlow(): Flow<List<SourceCatalogItem>> {
        return combine(
            appPreferences.SOURCES_LANGUAGES.flow(),
            appPreferences.FINDER_SOURCES_PINNED.flow()
        ) { activeLanguages, pinnedSourcesIds ->
            scraper.sourcesListCatalog
                .filter { it.language in activeLanguages }
                .map { SourceCatalogItem(catalog = it, pinned = it.id in pinnedSourcesIds) }
                .sortedByDescending { it.pinned }
        }.flowOn(Dispatchers.Default)
    }

    fun sourcesLanguagesListFlow(): Flow<List<SourceLanguageItem>> {
        return appPreferences.SOURCES_LANGUAGES.flow()
            .map { activeLanguages ->
                scraper
                    .sourcesLanguages
                    .map { SourceLanguageItem(it, active = activeLanguages.contains(it)) }
            }
    }
}