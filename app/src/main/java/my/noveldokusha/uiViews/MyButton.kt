package my.noveldokusha.uiViews

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import my.noveldokusha.ui.theme.InternalThemeObject
import my.noveldokusha.ui.theme.Themes
import my.noveldokusha.uiUtils.ifCase

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun MyButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    animate: Boolean = true,
    textAlign: TextAlign = TextAlign.Start,
    outterPadding: Dp = 4.dp,
    radius: Dp = 4.dp,
    borderWidth: Dp = 1.dp,
    backgroundColor: Color = MaterialTheme.colors.primary,
    textStyle: TextStyle = LocalTextStyle.current,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    content: @Composable (String, Dp, TextAlign) -> Unit = { text, radius, textAlign ->
        Text(
            text = text,
            modifier = Modifier.padding(12.dp),
            textAlign = textAlign,
            style = textStyle
        )
    }
)
{
    val shape = RoundedCornerShape(radius)
    Surface(
        modifier = modifier
            .ifCase(animate) { animateContentSize() }
            .padding(outterPadding)
            .border(borderWidth, MaterialTheme.colors.onSurface.copy(alpha = 0.2f), shape)
            .clip(shape)
            .combinedClickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = backgroundColor
    ) {
        content(text, radius, textAlign)
    }
}


@Preview
@Composable
fun Preview()
{
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