package my.noveldokusha.uiViews

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

// Wrapper to show alternative for previews.
// Use it only for one time images (not a list of them). Not very performant.
@Composable
fun ImageViewPreview(
    coverImageUrl: Any?,
    @DrawableRes alternative: Int,
    contentScale: ContentScale,
    modifier: Modifier = Modifier
)
{
    when (coverImageUrl)
    {
        null ->
            // Used only only because it works with previews
            Image(
                painterResource(alternative),
                contentDescription = null,
                contentScale = contentScale,
                modifier = modifier
            )
        else -> ImageView(
            imageModel = coverImageUrl,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}