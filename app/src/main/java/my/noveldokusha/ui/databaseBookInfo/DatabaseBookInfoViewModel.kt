package my.noveldokusha.ui.databaseBookInfo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.scraper.*
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.uiUtils.StateExtra_String
import timber.log.Timber
import javax.inject.Inject

interface DatabaseBookInfoStateBundle {
    val bookMetadata get() = BookMetadata(title = bookTitle, url = bookUrl)
    val database get() = scraper.getCompatibleDatabase(databaseUrlBase)!!

    var databaseUrlBase: String
    var bookUrl: String
    var bookTitle: String
}

@HiltViewModel
class DatabaseBookInfoViewModel @Inject constructor(
    state: SavedStateHandle
) : BaseViewModel(), DatabaseBookInfoStateBundle {
    override var databaseUrlBase: String by StateExtra_String(state)
    override var bookUrl: String by StateExtra_String(state)
    override var bookTitle: String by StateExtra_String(state)


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
            try {
                val doc = fetchDoc(bookMetadata.url)
                val data = withContext(Dispatchers.Default) { database.getBookData(doc) }
                withContext(Dispatchers.Main) {
                    bookData = data
                }
            } catch (e: Exception) {
                Timber.d("getBookData", e)
            }
        }
    }
}