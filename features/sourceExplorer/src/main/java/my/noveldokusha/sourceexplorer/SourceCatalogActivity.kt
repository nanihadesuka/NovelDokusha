package my.noveldokusha.sourceexplorer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import my.noveldoksuha.coreui.BaseActivity
import my.noveldoksuha.coreui.theme.Theme
import my.noveldokusha.core.utils.Extra_String
import my.noveldokusha.navigation.NavigationRoutes
import javax.inject.Inject


@AndroidEntryPoint
class SourceCatalogActivity : BaseActivity() {
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
    internal lateinit var navigationRoutes: NavigationRoutes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Theme(themeProvider = themeProvider) {
                SourceCatalogScreen(
                    state = viewModel.state,
                    onSearchTextInputChange = viewModel.state.searchTextInput::value::set,
                    onSearchTextInputSubmit = viewModel::onSearchText,
                    onSearchCatalogSubmit = viewModel::onSearchCatalog,
                    onListLayoutModeChange = viewModel.state.listLayoutMode::value::set,
                    onToolbarModeChange = viewModel.state.toolbarMode::value::set,
                    onOpenSourceWebPage = {
                        navigationRoutes.webView(this, viewModel.sourceBaseUrl).let(::startActivity)
                    },
                    onBookClicked = { navigationRoutes.chapters(this, it).let(::startActivity) },
                    onBookLongClicked = viewModel::addToLibraryToggle,
                    onPressBack = ::onBackPressed
                )
            }
        }
    }
}
