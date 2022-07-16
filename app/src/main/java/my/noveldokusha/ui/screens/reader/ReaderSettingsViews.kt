package my.noveldokusha.ui.screens.reader

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.twotone.FormatSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import my.noveldokusha.R
import my.noveldokusha.tools.TranslationModelState
import my.noveldokusha.ui.screens.reader.tools.FontsLoader
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.InternalTheme
import my.noveldokusha.utils.blockInteraction
import my.noveldokusha.utils.ifCase
import my.noveldokusha.utils.mix
import my.noveldokusha.utils.clickableWithUnboundedIndicator

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
    onTextFontChanged: (String) -> Unit,
    textSize: Float,
    onTextSizeChanged: (Float) -> Unit,
    liveTranslationSettingData: LiveTranslationSettingData,
    modifier: Modifier = Modifier,
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
        Box(Modifier.height(10.dp))
        LiveTranslationSetting(
            enable = liveTranslationSettingData.enable.value,
            listOfAvailableModels = liveTranslationSettingData.listOfAvailableModels,
            source = liveTranslationSettingData.source.value,
            target = liveTranslationSettingData.target.value,
            onEnable = liveTranslationSettingData.onEnable,
            onSourceChange = liveTranslationSettingData.onSourceChange,
            onTargetChange = liveTranslationSettingData.onTargetChange,
            onDownloadTranslationModel = liveTranslationSettingData.onDownloadTranslationModel
        )
    }
}

data class LiveTranslationSettingData(
    val enable: MutableState<Boolean>,
    val listOfAvailableModels: SnapshotStateList<TranslationModelState>,
    val source: MutableState<TranslationModelState?>,
    val target: MutableState<TranslationModelState?>,
    val onEnable: (Boolean) -> Unit,
    val onSourceChange: (TranslationModelState?) -> Unit,
    val onTargetChange: (TranslationModelState?) -> Unit,
    val onDownloadTranslationModel: (language: String) -> Unit,
)

@Composable
fun LiveTranslationSetting(
    enable: Boolean,
    listOfAvailableModels: List<TranslationModelState>,
    source: TranslationModelState?,
    target: TranslationModelState?,
    onEnable: (Boolean) -> Unit,
    onSourceChange: (TranslationModelState?) -> Unit,
    onTargetChange: (TranslationModelState?) -> Unit,
    onDownloadTranslationModel: (language: String) -> Unit,
) {

    var modelSelectorExpanded by rememberSaveable { mutableStateOf(false) }
    var modelSelectorExpandedForTarget by rememberSaveable { mutableStateOf(false) }
    var rowSize by remember { mutableStateOf(Size.Zero) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { layoutCoordinates ->
                rowSize = layoutCoordinates.size.toSize()
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .roundedOutline()
                .blockInteraction(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier
                    .roundedOutline()
                    .clickable { onEnable(!enable) },
                color = if (enable) MaterialTheme.colors.primary.mix(
                    ColorAccent,
                    fraction = 0.8f
                ) else MaterialTheme.colors.primary
            ) {
                Text(
                    text = "Live translation",
                    modifier = Modifier.padding(12.dp)
                )
            }
            AnimatedVisibility(visible = enable) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clickableWithUnboundedIndicator {
                                modelSelectorExpanded = !modelSelectorExpanded
                                modelSelectorExpandedForTarget = false
                            }
                    ) {
                        Text(
                            text = source?.locale?.displayLanguage
                                ?: stringResource(R.string.language_source_empty_text),
                            modifier = Modifier
                                .padding(6.dp)
                                .ifCase(source == null) { alpha(0.5f) },
                        )
                    }
                    Icon(Icons.Default.ArrowRightAlt, contentDescription = null)
                    Box(
                        modifier = Modifier
                            .clickableWithUnboundedIndicator {
                                modelSelectorExpanded = !modelSelectorExpanded
                                modelSelectorExpandedForTarget = true
                            }
                    ) {
                        Text(
                            text = target?.locale?.displayLanguage
                                ?: stringResource(R.string.language_target_empty_text),
                            modifier = Modifier
                                .padding(6.dp)
                                .ifCase(target == null) { alpha(0.5f) },
                        )
                    }
                }
            }
        }

        DropdownMenu(
            expanded = modelSelectorExpanded,
            onDismissRequest = { modelSelectorExpanded = false },
            offset = DpOffset(0.dp, 10.dp),
            modifier = Modifier
                .heightIn(max = 300.dp)
                .width(with(LocalDensity.current) { rowSize.width.toDp() })
        ) {

            DropdownMenuItem(
                onClick = {
                    if (modelSelectorExpandedForTarget) onTargetChange(null)
                    else onSourceChange(null)
                }
            ) {
                Box(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.language_clear_selection),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(4.dp)
                            .background(MaterialTheme.colors.secondary, CircleShape)
                            .padding(8.dp)
                            .align(Alignment.Center)
                    )
                }
            }

            listOfAvailableModels.forEach { item ->
                val isAvailable = item.model != null
                val isAlreadySelected =
                    if (modelSelectorExpandedForTarget) item.language == target?.language
                    else item.language == source?.language
                DropdownMenuItem(
                    onClick = {
                        if (modelSelectorExpandedForTarget) onTargetChange(item)
                        else onSourceChange(item)
                        modelSelectorExpanded = false
                    },
                    enabled = !isAlreadySelected && isAvailable
                ) {
                    Box(Modifier.weight(1f)) {
                        Text(
                            text = item.locale.displayLanguage,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(4.dp)
                                .fillMaxWidth()
                                .align(Alignment.Center)
                        )
                        if (item.model == null) Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .widthIn(min = 22.dp)
                                .height(22.dp)
                                .align(Alignment.CenterEnd)
                        ) {
                            when {
                                item.downloading -> IconButton(onClick = { }, enabled = false) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = MaterialTheme.colors.onPrimary
                                    )
                                }
                                else -> IconButton(
                                    onClick = { onDownloadTranslationModel(item.language) }) {
                                    Icon(
                                        Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = if (item.downloadingFailed) Color.Red
                                        else LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
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

private fun Modifier.roundedOutline(): Modifier = composed {
    border(
        width = 1.dp,
        color = MaterialTheme.colors.onPrimary.copy(alpha = 0.5f),
        shape = CircleShape
    )
        .background(
            color = MaterialTheme.colors.primary,
            shape = CircleShape
        )
        .clip(CircleShape)
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
            .roundedOutline()
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
    liveTranslationSettingData: LiveTranslationSettingData,
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
                .padding(bottom = 60.dp)
        ) {
            Settings(
                textFont = textFont,
                textSize = textSize,
                onTextFontChanged = onTextFontChanged,
                onTextSizeChanged = onTextSizeChanged,
                liveTranslationSettingData = liveTranslationSettingData,
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
            visible = true,
            liveTranslationSettingData = LiveTranslationSettingData(
                enable = remember { mutableStateOf(true) },
                listOfAvailableModels = remember { mutableStateListOf() },
                source = remember {
                    mutableStateOf(
                        TranslationModelState(
                            language = "fr",
                            model = null,
                            false,
                            false
                        )
                    )
                },
                target = remember {
                    mutableStateOf(
                        TranslationModelState(
                            language = "en",
                            model = null,
                            false,
                            false
                        )
                    )
                },
                onTargetChange = {},
                onEnable = {},
                onSourceChange = {},
                onDownloadTranslationModel = {}
            )
        )
    }
}