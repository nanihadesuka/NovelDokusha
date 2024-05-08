package my.noveldoksuha.coreui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.google.accompanist.systemuicontroller.rememberSystemUiController


@Composable
fun Theme(
    themeProvider: ThemeProvider,
    content: @Composable () -> @Composable Unit,
) {
    val followSystemsTheme by themeProvider.followSystem(rememberCoroutineScope())
    val selectedTheme by themeProvider.currentTheme(rememberCoroutineScope())

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
    val textSelectionColors = remember {
        TextSelectionColors(
            handleColor = ColorAccent,
            backgroundColor = ColorAccent.copy(alpha = 0.3f)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = shapes,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides colorScheme.onPrimary,
            LocalAppColor provides appColor,
            LocalTextSelectionColors provides textSelectionColors,
            content = content
        )
    }
}