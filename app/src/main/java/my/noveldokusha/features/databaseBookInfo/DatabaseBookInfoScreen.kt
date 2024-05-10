package my.noveldokusha.features.databaseBookInfo

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.noveldoksuha.coreui.theme.InternalTheme
import my.noveldoksuha.coreui.theme.isAtTop
import my.noveldokusha.R
import my.noveldokusha.scraper.DatabaseInterface
import my.noveldokusha.scraper.DatabaseInterface.BookData
import my.noveldokusha.scraper.SearchGenre
import my.noveldokusha.scraper.domain.BookResult
import my.noveldokusha.tooling.local_database.BookMetadata

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseBookInfoScreen(
    state: DatabaseBookInfoState,
    onSourcesClick: () -> Unit,
    onGenresClick: (List<SearchGenre>) -> Unit,
    onBookClick: (BookMetadata) -> Unit,
    onOpenInWeb: () -> Unit,
    onPressBack: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val isAtTop by scrollState.isAtTop(threshold = 40.dp)
            val alpha by animateFloatAsState(targetValue = if (isAtTop) 0f else 1f, label = "")
            val backgroundColor by animateColorAsState(
                targetValue = MaterialTheme.colorScheme.background.copy(alpha = alpha),
                label = ""
            )
            val titleColor by animateColorAsState(
                targetValue = MaterialTheme.colorScheme.onPrimary.copy(alpha = alpha),
                label = ""
            )
            Surface(color = backgroundColor) {
                Column {
                    TopAppBar(
                        scrollBehavior = scrollBehavior,
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Unspecified,
                            scrolledContainerColor = Color.Unspecified,
                        ),
                        title = {
                            Text(
                                text = state.book.value.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = titleColor
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onPressBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                            }
                        },
                        actions = {
                            IconButton(onClick = onOpenInWeb) {
                                Icon(Icons.Filled.Public, stringResource(R.string.open_in_browser))
                            }
                        }
                    )
                    HorizontalDivider(Modifier.alpha(alpha))
                }
            }
        },
        content = { innerPadding ->
            DatabaseBookInfoScreenBody(
                state = state,
                scrollState = scrollState,
                onSourcesClick = onSourcesClick,
                onGenresClick = onGenresClick,
                onBookClick = onBookClick,
                innerPadding = innerPadding
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
            databaseNameStrId = mutableStateOf(R.string.database_name_baka_updates),
            book = BookData(
                title = "Novel title",
                description = "Novel description goes here and here to and a little more to fill lines",
                coverImageUrl = "",
                alternativeTitles = listOf("Title 1", "Title 2", "Title 3"),
                authors = (1..3).map {
                    DatabaseInterface.AuthorMetadata("Author $it", "page url")
                } + DatabaseInterface.AuthorMetadata("Author", null),
                tags = (1..20).map { "tag $it" },
                genres = (1..8).map {
                    SearchGenre(
                        genreName = "genre $it",
                        id = "$it"
                    )
                },
                bookType = "Web novel",
                relatedBooks = (1..3).map {
                    BookResult(
                        "novel name $it",
                        "ulr",
                        "coverUrl",
                        "novel description $it"
                    )
                },
                similarRecommended = (1..6).map {
                    BookResult(
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
            scrollState = rememberScrollState(),
            onSourcesClick = {},
            onGenresClick = {},
            onBookClick = {},
            innerPadding = PaddingValues()
        )
        Spacer(modifier = Modifier.height(200.dp))
    }
}