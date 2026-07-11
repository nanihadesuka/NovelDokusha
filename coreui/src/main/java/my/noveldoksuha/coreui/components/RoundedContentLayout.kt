package my.noveldoksuha.coreui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import my.noveldoksuha.coreui.theme.backgroundCircle
import my.noveldoksuha.coreui.theme.outlineCircle

@Composable
fun RoundedContentLayout(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .backgroundCircle()
            .outlineCircle()
            .then(modifier)
    ) {
        content(this)
    }
}