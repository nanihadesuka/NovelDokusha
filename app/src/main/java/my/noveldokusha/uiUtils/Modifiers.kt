package my.noveldokusha.uiUtils

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun Modifier.ifCase(condition: Boolean, fn: @Composable Modifier.() -> Modifier): Modifier
{
    return if (condition) fn(this) else this
}