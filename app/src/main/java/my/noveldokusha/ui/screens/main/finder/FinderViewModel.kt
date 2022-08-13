package my.noveldokusha.ui.screens.main.finder

import androidx.compose.runtime.getValue
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import my.noveldokusha.AppPreferences
import my.noveldokusha.scraper.Scraper
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.utils.toState
import javax.inject.Inject

data class LanguagesActive(val language: String, val active: Boolean)
data class FinderCatalogItem(val catalog: SourceInterface.Catalog, val pinned: Boolean)

@HiltViewModel
class FinderViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val scraper: Scraper,
) : BaseViewModel() {
    val databaseList = scraper.databasesList.toList()
    val sourcesList by appPreferences.SOURCES_LANGUAGES
        .flow()
        .combine(appPreferences.FINDER_SOURCES_PINNED.flow()) { activeLangs, pinnedSources ->
            scraper
                .sourcesListCatalog
                .filter { it.language in activeLangs }
                .map { FinderCatalogItem(catalog = it, pinned = it.id in pinnedSources) }
                .sortedByDescending { it.pinned }
        }
        .flowOn(Dispatchers.Default)
        .toState(viewModelScope, listOf())

    val languagesList by appPreferences.SOURCES_LANGUAGES
        .flow()
        .map { activeLangs ->
            scraper
                .sourcesLanguages
                .map { LanguagesActive(it, active = activeLangs.contains(it)) }
        }
        .toState(viewModelScope, listOf())

    fun toggleSourceLanguage(language: String) {
        val langs = appPreferences.SOURCES_LANGUAGES.value
        appPreferences.SOURCES_LANGUAGES.value = when (language in langs) {
            true -> langs.minus(language)
            false -> langs.plus(language)
        }
    }

    fun onSourceSetPinned(id: String, pinned: Boolean) {
        appPreferences.FINDER_SOURCES_PINNED.value = appPreferences.FINDER_SOURCES_PINNED
            .value.let { if (pinned) it.plus(id) else it.minus(id) }
    }
}