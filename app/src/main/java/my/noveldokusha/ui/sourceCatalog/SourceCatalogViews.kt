package my.noveldokusha.ui.sourceCatalog

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import my.noveldokusha.AppPreferences
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.scraper.IteratorState
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.InternalTheme
import my.noveldokusha.uiViews.BooksVerticalGridView
import my.noveldokusha.uiViews.BooksVerticalListView

enum class ToolbarMode { MAIN, SEARCH }

@Composable
fun ToolbarMain(
    title: String,
    subtitle: String,
    onOpenSourceWebPage: () -> Unit,
    onPressMoreOptions: () -> Unit,
    onPressSearchForTitle: () -> Unit,
    optionsDropDownView: @Composable () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 0.dp, start = 12.dp, end = 12.dp)
            .height(56.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.subtitle1
            )
        }
        IconButton(onClick = { onOpenSourceWebPage() }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_baseline_public_24),
                contentDescription = stringResource(R.string.open_the_web_view)
            )
        }
        IconButton(onClick = { onPressSearchForTitle() }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_baseline_search_24),
                contentDescription = stringResource(R.string.search_for_title)
            )
        }

        IconButton(onClick = onPressMoreOptions) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.open_for_more_options)
            )
            optionsDropDownView()
        }
    }
}

@Composable
fun OptionsDropDown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    listLayoutMode: AppPreferences.LIST_LAYOUT_MODE,
    onSelectListLayout: (mode: AppPreferences.LIST_LAYOUT_MODE) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        Column(Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = "Books layout",
                Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Row(
                Modifier
                    .padding(4.dp)
                    .border(
                        Dp.Hairline,
                        MaterialTheme.colors.onPrimary.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp)
                    )
                    .clip(RoundedCornerShape(8.dp))
            ) {
                @Composable
                fun colorBackground(state: AppPreferences.LIST_LAYOUT_MODE) =
                    if (listLayoutMode == state) ColorAccent else MaterialTheme.colors.surface

                @Composable
                fun colorText(state: AppPreferences.LIST_LAYOUT_MODE) =
                    if (listLayoutMode == state) Color.White else MaterialTheme.colors.onPrimary
                Box(
                    Modifier
                        .weight(1f)
                        .background(colorBackground(AppPreferences.LIST_LAYOUT_MODE.verticalList))
                        .clickable { onSelectListLayout(AppPreferences.LIST_LAYOUT_MODE.verticalList) }
                ) {
                    Text(
                        text = stringResource(R.string.list),
                        modifier = Modifier
                            .padding(18.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = colorText(AppPreferences.LIST_LAYOUT_MODE.verticalList)
                    )
                }
                Box(
                    Modifier
                        .weight(1f)
                        .background(colorBackground(AppPreferences.LIST_LAYOUT_MODE.verticalGrid))
                        .clickable { onSelectListLayout(AppPreferences.LIST_LAYOUT_MODE.verticalGrid) }
                ) {
                    Text(
                        text = stringResource(R.string.grid),
                        Modifier
                            .padding(18.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = colorText(AppPreferences.LIST_LAYOUT_MODE.verticalGrid)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewList() {
    val state = rememberLazyListState()
    InternalTheme {
        BooksVerticalListView(
            list = (1..10).map { BookMetadata("Book $it", "url") },
            error = null,
            loadState = IteratorState.LOADING,
            onLoadNext = {},
            onBookClicked = {},
            onBookLongClicked = {},
            listState = state
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
fun PreviewGrid() {
    val state = rememberLazyListState()
    InternalTheme {
        BooksVerticalGridView(
            cells = GridCells.Fixed(2),
            list = (1..10).map { BookMetadata("Book $it", "url") },
            error = null,
            loadState = IteratorState.LOADING,
            onLoadNext = {},
            onBookClicked = {},
            onBookLongClicked = {},
            listState = state
        )
    }
}