package my.noveldoksuha.databaseexplorer.databaseSearch

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize
import my.noveldoksuha.coreui.BaseActivity
import my.noveldoksuha.coreui.theme.Theme
import my.noveldoksuha.coreui.theme.colorAttrRes
import my.noveldoksuha.databaseexplorer.R
import my.noveldoksuha.databaseexplorer.databaseBookInfo.DatabaseBookInfoActivity
import my.noveldokusha.core.utils.Extra_Parcelable
import my.noveldokusha.tooling.local_database.BookMetadata


sealed interface DatabaseSearchExtras : Parcelable {
    val databaseBaseUrl: String

    @Parcelize
    data class Catalog(override val databaseBaseUrl: String) : DatabaseSearchExtras

    @Parcelize
    data class Genres(
        override val databaseBaseUrl: String,
        val includedGenresIds: List<String>,
        val excludedGenresIds: List<String>,
    ) : DatabaseSearchExtras

    @Parcelize
    data class Title(
        override val databaseBaseUrl: String,
        val title: String
    ) : DatabaseSearchExtras
}

@AndroidEntryPoint
class DatabaseSearchActivity : BaseActivity() {

    class IntentData : Intent, DatabaseSearchStateBundle {
        override var extras: DatabaseSearchExtras by Extra_Parcelable()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, extras: DatabaseSearchExtras) : super(
            ctx, DatabaseSearchActivity::class.java
        ) {
            this.extras = extras
        }
    }

    private val viewModel by viewModels<DatabaseSearchViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = R.attr.colorSurface.colorAttrRes(this)
        setContent {
            Theme(themeProvider = themeProvider) {
                DatabaseSearchScreen(
                    state = viewModel.state,
                    onSearchCatalogSubmit = viewModel::onSearchCatalogSubmit,
                    onSearchInputSubmit = viewModel::onSearchInputSubmit,
                    onGenresFiltersSubmit = viewModel::onSearchGenresSubmit,
                    onSearchInputChange = viewModel.state.searchTextInput::value::set,
                    onSearchModeChange = viewModel.state.searchMode::value::set,
                    onBookClicked = ::openBookInfoPage,
                    onBookLongClicked = {},
                    onPressBack = ::onBackPressed
                )
            }
        }
    }

    private fun openBookInfoPage(book: BookMetadata) = DatabaseBookInfoActivity.IntentData(
        ctx = this,
        databaseUrlBase = viewModel.extras.databaseBaseUrl,
        bookMetadata = book
    ).let(::startActivity)
}
