package my.noveldokusha.ui.screens.databaseSearch

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.goToDatabaseBookInfo
import my.noveldokusha.ui.theme.Theme
import my.noveldokusha.utils.Extra_String
import my.noveldokusha.utils.colorAttrRes

@AndroidEntryPoint
class DatabaseSearchActivity : BaseActivity() {
    class IntentData : Intent, DatabaseSearchStateBundle {
        override var databaseBaseUrl by Extra_String()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, databaseBaseUrl: String) : super(
            ctx,
            DatabaseSearchActivity::class.java
        ) {
            this.databaseBaseUrl = databaseBaseUrl
        }
    }

    private val viewModel by viewModels<DatabaseSearchViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = R.attr.colorSurface.colorAttrRes(this)
        setContent {
            Theme(appPreferences = appPreferences) {
                DatabaseSearchScreen(
                    searchMode = viewModel.searchMode.value,
                    searchInput = viewModel.inputSearch.value,
                    genresList = viewModel.filtersSearch,
                    onSearchInputSubmit = viewModel::onSearchInputSubmit,
                    onGenresFiltersSubmit = viewModel::onSearchGenresSubmit,
                    onSearchInputChange = { viewModel.inputSearch.value = it },
                    onSearchModeChange = { viewModel.searchMode.value = it },
                    fetchIterator = viewModel.fetchIterator,
                    listLayoutMode = viewModel.listLayout,
                    onBookClicked = ::openBookInfoPage,
                    onBookLongClicked = {},
                    onPressBack = ::onBackPressed
                )
            }
        }
    }

    private fun openBookInfoPage(book: BookMetadata) = goToDatabaseBookInfo(
        databaseUrlBase = viewModel.databaseBaseUrl,
        book = book
    )
}
