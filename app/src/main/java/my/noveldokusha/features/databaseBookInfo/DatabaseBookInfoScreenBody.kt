package my.noveldokusha.features.databaseBookInfo

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import my.noveldokusha.R
import my.noveldokusha.tooling.local_database.BookMetadata
import my.noveldokusha.mappers.mapToBookMetadata
import my.noveldokusha.scraper.SearchGenre
import my.noveldokusha.ui.bounceOnPressed
import my.noveldokusha.ui.composeViews.BookImageButtonView
import my.noveldokusha.ui.composeViews.BookTitlePosition
import my.noveldokusha.ui.composeViews.ExpandableText
import my.noveldokusha.ui.composeViews.ImageView
import my.noveldokusha.ui.composeViews.MyButton
import my.noveldoksuha.coreui.theme.ColorAccent
import my.noveldoksuha.coreui.theme.clickableNoIndicator
import my.noveldoksuha.coreui.theme.textPadding

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class,
    ExperimentalAnimationApi::class
)
@Composable
fun DatabaseBookInfoScreenBody(
    state: DatabaseBookInfoState,
    scrollState: ScrollState,
    onSourcesClick: () -> Unit,
    onGenresClick: (genresIds: List<SearchGenre>) -> Unit,
    onBookClick: (book: BookMetadata) -> Unit,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val coverImg = state.book.value.coverImageUrl ?: R.drawable.ic_baseline_empty_24
    Column(
        modifier
            .verticalScroll(scrollState)
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
                    .padding(horizontal = 8.dp)
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    var showImageFullScreen by rememberSaveable { mutableStateOf(false) }
                    val interactionSource = remember { MutableInteractionSource() }
                    BookImageButtonView(
                        title = "",
                        coverImageModel = coverImg,
                        onClick = { showImageFullScreen = true },
                        bookTitlePosition = BookTitlePosition.Hidden,
                        interactionSource = interactionSource,
                        modifier = Modifier
                            .weight(1f)
                            .bounceOnPressed(interactionSource),
                    )
                    SelectionContainer(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = state.book.value.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickableNoIndicator { showImageFullScreen = false },
                            contentScale = ContentScale.Fit
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

        Column(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SelectionContainer {
                ExpandableText(
                    text = state.book.value.description,
                    linesForExpand = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Section(stringResource(R.string.alternative_titles)) {
                val titles by remember(state.book.value.alternativeTitles) {
                    derivedStateOf {
                        state.book.value.alternativeTitles.joinToString(separator = "\n")
                    }
                }
                if (state.book.value.alternativeTitles.isEmpty()) {
                    TextAnimated(text = stringResource(R.string.none_found))
                } else {
                    SelectionContainer {
                        ExpandableText(
                            text = titles,
                            linesForExpand = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            Section(stringResource(R.string.authors)) {
                if (state.book.value.authors.isEmpty()) {
                    TextAnimated(text = stringResource(id = R.string.none_found))
                } else {
                    SelectionContainer {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.book.value.authors.forEach {
                                Container {
                                    Text(text = it.name, modifier = Modifier.textPadding())
                                }
                            }
                        }
                    }
                }
            }
            Section(stringResource(R.string.genres)) {
                if (state.book.value.genres.isEmpty()) {
                    TextAnimated(text = stringResource(id = R.string.none_found))
                } else {
                    OutlinedCard(
                        onClick = { onGenresClick(state.book.value.genres) },
                        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        TextAnimated(
                            text = state.book.value.genres.joinToString(" · ") { it.genreName },
                            modifier = Modifier.textPadding()
                        )
                    }
                }
            }
            Section(stringResource(R.string.tags)) {
                if (state.book.value.tags.isEmpty()) {
                    TextAnimated(text = stringResource(id = R.string.none_found))
                } else {
                    Container {
                        val tagsText by remember {
                            derivedStateOf { state.book.value.tags.joinToString(" · ") }
                        }
                        SelectionContainer {
                            ExpandableText(
                                text = tagsText,
                                linesForExpand = 3,
                                modifier = Modifier.textPadding()
                            )
                        }
                    }
                }
            }
            Section(stringResource(R.string.related_books)) {
                if (state.book.value.relatedBooks.isEmpty()) {
                    TextAnimated(text = stringResource(id = R.string.none_found))
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (book in state.book.value.relatedBooks) MyButton(
                            text = book.title,
                            onClick = { onBookClick(book.mapToBookMetadata()) },
                            outerPadding = 0.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }
                }
            }
            Section(stringResource(R.string.similar_recommended)) {
                if (state.book.value.similarRecommended.isEmpty()) {
                    TextAnimated(text = stringResource(id = R.string.none_found))
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (book in state.book.value.similarRecommended) MyButton(
                            text = book.title,
                            onClick = { onBookClick(book.mapToBookMetadata()) },
                            outerPadding = 0.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(200.dp))
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            color = ColorAccent,
            style = MaterialTheme.typography.titleMedium,
        )
        content()
    }
}

@Composable
private fun Container(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.small,
        content = content,
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
        transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "",
        modifier = modifier,
    ) { target ->
        Text(
            text = target,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
