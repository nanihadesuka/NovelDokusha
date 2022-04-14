package my.noveldokusha.uiViews

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun SetSystemBarTransparent(alpha : Float)
{
    val systemUiController = rememberSystemUiController()
    val useDarkIcons = MaterialTheme.colors.isLight
    val color = MaterialTheme.colors.primary.copy(alpha = alpha)
    SideEffect {
        systemUiController.setSystemBarsColor(
            color = color,
            darkIcons = useDarkIcons
        )
    }
}