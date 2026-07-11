package my.noveldoksuha.coreui.composableActions

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import my.noveldoksuha.coreui.theme.isLightTheme

@Composable
fun SetSystemBarTransparent(alpha: Float = 0f) {
    val systemUiController = rememberSystemUiController()
    val useDarkIcons = MaterialTheme.colorScheme.isLightTheme()
    val baseColor = MaterialTheme.colorScheme.primary
    val color = remember(alpha, baseColor) { baseColor.copy(alpha = alpha) }
    SideEffect {
        systemUiController.setSystemBarsColor(
            color = color,
            darkIcons = useDarkIcons
        )
    }
}