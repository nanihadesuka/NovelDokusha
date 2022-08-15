package my.noveldokusha.ui.screens.main.finder

import androidx.compose.runtime.getValue
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import my.noveldokusha.AppPreferences
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.ui.repositories.ScraperRepository
import my.noveldokusha.utils.toState
import javax.inject.Inject



@HiltViewModel
class FinderViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val scraperRepository: ScraperRepository,
) : BaseViewModel() {
    val databaseList = scraperRepository.databaseList()
    val sourcesList by scraperRepository.sourcesCatalogListFlow()
        .toState(viewModelScope, listOf())

    val languagesList by scraperRepository.sourcesLanguagesListFlow()
        .toState(viewModelScope, listOf())

    fun toggleSourceLanguage(language: String) {
        val languages = appPreferences.SOURCES_LANGUAGES.value
        appPreferences.SOURCES_LANGUAGES.value = when (language in languages) {
            true -> languages.minus(language)
            false -> languages.plus(language)
        }
    }

    fun onSourceSetPinned(id: String, pinned: Boolean) {
        appPreferences.FINDER_SOURCES_PINNED.value = appPreferences.FINDER_SOURCES_PINNED
            .value.let { if (pinned) it.plus(id) else it.minus(id) }
    }
}