package my.noveldokusha.ui.screens.databaseBookInfo

import androidx.compose.runtime.mutableStateOf
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
    stateHandle: SavedStateHandle,
    private val scraper: Scraper
) : BaseViewModel(), DatabaseBookInfoStateBundle {
    override var databaseUrlBase: String by StateExtra_String(stateHandle)
    override var bookUrl: String by StateExtra_String(stateHandle)
    override var bookTitle: String by StateExtra_String(stateHandle)

    val database = scraper.getCompatibleDatabase(databaseUrlBase)!!

    val state = DatabaseBookInfoState(
        databaseName = mutableStateOf(database.name),
        book = mutableStateOf(
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

    )

    init {
        viewModelScope.launch {
            database.getBookData(bookMetadata.url)
                .onSuccess { state.book.value = it }
                .onError { Timber.d(it.exception) }
        }
    }
}