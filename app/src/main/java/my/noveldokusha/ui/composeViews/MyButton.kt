package my.noveldokusha.ui.composeViews

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.InternalTheme
import my.noveldokusha.ui.theme.Themes
import my.noveldokusha.ui.theme.selectableMinHeight
import my.noveldokusha.utils.ifCase

@Composable
fun MyButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    animate: Boolean = true,
    textAlign: TextAlign = TextAlign.Start,
    outerPadding: Dp = 4.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    minHeight: Dp = selectableMinHeight,
    shape: Shape = MaterialTheme.shapes.medium,
    borderWidth: Dp = 1.dp,
    borderColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    textStyle: TextStyle = LocalTextStyle.current,
    selected: Boolean = false,
    selectedBackgroundColor: Color = ColorAccent,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit = {

        val color = when {
            selected && (textStyle.color.luminance() < 0.5) -> Color.White
            else -> textStyle.color
        }
        Text(
            text = text,
            style = textStyle,
            color = color,
            textAlign = textAlign,
            modifier = Modifier
                .padding(contentPadding)
                .wrapContentHeight()
                .align(Alignment.Center),
        )
    }
) {
    InternalButton(
        modifier = modifier,
        enabled = enabled,
        animate = animate,
        outerPadding = outerPadding,
        minHeight = minHeight,
        minWidth = Dp.Unspecified,
        shape = shape,
        borderWidth = borderWidth,
        borderColor = borderColor,
        backgroundColor = backgroundColor,
        selectedBackgroundColor = selectedBackgroundColor,
        selected = selected,
        onClick = onClick,
        onLongClick = onLongClick,
        content = content,
    )
}

@Composable
fun MyIconButton(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    animate: Boolean = true,
    contentDescription: String? = null,
    outerPadding: Dp = 4.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    minSize: Dp = selectableMinHeight,
    shape: Shape = MaterialTheme.shapes.large,
    borderWidth: Dp = 1.dp,
    borderColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    selectedBackgroundColor: Color = ColorAccent,
    selected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit = {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier
                .padding(contentPadding)
                .align(Alignment.Center),
        )
    }
) {
    InternalButton(
        modifier = modifier,
        enabled = enabled,
        animate = animate,
        outerPadding = outerPadding,
        minHeight = minSize,
        minWidth = minSize,
        shape = shape,
        borderWidth = borderWidth,
        borderColor = borderColor,
        backgroundColor = backgroundColor,
        selectedBackgroundColor = selectedBackgroundColor,
        selected = selected,
        onClick = onClick,
        onLongClick = onLongClick,
        content = content,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InternalButton(
    modifier: Modifier,
    enabled: Boolean,
    animate: Boolean,
    outerPadding: Dp,
    minHeight: Dp,
    minWidth: Dp,
    shape: Shape,
    borderWidth: Dp,
    borderColor: Color,
    backgroundColor: Color,
    selectedBackgroundColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    content: @Composable BoxScope.() -> Unit
) {
    val background by animateColorAsState(
        targetValue = if (selected) selectedBackgroundColor else backgroundColor, label = ""
    )
    Surface(
        modifier = modifier
            .ifCase(animate) { animateContentSize() }
            .padding(outerPadding)
            .heightIn(min = minHeight)
            .widthIn(min = minWidth)
            .border(borderWidth, borderColor, shape)
            .clip(shape)
            .combinedClickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = background
    ) {
        Box(propagateMinConstraints = true) {
            content(this)
        }
    }
}


@Preview
@Composable
fun Preview() {
    Column {
        for (theme in Themes.values()) InternalTheme(theme) {
            MyButton(
                text = "Theme ${theme.name}",
                modifier = Modifier.fillMaxWidth(),
                onClick = {},
            )
        }
    }
}

@Preview
@Composable
fun PreviewIcon() {
    Column {
        for (theme in Themes.values()) InternalTheme(theme) {
            MyIconButton(
                icon = Icons.Filled.Home,
                modifier = Modifier.fillMaxWidth(),
                onClick = {}
            )
        }
    }
}