package my.noveldokusha.catalogexplorer

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import my.noveldoksuha.coreui.components.CollapsibleDivider
import my.noveldokusha.navigation.NavigationRouteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogExplorerScreen(
    navigationRouteViewModel: NavigationRouteViewModel = viewModel()
) {
    val viewModel: CatalogExplorerViewModel = viewModel()

    val context by rememberUpdatedState(newValue = LocalContext.current)
    var languagesOptionsExpanded by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        snapAnimationSpec = null,
        flingAnimationSpec = null
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                TopAppBar(
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Unspecified,
                        scrolledContainerColor = Color.Unspecified,
                    ),
                    title = {
                        Text(
                            text = stringResource(id = R.string.title_finder),
                            style = MaterialTheme.typography.headlineSmall
                        )
                    },
                    actions = {
                        IconButton(onClick = {
                            navigationRouteViewModel.globalSearch(
                                context,
                                text = ""
                            ).let(context::startActivity)
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_baseline_search_24),
                                contentDescription = stringResource(R.string.search_for_title)
                            )
                        }
                        IconButton(onClick = {
                            languagesOptionsExpanded = !languagesOptionsExpanded
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_baseline_languages_24),
                                contentDescription = stringResource(R.string.open_for_more_options)
                            )
                            LanguagesDropDown(
                                expanded = languagesOptionsExpanded,
                                languageItemList = viewModel.languagesList,
                                onDismiss = { languagesOptionsExpanded = false },
                                onSourceLanguageItemToggle = { viewModel.toggleSourceLanguage(it.language) }
                            )
                        }
                    }
                )
                CollapsibleDivider(scrollBehavior.state)
            }
        },
        content = { innerPadding ->
            CatalogList(
                innerPadding = innerPadding,
                databasesList = viewModel.databaseList,
                sourcesList = viewModel.sourcesList,
                onDatabaseClick = {
                    navigationRouteViewModel.databaseSearch(
                        context,
                        databaseBaseUrl = it.baseUrl
                    ).let(context::startActivity)
                },
                onSourceClick = {
                    navigationRouteViewModel.sourceCatalog(
                        context,
                        sourceBaseUrl = it.baseUrl
                    ).let(context::startActivity)
                },
                onSourceSetPinned = viewModel::onSourceSetPinned
            )
        }
    )
}
