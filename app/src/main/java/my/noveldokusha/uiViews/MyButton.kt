package my.noveldokusha.uiViews

import android.widget.Space
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import my.noveldokusha.ui.theme.InternalThemeObject
import my.noveldokusha.ui.theme.Themes
import my.noveldokusha.uiUtils.ifCase
import org.w3c.dom.Text

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MyButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    animate: Boolean = true,
    textAlign: TextAlign = TextAlign.Start,
    radius: Dp = 12.dp,
    onClick: () -> Unit,
    content: @Composable (String, Dp, TextAlign) -> Unit = { text, radius, textAlign ->
        Text(
            text = text,
            modifier = Modifier.padding(radius),
            textAlign = textAlign
        )
    }
) {

    val shape = RoundedCornerShape(radius)
    // Remainder: only use modifier properties, dont use the parameters (wonky compose bugs?)
    Surface(
        modifier = modifier
            .ifCase(animate) { animateContentSize() }
            .padding(4.dp)
            .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.2f), shape)
            .clip(shape)
            .clickable(
                enabled = enabled,
                role = Role.Button
            ) { onClick() },
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.02f)
    ) {
        content(text, radius, textAlign)
    }
}


@Preview
@Composable
fun Preview() {
    Column {
        for (theme in Themes.values()) InternalThemeObject(theme) {
            MyButton(
                text = "Theme ${theme.name}",
                modifier = Modifier.fillMaxWidth(),
                onClick = {}
            )
        }
    }
}