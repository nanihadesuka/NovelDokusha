package my.noveldokusha.ui.screens.databaseBookInfo

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.scraper.DatabaseInterface
import my.noveldokusha.ui.composeViews.ImageView
import my.noveldokusha.ui.composeViews.MyButton
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.ImageBorderShape
import my.noveldokusha.ui.theme.InternalTheme

@Composable
private fun Title(name: String) {
    Text(
        text = name,
        fontWeight = FontWeight.Bold,
        color = ColorAccent,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun TextAnimated(text: String) {
    AnimatedContent(
        targetState = text,
        transitionSpec = { fadeIn() with fadeOut() }
    ) { target ->
        Text(
            text = target,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun DatabaseBookInfoView(
    data: DatabaseInterface.BookData,
    onSourcesClick: () -> Unit,
    onAuthorsClick: (author: DatabaseInterface.AuthorMetadata) -> Unit,
    onGenresClick: (genres: List<String>) -> Unit,
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
                        .height(340.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                0f to MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                                1f to MaterialTheme.colorScheme.surface,
                            )
                        )
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(top = 50.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = data.title,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                ImageView(
                    imageModel = coverImg,
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier
                        .height(340.dp)
                        .clip(ImageBorderShape)
                )
                MyButton(
                    text = stringResource(R.string.search_for_sources),
                    onClick = onSourcesClick,
                )
            }
        }
        Column(Modifier.padding(horizontal = 8.dp)) {

            Title(stringResource(R.string.description))
            TextAnimated(data.description)

            Title(stringResource(R.string.alternative_titles))
            if (data.alternativeTitles.isEmpty())
                TextAnimated(text = stringResource(R.string.none_found))
            for (title in data.alternativeTitles)
                TextAnimated(text = title)

            Title(stringResource(R.string.authors))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                for (author in data.authors) MyButton(
                    text = author.name,
                    enabled = author.url != null,
                    onClick = { onAuthorsClick(author) },
                    outerPadding = 0.dp,
                )
            }

            Title(stringResource(R.string.tags))
            TextAnimated(text = data.tags.joinToString(" · "))

            Title(stringResource(R.string.genres))
            MyButton(
                text = data.genres.joinToString(" · "),
                onClick = { onGenresClick(data.genres) },
                outerPadding = 0.dp,
            )

            Title(stringResource(R.string.type))
            TextAnimated(text = data.bookType)

            Title(stringResource(R.string.related_books))
            if (data.relatedBooks.isEmpty())
                TextAnimated(text = stringResource(id = R.string.none_found))
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
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


@Preview
@Composable
fun Preview() {
    InternalTheme {
        DatabaseBookInfoView(
            scrollState = rememberScrollState(),
            data = DatabaseInterface.BookData(
                title = "Novel title",
                description = "Novel description goes here and here to and a little more to fill lines",
                coverImageUrl = "",
                alternativeTitles = listOf("Title 1", "Title 2", "Title 3"),
                authors = (1..3).map { DatabaseInterface.AuthorMetadata("Author $it", "page url") },
                tags = (1..20).map { "tag $it" },
                genres = (1..5).map { "genre $it" },
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