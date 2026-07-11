package my.noveldoksuha.coreui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
    ListItem(
        modifier = Modifier
            .clickable(onClick = onClick)
            .then(modifier)
            .heightIn(min = 60.dp),
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        supportingContent = {
            Column {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (info.isNotBlank()) Text(
                    text = info,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
    )
}