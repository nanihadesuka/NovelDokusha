package my.noveldokusha.uiUtils

import androidx.compose.ui.Modifier

fun Modifier.ifCase(condition: Boolean, fn: Modifier.() -> Modifier): Modifier {
    return if (condition) fn(this) else this
}