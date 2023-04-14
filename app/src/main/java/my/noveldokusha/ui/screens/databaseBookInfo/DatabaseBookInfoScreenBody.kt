package my.noveldokusha.ui.screens.databaseBookInfo

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.scraper.DatabaseInterface
import my.noveldokusha.scraper.SearchGenre
import my.noveldokusha.ui.composeViews.BookImageButtonView
import my.noveldokusha.ui.composeViews.BookTitlePosition
import my.noveldokusha.ui.composeViews.ExpandableText
import my.noveldokusha.ui.composeViews.ImageView
import my.noveldokusha.ui.composeViews.MyButton
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.utils.textPadding

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class,
    ExperimentalAnimationApi::class
)
@Composable
fun DatabaseBookInfoScreenBody(
    state: DatabaseBookInfoState,
    onSourcesClick: () -> Unit,
    onAuthorsClick: (author: DatabaseInterface.AuthorMetadata) -> Unit,
    onGenresClick: (genresIds: List<SearchGenre>) -> Unit,
    onBookClick: (book: BookMetadata) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coverImg = state.book.value.coverImageUrl ?: R.drawable.ic_baseline_empty_24
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
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
                        coverImageModel = coverImg,
                        onClick = { showImageFullScreen = true },
                        modifier = Modifier.weight(1f),
                        bookTitlePosition = BookTitlePosition.Hidden
                    )
                    Text(
                        text = state.book.value.title,
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
                            imageModel = state.book.value.coverImageUrl,
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

            ExpandableText(text = state.book.value.description, linesForExpand = 8)

            Title(stringResource(R.string.alternative_titles))
            if (state.book.value.alternativeTitles.isEmpty())
                TextAnimated(text = stringResource(R.string.none_found))
            for (title in state.book.value.alternativeTitles)
                TextAnimated(text = title)

            Title(stringResource(R.string.authors))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.book.value.authors.forEach {
                    SuggestionChip(
                        onClick = { onAuthorsClick(it) },
                        label = { Text(text = it.name) }
                    )
                }
            }

            Title(stringResource(R.string.genres))
            if (state.book.value.genres.isNotEmpty()) OutlinedCard(
                onClick = { onGenresClick(state.book.value.genres) },
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                TextAnimated(
                    text = state.book.value.genres.joinToString(" · ") { it.genreName },
                    modifier = Modifier.textPadding()
                )
            }

            Title(stringResource(R.string.tags))
            if (state.book.value.tags.isNotEmpty()) ElevatedCard(
                modifier = Modifier.padding(top = 8.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                val tagsText by remember { derivedStateOf { state.book.value.tags.joinToString(" · ") } }
                ExpandableText(
                    text = tagsText,
                    linesForExpand = 3,
                    modifier = Modifier.textPadding()
                )
            }

            Title(stringResource(R.string.related_books))
            if (state.book.value.relatedBooks.isEmpty())
                TextAnimated(text = stringResource(id = R.string.none_found))
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                for (book in state.book.value.relatedBooks) MyButton(
                    text = book.title,
                    onClick = { onBookClick(book) },
                    outerPadding = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }

            Title(stringResource(R.string.similar_recommended))
            if (state.book.value.similarRecommended.isEmpty())
                TextAnimated(text = stringResource(id = R.string.none_found))
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                for (book in state.book.value.similarRecommended) MyButton(
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
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
