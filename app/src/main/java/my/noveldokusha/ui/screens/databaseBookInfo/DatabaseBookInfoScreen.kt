package my.noveldokusha.ui.screens.databaseBookInfo

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.scraper.DatabaseInterface
import my.noveldokusha.scraper.SearchGenre
import my.noveldokusha.ui.composeViews.BookImageButtonView
import my.noveldokusha.ui.composeViews.BookTitlePosition
import my.noveldokusha.ui.composeViews.ImageView
import my.noveldokusha.ui.composeViews.MyButton
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.InternalTheme
import my.noveldokusha.utils.textPadding

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class,
    ExperimentalMaterialApi::class
)
@Composable
fun DatabaseBookInfoScreen(
    data: DatabaseInterface.BookData,
    onSourcesClick: () -> Unit,
    onAuthorsClick: (author: DatabaseInterface.AuthorMetadata) -> Unit,
    onGenresClick: (genresIds: List<SearchGenre>) -> Unit,
    onBookClick: (book: BookMetadata) -> Unit,
    scrollState: ScrollState,
) {
    val coverImg = data.coverImageUrl ?: R.drawable.ic_baseline_empty_24
    Column(
        Modifier.verticalScroll(scrollState)
    ) {
        Box {
            Box {
                ImageView(
                    imageModel = coverImg,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .alpha(0.2f)
                        .fillMaxWidth()
                        .height(280.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                0f to MaterialTheme.colorScheme.primary.copy(alpha = 0f),
                                1f to MaterialTheme.colorScheme.primary,
                            )
                        )
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    var showImageFullScreen by remember { mutableStateOf(false) }
                    BookImageButtonView(
                        title = "",
                        coverImageUrl = data.coverImageUrl ?: "",
                        onClick = { showImageFullScreen = true },
                        modifier = Modifier.weight(1f),
                        bookTitlePosition = BookTitlePosition.Hidden
                    )
                    Text(
                        text = data.title,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (showImageFullScreen) Dialog(
                        onDismissRequest = { showImageFullScreen = false },
                        properties = DialogProperties(
                            usePlatformDefaultWidth = false,
                            dismissOnBackPress = true,
                            dismissOnClickOutside = true
                        )
                    ) {
                        ImageView(
                            imageModel = data.coverImageUrl,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
                FilledTonalButton(
                    onClick = onSourcesClick,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(8.dp)
                ) {
                    Text(text = stringResource(R.string.search_for_sources))
                }
            }
        }

        Column(Modifier.padding(horizontal = 12.dp)) {

            TextAnimated(data.description)

            Title(stringResource(R.string.alternative_titles))
            if (data.alternativeTitles.isEmpty())
                TextAnimated(text = stringResource(R.string.none_found))
            for (title in data.alternativeTitles)
                TextAnimated(text = title)

            Title(stringResource(R.string.authors))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                data.authors.forEach {
                    SuggestionChip(
                        onClick = { onAuthorsClick(it) },
                        label = { Text(text = it.name) }
                    )
                }
            }

            Title(stringResource(R.string.genres))
            if (data.genres.isNotEmpty()) OutlinedCard(
                onClick = { onGenresClick(data.genres) },
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                TextAnimated(
                    text = data.genres.joinToString(" · ") { it.genreName },
                    modifier = Modifier.textPadding()
                )
            }

            Title(stringResource(R.string.tags))
            if (data.tags.isNotEmpty()) ElevatedCard(
                modifier = Modifier.padding(top = 8.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                TextAnimated(
                    text = data.tags.joinToString(" · "),
                    modifier = Modifier.textPadding()
                )
            }

            Title(stringResource(R.string.related_books))
            if (data.relatedBooks.isEmpty())
                TextAnimated(text = stringResource(id = R.string.none_found))
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                for (book in data.relatedBooks) MyButton(
                    text = book.title,
                    onClick = { onBookClick(book) },
                    outerPadding = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }

            Title(stringResource(R.string.similar_recommended))
            if (data.similarRecommended.isEmpty())
                TextAnimated(text = stringResource(id = R.string.none_found))
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                for (book in data.similarRecommended) MyButton(
                    text = book.title,
                    onClick = { onBookClick(book) },
                    outerPadding = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
        }
        Spacer(modifier = Modifier.height(200.dp))
    }
}

@Composable
private fun Title(name: String) {
    Text(
        text = name,
        color = ColorAccent,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun TextAnimated(
    text: String,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = text,
        transitionSpec = { fadeIn() with fadeOut() }, label = "",
        modifier = modifier,
    ) { target ->
        Text(
            text = target,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(heightDp = 1500)
@Preview(heightDp = 1500, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewView() {
    InternalTheme {
        DatabaseBookInfoScreen(
            scrollState = rememberScrollState(),
            data = DatabaseInterface.BookData(
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
            ),
            onSourcesClick = {},
            onAuthorsClick = {},
            onGenresClick = {},
            onBookClick = {},
        )
        Spacer(modifier = Modifier.height(200.dp))
    }
}