package my.noveldokusha.ui.reader

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.noveldokusha.R
import my.noveldokusha.ui.theme.InternalTheme
import my.noveldokusha.ui.theme.Themes
import my.noveldokusha.uiViews.GlideImageFadeIn
import java.io.File

@Composable
fun ReaderItemView(
    item: ReaderItem,
    localBookBaseFolder: File,
    textStyle: TextStyle
) {
    when (item) {
        is ReaderItem.TITLE -> Row(
            modifier = Modifier.padding(
                start = 17.dp,
                end = 17.dp,
                top = 46.dp,
                bottom = 17.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_decoration_round),
                contentDescription = null,
                tint = Color(0xFFBBBBBB),
                modifier = Modifier.padding(end = 12.dp)
            )
            Text(
                text = item.text,
                fontSize = textStyle.fontSize * 1.4f,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        is ReaderItem.BODY -> when {
            item.image != null -> GlideImageFadeIn(
                imageModel = File(localBookBaseFolder, item.image!!.path),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .aspectRatio(1f / item.image!!.yrel),
                250
            )
            else -> Text(
                text = item.text + "\n",
                style = textStyle,
                modifier = Modifier.padding(horizontal = 15.dp)
            )
        }
        is ReaderItem.BOOK_END -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = R.string.reader_no_more_chapters),
                style = textStyle,
                fontSize = textStyle.fontSize * 1.4f,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(15.dp)
            )
        }
        is ReaderItem.BOOK_START -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = R.string.reader_first_chapter),
                style = textStyle,
                fontSize = textStyle.fontSize * 1.4f,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(15.dp)
            )
        }
        is ReaderItem.DIVIDER -> Divider()
        is ReaderItem.ERROR -> Text(
            text = item.text,
            fontFamily = FontFamily.Monospace,
            color = colorResource(id = R.color.colorError),
            maxLines = 35,
            modifier = Modifier.padding(17.dp)
        )
        is ReaderItem.PROGRESSBAR -> Box(
            modifier = Modifier
                .padding(40.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(50.dp)
            )
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 360
)
@Composable
private fun BookItemPreview(@PreviewParameter(BookItemDataProvider::class) item: ReaderItem) {
    InternalTheme(Themes.BLACK) {
        ReaderItemView(
            item = item,
            localBookBaseFolder = File(""),
            textStyle = LocalTextStyle.current.copy(
                fontSize = 14.sp,
                fontFamily = FontFamily.Serif,
            )
        )
    }
}

class BookItemDataProvider : PreviewParameterProvider<ReaderItem> {
    val chapterUrl = "https://www.novelkoool.com/series/martial-genius/god-reveal"

    override val values = sequenceOf(
        ReaderItem.TITLE(chapterUrl = chapterUrl, 0, "Hello to god"),
        ReaderItem.BODY(
            chapterUrl = chapterUrl,
            1,
            "Here I'm at the culmination of the cooking arts.\nMr. potato is going to be sliced really good.",
            ReaderItem.LOCATION.FIRST
        ),
        ReaderItem.PROGRESSBAR(chapterUrl = chapterUrl),
        ReaderItem.DIVIDER(chapterUrl = chapterUrl),
        ReaderItem.BOOK_START(chapterUrl = chapterUrl),
        ReaderItem.BOOK_END(chapterUrl = chapterUrl),
        ReaderItem.ERROR(
            chapterUrl = chapterUrl,
            "Error:\nError message\n\nReason:\nReason message\n\nError stack:\nCall stack tree "
        )
    )
}
