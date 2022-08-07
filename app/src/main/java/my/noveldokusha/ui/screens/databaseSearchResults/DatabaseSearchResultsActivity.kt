package my.noveldokusha.ui.screens.databaseSearchResults

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import dagger.hilt.android.AndroidEntryPoint
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.screens.databaseBookInfo.DatabaseBookInfoActivity
import my.noveldokusha.ui.theme.Theme
import my.noveldokusha.uiViews.BooksVerticalView
import my.noveldokusha.utils.Extra_String
import my.noveldokusha.utils.Extra_StringArrayList
import my.noveldokusha.utils.capitalize
import my.noveldokusha.utils.copyToClipboard
import java.util.*

@AndroidEntryPoint
class DatabaseSearchResultsActivity : BaseActivity() {
    class IntentData : Intent, DatabaseSearchResultsStateBundle {
        override var databaseUrlBase by Extra_String()
        override var text by Extra_String()
        override var genresIncludeId by Extra_StringArrayList()
        override var genresExcludeId by Extra_StringArrayList()
        override var searchMode by Extra_String()
        override var authorName by Extra_String()
        override var urlAuthorPage by Extra_String()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, databaseUrlBase: String, input: SearchMode) : super(
            ctx,
            DatabaseSearchResultsActivity::class.java
        ) {
            set(databaseUrlBase, input)
        }
    }

    sealed class SearchMode {
        data class Text(val text: String) : SearchMode()
        data class Genres(
            val genresIncludeId: ArrayList<String>,
            val genresExcludeId: ArrayList<String>
        ) : SearchMode()

        data class AuthorSeries(val authorName: String, val urlAuthorPage: String) : SearchMode()
    }

    private val viewModel by viewModels<DatabaseSearchResultsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title =
            getString(R.string.database) + ": " + viewModel.database.name.capitalize(Locale.ROOT)
        val subtitle = when (val res = viewModel.input) {
            is SearchMode.AuthorSeries -> getString(R.string.search_by_author) + ": " + res.authorName
            is SearchMode.Genres -> getString(R.string.search_by_genre)
            is SearchMode.Text -> getString(R.string.search_by_title) + ": " + res.text
        }

        setContent {
            Theme(appPreferences = appPreferences) {
                Column {
                    ToolbarMain(title = title, subtitle = subtitle)
                    BooksVerticalView(
                        layoutMode = viewModel.listLayout,
                        list = viewModel.fetchIterator.list,
                        listState = rememberLazyGridState(),
                        error = viewModel.fetchIterator.error,
                        loadState = viewModel.fetchIterator.state,
                        onLoadNext = { viewModel.fetchIterator.fetchNext() },
                        onBookClicked = ::openBookInfoPage,
                        onBookLongClicked = {},
                        onReload = { viewModel.fetchIterator.reloadFailedLastLoad() },
                        onCopyError = ::copyToClipboard
                    )
                }
            }
        }
    }

    fun openBookInfoPage(book: BookMetadata) {
        DatabaseBookInfoActivity
            .IntentData(this, databaseUrlBase = viewModel.databaseUrlBase, bookMetadata = book)
            .let(::startActivity)
    }
}
