package my.noveldokusha.ui.screens.databaseSearch

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import dagger.hilt.android.AndroidEntryPoint
import my.noveldokusha.AppPreferences
import my.noveldokusha.R
import my.noveldokusha.ui.screens.databaseSearchResults.DatabaseSearchResultsActivity
import my.noveldokusha.ui.theme.Theme
import my.noveldokusha.uiUtils.Extra_String
import my.noveldokusha.uiUtils.colorAttrRes
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@AndroidEntryPoint
class DatabaseSearchActivity : ComponentActivity() {
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

    @Inject
    lateinit var appPreferences: AppPreferences

    private val viewModel by viewModels<DatabaseSearchViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = R.attr.colorSurface.colorAttrRes(this)
        setContent {
            Theme(appPreferences = appPreferences) {
                DatabaseSearchView(
                    title = stringResource(R.string.database),
                    subtitle = viewModel.database.name.capitalize(Locale.ROOT),
                    searchText = viewModel.searchText,
                    genresList = viewModel.genresList,
                    onTitleSearchClick = ::openSearchPageByTitle,
                    onGenreSearchClick = ::openSearchPageByGenres
                )
            }
        }
    }

    fun openSearchPageByGenres() {
        val list = viewModel.genresList
        val input = DatabaseSearchResultsActivity.SearchMode.Genres(
            genresIncludeId = ArrayList(list.filter { it.state == ToggleableState.On }
                .map { it.genreId }),
            genresExcludeId = ArrayList(list.filter { it.state == ToggleableState.Indeterminate }
                .map { it.genreId })
        )

        DatabaseSearchResultsActivity
            .IntentData(
                this@DatabaseSearchActivity,
                databaseUrlBase = viewModel.database.baseUrl,
                input = input
            )
            .let(this@DatabaseSearchActivity::startActivity)
    }

    fun openSearchPageByTitle(text: String) {
        DatabaseSearchResultsActivity
            .IntentData(
                this@DatabaseSearchActivity,
                databaseUrlBase = viewModel.database.baseUrl,
                input = DatabaseSearchResultsActivity.SearchMode.Text(text = text)
            ).let(this@DatabaseSearchActivity::startActivity)
    }
}
