package my.noveldokusha.ui.composeViews

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import my.noveldokusha.R

@Composable
fun ImageViewGlide(
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
        GlideImage(
            imageModel = { model },
            requestBuilder = {
                Glide
                    .with(LocalContext.current)
                    .asDrawable()
                    .transition(DrawableTransitionOptions.withCrossFade(fadeInDurationMillis))
            },
            imageOptions = ImageOptions(
                contentDescription = contentDescription,
                contentScale = contentScale,
            ),
            modifier = modifier,
            failure = {
                GlideImage(imageModel = { error })
            }
        )
    }
}