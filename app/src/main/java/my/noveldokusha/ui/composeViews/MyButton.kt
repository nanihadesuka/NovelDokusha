package my.noveldokusha.uiViews

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.InternalThemeObject
import my.noveldokusha.ui.theme.Themes
import my.noveldokusha.utils.ifCase

private val defaultButtonContent =
    @Composable { text: String, shape: Shape, textAlign: TextAlign, textStyle: TextStyle, contentPadding: Dp ->
        Text(
            text = text,
            modifier = Modifier
                .padding(contentPadding)
                .wrapContentHeight(Alignment.CenterVertically),
            textAlign = textAlign,
            style = textStyle
        )
    }

private val defaultIconButtonContent =
    @Composable { icon: ImageVector, contentPadding: Dp, contentDescription: String? ->
        Icon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier
                .padding(contentPadding)
                .wrapContentHeight(Alignment.CenterVertically),
        )
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    animate: Boolean = true,
    textAlign: TextAlign = TextAlign.Start,
    outerPadding: Dp = 4.dp,
    contentPadding: Dp = 12.dp,
    shape: Shape = RoundedCornerShape(4.dp),
    borderWidth: Dp = 1.dp,
    backgroundColor: Color = MaterialTheme.colors.primary,
    selectedBackgroundColor: Color = ColorAccent,
    textStyle: TextStyle = LocalTextStyle.current,
    selected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    content: @Composable (String, Shape, TextAlign, TextStyle, Dp) -> Unit = defaultButtonContent
) {
    val background by animateColorAsState(
        targetValue = if (selected) selectedBackgroundColor else backgroundColor
    )
    Surface(
        modifier = modifier
            .ifCase(animate) { animateContentSize() }
            .padding(outerPadding)
            .border(borderWidth, MaterialTheme.colors.onSurface.copy(alpha = 0.2f), shape)
            .clip(shape)
            .combinedClickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = background,
    ) {
        content(text, shape, textAlign, textStyle, contentPadding)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyIconButton(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    animate: Boolean = true,
    contentDescription: String? = null,
    outerPadding: Dp = 4.dp,
    contentPadding: Dp = 12.dp,
    shape: Shape = RoundedCornerShape(4.dp),
    borderWidth: Dp = 1.dp,
    backgroundColor: Color = MaterialTheme.colors.primary,
    selectedBackgroundColor: Color = ColorAccent,
    selected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    content: @Composable (ImageVector, Dp, String?) -> Unit = defaultIconButtonContent
) {
    val background by animateColorAsState(
        targetValue = if (selected) selectedBackgroundColor else backgroundColor
    )
    Surface(
        modifier = modifier
            .ifCase(animate) { animateContentSize() }
            .padding(outerPadding)
            .border(borderWidth, MaterialTheme.colors.onSurface.copy(alpha = 0.2f), shape)
            .clip(shape)
            .combinedClickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = background
    ) {
        content(icon, contentPadding, contentDescription)
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