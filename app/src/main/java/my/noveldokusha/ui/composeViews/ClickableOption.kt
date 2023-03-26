package my.noveldokusha.ui.composeViews

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
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
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (info.isNotBlank()) Text(
            text = info,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}