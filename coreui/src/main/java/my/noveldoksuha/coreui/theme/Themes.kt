package my.noveldoksuha.coreui.theme

import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import my.noveldoksuha.coreui.R

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
}