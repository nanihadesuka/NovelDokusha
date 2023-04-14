package my.noveldokusha.ui.screens.databaseBookInfo

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import my.noveldokusha.scraper.DatabaseInterface

data class DatabaseBookInfoState(
    val databaseName: State<String>,
    val book: MutableState<DatabaseInterface.BookData>,
)