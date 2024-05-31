package my.noveldokusha.features.reader.ui.bookContent

import android.content.res.Configuration
import android.graphics.Typeface
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.noveldoksuha.coreui.R
import my.noveldoksuha.coreui.components.ImageView
import my.noveldoksuha.coreui.theme.ColorAccent
import my.noveldoksuha.coreui.theme.InternalTheme
import my.noveldokusha.core.rememberResolvedBookImagePath
import my.noveldokusha.features.reader.domain.ImgEntry
import my.noveldokusha.features.reader.domain.ReaderItem
import my.noveldokusha.features.reader.features.TextSynthesis
import my.noveldokusha.texttospeech.Utterance

@Composable
internal fun ReaderBookContent(
    state: LazyListState,
    items: List<ReaderItem>,
    bookUrl: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit,
    fontFamily: FontFamily,
    currentTextSelectability: () -> Boolean,
    currentSpeakerActiveItem: () -> TextSynthesis,
    onChapterStartVisible: (chapterUrl: String) -> Unit,
    onChapterEndVisible: (chapterUrl: String) -> Unit,
    onReloadReader: () -> Unit,
    onClick: () -> Unit,
) {
    LaunchedEffect(fontSize) {
        Log.w("STATES", "fontSize: $fontSize")
    }
    LaunchedEffect(fontFamily) {
        Log.w("STATES", "fontFamily: $fontFamily")
    }
    LaunchedEffect(currentTextSelectability) {
        Log.w("STATES", "currentTextSelectability: $currentTextSelectability")
    }


    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    ) {
        ToggleableTextSelection(enabled = currentTextSelectability()) {
            LazyColumn(
                state = state,
                modifier = modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(
                    items = items,
                    contentType = ::itemContentType,
                ) { item ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                rememberItemReadBackgroundColor(
                                    item,
                                    currentSpeakerActiveItem
                                ).value
                            )
                    ) {
                        when (item) {
                            is ReaderItem.Body -> {
                                Column {
                                    Text(
                                        text = item.textToDisplay,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp),
                                        fontFamily = fontFamily,
                                        fontSize = fontSize,
                                    )
                                    Text(
                                        text = "",
                                        fontFamily = fontFamily,
                                        fontSize = fontSize
                                    )
                                }
                                LaunchedEffect(Unit) {
                                    when (item.location) {
                                        ReaderItem.Location.FIRST -> onChapterStartVisible(item.chapterUrl)
                                        ReaderItem.Location.LAST -> onChapterEndVisible(item.chapterUrl)
                                        ReaderItem.Location.MIDDLE -> Unit
                                    }
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
                                        .aspectRatio(1f / item.image.yrel.coerceAtLeast(0.01f))
                                        .padding(vertical = 2.dp),
                                    error = R.drawable.default_book_cover,
                                    contentScale = ContentScale.FillWidth
                                )
                                LaunchedEffect(Unit) {
                                    when (item.location) {
                                        ReaderItem.Location.FIRST -> onChapterStartVisible(item.chapterUrl)
                                        ReaderItem.Location.LAST -> onChapterEndVisible(item.chapterUrl)
                                        ReaderItem.Location.MIDDLE -> Unit
                                    }
                                }
                            }
                            is ReaderItem.BookEnd -> {
                                Text(
                                    text = stringResource(id = R.string.reader_no_more_chapters),
                                    modifier = Modifier.align(Alignment.TopCenter),
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontFamily = fontFamily,
                                    fontSize = fontSize * 1.5f,
                                )
                            }
                            is ReaderItem.BookStart -> {
                                Text(
                                    text = stringResource(id = R.string.reader_first_chapter),
                                    modifier = Modifier.align(Alignment.TopCenter),
                                    fontFamily = fontFamily,
                                    fontSize = fontSize * 1.5f,
                                )
                            }
                            is ReaderItem.Title -> {
                                Text(
                                    text = item.textToDisplay,
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(
                                        horizontal = 10.dp,
                                        vertical = 24.dp
                                    ),
                                    fontFamily = fontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = fontSize * 2f,
                                )
                            }
                            is ReaderItem.Divider -> {
                                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            }
                            is ReaderItem.Error -> {
                                Box(
                                    Modifier
                                        .padding(4.dp)
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.errorContainer.copy(0.5f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable(onClick = onReloadReader)
                                ) {
                                    Text(
                                        text = item.text,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.align(Alignment.TopCenter),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = fontSize,
                                    )
                                }
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
                                        .size(36.dp)
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
                                    fontFamily = fontFamily,
                                    fontSize = fontSize,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleableTextSelection(
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    if (enabled) {
        SelectionContainer(content = content)
    } else {
        content()
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
    val chapterIndex = 0
    val chapterUrl = "example-chapter-url"

    val list = remember {
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
            state = rememberLazyListState(),
            items = list,
            bookUrl = "bookUrl",
            fontFamily = FontFamily(Typeface.create("serif", Typeface.NORMAL)),
            fontSize = 12.sp,
            onClick = {},
            onReloadReader = {},
            onChapterStartVisible = {},
            onChapterEndVisible = {},
            currentTextSelectability = { false },
            currentSpeakerActiveItem = {
                TextSynthesis(
                    itemPos = ReaderItem.Body(
                        location = ReaderItem.Location.MIDDLE,
                        chapterIndex = chapterIndex,
                        text = "text",
                        chapterUrl = chapterUrl,
                        textTranslated = "texto traducido",
                        chapterItemPosition = 1
                    ),
                    playState = Utterance.PlayState.PLAYING,
                )
            }
        )
    }
}
