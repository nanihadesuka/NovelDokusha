package my.noveldoksuha.coreui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import my.noveldoksuha.coreui.R

@Composable
fun ImageView(
    imageModel: Any?,
    modifier: Modifier = Modifier,
    fadeInDurationMillis: Int = 250,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    @DrawableRes error: Int = R.drawable.default_book_cover,
    colorFilter: ColorFilter? = null,
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
        val res = when (val modelCopy = model) {
            is Int -> modelCopy
            else -> error
        }
        Image(
            painter = painterResource(res),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
            colorFilter = colorFilter,
        )
    } else {
        val context by rememberUpdatedState(LocalContext.current)
        val imageRequest by remember(model) {
            derivedStateOf {
                ImageRequest
                    .Builder(context)
                    .data(model)
                    .crossfade(fadeInDurationMillis)
                    .build()
            }
        }
        val imageErrorRequest by remember(error) {
            derivedStateOf {
                ImageRequest
                    .Builder(context)
                    .data(error)
                    .crossfade(fadeInDurationMillis)
                    .build()
            }
        }
        AsyncImage(
            model = imageRequest,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
            colorFilter = colorFilter,
            error = rememberAsyncImagePainter(
                model = imageErrorRequest,
                contentScale = contentScale
            )
        )
    }
}