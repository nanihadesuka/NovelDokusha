package my.noveldokusha.ui.screens.databaseBookInfo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.scraper.DatabaseInterface
import my.noveldokusha.scraper.Scraper
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.utils.StateExtra_String
import timber.log.Timber
import javax.inject.Inject

interface DatabaseBookInfoStateBundle {
    val bookMetadata get() = BookMetadata(title = bookTitle, url = bookUrl)

    var databaseUrlBase: String
    var bookUrl: String
    var bookTitle: String
}

@HiltViewModel
class DatabaseBookInfoViewModel @Inject constructor(
    state: SavedStateHandle,
    private val scraper: Scraper
) : BaseViewModel(), DatabaseBookInfoStateBundle {
    override var databaseUrlBase: String by StateExtra_String(state)
    override var bookUrl: String by StateExtra_String(state)
    override var bookTitle: String by StateExtra_String(state)

    val database get() = scraper.getCompatibleDatabase(databaseUrlBase)!!

    var bookData by mutableStateOf(
        DatabaseInterface.BookData(
            title = bookTitle,
            description = "",
            coverImageUrl = null,
            alternativeTitles = listOf(),
            authors = listOf(),
            tags = listOf(),
            genres = listOf(),
            bookType = "",
            relatedBooks = listOf(),
            similarRecommended = listOf()
        )
    )

    init {

        viewModelScope.launch {
            database.getBookData(bookMetadata.url)
                .onSuccess { bookData = it }
                .onError { Timber.d(it.exception) }
        }
    }
}