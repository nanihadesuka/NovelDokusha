package my.noveldokusha.ui.composeViews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.noveldokusha.R
import my.noveldokusha.ui.theme.ImageBorderRadius
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
        shape = RoundedCornerShape(ImageBorderRadius),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1 / 1.45f)
                .clip(MaterialTheme.shapes.medium)
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
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            0f to MaterialTheme.colors.primary.copy(alpha = 0f),
                            0.44f to MaterialTheme.colors.primary.copy(alpha = 0.5f),
                            1f to MaterialTheme.colors.primary.copy(alpha = 0.85f),
                        )
                    )
                    .padding(top = 30.dp, bottom = 8.dp)
                    .padding(horizontal = 8.dp)
            )
        }
    }
}

@Preview
@Composable
private fun PreviewView() {
    InternalTheme {
        BookImageButtonView(
            title = "Hello there",
            coverImageUrl = "",
            onClick = { },
            onLongClick = { }
        )
    }
}