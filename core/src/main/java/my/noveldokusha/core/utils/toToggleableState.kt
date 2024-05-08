package my.noveldokusha.core.utils

import androidx.compose.ui.state.ToggleableState
import my.noveldokusha.core.AppPreferences

fun AppPreferences.TERNARY_STATE.toToggleableState() = when (this) {
    AppPreferences.TERNARY_STATE.active -> ToggleableState.On
    AppPreferences.TERNARY_STATE.inverse -> ToggleableState.Indeterminate
    AppPreferences.TERNARY_STATE.inactive -> ToggleableState.Off
}