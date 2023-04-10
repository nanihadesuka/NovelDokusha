package my.noveldokusha.ui.theme

import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import my.noveldokusha.AppPreferences
import my.noveldokusha.R


enum class Themes(
    val isLight: Boolean,
    @StringRes val nameId: Int,
    @StyleRes val themeId: Int,
) {
    LIGHT(
        isLight = true,
        nameId = R.string.theme_name_light,
        themeId = R.style.AppTheme_Light,
    ),
    DARK(
        isLight = false,
        nameId = R.string.theme_name_dark,
        themeId = R.style.AppTheme_BaseDark_Dark,
    ),
    BLACK(
        isLight = false,
        nameId = R.string.theme_name_black,
        themeId = R.style.AppTheme_BaseDark_Black,
    );

    companion object {
        val list = Themes.values().toList()
        fun fromIDTheme(@StyleRes id: Int) = list.find { it.themeId == id } ?: LIGHT
    }
}

@Composable
fun Theme(
    appPreferences: AppPreferences,
    content: @Composable () -> @Composable Unit,
) {
    val followSystemsTheme by appPreferences.THEME_FOLLOW_SYSTEM.state(rememberCoroutineScope())
    val selectedThemeId = appPreferences.THEME_ID.state(rememberCoroutineScope())
    val selectedTheme by remember { derivedStateOf { Themes.fromIDTheme(selectedThemeId.value) } }
    val isSystemThemeLight = !isSystemInDarkTheme()
    val theme: Themes = when (followSystemsTheme) {
        true -> when {
            isSystemThemeLight && !selectedTheme.isLight -> Themes.LIGHT
            !isSystemThemeLight && selectedTheme.isLight -> Themes.DARK
            else -> selectedTheme
        }
        false -> selectedTheme
    }
    InternalTheme(
        theme = theme,
        content = content,
    )
}

@Composable
fun InternalTheme(
    theme: Themes = if (isSystemInDarkTheme()) Themes.DARK else Themes.LIGHT,
    content: @Composable () -> Unit
) {
    val colorScheme = when (theme) {
        Themes.LIGHT -> light_colorScheme
        Themes.DARK -> dark_colorScheme
        Themes.BLACK -> black_colorScheme
    }

    val appColor = when (theme) {
        Themes.LIGHT -> light_appColor
        Themes.DARK -> dark_appColor
        Themes.BLACK -> black_appColor
    }

    val systemUiController = rememberSystemUiController()
    systemUiController.setSystemBarsColor(
        color = colorScheme.primary,
        darkIcons = theme.isLight
    )

    CompositionLocalProvider(
        LocalContentColor provides colorScheme.onPrimary,
        LocalAppColor provides appColor
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}