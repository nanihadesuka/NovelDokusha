package my.noveldokusha.ui.composeViews

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import my.noveldokusha.ui.theme.ColorAccent

@Composable
fun Section(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        color = MaterialTheme.colors.primaryVariant,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 16.dp),
                color = ColorAccent,
                textAlign = TextAlign.Center,
            )
            Divider(color = MaterialTheme.colors.secondary)
            content()
        }
    }
}