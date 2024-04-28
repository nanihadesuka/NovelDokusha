package my.noveldokusha.features.globalSourceSearch

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import my.noveldokusha.R
import my.noveldokusha.feature.local_database.BookMetadata
import my.noveldokusha.ui.composeViews.TopAppBarSearch
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.InternalTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSourceSearchScreen(
    searchInput: String,
    listSources: List<SourceResults>,
    onSearchInputChange: (String) -> Unit,
    onSearchInputSubmit: (String) -> Unit,
    onBookClick: (book: BookMetadata) -> Unit,
    onPressBack: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        snapAnimationSpec = null,
        flingAnimationSpec = null
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                TopAppBarSearch(
                    focusRequester = focusRequester,
                    searchTextInput = searchInput,
                    onSearchTextChange = onSearchInputChange,
                    onTextDone = onSearchInputSubmit,
                    onClose = onPressBack,
                    placeholderText = stringResource(R.string.global_search),
                    scrollBehavior = scrollBehavior,
                )
                val progress by remember {
                    derivedStateOf {
                        val totalCount = listSources.size.coerceAtLeast(1)
                        val finishedCount = listSources.count { it.fetchIterator.hasFinished }
                        finishedCount.toFloat() / totalCount.toFloat()
                    }
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = ColorAccent,
                )
            }
        },
        content = { innerPadding ->
            GlobalSourceSearchScreenBody(
                listSources = listSources,
                contentPadding = innerPadding,
                onBookClick = onBookClick
            )
        }
    )
}

@Preview
@Composable
private fun PreviewView() {
    InternalTheme {
        GlobalSourceSearchScreen(
            searchInput = "Some text here",
            listSources = listOf(),
            onSearchInputChange = { },
            onSearchInputSubmit = { },
            onBookClick = { },
            onPressBack = { },
        )
    }
}
