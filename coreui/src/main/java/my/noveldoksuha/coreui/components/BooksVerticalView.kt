package my.noveldoksuha.coreui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.noveldoksuha.coreui.R
import my.noveldoksuha.coreui.composableActions.ListGridLoadWatcher
import my.noveldoksuha.coreui.modifiers.bounceOnPressed
import my.noveldoksuha.coreui.states.IteratorState
import my.noveldoksuha.coreui.theme.ColorAccent
import my.noveldokusha.core.Response
import my.noveldokusha.core.appPreferences.ListLayoutMode
import my.noveldokusha.core.domain.CloudfareVerificationBypassFailedException
import my.noveldokusha.core.domain.WebViewCookieManagerInitializationFailedException
import my.noveldokusha.core.rememberResolvedBookImagePath
import my.noveldokusha.feature.local_database.BookMetadata

@Composable
fun BooksVerticalView(
    list: List<BookMetadata>,
    state: LazyGridState,
    error: Response.Error?,
    loadState: IteratorState,
    layoutMode: ListLayoutMode,
    onLoadNext: () -> Unit,
    onBookClicked: (book: BookMetadata) -> Unit,
    onBookLongClicked: (bookItem: BookMetadata) -> Unit,
    onReload: () -> Unit = {},
    onCopyError: (String) -> Unit = {},
    onWebViewOpen: () -> Unit = {},
    cells: GridCells = GridCells.Adaptive(120.dp),
    innerPadding: PaddingValues = PaddingValues(),
) {

    val columns by remember(layoutMode, cells) {
        derivedStateOf {
            when (layoutMode) {
                ListLayoutMode.VerticalList -> GridCells.Fixed(1)
                ListLayoutMode.VerticalGrid -> cells
            }
        }
    }

    ListGridLoadWatcher(
        listState = state,
        loadState = loadState,
        onLoadNext = onLoadNext
    )

    LazyVerticalGrid(
        columns = columns,
        state = state,
        modifier = Modifier
            .fillMaxWidth()
            .padding(innerPadding),
        contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 260.dp)
    ) {
        items(list) {
            val interactionSource = remember { MutableInteractionSource() }
            when (layoutMode) {
                ListLayoutMode.VerticalList -> MyButton(
                    text = it.title,
                    onClick = { onBookClicked(it) },
                    onLongClick = { onBookLongClicked(it) },
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceOnPressed(interactionSource)
                )
                ListLayoutMode.VerticalGrid -> BookImageButtonView(
                    title = it.title,
                    coverImageModel = rememberResolvedBookImagePath(
                        bookUrl = it.url,
                        imagePath = it.coverImageUrl
                    ),
                    interactionSource = interactionSource,
                    onClick = { onBookClicked(it) },
                    onLongClick = { onBookLongClicked(it) },
                    modifier = Modifier.bounceOnPressed(interactionSource)
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
            ) {
                when (loadState) {
                    IteratorState.LOADING -> CircularProgressIndicator(
                        color = ColorAccent
                    )
                    IteratorState.CONSUMED -> Text(
                        text = when {
                            list.isEmpty() -> stringResource(R.string.no_results_found)
                            else -> stringResource(R.string.no_more_results)
                        },
                        color = ColorAccent
                    )
                    else -> Unit
                }
            }
        }

        when (error?.exception) {
            is WebViewCookieManagerInitializationFailedException -> {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ClickableOption(
                        title = stringResource(R.string.cloudfare_firewall_detected),
                        subtitle = stringResource(R.string.please_open_web_view_and_try_verify_then_reload),
                        onClick = onWebViewOpen,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            is CloudfareVerificationBypassFailedException -> {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ClickableOption(
                        title = stringResource(R.string.cloudfare_firewall_failed_bypass),
                        subtitle = stringResource(R.string.tried_but_failed_to_bypass_cloudfare_firewall),
                        onClick = onWebViewOpen,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            null -> Unit
        }

        if (error != null) item(span = { GridItemSpan(maxLineSpan) }) {
            ErrorView(error = error.message, onReload = onReload, onCopyError = onCopyError)
        }
    }
}



