package my.noveldokusha.core

import my.noveldoksuha.coreui.theme.Themes

val PreferenceThemes.toTheme
    get() = when (this) {
        PreferenceThemes.Light -> Themes.LIGHT
        PreferenceThemes.Dark -> Themes.DARK
        PreferenceThemes.Black -> Themes.BLACK
    }

val Themes.toPreferenceTheme
    get() = when (this) {
        Themes.LIGHT -> PreferenceThemes.Light
        Themes.DARK -> PreferenceThemes.Dark
        Themes.BLACK -> PreferenceThemes.Black
    }