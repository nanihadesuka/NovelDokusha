package my.noveldoksuha.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import my.noveldokusha.core.appPreferences.AppPreferences
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

    /**
     * Reactive flow of catalog sources filtered by active languages and
     * sorted by pinned status.
     *
     * Combines THREE flows:
     *   1. SOURCES_LANGUAGES_ISO639_1 — user's active language filters
     *   2. FINDER_SOURCES_PINNED — user's pinned source IDs
     *   3. scraper.sourcesListFlow — the source list itself (emits when
     *      built-in sources are loaded at startup AND when external Lua
     *      sources are loaded asynchronously from HnDK0's GitHub repo)
     *
     * Without observing the source list flow, external Lua sources loaded
     * after app start would never appear in the UI.
     */
    fun sourcesCatalogListFlow(): Flow<List<CatalogItem>> {
        return combine(
            appPreferences.SOURCES_LANGUAGES_ISO639_1.flow(),
            appPreferences.FINDER_SOURCES_PINNED.flow(),
            scraper.sourcesListFlow
        ) { activeLanguages, pinnedSourcesIds, sourcesList ->
            sourcesList
                .filterIsInstance<SourceInterface.Catalog>()
                .filter { it.language == null || it.language?.iso639_1 in activeLanguages }
                .map { CatalogItem(catalog = it, pinned = it.id in pinnedSourcesIds) }
                .sortedByDescending { it.pinned }
        }.flowOn(Dispatchers.Default)
    }

    /**
     * Reactive flow of available source languages with active status.
     * Also observes scraper.sourcesListFlow so new languages appear when
     * external Lua sources are loaded.
     */
    fun sourcesLanguagesListFlow(): Flow<List<LanguageItem>> {
        return combine(
            appPreferences.SOURCES_LANGUAGES_ISO639_1.flow(),
            scraper.sourcesListFlow
        ) { activeLanguages, sourcesList ->
            sourcesList
                .filterIsInstance<SourceInterface.Catalog>()
                .mapNotNull { it.language }
                .toSet()
                .map { language ->
                    LanguageItem(language, active = activeLanguages.contains(language.iso639_1))
                }
        }
    }
}
