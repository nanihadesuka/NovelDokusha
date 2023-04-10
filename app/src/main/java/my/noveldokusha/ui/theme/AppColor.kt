package my.noveldokusha.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColor(
    val tabSurface: Color,
    val bookSurface: Color,
    val checkboxPositive: Color,
    val checkboxNegative: Color,
    val checkboxNeutral: Color
)

val light_appColor = AppColor(
    tabSurface = Grey75,
    bookSurface = Grey75,
    checkboxPositive = Success500,
    checkboxNegative = Error500,
    checkboxNeutral = Grey900,
)

val dark_appColor = AppColor(
    tabSurface = Grey900,
    bookSurface = Grey800,
    checkboxPositive = Success500,
    checkboxNegative = Error500,
    checkboxNeutral = Grey900,
)

val black_appColor = AppColor(
    tabSurface = Grey900,
    bookSurface = Grey900,
    checkboxPositive = Success500,
    checkboxNegative = Error500,
    checkboxNeutral = Grey900,
)

val LocalAppColor = compositionLocalOf { light_appColor }
val MaterialTheme.colorApp: AppColor
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColor.current