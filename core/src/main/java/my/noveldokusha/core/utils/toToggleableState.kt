package my.noveldokusha.core.utils

import androidx.compose.ui.state.ToggleableState
import my.noveldokusha.core.appPreferences.TernaryState

fun TernaryState.toToggleableState() = when (this) {
    TernaryState.Active -> ToggleableState.On
    TernaryState.Inverse -> ToggleableState.Indeterminate
    TernaryState.Inactive -> ToggleableState.Off
}