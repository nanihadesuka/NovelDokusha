package my.noveldokusha.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import my.noveldokusha.AppPreferences
import my.noveldokusha.core.LanguageCode
import my.noveldokusha.scraper.Scraper
import my.noveldokusha.scraper.SourceInterface
import javax.inject.Inject
import javax.inject.Singleton

data class LanguageItem(val language: LanguageCode, val active: Boolean)
data class CatalogItem(val catalog: SourceInterface.Catalog, val pinned: Boolean)

@Singleton
class ScraperRepository @Inject constructor(
    private val appPreferences: AppPreferences,
    private val scraper: Scraper,
) {

    fun databaseList(): List<my.noveldokusha.scraper.DatabaseInterface> {
        return scraper.databasesList.toList()
    }

    fun sourcesCatalogListFlow(): Flow<List<CatalogItem>> {
        return combine(
            appPreferences.SOURCES_LANGUAGES_ISO639_1.flow(),
            appPreferences.FINDER_SOURCES_PINNED.flow()
        ) { activeLanguages, pinnedSourcesIds ->
            scraper.sourcesCatalogsList
                .filter { it.language == null || it.language?.iso639_1 in activeLanguages }
                .map { CatalogItem(catalog = it, pinned = it.id in pinnedSourcesIds) }
                .sortedByDescending { it.pinned }
        }.flowOn(Dispatchers.Default)
    }

    fun sourcesLanguagesListFlow(): Flow<List<LanguageItem>> {
        return appPreferences.SOURCES_LANGUAGES_ISO639_1.flow()
            .map { activeLanguages ->
                scraper
                    .sourcesCatalogsLanguagesList
                    .map {
                        LanguageItem(it, active = activeLanguages.contains(it.iso639_1))
                    }
            }
    }
}