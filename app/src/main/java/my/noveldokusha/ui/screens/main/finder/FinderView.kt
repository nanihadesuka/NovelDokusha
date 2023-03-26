package my.noveldokusha.ui.screens.main.finder

import android.content.Context
import android.net.Uri
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import my.noveldokusha.R
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.repository.SourceCatalogItem
import my.noveldokusha.repository.SourceLanguageItem
import my.noveldokusha.scraper.DatabaseInterface
import my.noveldokusha.scraper.Scraper
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.ui.composeViews.AnimatedTransition
import my.noveldokusha.ui.composeViews.ImageViewGlide
import my.noveldokusha.ui.composeViews.MyButton
import my.noveldokusha.ui.composeViews.ToolbarModeSearch
import my.noveldokusha.ui.screens.databaseSearch.DatabaseSearchActivity
import my.noveldokusha.ui.screens.globalSourceSearch.GlobalSourceSearchActivity
import my.noveldokusha.ui.screens.sourceCatalog.SourceCatalogActivity
import my.noveldokusha.ui.screens.sourceCatalog.ToolbarMode
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.ImageBorderShape
import my.noveldokusha.ui.theme.InternalTheme
import my.noveldokusha.utils.backgroundRounded
import my.noveldokusha.utils.drawBottomLine
import my.noveldokusha.utils.outlineRounded
import okhttp3.Request
import okhttp3.Response

@Composable
fun FinderView() {
    val context by rememberUpdatedState(newValue = LocalContext.current)
    val viewModel = viewModel<FinderViewModel>()
    val title = stringResource(id = R.string.title_finder)
    var searchText by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val toolbarMode = rememberSaveable { mutableStateOf(ToolbarMode.MAIN) }
    var languagesOptionsExpanded by remember { mutableStateOf(false) }
    val focusManager by rememberUpdatedState(newValue = LocalFocusManager.current)

    Column {
        when (toolbarMode.value) {
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
                        onToggleLanguage = { viewModel.toggleSourceLanguage(it.language) }
                    )
                }
            )
            ToolbarMode.SEARCH -> ToolbarModeSearch(
                focusRequester = focusRequester,
                searchText = searchText,
                onSearchTextChange = {
                    searchText = it
                },
                onClose = {
                    focusManager.clearFocus()
                    toolbarMode.value = ToolbarMode.MAIN
                },
                onTextDone = { context.goToGlobalSearch(searchText) },
                placeholderText = stringResource(R.string.global_search),
                showUnderline = true
            )
        }
        FinderBody(
            databasesList = viewModel.databaseList,
            sourcesList = viewModel.sourcesList,
            onDatabaseClick = context::goToDatabaseSearch,
            onSourceClick = context::goToSourceCatalog,
            onSourceSetPinned = viewModel::onSourceSetPinned
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FinderBody(
    databasesList: List<DatabaseInterface>,
    sourcesList: List<SourceCatalogItem>,
    onDatabaseClick: (DatabaseInterface) -> Unit,
    onSourceClick: (SourceInterface.Catalog) -> Unit,
    onSourceSetPinned: (id: String, pinned: Boolean) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 200.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                text = stringResource(id = R.string.database),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = ColorAccent,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )
        }

        items(databasesList) {
            Button(
                text = it.name,
                iconUrl = it.iconUrl,
                onClick = { onDatabaseClick(it) },
            )
        }

        item {
            Text(
                text = stringResource(id = R.string.sources),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = ColorAccent,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )
        }

        items(sourcesList) {
            Button(
                text = it.catalog.name,
                iconUrl = it.catalog.iconUrl,
                onClick = { onSourceClick(it.catalog) },
                pinContent = {
                    Box(
                        contentAlignment = Alignment.BottomEnd,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = { onSourceSetPinned(it.catalog.id, !it.pinned) },
                        ) {
                            AnimatedTransition(targetState = it.pinned) { pinned ->
                                Icon(
                                    if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = stringResource(R.string.pin_or_unpin_source)
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ToolbarMain(
    title: String,
    onSearchPress: () -> Unit,
    onLanguagesOptionsPress: () -> Unit,
    languagesDropDownView: @Composable () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxWidth()
            .drawBottomLine()
            .padding(top = 8.dp, bottom = 0.dp, start = 12.dp, end = 12.dp)
            .height(56.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onSearchPress) {
            Icon(
                painter = painterResource(id = R.drawable.ic_baseline_search_24),
                contentDescription = stringResource(R.string.search_for_title)
            )
        }
        IconButton(onClick = onLanguagesOptionsPress) {
            Icon(
                painter = painterResource(id = R.drawable.ic_baseline_languages_24),
                contentDescription = stringResource(R.string.open_for_more_options)
            )
            languagesDropDownView()
        }
    }
}

@Composable
fun LanguagesDropDown(
    expanded: Boolean,
    list: List<SourceLanguageItem>,
    onDismiss: () -> Unit,
    onToggleLanguage: (SourceLanguageItem) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        Text(
            text = stringResource(R.string.sources_languages),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
        )
        Column(
            Modifier
                .padding(horizontal = 8.dp)
                .backgroundRounded()
                .outlineRounded(width = Dp.Hairline)
                .widthIn(min = 128.dp)
        ) {
            list.forEach { lang ->
                MyButton(
                    text = lang.language,
                    onClick = { onToggleLanguage(lang) },
                    selected = lang.active,
                    borderWidth = Dp.Unspecified,
                    textAlign = TextAlign.Center,
                    outerPadding = 0.dp,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                )
            }
        }
    }
}

@Composable
private fun Button(
    text: String,
    iconUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pinContent: @Composable RowScope.() -> Unit = {}
) {
    MyButton(
        text = text,
        onClick = onClick,
        outerPadding = 0.dp,
        backgroundColor = MaterialTheme.colorScheme.primary,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ImageViewGlide(
                imageModel = iconUrl,
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(18.dp)
                    .clip(ImageBorderShape)
            )
            Text(
                text = text,
                fontWeight = FontWeight.Bold
            )
            pinContent(this)
        }
    }
}

@Preview
@Composable
fun PreviewView() {

    val scraper = Scraper(object : NetworkClient {
        override suspend fun get(url: String) = Response.Builder().build()
        override suspend fun get(url: Uri.Builder) = Response.Builder().build()
        override suspend fun call(request: Request.Builder, followRedirects: Boolean) =
            Response.Builder().build()
    })

    val sourcesList = scraper.sourcesListCatalog.toList().mapIndexed { index, it ->
        SourceCatalogItem(
            catalog = it, pinned = index < 3
        )
    }

    InternalTheme {
        FinderBody(
            databasesList = scraper.databasesList.toList(),
            sourcesList = sourcesList,
            onDatabaseClick = {},
            onSourceClick = {},
            onSourceSetPinned = { _, _ -> },
        )
    }
}

private fun Context.goToSourceCatalog(source: SourceInterface.Catalog) {
    SourceCatalogActivity
        .IntentData(this, sourceBaseUrl = source.baseUrl)
        .let(::startActivity)
}

private fun Context.goToDatabaseSearch(database: DatabaseInterface) {
    DatabaseSearchActivity
        .IntentData(this, databaseBaseUrl = database.baseUrl)
        .let(::startActivity)
}

private fun Context.goToGlobalSearch(text: String) {
    GlobalSourceSearchActivity
        .IntentData(this, text)
        .let(::startActivity)
}
