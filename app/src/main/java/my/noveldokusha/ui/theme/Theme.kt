package my.noveldokusha.ui.theme

import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.luminance
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import my.noveldokusha.AppPreferences
import my.noveldokusha.R

@Composable
fun ColorScheme.isLightTheme() = background.luminance() > 0.5

private val light_ColorPalette = ColorScheme(
    primary = Grey25,
    onPrimary = Grey900,
    primaryContainer = Grey50,
    onPrimaryContainer = Grey800,
    inversePrimary = Grey900,
    secondary = Grey25,
    onSecondary = Grey900,
    secondaryContainer = ColorAccent,
    onSecondaryContainer = Grey25,
    tertiary = Grey200,
    onTertiary = Grey600,
    tertiaryContainer = Grey50,
    onTertiaryContainer = Grey900,
    background = Grey25,
    onBackground = Grey900,
    surface = Grey25,
    onSurface = Grey900,
    surfaceVariant = Grey50,
    onSurfaceVariant = Grey900,
    surfaceTint = Grey300,
    inverseSurface = Grey900,
    inverseOnSurface = Grey25,
    error = Error300,
    onError = Grey900,
    errorContainer = Error200,
    onErrorContainer = Grey800,
    outline = Grey800,
    outlineVariant = Grey200,
    scrim = Grey300,
)

private val dark_ColorPalette = ColorScheme(
    primary = Grey900,
    onPrimary = Grey25,
    primaryContainer = Grey800,
    onPrimaryContainer = Grey100,
    inversePrimary = Grey25,
    secondary = Grey900,
    onSecondary = Grey25,
    secondaryContainer = ColorAccent,
    onSecondaryContainer = Grey75,
    tertiary = Grey700,
    onTertiary = Grey300,
    tertiaryContainer = Grey600,
    onTertiaryContainer = Grey50,
    background = Grey900,
    onBackground = Grey50,
    surface = Grey900,
    onSurface = Grey25,
    surfaceVariant = Grey900,
    onSurfaceVariant = Grey50,
    surfaceTint = Grey700,
    inverseSurface = Grey25,
    inverseOnSurface = Grey900,
    error = Error600,
    onError = Grey25,
    errorContainer = Error800,
    onErrorContainer = Grey50,
    outline = Grey25,
    outlineVariant = Grey700,
    scrim = Grey800,
)

private val grey_ColorPalette = dark_ColorPalette
private val black_ColorPalette = dark_ColorPalette

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
    GREY(
        isLight = false,
        nameId = R.string.theme_name_grey,
        themeId = R.style.AppTheme_BaseDark_Grey,
    ),
    BLACK(
        isLight = false,
        nameId = R.string.theme_name_black,
        themeId = R.style.AppTheme_BaseDark_Black,
    );

    companion object {
        fun fromIDTheme(@StyleRes id: Int) = list.find { it.themeId == id } ?: LIGHT
        val list = listOf(LIGHT, DARK, GREY, BLACK)
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
        Themes.LIGHT -> light_ColorPalette
        Themes.DARK -> dark_ColorPalette
        Themes.GREY -> grey_ColorPalette
        Themes.BLACK -> black_ColorPalette
    }

    val systemUiController = rememberSystemUiController()
    systemUiController.setSystemBarsColor(
        color = colorScheme.primary,
        darkIcons = theme.isLight
    )

    CompositionLocalProvider(
        LocalContentColor provides colorScheme.onPrimary
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}