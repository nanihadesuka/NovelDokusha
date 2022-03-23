package my.noveldokusha.ui.databaseSearchResults

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import my.noveldokusha.AppPreferences
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.ui.databaseBookInfo.DatabaseBookInfoActivity
import my.noveldokusha.ui.theme.Theme
import my.noveldokusha.uiUtils.Extra_String
import my.noveldokusha.uiUtils.Extra_StringArrayList
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class DatabaseSearchResultsActivity : ComponentActivity() {
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

    @Inject
    lateinit var appPreferences: AppPreferences

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
                DatabaseSearchResultsView(
                    title = title,
                    subtitle = subtitle,
                    list = viewModel.fetchIterator.list,
                    error = viewModel.fetchIterator.error,
                    loadState = viewModel.fetchIterator.state,
                    onLoadNext = { viewModel.fetchIterator.fetchNext() },
                    onBookClicked = ::openBook,
                )
            }
        }
    }

    fun openBook(book: BookMetadata) {
        DatabaseBookInfoActivity
            .IntentData(this, databaseUrlBase = viewModel.databaseUrlBase, bookMetadata = book)
            .let(::startActivity)
    }
}
