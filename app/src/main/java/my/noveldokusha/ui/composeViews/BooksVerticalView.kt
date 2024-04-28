package my.noveldokusha.ui.composeViews

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
import my.noveldokusha.AppPreferences
import my.noveldokusha.R
import my.noveldokusha.composableActions.ListGridLoadWatcher
import my.noveldokusha.core.rememberResolvedBookImagePath
import my.noveldokusha.feature.local_database.BookMetadata
import my.noveldokusha.ui.bounceOnPressed
import my.noveldokusha.ui.theme.ColorAccent

@Composable
fun BooksVerticalView(
    list: List<BookMetadata>,
    state: LazyGridState,
    error: String?,
    loadState: my.noveldokusha.network.IteratorState,
    layoutMode: AppPreferences.LIST_LAYOUT_MODE,
    onLoadNext: () -> Unit,
    onBookClicked: (book: BookMetadata) -> Unit,
    onBookLongClicked: (bookItem: BookMetadata) -> Unit,
    onReload: () -> Unit = {},
    onCopyError: (String) -> Unit = {},
    cells: GridCells = GridCells.Adaptive(160.dp),
    innerPadding: PaddingValues = PaddingValues(),
) {

    val columns by remember(layoutMode, cells) {
        derivedStateOf {
            when (layoutMode) {
                AppPreferences.LIST_LAYOUT_MODE.verticalList -> GridCells.Fixed(1)
                AppPreferences.LIST_LAYOUT_MODE.verticalGrid -> cells
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
                AppPreferences.LIST_LAYOUT_MODE.verticalList -> MyButton(
                    text = it.title,
                    onClick = { onBookClicked(it) },
                    onLongClick = { onBookLongClicked(it) },
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceOnPressed(interactionSource)
                )
                AppPreferences.LIST_LAYOUT_MODE.verticalGrid -> BookImageButtonView(
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
                    my.noveldokusha.network.IteratorState.LOADING -> CircularProgressIndicator(
                        color = ColorAccent
                    )
                    my.noveldokusha.network.IteratorState.CONSUMED -> Text(
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

        if (error != null) item(span = { GridItemSpan(maxLineSpan) }) {
            ErrorView(error = error, onReload = onReload, onCopyError = onCopyError)
        }
    }
}



