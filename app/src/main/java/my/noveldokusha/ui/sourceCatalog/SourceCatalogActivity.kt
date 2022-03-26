package my.noveldokusha.ui.sourceCatalog

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import dagger.hilt.android.AndroidEntryPoint
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState
import my.noveldokusha.AppPreferences
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.ui.chaptersList.ChaptersActivity
import my.noveldokusha.ui.theme.Theme
import my.noveldokusha.ui.webView.WebViewActivity
import my.noveldokusha.uiUtils.Extra_String
import java.util.*
import javax.inject.Inject


@AndroidEntryPoint
class SourceCatalogActivity : ComponentActivity() {
    class IntentData : Intent, SourceCatalogStateBundle {
        override var sourceBaseUrl by Extra_String()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, sourceBaseUrl: String) : super(
            ctx,
            SourceCatalogActivity::class.java
        ) {
            this.sourceBaseUrl = sourceBaseUrl
        }
    }

    private val viewModel by viewModels<SourceCatalogViewModel>()

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val title = "Catalog"
            val subtitle = viewModel.source.name.capitalize(Locale.ROOT)

            Theme(appPreferences = appPreferences) {
                val state = rememberCollapsingToolbarScaffoldState()
                CollapsingToolbarScaffold(
                    modifier = Modifier,
                    state = state,
                    scrollStrategy = ScrollStrategy.EnterAlwaysCollapsed,
                    toolbar = {
                        val searchText = rememberSaveable { mutableStateOf("") }
                        val focusRequester = remember { FocusRequester() }
                        val toolbarMode = rememberSaveable { mutableStateOf(ToolbarMode.MAIN) }
                        when (toolbarMode.value) {
                            ToolbarMode.MAIN -> MainToolbarMode(
                                title = title,
                                subtitle = subtitle,
                                toolbarMode = toolbarMode,
                                onOpenSourceWebPage = ::openSourceWebPage
                            )
                            ToolbarMode.SEARCH -> SearchToolbarMode(
                                focusRequester = focusRequester,
                                searchText = searchText,
                                onClose = {
                                    toolbarMode.value = ToolbarMode.MAIN
                                    viewModel.startCatalogListMode()
                                },
                                onTextDone = { viewModel.startCatalogSearchMode(searchText.value) }
                            )
                        }
                    }
                ) {
                    SourceCatalogView(
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

    fun openSourceWebPage() {
        WebViewActivity.IntentData(this, viewModel.sourceBaseUrl).let(::startActivity)
    }

    fun openBookPage(book: BookMetadata) {
        ChaptersActivity.IntentData(
            this,
            bookMetadata = book
        ).let(::startActivity)
    }

    fun addBookToLibrary(book: BookMetadata) {
        viewModel.addToLibraryToggle(book)
    }
}

