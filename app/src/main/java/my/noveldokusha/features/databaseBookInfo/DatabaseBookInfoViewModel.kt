package my.noveldokusha.features.databaseBookInfo

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import my.noveldokusha.tooling.local_database.BookMetadata
import my.noveldokusha.scraper.DatabaseInterface
import my.noveldokusha.scraper.Scraper
import my.noveldoksuha.coreui.BaseViewModel
import my.noveldokusha.core.utils.StateExtra_String
import timber.log.Timber
import javax.inject.Inject

interface DatabaseBookInfoStateBundle {
    val bookMetadata
        get() = BookMetadata(
            title = bookTitle,
            url = bookUrl
        )

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
        databaseNameStrId = mutableStateOf(database.nameStrId),
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