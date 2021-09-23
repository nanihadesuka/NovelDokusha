package my.noveldokusha.ui.globalSourceSearch

import android.os.Parcelable
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import my.noveldokusha.App
import my.noveldokusha.SOURCES_LANGUAGES
import my.noveldokusha.SOURCES_LANGUAGES_flow
import my.noveldokusha.appSharedPreferences
import my.noveldokusha.scraper.FetchIterator
import my.noveldokusha.scraper.scrubber
import my.noveldokusha.scraper.source_interface
import my.noveldokusha.ui.BaseViewModel

class GlobalSourceSearchModel(val input: String) : BaseViewModel()
{
    private val preferences = App.instance.appSharedPreferences()

    val globalResults = preferences.SOURCES_LANGUAGES.let { activeLangs ->
        scrubber.sourcesListCatalog
            .filter { it.language in activeLangs }
            .map { SourceResults(it, input, viewModelScope) }
    }
}

data class SourceResults(val source: source_interface.catalog, val searchInput: String, val coroutineScope: CoroutineScope)
{
    var savedState: Parcelable? = null
    val booksFetchIterator = FetchIterator(coroutineScope) { source.getCatalogSearch(it, searchInput) }

    init
    {
        booksFetchIterator.fetchNext()
    }
}
