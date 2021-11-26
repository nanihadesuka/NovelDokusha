package my.noveldokusha.ui.globalSourceSearch

import android.content.Context
import android.content.SharedPreferences
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import my.noveldokusha.SOURCES_LANGUAGES
import my.noveldokusha.appSharedPreferences
import my.noveldokusha.scraper.FetchIterator
import my.noveldokusha.scraper.scrubber
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.uiUtils.StateExtra_String
import javax.inject.Inject

interface GlobalSourceSearchStateBundle
{
    val input: String
}

@HiltViewModel
class GlobalSourceSearchModel @Inject constructor(
    @ApplicationContext context: Context,
    state: SavedStateHandle,
    val preferences: SharedPreferences
) : BaseViewModel(), GlobalSourceSearchStateBundle
{
    override val input by StateExtra_String(state)

    val globalResults = preferences.SOURCES_LANGUAGES.let { activeLangs ->
        scrubber.sourcesListCatalog
            .filter { it.language in activeLangs }
            .map { SourceResults(it, input, viewModelScope) }
    }
}

data class SourceResults(val source: SourceInterface.catalog, val searchInput: String, val coroutineScope: CoroutineScope)
{
    var savedState: Parcelable? = null
    val booksFetchIterator = FetchIterator(coroutineScope) { source.getCatalogSearch(it, searchInput) }

    init
    {
        booksFetchIterator.fetchNext()
    }
}
