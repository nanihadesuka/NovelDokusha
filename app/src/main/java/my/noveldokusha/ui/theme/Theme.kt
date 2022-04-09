package my.noveldokusha.ui.theme

import androidx.annotation.StyleRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.flow.mapNotNull
import my.noveldokusha.AppPreferences
import my.noveldokusha.R
import java.sql.Wrapper

private val light_ColorPalette = Colors(
    primary = Color(0xFFFBFBFB),
    primaryVariant = Color(0xFFFBFBFB),
    secondary = Color(0xFFFBFBFB),
    secondaryVariant = Color(0xFFFBFBFB),
    background = Color(0xFFFBFBFB),
    surface = Color(0xFFFBFBFB),
    error = Color(0xFFFBFBFB),
    onPrimary = Color(0xFF111111),
    onSecondary = Color(0xFF111111),
    onBackground = Color(0xFF111111),
    onSurface = Color(0xFF111111),
    onError = Color.Red,
    isLight = true
)

private val dark_ColorPalette = Colors(
    primary = Color(0xFF202020),
    primaryVariant = Color(0xFF202020),
    secondary = Color(0xFF242424),
    secondaryVariant = Color(0xFF242424),
    background = Color(0xFF181818),
    surface = Color(0xFF181818),
    error = Color(0xFF181818),
    onPrimary = Color(0xFFEEEEEE),
    onSecondary = Color(0xFFEEEEEE),
    onBackground = Color(0xFFEEEEEE),
    onSurface = Color(0xFFEEEEEE),
    onError = Color.Red,
    isLight = false
)

private val grey_ColorPalette = Colors(
    primary = Color(0xFF333333),
    primaryVariant = Color(0xFF333333),
    secondary = Color(0xFF333333),
    secondaryVariant = Color(0xFF333333),
    background = Color(0xFF242424),
    surface = Color(0xFF242424),
    error = Color(0xFF242424),
    onPrimary = Color(0xFFEEEEEE),
    onSecondary = Color(0xFFEEEEEE),
    onBackground = Color(0xFFEEEEEE),
    onSurface = Color(0xFFEEEEEE),
    onError = Color.Red,
    isLight = false
)

private val black_ColorPalette = Colors(
    primary = Color.Black,
    primaryVariant = Color.Black,
    secondary = Color.Black,
    secondaryVariant = Color.Black,
    background = Color.Black,
    surface = Color.Black,
    error = Color.Black,
    onPrimary = Color(0xFFEEEEEE),
    onSecondary = Color(0xFFEEEEEE),
    onBackground = Color(0xFFEEEEEE),
    onSurface = Color(0xFFEEEEEE),
    onError = Color.Red,
    isLight = false
)

enum class Themes
{
    LIGHT,
    DARK,
    GREY,
    BLACK;

    companion object
    {
        fun fromIDTheme(@StyleRes id: Int) = when (id)
        {
            R.style.AppTheme_Light -> LIGHT
            R.style.AppTheme_BaseDark_Dark -> DARK
            R.style.AppTheme_BaseDark_Grey -> GREY
            R.style.AppTheme_BaseDark_Black -> BLACK
            else -> null
        }

        fun toIDTheme(theme: Themes) = when (theme)
        {
            LIGHT -> R.style.AppTheme_Light
            DARK -> R.style.AppTheme_BaseDark_Dark
            GREY -> R.style.AppTheme_BaseDark_Grey
            BLACK -> R.style.AppTheme_BaseDark_Black
        }

        val list = listOf(LIGHT, DARK, GREY, BLACK)
        val pairs = mapOf(
            LIGHT to "Light",
            DARK to "Dark",
            GREY to "Grey",
            BLACK to "Black"
        )
    }
}

@Composable
fun Theme(
    appPreferences: AppPreferences,
    wrapper: @Composable (fn: @Composable () -> @Composable Unit) -> Unit = { fn -> Surface(Modifier.fillMaxSize()) { fn() } },
    content: @Composable () -> @Composable Unit,
)
{
    // Done so the first load is not undefined (visually annoying)
    val initialThemeFollowSystem by remember {
        mutableStateOf(appPreferences.THEME_FOLLOW_SYSTEM.value)
    }
    val initialThemeType by remember {
        mutableStateOf(Themes.fromIDTheme(appPreferences.THEME_ID.value) ?: Themes.LIGHT)
    }

    val themeFollowSystem by remember {
        appPreferences.THEME_FOLLOW_SYSTEM.flow()
    }.collectAsState(initialThemeFollowSystem)

    val themeType by remember {
        appPreferences.THEME_ID.flow().mapNotNull(Themes::fromIDTheme)
    }.collectAsState(initialThemeType)

    val isSystemThemeLight = !isSystemInDarkTheme()
    val isThemeLight by derivedStateOf {
        themeType !in setOf(Themes.DARK, Themes.GREY, Themes.BLACK)
    }

    val theme: Themes = when (themeFollowSystem)
    {
        true -> when
        {
            isSystemThemeLight && !isThemeLight -> Themes.LIGHT
            !isSystemThemeLight && isThemeLight -> Themes.DARK
            else -> themeType
        }
        false -> themeType
    }

    InternalTheme(
        theme = theme,
        content = content,
        wrapper = wrapper
    )
}

@Composable
fun InternalTheme(
    theme: Themes = if (isSystemInDarkTheme()) Themes.DARK else Themes.LIGHT,
    wrapper: @Composable (fn: @Composable () -> Unit) -> Unit = { fn -> Surface(Modifier.fillMaxSize()) { fn() } },
    content: @Composable () -> Unit
)
{
    val palette = when (theme)
    {
        Themes.LIGHT -> light_ColorPalette
        Themes.DARK -> dark_ColorPalette
        Themes.GREY -> grey_ColorPalette
        Themes.BLACK -> black_ColorPalette
    }

    val systemUiController = rememberSystemUiController()
    systemUiController.setSystemBarsColor(
        color = palette.surface,
        darkIcons = palette.isLight
    )

    MaterialTheme(
        colors = palette,
        typography = Typography,
        shapes = Shapes,
        content = { wrapper { content() } }
    )
}

// InternalTheme but for theming an object (wont try to fill all space)
@Composable
fun InternalThemeObject(
    theme: Themes = if (isSystemInDarkTheme()) Themes.DARK else Themes.LIGHT,
    wrapper: @Composable (fn: @Composable () -> Unit) -> Unit = { fn -> Surface { fn() } },
    content: @Composable () -> Unit
)
{
    val palette = when (theme)
    {
        Themes.LIGHT -> light_ColorPalette
        Themes.DARK -> dark_ColorPalette
        Themes.GREY -> grey_ColorPalette
        Themes.BLACK -> black_ColorPalette
    }

    val systemUiController = rememberSystemUiController()
    systemUiController.setSystemBarsColor(
        color = palette.surface,
        darkIcons = palette.isLight
    )

    MaterialTheme(
        colors = palette,
        typography = Typography,
        shapes = Shapes,
        content = { wrapper { content() } }
    )
}