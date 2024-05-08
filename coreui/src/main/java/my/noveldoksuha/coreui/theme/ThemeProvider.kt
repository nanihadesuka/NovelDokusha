package my.noveldoksuha.coreui.theme

import androidx.compose.runtime.State
import kotlinx.coroutines.CoroutineScope

interface ThemeProvider {

    fun followSystem(stateCoroutineScope: CoroutineScope): State<Boolean>

    fun currentTheme(stateCoroutineScope: CoroutineScope): State<Themes>
}