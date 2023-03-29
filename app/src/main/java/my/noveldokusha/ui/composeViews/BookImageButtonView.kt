package my.noveldokusha.ui.composeViews

import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import my.noveldokusha.R
import my.noveldokusha.ui.theme.Grey25
import my.noveldokusha.ui.theme.ImageBorderShape
import my.noveldokusha.ui.theme.InternalTheme

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
                            0f to Color.Black.copy(alpha = 0.0f),
                            0.4f to Color.Black.copy(alpha = 0.2f),
                            1f to Color.Black.copy(alpha = 0.4f),
                        )
                    )
                    .padding(top = 30.dp, bottom = 8.dp)
                    .padding(horizontal = 8.dp),
                style = MaterialTheme.typography.bodySmall.copy(
                    shadow = Shadow(
                        color = Color.Black,
                        offset = Offset.Zero,
                        blurRadius = 7f
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = Grey25
                )
            )
        }
    }
}

@Preview
@Composable
private fun PreviewView() {
    InternalTheme {
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