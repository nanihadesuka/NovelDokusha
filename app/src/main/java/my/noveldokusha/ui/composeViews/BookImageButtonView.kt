package my.noveldokusha.ui.composeViews

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import my.noveldokusha.R
import my.noveldokusha.ui.theme.Grey25
import my.noveldokusha.ui.theme.Grey800
import my.noveldokusha.ui.theme.ImageBorderShape
import my.noveldokusha.ui.theme.InternalThemeObject
import my.noveldokusha.ui.theme.Themes

@OptIn(ExperimentalTextApi::class)
@Composable
fun BookImageButtonView(
    title: String,
    coverImageUrl: Any,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    MyButton(
        text = title,
        onClick = onClick,
        onLongClick = onLongClick,
        shape = ImageBorderShape,
        borderWidth = Dp.Unspecified,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1 / 1.45f)
                .clip(ImageBorderShape)
                .background(MaterialTheme.colorScheme.background)
        ) {
            ImageView(
                imageModel = coverImageUrl,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                error = R.drawable.default_book_cover,
            )
            Text(
                text = title,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            0f to MaterialTheme.colorScheme.background.copy(alpha = 0.0f),
                            0.4f to MaterialTheme.colorScheme.background.copy(alpha = 0.2f),
                            1f to MaterialTheme.colorScheme.background.copy(alpha = 0.4f),
                        )
                    )
                    .padding(top = 30.dp, bottom = 8.dp)
                    .padding(horizontal = 8.dp),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Grey800,
                    drawStyle = Stroke(
                        miter = 4f,
                        width = 4f,
                        join = StrokeJoin.Miter
                    )
                )
            )
            Text(
                text = title,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .align(Alignment.BottomCenter)
                    .padding(top = 30.dp, bottom = 8.dp)
                    .padding(horizontal = 8.dp),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Grey25,
                )
            )

        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
private fun PreviewView() {
    InternalThemeObject(theme = if (isSystemInDarkTheme()) Themes.DARK else Themes.LIGHT) {
        Box(contentAlignment = Alignment.Center) {
            BookImageButtonView(
                title = "Hello there",
                coverImageUrl = "",
                onClick = { },
                onLongClick = { }
            )
        }
    }
}