package my.noveldokusha.ui.main.Finder

import android.os.Bundle
import android.view.*
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import my.noveldokusha.*
import my.noveldokusha.databinding.ActivityMainFragmentFinderBinding
import my.noveldokusha.scraper.DatabaseInterface
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.ui.databaseSearch.DatabaseSearchActivity
import my.noveldokusha.ui.globalSourceSearch.GlobalSourceSearchActivity
import my.noveldokusha.ui.sourceCatalog.SourceCatalogActivity
import my.noveldokusha.ui.sourceCatalog.ToolbarMode
import my.noveldokusha.ui.theme.Theme
import my.noveldokusha.uiToolbars.ToolbarModeSearch
import javax.inject.Inject

@AndroidEntryPoint
class FinderFragment : Fragment()
{
    @Inject
    lateinit var appPreferences: AppPreferences

    private val viewModel by viewModels<FinderViewModel>()
    private lateinit var viewBind: ActivityMainFragmentFinderBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View
    {
        viewBind = ActivityMainFragmentFinderBinding.inflate(inflater, container, false)
        viewBind.composeContainer.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        viewBind.composeContainer.setContent {

            val title = stringResource(id = R.string.app_name)
            val searchText = rememberSaveable { mutableStateOf("") }
            val focusRequester = remember { FocusRequester() }
            val toolbarMode = rememberSaveable { mutableStateOf(ToolbarMode.MAIN) }
            var languagesOptionsExpanded by remember { mutableStateOf(false) }

            Theme(appPreferences = appPreferences) {
                Column {
                    when (toolbarMode.value)
                    {
                        ToolbarMode.MAIN -> ToolbarMain(
                            title = title,
                            onSearchPress = {
                                toolbarMode.value = ToolbarMode.SEARCH
                            },
                            onLanguagesOptionsPress = {
                                languagesOptionsExpanded = !languagesOptionsExpanded
                            },
                            languagesDropDownView = {
                                LanguagesDropDown(
                                    expanded = languagesOptionsExpanded,
                                    list = viewModel.languagesList,
                                    onDismiss = { languagesOptionsExpanded = false },
                                    onToggleLanguage = { toggleSourceLanguage(it.language) }
                                )
                            }
                        )
                        ToolbarMode.SEARCH -> ToolbarModeSearch(
                            focusRequester = focusRequester,
                            searchText = searchText,
                            onClose = {
                                toolbarMode.value = ToolbarMode.MAIN
                            },
                            onTextDone = { goToGlobalSearch(searchText.value) },
                            placeholderText = stringResource(R.string.global_search)
                        )
                    }
                    FinderBody(
                        databasesList = viewModel.databaseList,
                        sourcesList = viewModel.sourcesList,
                        onDatabaseClick = ::goToDatabaseSearch,
                        onSourceClick = ::goToSourceCatalog,
                    )
                }
            }
        }

        return viewBind.root
    }

    private fun toggleSourceLanguage(language: String)
    {
        val langs = appPreferences.SOURCES_LANGUAGES.value
        appPreferences.SOURCES_LANGUAGES.value = when (language in langs)
        {
            true -> langs.minus(language)
            false -> langs.plus(language)
        }
    }

    private fun goToSourceCatalog(source: SourceInterface.catalog)
    {
        SourceCatalogActivity
            .IntentData(requireContext(), sourceBaseUrl = source.baseUrl)
            .let(::startActivity)
    }

    private fun goToDatabaseSearch(database: DatabaseInterface)
    {
        DatabaseSearchActivity
            .IntentData(requireContext(), databaseBaseUrl = database.baseUrl)
            .let(::startActivity)
    }

    private fun goToGlobalSearch(text: String)
    {
        GlobalSourceSearchActivity
            .IntentData(requireContext(), text)
            .let(this@FinderFragment::startActivity)
    }
}
