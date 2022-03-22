package my.noveldokusha.ui.databaseBookInfo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import my.noveldokusha.AppPreferences
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.scraper.DatabaseInterface
import my.noveldokusha.scraper.Response
import my.noveldokusha.ui.databaseSearchResults.DatabaseSearchResultsActivity
import my.noveldokusha.ui.globalSourceSearch.GlobalSourceSearchActivity
import my.noveldokusha.ui.theme.Theme
import my.noveldokusha.uiUtils.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@AndroidEntryPoint
class DatabaseBookInfoActivity : ComponentActivity() {
    class IntentData : Intent, DatabaseBookInfoStateBundle {
        override var databaseUrlBase by Extra_String()
        override var bookUrl by Extra_String()
        override var bookTitle by Extra_String()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, databaseUrlBase: String, bookMetadata: BookMetadata) : super(
            ctx,
            DatabaseBookInfoActivity::class.java
        ) {
            this.databaseUrlBase = databaseUrlBase
            this.bookUrl = bookMetadata.url
            this.bookTitle = bookMetadata.title
        }
    }

    @Inject
    lateinit var appPreferences: AppPreferences

    private val viewModel by viewModels<DatabaseBookInfoViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val reply by viewModel.bookData.collectAsState(initial = null)

            Theme(appPreferences = appPreferences) {

                if (reply != null) {
                    when (val data = reply) {
                        is Response.Success -> DatabaseBookInfoView(
                            data = data.data,
                            onSourcesClick = ::openGlobalSearch,
                            onAuthorsClick = ::openSearchByAuthor,
                            onGenresClick = ::openSearchByGenres,
                            onBookClick = ::openSearchByTitle
                        )
                        else -> Unit
                    }
                }
            }
        }
    }

    fun openGlobalSearch() {
        GlobalSourceSearchActivity.IntentData(
            this,
            input = viewModel.bookMetadata.title
        ).let(this@DatabaseBookInfoActivity::startActivity)
    }

    fun openSearchByAuthor(author: DatabaseInterface.BookAuthor) {
        if (author.url == null)
            return
        val input = DatabaseSearchResultsActivity.SearchMode.AuthorSeries(
            authorName = author.name, urlAuthorPage = author.url
        )
        val intent = DatabaseSearchResultsActivity.IntentData(
            this@DatabaseBookInfoActivity,
            viewModel.database.baseUrl,
            input
        )
        startActivity(intent)
    }

    fun openSearchByGenres(genres: List<String>) {
        val input = DatabaseSearchResultsActivity.SearchMode.Genres(
            genresIncludeId = ArrayList(genres),
            genresExcludeId = arrayListOf()
        )
        val intent = DatabaseSearchResultsActivity.IntentData(
            this@DatabaseBookInfoActivity,
            viewModel.database.baseUrl,
            input
        )
        startActivity(intent)
    }

    fun openSearchByTitle(book: BookMetadata) {
        val intent = IntentData(
            this@DatabaseBookInfoActivity,
            databaseUrlBase = viewModel.database.baseUrl,
            bookMetadata = book
        )
        startActivity(intent)
    }
}
