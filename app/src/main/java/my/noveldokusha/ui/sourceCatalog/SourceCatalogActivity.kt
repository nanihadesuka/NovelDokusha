package my.noveldokusha.ui.sourceCatalog

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import dagger.hilt.android.AndroidEntryPoint
import my.noveldokusha.AppPreferences
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.ui.chaptersList.ChaptersActivity
import my.noveldokusha.ui.theme.Theme
import my.noveldokusha.ui.webView.WebViewActivity
import my.noveldokusha.uiToolbars.ToolbarModeSearch
import my.noveldokusha.uiUtils.Extra_String
import java.util.*
import javax.inject.Inject


@AndroidEntryPoint
class SourceCatalogActivity : ComponentActivity()
{
    class IntentData : Intent, SourceCatalogStateBundle
    {
        override var sourceBaseUrl by Extra_String()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, sourceBaseUrl: String) : super(
            ctx,
            SourceCatalogActivity::class.java
        )
        {
            this.sourceBaseUrl = sourceBaseUrl
        }
    }

    private val viewModel by viewModels<SourceCatalogViewModel>()

    @Inject
    lateinit var appPreferences: AppPreferences

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        setContent {
            val title = stringResource(R.string.catalog)
            val subtitle = viewModel.source.name.capitalize(Locale.ROOT)

            val searchText = rememberSaveable { mutableStateOf("") }
            val focusRequester = remember { FocusRequester() }
            val toolbarMode = rememberSaveable { mutableStateOf(ToolbarMode.MAIN) }

            Theme(appPreferences = appPreferences) {
                Column {
                    when (toolbarMode.value)
                    {
                        ToolbarMode.MAIN -> ToolbarMain(
                            title = title,
                            subtitle = subtitle,
                            toolbarMode = toolbarMode,
                            onOpenSourceWebPage = ::openSourceWebPage
                        )
                        ToolbarMode.SEARCH -> ToolbarModeSearch(
                            focusRequester = focusRequester,
                            searchText = searchText,
                            onClose = {
                                toolbarMode.value = ToolbarMode.MAIN
                                viewModel.startCatalogListMode()
                            },
                            onTextDone = { viewModel.startCatalogSearchMode(searchText.value) },
                            placeholderText = stringResource(R.string.search_by_title)
                        )
                    }
                    SourceCatalogGridView(
                        cells = GridCells.Fixed(2),
                        list = viewModel.fetchIterator.list,
                        error = viewModel.fetchIterator.error,
                        loadState = viewModel.fetchIterator.state,
                        onLoadNext = { viewModel.fetchIterator.fetchNext() },
                        onBookClicked = ::openBookPage,
                        onBookLongClicked = ::addBookToLibrary
                    )
                }
            }
        }
    }

    fun openSourceWebPage()
    {
        WebViewActivity.IntentData(this, viewModel.sourceBaseUrl).let(::startActivity)
    }

    fun openBookPage(book: BookMetadata)
    {
        ChaptersActivity.IntentData(
            this,
            bookMetadata = book
        ).let(::startActivity)
    }

    fun addBookToLibrary(book: BookMetadata)
    {
        viewModel.addToLibraryToggle(book)
    }
}
