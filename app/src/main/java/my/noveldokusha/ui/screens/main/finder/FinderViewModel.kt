package my.noveldokusha.ui.screens.main.finder

import androidx.compose.runtime.getValue
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import my.noveldokusha.AppPreferences
import my.noveldokusha.scraper.Scraper
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.utils.toState
import javax.inject.Inject

data class LanguagesActive(val language: String, val active: Boolean)

@HiltViewModel
class FinderViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val scraper: Scraper,
) : BaseViewModel() {
    val databaseList = scraper.databasesList.toList()
    val sourcesList by appPreferences.SOURCES_LANGUAGES
        .flow()
        .map { activeLangs ->
            scraper
                .sourcesListCatalog
                .filter { it.language in activeLangs }
        }.toState(viewModelScope, listOf())

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
}