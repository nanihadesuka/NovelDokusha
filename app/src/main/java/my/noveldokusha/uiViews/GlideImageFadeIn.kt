package my.noveldokusha.uiViews

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.skydoves.landscapist.glide.GlideImage
import java.io.File

@Composable
fun GlideImageFadeIn(
    imageModel: Any?,
    modifier: Modifier = Modifier,
    fadeInDurationMillis: Int = 250
) {
    val timeNow by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var showImmediatelly by rememberSaveable { mutableStateOf(false) }
    var showImage by rememberSaveable { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (showImage) 1f else 0f,
        animationSpec = tween(if (showImmediatelly) 75 else fadeInDurationMillis)
    )

    GlideImage(
        imageModel = imageModel,
        modifier = modifier.alpha(alpha),
        contentScale = ContentScale.Fit,
        requestListener = object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: com.bumptech.glide.request.target.Target<Drawable>?,
                isFirstResource: Boolean
            ): Boolean {
                return false
            }

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: com.bumptech.glide.request.target.Target<Drawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                showImmediatelly = (System.currentTimeMillis() - timeNow) < 75L
                showImage = true
                return true
            }
        }
    )
}