package my.noveldokusha.ui.composeViews

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ClickableOption(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    info: String = "",
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .then(modifier)
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.subtitle1,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.caption,
        )
        if (info.isNotBlank()) Text(
            text = info,
            style = MaterialTheme.typography.caption
        )
    }
}