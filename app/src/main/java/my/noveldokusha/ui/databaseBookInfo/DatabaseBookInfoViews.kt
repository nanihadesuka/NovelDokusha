package my.noveldokusha.ui.databaseBookInfo

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.scraper.DatabaseInterface
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.InternalTheme
import my.noveldokusha.uiViews.GlideImageFadeIn
import my.noveldokusha.uiViews.MyButton

@Composable
private fun Title(name: String) {
    Text(
        text = name,
        fontWeight = FontWeight.Bold,
        color = ColorAccent,
        style = MaterialTheme.typography.subtitle1,
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DatabaseBookInfoView(
    data: DatabaseInterface.BookData,
    onSourcesClick: () -> Unit,
    onAuthorsClick: (author: DatabaseInterface.BookAuthor) -> Unit,
    onGenresClick: (genres: List<String>) -> Unit,
    onBookClick: (book: BookMetadata) -> Unit,
) {
    Column(
        Modifier
            .padding(10.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = data.title,
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            MyButton(
                text = stringResource(R.string.sources),
                onClick = onSourcesClick
            )
        }
        when (data.coverImageUrl) {
            null -> Column {
                Image(
                    painterResource(R.drawable.ic_launcher_screen_icon),
                    contentDescription = stringResource(R.string.no_cover_found),
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier
                        .height(125.dp)
                        .align(Alignment.CenterHorizontally)
                )
                Text(
                    text = stringResource(R.string.no_cover_found),
                    Modifier.fillMaxWidth(1f),
                    textAlign = TextAlign.Center
                )
            }
            else -> GlideImageFadeIn(
                imageModel = data.coverImageUrl,
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .height(300.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
        }

        Title(stringResource(R.string.description))
        TextAnimated(data.description)

        Title(stringResource(R.string.alternative_titles))
        if (data.alternativeTitles.isEmpty())
            TextAnimated(text = stringResource(R.string.none_found))
        for (title in data.alternativeTitles)
            TextAnimated(text = title)

        Title(stringResource(R.string.authors))
        Row {
            for (author in data.authors) MyButton(
                text = author.name,
                enabled = author.url != null,
                onClick = { onAuthorsClick(author) },
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        Title(stringResource(R.string.tags))
        TextAnimated(text = data.tags.joinToString(" · "))

        Title(stringResource(R.string.genres))
        MyButton(
            text = data.genres.joinToString(" · "),
            onClick = { onGenresClick(data.genres) }
        )

        Title(stringResource(R.string.type))
        TextAnimated(text = data.bookType)

        Title(stringResource(R.string.related_books))
        if (data.relatedBooks.isEmpty())
            TextAnimated(text = stringResource(id = R.string.none_found))
        for (book in data.relatedBooks) MyButton(
            text = book.title,
            onClick = { onBookClick(book) },
            modifier = Modifier
                .padding(bottom = 4.dp)
                .fillMaxWidth()
        )

        Title(stringResource(R.string.similar_recommended))
        if (data.similarRecommended.isEmpty())
            TextAnimated(text = stringResource(id = R.string.none_found))
        for (book in data.similarRecommended) MyButton(
            text = book.title,
            onClick = { onBookClick(book) },
            modifier = Modifier
                .padding(bottom = 4.dp)
                .fillMaxWidth()
        )
    }
}


@Preview
@Composable
fun Preview() {

    InternalTheme {
        DatabaseBookInfoView(
            DatabaseInterface.BookData(
                title = "Novel title",
                description = "Novel description goes here and here to and a little more to fill lines",
                coverImageUrl = null,
                alternativeTitles = listOf("Title 1", "Title 2", "Title 3"),
                authors = (1..3).map { DatabaseInterface.BookAuthor("Author $it", "page url") },
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
    }
}