package my.noveldokusha.composableActions

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import my.noveldokusha.ui.theme.isLight

@Composable
fun SetSystemBarTransparent(alpha: Float) {
    val systemUiController = rememberSystemUiController()
    val useDarkIcons = MaterialTheme.colorScheme.isLight()
    val color = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
    SideEffect {
        systemUiController.setSystemBarsColor(
            color = color,
            darkIcons = useDarkIcons
        )
    }
}