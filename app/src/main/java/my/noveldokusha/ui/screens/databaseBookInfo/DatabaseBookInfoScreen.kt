package my.noveldokusha.ui.screens.databaseBookInfo

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.scraper.DatabaseInterface
import my.noveldokusha.scraper.SearchGenre
import my.noveldokusha.ui.composeViews.CollapsibleDivider
import my.noveldokusha.ui.theme.InternalTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseBookInfoScreen(
    state: DatabaseBookInfoState,
    onSourcesClick: () -> Unit,
    onAuthorsClick: (DatabaseInterface.AuthorMetadata) -> Unit,
    onGenresClick: (List<SearchGenre>) -> Unit,
    onBookClick: (BookMetadata) -> Unit,
    onOpenInWeb: () -> Unit,
    onPressBack: () -> Unit,
) {
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
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
                    title = {
                        Text(
                            text = state.databaseName.value,
                            style = MaterialTheme.typography.titleSmall
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onPressBack) {
                            Icon(Icons.Filled.ArrowBack, null)
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenInWeb) {
                            Icon(Icons.Filled.Public, stringResource(R.string.open_in_browser))
                        }
                    }
                )
                CollapsibleDivider(scrollState = scrollBehavior.state)
            }
        },
        content = { innerPadding ->
            DatabaseBookInfoScreenBody(
                state = state,
                onSourcesClick = onSourcesClick,
                onAuthorsClick = onAuthorsClick,
                onGenresClick = onGenresClick,
                onBookClick = onBookClick,
                modifier = Modifier.padding(innerPadding)
            )
        }
    )
}

@Preview(heightDp = 1500)
@Preview(heightDp = 1500, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewView() {
    val state = remember {
        DatabaseBookInfoState(
            databaseName = mutableStateOf("Name"),
            book = DatabaseInterface.BookData(
                title = "Novel title",
                description = "Novel description goes here and here to and a little more to fill lines",
                coverImageUrl = "",
                alternativeTitles = listOf("Title 1", "Title 2", "Title 3"),
                authors = (1..3).map {
                    DatabaseInterface.AuthorMetadata("Author $it", "page url")
                } + DatabaseInterface.AuthorMetadata("Author", null),
                tags = (1..20).map { "tag $it" },
                genres = (1..8).map { SearchGenre(genreName = "genre $it", id = "$it") },
                bookType = "Web novel",
                relatedBooks = (1..3).map {
                    BookMetadata(
                        "novel name $it",
                        "ulr",
                        "coverUrl",
                        "novel description $it"
                    )
                },
                similarRecommended = (1..6).map {
                    BookMetadata(
                        "novel name $it",
                        "ulr",
                        "coverUrl",
                        "novel description $it"
                    )
                },
            ).let(::mutableStateOf)
        )
    }
    InternalTheme {
        DatabaseBookInfoScreenBody(
            state = state,
            onSourcesClick = {},
            onAuthorsClick = {},
            onGenresClick = {},
            onBookClick = {},
        )
        Spacer(modifier = Modifier.height(200.dp))
    }
}