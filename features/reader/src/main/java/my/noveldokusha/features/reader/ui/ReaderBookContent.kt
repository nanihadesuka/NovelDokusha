package my.noveldokusha.features.reader.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.noveldoksuha.coreui.R
import my.noveldoksuha.coreui.components.ImageView
import my.noveldoksuha.coreui.theme.ColorAccent
import my.noveldoksuha.coreui.theme.InternalTheme
import my.noveldokusha.core.rememberResolvedBookImagePath
import my.noveldokusha.features.reader.domain.ImgEntry
import my.noveldokusha.features.reader.domain.ReaderItem

@Composable
internal fun ReaderBookContent(
    items: List<ReaderItem>,
    bookUrl: String,
    modifier: Modifier = Modifier,
    showPadding: Boolean = true
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
    ) {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showPadding) item {
                Spacer(modifier = Modifier.height(700.dp))
            }

            items(
                items = items,
                contentType = ::itemContentType,
            ) { item ->
                Box(Modifier.fillMaxWidth()) {
                    when (item) {
                        is ReaderItem.Body -> {
                            Column {
                                Text(
                                    text = item.text,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp)
                                )
                                Text(text = "")
                            }
                        }
                        is ReaderItem.Image -> {
                            val imageModel = rememberResolvedBookImagePath(
                                bookUrl = bookUrl,
                                imagePath = item.image.path
                            )
                            ImageView(
                                imageModel = imageModel,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f/item.image.yrel.coerceAtLeast(0.01f))
                                    .padding(vertical = 2.dp),
                                error = R.drawable.default_book_cover,
                                contentScale = ContentScale.FillWidth
                            )
                        }
                        is ReaderItem.BookEnd -> {
                            Text(
                                text = stringResource(id = R.string.reader_no_more_chapters),
                                modifier = Modifier.align(Alignment.TopCenter),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        is ReaderItem.BookStart -> {
                            Text(
                                text = stringResource(id = R.string.reader_first_chapter),
                                modifier = Modifier.align(Alignment.TopCenter),
                            )
                        }
                        is ReaderItem.Title -> {
                            Text(
                                text = item.textToDisplay,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 24.dp),
                            )
                        }
                        is ReaderItem.Divider -> {
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        }
                        is ReaderItem.Error -> {
                            Text(
                                text = item.text,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.align(Alignment.TopCenter),
                            )
                        }
                        is ReaderItem.GoogleTranslateAttribution -> {
                            ImageView(
                                imageModel = R.drawable.google_translate_attribution_greyscale,
                                contentScale = ContentScale.FillHeight,
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(vertical = 8.dp, horizontal = 10.dp)
                                    .height(16.dp),
                            )
                        }
                        is ReaderItem.Padding -> {
                            Spacer(modifier = Modifier.height(700.dp))
                        }
                        is ReaderItem.Progressbar -> {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .size(24.dp)
                                    .align(Alignment.TopCenter),
                                color = ColorAccent,
                            )
                        }
                        is ReaderItem.Translating -> {
                            Text(
                                text = stringResource(
                                    my.noveldokusha.reader.R.string.translating_from_lang_a_to_lang_b,
                                    item.sourceLang,
                                    item.targetLang
                                ),
                                modifier = Modifier.align(Alignment.TopCenter),
                            )
                        }
                    }
                }
            }

            if (showPadding) item {
                Spacer(modifier = Modifier.height(700.dp))
            }
        }
    }
}

private fun itemContentType(item: ReaderItem): Int = when (item) {
    is ReaderItem.Body -> 0
    is ReaderItem.BookEnd -> 1
    is ReaderItem.BookStart -> 2
    is ReaderItem.Image -> 3
    is ReaderItem.Title -> 4
    is ReaderItem.Divider -> 5
    is ReaderItem.Error -> 6
    is ReaderItem.GoogleTranslateAttribution -> 7
    is ReaderItem.Padding -> 8
    is ReaderItem.Progressbar -> 9
    is ReaderItem.Translating -> 10
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    val list = remember {
        val chapterIndex = 0
        val chapterUrl = "example-chapter-url"
        listOf(
            ReaderItem.BookStart(
                chapterIndex = chapterIndex,
            ),
            ReaderItem.Title(
                chapterIndex = chapterIndex,
                chapterUrl = chapterUrl,
                text = "Title chapter 1",
                chapterItemPosition = -1,
                textTranslated = "Título del capítulo 1"
            ),
            ReaderItem.GoogleTranslateAttribution(
                chapterIndex = chapterIndex,
            ),
            ReaderItem.Body(
                chapterIndex = chapterIndex,
                chapterUrl = chapterUrl,
                text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. ",
                location = ReaderItem.Location.FIRST,
                chapterItemPosition = 0,
                textTranslated = "__**Lorem ipsum dolor sit amet, consectetur adipiscing elit.**__ ",
            ),
            ReaderItem.Body(
                chapterIndex = chapterIndex,
                chapterUrl = chapterUrl,
                text = "second verse: Lorem ipsum dolor sit amet, consectetur adipiscing elit. ",
                location = ReaderItem.Location.MIDDLE,
                chapterItemPosition = 1,
                textTranslated = "segundo verso: __**Lorem ipsum dolor sit amet, consectetur adipiscing elit.**__ ",
            ),
            ReaderItem.Image(
                chapterIndex = chapterIndex,
                chapterUrl = chapterUrl,
                text = "text",
                location = ReaderItem.Location.MIDDLE,
                chapterItemPosition = 2,
                image = ImgEntry(path = "example-image-path", yrel = 1.2f),
            ),
            ReaderItem.Body(
                chapterIndex = chapterIndex,
                chapterUrl = chapterUrl,
                text = "third verse: Lorem ipsum dolor sit amet, consectetur adipiscing elit. ",
                location = ReaderItem.Location.LAST,
                chapterItemPosition = 3,
                textTranslated = "tercer verso: __**Lorem ipsum dolor sit amet, consectetur adipiscing elit.**__ ",
            ),
            ReaderItem.BookEnd(
                chapterIndex = chapterIndex,
            ),
            ReaderItem.Divider(
                chapterIndex = chapterIndex,
            ),
            ReaderItem.Error(
                chapterIndex = chapterIndex,
                text = "Failed to load next chapter"
            ),
            ReaderItem.Padding(
                chapterIndex = chapterIndex,
            ),
            ReaderItem.Progressbar(
                chapterIndex = chapterIndex,
            ),
            ReaderItem.Translating(
                chapterIndex = chapterIndex,
                sourceLang = "English",
                targetLang = "Spanish"
            ),
        )
    }

    InternalTheme {
        ReaderBookContent(
            items = list,
            bookUrl = "bookUrl",
            showPadding = false
        )
    }
}
