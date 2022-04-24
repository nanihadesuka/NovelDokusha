package my.noveldokusha.ui.main.Finder

import androidx.compose.runtime.getValue
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import my.noveldokusha.AppPreferences
import my.noveldokusha.scraper.scraper
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.uiUtils.toState
import javax.inject.Inject

@HiltViewModel
class FinderViewModel @Inject constructor(
    appPreferences: AppPreferences
) : BaseViewModel()
{
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
}