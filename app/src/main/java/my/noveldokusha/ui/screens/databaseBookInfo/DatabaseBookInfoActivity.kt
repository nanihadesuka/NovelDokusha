package my.noveldokusha.ui.screens.databaseBookInfo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import my.noveldokusha.composableActions.SetSystemBarTransparent
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.scraper.DatabaseInterface
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.screens.databaseSearch.DatabaseSearchActivity
import my.noveldokusha.ui.screens.globalSourceSearch.GlobalSourceSearchActivity
import my.noveldokusha.ui.theme.Theme
import my.noveldokusha.utils.Extra_String

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
            val alpha by remember {
                derivedStateOf {
                    val value = (scrollState.value - 50).coerceIn(0, 200).toFloat()
                    val maxvalue = (scrollState.maxValue).coerceIn(1, 200).toFloat()
                    value / maxvalue
                }
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

    fun openSearchPageByAuthor(author: DatabaseInterface.AuthorMetadata) {
        if (author.url == null)
            return
        DatabaseSearchActivity
            .IntentData(this, databaseBaseUrl = viewModel.database.baseUrl)
            .let(::startActivity)
    }

    fun openSearchPageByGenres(genres: List<String>) {
        DatabaseSearchActivity
            .IntentData(this, databaseBaseUrl = viewModel.database.baseUrl)
            .let(::startActivity)
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
