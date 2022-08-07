package my.noveldokusha.ui.screens.databaseBookInfo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import my.noveldokusha.AppPreferences
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.scraper.DatabaseInterface
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.screens.databaseSearchResults.DatabaseSearchResultsActivity
import my.noveldokusha.ui.screens.globalSourceSearch.GlobalSourceSearchActivity
import my.noveldokusha.ui.theme.Theme
import my.noveldokusha.uiViews.SetSystemBarTransparent
import my.noveldokusha.utils.Extra_String
import javax.inject.Inject

@AndroidEntryPoint
class DatabaseBookInfoActivity : BaseActivity() {
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

    private val viewModel by viewModels<DatabaseBookInfoViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val scrollState = rememberScrollState()
            val alpha by derivedStateOf {
                val value = (scrollState.value - 50).coerceIn(0, 200).toFloat()
                val maxvalue = (scrollState.maxValue).coerceIn(1, 200).toFloat()
                value / maxvalue
            }

            Theme(appPreferences = appPreferences) {

                SetSystemBarTransparent(alpha)
                DatabaseBookInfoView(
                    scrollState = scrollState,
                    data = viewModel.bookData,
                    onSourcesClick = ::openGlobalSearchPage,
                    onAuthorsClick = ::openSearchPageByAuthor,
                    onGenresClick = ::openSearchPageByGenres,
                    onBookClick = ::openSearchPageByTitle
                )
            }
        }
    }

    fun openGlobalSearchPage() {
        GlobalSourceSearchActivity.IntentData(
            this,
            input = viewModel.bookMetadata.title
        ).let(this@DatabaseBookInfoActivity::startActivity)
    }

    fun openSearchPageByAuthor(author: DatabaseInterface.BookAuthor) {
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

    fun openSearchPageByGenres(genres: List<String>) {
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

    fun openSearchPageByTitle(book: BookMetadata) {
        val intent = IntentData(
            this@DatabaseBookInfoActivity,
            databaseUrlBase = viewModel.database.baseUrl,
            bookMetadata = book
        )
        startActivity(intent)
    }
}
