package my.noveldokusha.core.utils

import androidx.compose.ui.state.ToggleableState
import my.noveldokusha.core.appPreferences.TernaryState

fun TernaryState.toToggleableState() = when (this) {
    TernaryState.active -> ToggleableState.On
    TernaryState.inverse -> ToggleableState.Indeterminate
    TernaryState.inactive -> ToggleableState.Off
}