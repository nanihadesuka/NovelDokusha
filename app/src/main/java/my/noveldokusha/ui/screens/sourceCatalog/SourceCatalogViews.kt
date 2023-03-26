package my.noveldokusha.ui.screens.sourceCatalog

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import my.noveldokusha.AppPreferences.LIST_LAYOUT_MODE
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.network.IteratorState
import my.noveldokusha.ui.composeViews.BooksVerticalView
import my.noveldokusha.ui.composeViews.MyButton
import my.noveldokusha.ui.theme.InternalTheme
import my.noveldokusha.utils.backgroundRounded
import my.noveldokusha.utils.outlineRounded

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
            .background(MaterialTheme.colorScheme.surface)
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
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleMedium
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
    listLayoutMode: LIST_LAYOUT_MODE,
    onSelectListLayout: (mode: LIST_LAYOUT_MODE) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        Text(
            text = stringResource(R.string.layout),
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
            MyButton(
                text = stringResource(id = R.string.list),
                onClick = { onSelectListLayout(LIST_LAYOUT_MODE.verticalList) },
                selected = listLayoutMode == LIST_LAYOUT_MODE.verticalList,
                borderWidth = Dp.Unspecified,
                textAlign = TextAlign.Center,
                outerPadding = 0.dp,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(0.dp),
            )
            MyButton(
                text = stringResource(id = R.string.grid),
                onClick = { onSelectListLayout(LIST_LAYOUT_MODE.verticalGrid) },
                selected = listLayoutMode == LIST_LAYOUT_MODE.verticalGrid,
                borderWidth = Dp.Unspecified,
                textAlign = TextAlign.Center,
                outerPadding = 0.dp,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(0.dp),
            )
        }
    }
}

@ExperimentalFoundationApi
@Preview
@Composable
fun PreviewList() {
    InternalTheme {
        BooksVerticalView(
            list = (1..10).map { BookMetadata("Book $it", "url") },
            error = null,
            loadState = IteratorState.LOADING,
            onLoadNext = {},
            onBookClicked = {},
            onBookLongClicked = {},
            state = rememberLazyGridState(),
            layoutMode = LIST_LAYOUT_MODE.verticalList
        )
    }
}

@ExperimentalFoundationApi
@Preview
@Composable
fun PreviewGrid() {
    InternalTheme {
        BooksVerticalView(
            cells = GridCells.Fixed(2),
            list = (1..10).map { BookMetadata("Book $it", "url") },
            error = null,
            loadState = IteratorState.LOADING,
            onLoadNext = {},
            onBookClicked = {},
            onBookLongClicked = {},
            state = rememberLazyGridState(),
            layoutMode = LIST_LAYOUT_MODE.verticalGrid
        )
    }
}