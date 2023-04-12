package my.noveldokusha.ui.composeViews

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import my.noveldokusha.R

@Composable
fun ImageView(
    imageModel: Any?,
    modifier: Modifier = Modifier,
    fadeInDurationMillis: Int = 250,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    @DrawableRes error: Int = R.drawable.default_book_cover
) {
    val model by remember(imageModel, error) {
        derivedStateOf {
            when (imageModel) {
                is String -> imageModel.ifBlank { error }
                null -> run { error }
                else -> imageModel
            }
        }
    }
    if (LocalInspectionMode.current) {
        Image(
            painterResource(error),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        val context by rememberUpdatedState(LocalContext.current)
        val imageModel by remember {
            derivedStateOf {
                ImageRequest
                    .Builder(context)
                    .data(model)
                    .crossfade(fadeInDurationMillis)
                    .build()
            }
        }
        val imageErrorModel by remember {
            derivedStateOf {
                ImageRequest
                    .Builder(context)
                    .data(error)
                    .crossfade(fadeInDurationMillis)
                    .build()
            }
        }
        AsyncImage(
            model = imageModel,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
            error = rememberAsyncImagePainter(
                model = imageErrorModel,
                contentScale = contentScale
            )
        )
    }
}