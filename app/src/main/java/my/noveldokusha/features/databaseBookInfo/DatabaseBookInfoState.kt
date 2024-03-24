package my.noveldokusha.features.databaseBookInfo

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import my.noveldokusha.scraper.DatabaseInterface

data class DatabaseBookInfoState(
    val databaseNameStrId: State<Int>,
    val book: MutableState<DatabaseInterface.BookData>,
)