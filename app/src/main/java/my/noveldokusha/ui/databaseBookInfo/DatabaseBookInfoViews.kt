package my.noveldokusha.ui.databaseBookInfo

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.scraper.DatabaseInterface
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.ImageBorderRadius
import my.noveldokusha.ui.theme.InternalTheme
import my.noveldokusha.uiViews.ImageViewPreview
import my.noveldokusha.uiViews.MyButton
import my.noveldokusha.uiViews.SmoothGraphBars
import java.util.Collections.max

@Composable
private fun Title(name: String)
{
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
private fun TextAnimated(text: String)
{
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
    onAuthorsClick: (author: DatabaseInterface.BookAuthor) -> Unit,
    onGenresClick: (genres: List<String>) -> Unit,
    onBookClick: (book: BookMetadata) -> Unit,
    scrollState: ScrollState,
)
{
    Column(
        Modifier.verticalScroll(scrollState)
    ) {
        Box {
            Box {
                ImageViewPreview(
                    coverImageUrl = data.coverImageUrl,
                    alternative = R.drawable.ic_launcher_screen_icon,
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
                                0f to MaterialTheme.colors.surface.copy(alpha = 0f),
                                1f to MaterialTheme.colors.surface,
                            )
                        )
                )
            }
            Column(
                Modifier
                    .padding(horizontal = 8.dp)
                    .padding(top = 50.dp)
            ) {
                Text(
                    text = data.title,
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                ImageViewPreview(
                    coverImageUrl = data.coverImageUrl,
                    alternative = R.drawable.ic_launcher_screen_icon,
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier
                        .height(340.dp)
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(ImageBorderRadius))
                )
                MyButton(
                    text = stringResource(R.string.search_for_sources),
                    onClick = onSourcesClick,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp),
                    outterPadding = 0.dp
                )
            }
        }

        Column(Modifier.padding(horizontal = 8.dp)) {

            Title(stringResource(R.string.description))
            TextAnimated(data.description)

            if(data.scoresFromLowtoHighNormalized.isNotEmpty())
            {
                Title(stringResource(R.string.book_rating_title))
                RatingsChart(
                    scoresFromLowtoHighNormalized = data.scoresFromLowtoHighNormalized,
                    numberOfVotes = data.numberOfVotes,
                    ratingOverTen = data.ratingOverTen
                )
            }

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
                    outterPadding = 0.dp,
                    onClick = { onAuthorsClick(author) }
                )
            }

            Title(stringResource(R.string.tags))
            TextAnimated(text = data.tags.joinToString(" · "))

            Title(stringResource(R.string.genres))
            MyButton(
                text = data.genres.joinToString(" · "),
                outterPadding = 0.dp,
                onClick = { onGenresClick(data.genres) }
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
                    outterPadding = 0.dp,
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
                    outterPadding = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
        }
        Spacer(modifier = Modifier.height(200.dp))
    }
}

@Composable
fun RatingsChart(
    scoresFromLowtoHighNormalized: List<Float>,
    ratingOverTen: Float,
    numberOfVotes: Int
)
{
    Row {
        Text(text = "$ratingOverTen/10", modifier = Modifier.padding(end = 8.dp))
        Text(text = stringResource(R.string.n_of_votes, numberOfVotes))
    }
    SmoothGraphBars(
        unitaryHeights = scoresFromLowtoHighNormalized,
        brush = Brush.horizontalGradient(
            0.2f to Color.Red,
            0.5f to Color(0xFFFF8800),
            0.8f to Color.Green
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
    )
    Row(
        horizontalArrangement = Arrangement.SpaceAround,
        modifier = Modifier.fillMaxWidth()
    ) {
        for (level in 1..10)
            Text(
                text = level.toString(),
                fontSize = 12.sp,
                modifier = Modifier.alpha(0.5f)
            )
    }
}


@Preview
@Composable
fun Preview()
{
    val scoresFromLowtoHighNormalized = run {
        val l = listOf(
            0.16f, 0.1f, 0.12f, 0.69f, 0.16f, 0.06f, 0.02f, 0.04f, 0.02f, 0.04f
        )
        val max = max(l)
        l.map { it / max }
    }


    InternalTheme {
        DatabaseBookInfoView(
            scrollState = rememberScrollState(),
            data = DatabaseInterface.BookData(
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
                scoresFromLowtoHighNormalized = scoresFromLowtoHighNormalized,
                ratingOverTen = 4.5f,
                numberOfVotes = 324
            ),
            onSourcesClick = {},
            onAuthorsClick = {},
            onGenresClick = {},
            onBookClick = {},
        )
        Spacer(modifier = Modifier.height(200.dp))
    }
}