package my.noveldokusha.ui.screens.reader.settingsViews

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import my.noveldokusha.R
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.selectableMinHeight

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SelectableTextSetting(
    enable: Boolean,
    onEnable: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {

        val textColor = when (enable) {
            true -> Color.White
            false -> MaterialTheme.colorScheme.onPrimary
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(CircleShape)
                .height(selectableMinHeight)
                .toggleable(
                    value = enable,
                    onValueChange = { onEnable(!enable) }
                )
                .background(
                    if (enable) ColorAccent
                    else MaterialTheme.colorScheme.secondary
                )
                .padding(8.dp)
                .padding(start = 6.dp)
        ) {
            Text(
                text = stringResource(R.string.allow_text_selection),
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            AnimatedContent(targetState = enable) { follow ->
                Icon(
                    if (follow) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel,
                    contentDescription = null,
                    tint = textColor
                )
            }
        }
    }
}