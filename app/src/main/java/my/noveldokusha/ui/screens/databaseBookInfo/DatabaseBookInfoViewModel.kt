package my.noveldokusha.ui.screens.databaseBookInfo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.scraper.DatabaseInterface
import my.noveldokusha.scraper.Scraper
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.utils.StateExtra_String
import my.noveldokusha.utils.toDocument
import my.noveldokusha.utils.tryAsResponse
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
    private val networkClient: NetworkClient,
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
        viewModelScope.launch(Dispatchers.IO) {
            tryAsResponse {
                val doc = networkClient.get(bookMetadata.url).toDocument()
                val data = withContext(Dispatchers.Default) {
                    scraper.getCompatibleDatabase(databaseUrlBase)!!.getBookData(doc)
                }
                withContext(Dispatchers.Main) {
                    bookData = data
                }
            }.onError {
                Timber.d(it.exception)
            }
        }
    }
}