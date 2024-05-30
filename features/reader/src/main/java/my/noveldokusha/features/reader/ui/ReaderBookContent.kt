package my.noveldokusha.features.reader.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.noveldoksuha.coreui.R
import my.noveldoksuha.coreui.components.ImageView
import my.noveldokusha.core.rememberResolvedBookImagePath
import my.noveldokusha.features.reader.domain.ReaderItem

@Composable
internal fun ReaderBookContent(
    items: List<ReaderItem>,
    bookUrl: String,
    modifier: Modifier
) {
    val itemsState = remember { mutableStateListOf<ReaderItem>() }
    val bookUrlState by remember(bookUrl) { derivedStateOf { bookUrl } }

    LaunchedEffect(items.size, items.hashCode()) {
        itemsState.clear()
        itemsState.addAll(items)
    }

    LazyColumn(
        modifier = modifier
    ) {
        items(itemsState) { item ->
            when (item) {
                is ReaderItem.Body -> {
                    Text(text = item.text)
                }
                is ReaderItem.Image -> {
                    val imageModel = rememberResolvedBookImagePath(
                        bookUrl = bookUrlState,
                        imagePath = item.image.path
                    )
                    ImageView(
                        imageModel = imageModel,
                        modifier = Modifier.fillMaxWidth(),
                        error = R.drawable.default_book_cover,
                        contentScale = ContentScale.FillWidth
                    )
                }
                is ReaderItem.BookEnd -> {
                    Text(text = stringResource(id = R.string.reader_no_more_chapters))
                }
                is ReaderItem.BookStart -> {
                    Text(text = stringResource(id = R.string.reader_first_chapter))
                }
                is ReaderItem.Title -> {
                    Text(text = item.textToDisplay)
                }
                is ReaderItem.Divider -> {
                    HorizontalDivider()
                }
                is ReaderItem.Error -> {
                    Text(text = item.text, color = MaterialTheme.colorScheme.error)
                }
                is ReaderItem.GoogleTranslateAttribution -> {
                    ImageView(
                        imageModel = R.drawable.google_translate_attribution_greyscale,
                        modifier = Modifier.height(16.dp)
                    )
                }
                is ReaderItem.Padding -> {
                    Spacer(modifier = Modifier.height(700.dp))
                }
                is ReaderItem.Progressbar -> {
                    CircularProgressIndicator()
                }
                is ReaderItem.Translating -> {
                    Text(
                        text = stringResource(
                            my.noveldokusha.reader.R.string.translating_from_lang_a_to_lang_b,
                            item.sourceLang,
                            item.targetLang
                        )
                    )
                }
            }
        }
    }
}