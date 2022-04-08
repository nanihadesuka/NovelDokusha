package my.noveldokusha.uiViews

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

@Composable
fun ImageView(
    imageModel: Any?,
    modifier: Modifier = Modifier,
    fadeInDurationMillis: Int = 250,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    error: Any? = null
)
{
    val model by remember(imageModel, error) {
        derivedStateOf {
            when (imageModel)
            {
                is String -> imageModel.ifBlank { error }
                null -> run { error }
                else -> imageModel
            }
        }
    }

    AsyncImage(
        model = ImageRequest
            .Builder(LocalContext.current)
            .data(model)
            .crossfade(fadeInDurationMillis)
            .build(),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        error = rememberAsyncImagePainter(
            model = ImageRequest
                .Builder(LocalContext.current)
                .data(error)
                .crossfade(fadeInDurationMillis)
                .build(),
            contentScale = contentScale
        )
    )
}