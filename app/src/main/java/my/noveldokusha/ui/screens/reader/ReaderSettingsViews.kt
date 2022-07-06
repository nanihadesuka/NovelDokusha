package my.noveldokusha.ui.screens.reader

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.twotone.FormatSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import my.noveldokusha.ui.theme.InternalTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun CurrentBookInfo(
    chapterTitle: String,
    chapterCurrentNumber: Int,
    chapterPercentageProgress: Float,
    chaptersTotalSize: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colors.primary,
                        MaterialTheme.colors.primary,
                        MaterialTheme.colors.primary,
                        MaterialTheme.colors.primary.copy(alpha = 0f)
                    )
                )
            )
            .padding(horizontal = 20.dp)
            .padding(top = 35.dp, bottom = 50.dp)
    ) {
        Text(
            text = chapterTitle,
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding()
                .animateContentSize()
        )
        Text(
            text = "Chapter: $chapterCurrentNumber/$chaptersTotalSize",
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding()
        )
        Text(
            text = "Progress: ${chapterPercentageProgress.toInt()}%",
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding()
        )
    }
}

@Composable
private fun Settings(
    textFont: String,
    textSize: Float,
    modifier: Modifier = Modifier,
    onTextFontChanged: (String) -> Unit,
    onTextSizeChanged: (Float) -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(10.dp)
    ) {
        TextSizeSlider(
            textSize,
            onTextSizeChanged
        )
        Box(Modifier.height(10.dp))
        TextFontDropDown(
            textFont,
            onTextFontChanged
        )
    }
}

@Composable
fun TextSizeSlider(
    textSize: Float,
    onTextSizeChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    RoundedContentLayout(modifier) {
        Icon(
            imageVector = Icons.TwoTone.FormatSize,
            contentDescription = null,
            modifier = Modifier
                .width(50.dp)
        )
        var pos by remember { mutableStateOf(textSize) }
        Slider(
            value = pos,
            onValueChange = {
                pos = it
                onTextSizeChanged(pos)
            },
            valueRange = 8f..24f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colors.onPrimary,
                activeTrackColor = MaterialTheme.colors.onPrimary,
                activeTickColor = MaterialTheme.colors.primary.copy(alpha = 0.2f),
                inactiveTickColor = MaterialTheme.colors.primary.copy(alpha = 0.2f),
            ),
            modifier = Modifier.padding(end = 16.dp)
        )
    }
}


@Composable
fun TextFontDropDown(
    textFont: String,
    onTextFontChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var rowSize by remember { mutableStateOf(Size.Zero) }
    var expanded by remember { mutableStateOf(false) }
    val fontLoader = FontsLoader()
    RoundedContentLayout(
        modifier
            .clickable { expanded = true }
            .onGloballyPositioned { layoutCoordinates ->
                rowSize = layoutCoordinates.size.toSize()
            }
    ) {
        Icon(
            imageVector = Icons.Default.FontDownload,
            contentDescription = null,
            modifier = Modifier.width(50.dp)
        )
        Text(
            text = textFont,
            style = MaterialTheme.typography.h6,
            fontFamily = fontLoader.getFontFamily(textFont),
            modifier = Modifier
                .padding(end = 50.dp)
                .weight(1f),
            textAlign = TextAlign.Center
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(0.dp, 10.dp),
            modifier = Modifier
                .heightIn(max = 300.dp)
                .width(with(LocalDensity.current) { rowSize.width.toDp() })
        ) {
            FontsLoader.availableFonts.forEach { item ->
                DropdownMenuItem(
                    onClick = { onTextFontChanged(item) }
                ) {
                    Text(
                        text = item,
                        fontFamily = fontLoader.getFontFamily(item),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }
    }
}


@Composable
private fun RoundedContentLayout(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colors.onPrimary.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .background(
                color = MaterialTheme.colors.primary,
                shape = CircleShape
            )
            .clip(CircleShape)
            .then(modifier)
    ) {
        content(this)
    }
}

@Composable
fun ReaderInfoView(
    chapterTitle: String,
    chapterCurrentNumber: Int,
    chapterPercentageProgress: Float,
    chaptersTotalSize: Int,
    textFont: String,
    textSize: Float,
    onTextFontChanged: (String) -> Unit,
    onTextSizeChanged: (Float) -> Unit,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    ConstraintLayout(modifier.fillMaxSize()) {
        val (info, settings) = createRefs()
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.constrainAs(info) {
                top.linkTo(parent.top)
                width = Dimension.matchParent
            }
        ) {
            CurrentBookInfo(
                chapterTitle = chapterTitle,
                chapterCurrentNumber = chapterCurrentNumber,
                chapterPercentageProgress = chapterPercentageProgress,
                chaptersTotalSize = chaptersTotalSize,

                )
        }
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .constrainAs(settings) {
                    bottom.linkTo(parent.bottom)
                    width = Dimension.matchParent
                }
                .padding(bottom = 10.dp)
        ) {
            Settings(
                textFont = textFont,
                textSize = textSize,
                onTextFontChanged = onTextFontChanged,
                onTextSizeChanged = onTextSizeChanged,
            )
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 360
)
@Composable
private fun ViewsPreview() {
    InternalTheme {
        ReaderInfoView(
            chapterTitle = "Chapter title",
            chapterCurrentNumber = 64,
            chapterPercentageProgress = 20f,
            chaptersTotalSize = 540,
            textFont = "serif",
            textSize = 15f,
            onTextSizeChanged = {},
            onTextFontChanged = {},
            visible = true
        )
    }
}