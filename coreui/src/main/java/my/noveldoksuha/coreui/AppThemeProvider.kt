package my.noveldoksuha.coreui

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import kotlinx.coroutines.CoroutineScope
import my.noveldoksuha.coreui.mappers.toTheme
import my.noveldoksuha.coreui.theme.ThemeProvider
import my.noveldoksuha.coreui.theme.Themes
import my.noveldokusha.core.appPreferences.AppPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AppThemeProvider @Inject constructor(
    private val appPreferences: AppPreferences
) : ThemeProvider {

    override fun followSystem(stateCoroutineScope: CoroutineScope): State<Boolean> {
        return appPreferences.THEME_FOLLOW_SYSTEM.state(stateCoroutineScope)
    }

    override fun currentTheme(stateCoroutineScope: CoroutineScope): State<Themes> = derivedStateOf {
        appPreferences.THEME_ID.state(stateCoroutineScope).value.toTheme
    }
}