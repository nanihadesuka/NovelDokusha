package my.noveldokusha.ui.screens.reader

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.twotone.FormatSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import my.noveldokusha.R
import my.noveldokusha.tools.TranslationModelState
import my.noveldokusha.tools.Utterance
import my.noveldokusha.tools.VoiceData
import my.noveldokusha.ui.screens.main.settings.SettingsTheme
import my.noveldokusha.ui.screens.reader.settingsViews.*
import my.noveldokusha.ui.screens.reader.tools.LiveTranslationSettingData
import my.noveldokusha.ui.screens.reader.tools.TextSynthesis
import my.noveldokusha.ui.screens.reader.tools.TextToSpeechSettingData
import my.noveldokusha.ui.theme.InternalTheme
import my.noveldokusha.ui.theme.Themes
import my.noveldokusha.uiViews.MyButton
import my.noveldokusha.uiViews.MyIconButton
import my.noveldokusha.utils.rememberMutableStateOf

private enum class CurrentSettingVisible {
    None, TextSize, TextFont, LiveTranslation, TextToSpeech, Theme, SelectableText
}

@Composable
private fun CurrentBookInfo(
    chapterTitle: String,
    chapterCurrentNumber: Int,
    chapterPercentageProgress: Float,
    chaptersTotalSize: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(
            text = chapterTitle,
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding()
                .animateContentSize()
        )
        Text(
            text = stringResource(
                id = R.string.chapter_x_over_n,
                chapterCurrentNumber,
                chaptersTotalSize
            ),
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding()
        )
        Text(
            text = stringResource(id = R.string.progress_x_percentage, chapterPercentageProgress),
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding()
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun Settings(
    textFont: String,
    onTextFontChanged: (String) -> Unit,
    textSize: Float,
    onTextSizeChanged: (Float) -> Unit,
    liveTranslationSettingData: LiveTranslationSettingData,
    textToSpeechSettingData: TextToSpeechSettingData,
    modifier: Modifier = Modifier,
    visibleSetting: MutableState<CurrentSettingVisible>,
    settingsListState: LazyListState,
    currentTheme: Themes,
    currentFollowSystem: Boolean,
    selectableText: Boolean,
    onSelectableTextChange: (Boolean) -> Unit,
    onFollowSystem: (Boolean) -> Unit,
    onThemeSelected: (Themes) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Box(Modifier.padding(horizontal = 24.dp)) {
            AnimatedContent(targetState = visibleSetting.value) { target ->
                when (target) {
                    CurrentSettingVisible.TextSize -> TextSizeSetting(
                        textSize,
                        onTextSizeChanged
                    )
                    CurrentSettingVisible.TextFont -> TextFontSetting(
                        textFont,
                        onTextFontChanged
                    )
                    CurrentSettingVisible.LiveTranslation -> LiveTranslationSetting(
                        enable = liveTranslationSettingData.enable.value,
                        listOfAvailableModels = liveTranslationSettingData.listOfAvailableModels,
                        source = liveTranslationSettingData.source.value,
                        target = liveTranslationSettingData.target.value,
                        onEnable = liveTranslationSettingData.onEnable,
                        onSourceChange = liveTranslationSettingData.onSourceChange,
                        onTargetChange = liveTranslationSettingData.onTargetChange,
                        onDownloadTranslationModel = liveTranslationSettingData.onDownloadTranslationModel
                    )
                    CurrentSettingVisible.Theme -> SettingsTheme(
                        currentTheme = currentTheme,
                        currentFollowSystem = currentFollowSystem,
                        onFollowSystem = onFollowSystem,
                        onThemeSelected = onThemeSelected,
                    )
                    CurrentSettingVisible.SelectableText -> SelectableTextSetting(
                        enable = selectableText,
                        onEnable = onSelectableTextChange
                    )
                    CurrentSettingVisible.TextToSpeech -> TextToSpeechSetting(
                        isPlaying = textToSpeechSettingData.isPlaying.value,
                        isLoadingChapter = textToSpeechSettingData.isLoadingChapter.value,
                        voicePitch = textToSpeechSettingData.voicePitch.value,
                        voiceSpeed = textToSpeechSettingData.voiceSpeed.value,
                        setVoicePitch = textToSpeechSettingData.setVoicePitch,
                        setVoiceSpeed = textToSpeechSettingData.setVoiceSpeed,
                        currentVoice = textToSpeechSettingData.activeVoice.value,
                        isActive = textToSpeechSettingData.isThereActiveItem.value,
                        customSavedVoicesStates = textToSpeechSettingData.customSavedVoices.value,
                        availableVoices = textToSpeechSettingData.availableVoices,
                        playCurrent = textToSpeechSettingData.setPlaying,
                        playPreviousItem = textToSpeechSettingData.playPreviousItem,
                        playPreviousChapter = textToSpeechSettingData.playPreviousChapter,
                        playNextItem = textToSpeechSettingData.playNextItem,
                        playNextChapter = textToSpeechSettingData.playNextChapter,
                        setVoice = textToSpeechSettingData.setVoiceId,
                        playFirstVisibleItem = textToSpeechSettingData.playFirstVisibleItem,
                        scrollToActiveItem = textToSpeechSettingData.scrollToActiveItem,
                        setCustomSavedVoices = textToSpeechSettingData.setCustomSavedVoices,
                    )
                    CurrentSettingVisible.None -> Unit
                }
            }
        }
        SettingsRowList(
            listState = settingsListState,
            visibleSetting = visibleSetting.value,
            setVisibleSetting = { visibleSetting.value = it },
            liveTranslationSettingDataIsAvailable = liveTranslationSettingData.isAvailable
        )
    }
}

@Composable
private fun SettingsRowList(
    listState: LazyListState,
    visibleSetting: CurrentSettingVisible,
    setVisibleSetting: (CurrentSettingVisible) -> Unit,
    liveTranslationSettingDataIsAvailable: Boolean
) {
    fun toggleOrOpen(state: CurrentSettingVisible) {
        setVisibleSetting(if (visibleSetting == state) CurrentSettingVisible.None else state)
    }

    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(MaterialTheme.colors.secondary),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp + 200.dp,
            top = 4.dp,
            bottom = 4.dp
        )
    ) {
        item {
            MyIconButton(
                icon = Icons.TwoTone.FormatSize,
                selected = visibleSetting == CurrentSettingVisible.TextSize,
                shape = CircleShape,
                contentPadding = 8.dp,
                modifier = Modifier.size(50.dp),
                onClick = { toggleOrOpen(CurrentSettingVisible.TextSize) },
            )
        }
        item {
            MyIconButton(
                icon = Icons.Default.FontDownload,
                selected = visibleSetting == CurrentSettingVisible.TextFont,
                shape = CircleShape,
                contentPadding = 8.dp,
                modifier = Modifier.size(50.dp),
                onClick = { toggleOrOpen(CurrentSettingVisible.TextFont) },
            )
        }
        if (liveTranslationSettingDataIsAvailable) item {
            MyButton(
                text = stringResource(id = R.string.live_translation),
                selected = visibleSetting == CurrentSettingVisible.LiveTranslation,
                shape = CircleShape,
                contentPadding = 8.dp,
                modifier = Modifier.height(50.dp),
                onClick = { toggleOrOpen(CurrentSettingVisible.LiveTranslation) },
            )
        }
        item {
            MyButton(
                text = stringResource(R.string.text_to_speech),
                selected = visibleSetting == CurrentSettingVisible.TextToSpeech,
                shape = CircleShape,
                contentPadding = 8.dp,
                modifier = Modifier.height(50.dp),
                onClick = { toggleOrOpen(CurrentSettingVisible.TextToSpeech) },
            )
        }
        item {
            MyButton(
                text = stringResource(id = R.string.theme),
                selected = visibleSetting == CurrentSettingVisible.Theme,
                shape = CircleShape,
                contentPadding = 8.dp,
                modifier = Modifier.height(50.dp),
                onClick = { toggleOrOpen(CurrentSettingVisible.Theme) },
            )
        }
        item {
            MyButton(
                text = stringResource(R.string.text_selection),
                selected = visibleSetting == CurrentSettingVisible.SelectableText,
                shape = CircleShape,
                contentPadding = 8.dp,
                modifier = Modifier.height(50.dp),
                onClick = { toggleOrOpen(CurrentSettingVisible.SelectableText) },
            )
        }
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
    currentTheme: Themes,
    currentFollowSystem: Boolean,
    selectableText: Boolean,
    onSelectableTextChange: (Boolean) -> Unit,
    onFollowSystem: (Boolean) -> Unit,
    onThemeSelected: (Themes) -> Unit,
    onTextFontChanged: (String) -> Unit,
    onTextSizeChanged: (Float) -> Unit,
    liveTranslationSettingData: LiveTranslationSettingData,
    textToSpeechSettingData: TextToSpeechSettingData,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val visibleSetting = rememberSaveable { mutableStateOf(CurrentSettingVisible.None) }
    val settingsListState = rememberLazyListState()

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
            Column(
                modifier = Modifier
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
                    .padding(bottom = 50.dp)
                    .windowInsetsPadding(WindowInsets.displayCutout)
                    .windowInsetsPadding(WindowInsets.systemBars)
            ) {
                CurrentBookInfo(
                    chapterTitle = chapterTitle,
                    chapterCurrentNumber = chapterCurrentNumber,
                    chapterPercentageProgress = chapterPercentageProgress,
                    chaptersTotalSize = chaptersTotalSize,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                )
            }
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
                textToSpeechSettingData = textToSpeechSettingData,
                visibleSetting = visibleSetting,
                settingsListState = settingsListState,
                currentTheme = currentTheme,
                currentFollowSystem = currentFollowSystem,
                selectableText = selectableText,
                onSelectableTextChange = onSelectableTextChange,
                onFollowSystem = onFollowSystem,
                onThemeSelected = onThemeSelected,
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

    val liveTranslationSettingData = LiveTranslationSettingData(
        isAvailable = true,
        enable = remember { mutableStateOf(true) },
        listOfAvailableModels = remember { mutableStateListOf() },
        source = remember {
            mutableStateOf(
                TranslationModelState(
                    language = "fr",
                    available = true,
                    downloading = false,
                    downloadingFailed = false
                )
            )
        },
        target = remember {
            mutableStateOf(
                TranslationModelState(
                    language = "en",
                    available = true,
                    downloading = false,
                    downloadingFailed = false
                )
            )
        },
        onTargetChange = {},
        onEnable = {},
        onSourceChange = {},
        onDownloadTranslationModel = {}
    )

    val textToSpeechSettingData = TextToSpeechSettingData(
        isPlaying = rememberMutableStateOf(false),
        isLoadingChapter = rememberMutableStateOf(false),
        voicePitch = rememberMutableStateOf(1f),
        voiceSpeed = rememberMutableStateOf(1f),
        availableVoices = remember { mutableStateListOf() },
        activeVoice = remember {
            mutableStateOf(
                VoiceData(
                    id = "",
                    language = "",
                    quality = 100,
                    needsInternet = true
                )
            )
        },
        currentActiveItemState = remember {
            mutableStateOf(
                TextSynthesis(
                    playState = Utterance.PlayState.PLAYING,
                    item = ReaderItem.Title(
                        chapterUrl = "",
                        chapterItemIndex = 1,
                        chapterIndex = 1,
                        text = ""
                    )
                )
            )
        },
        isThereActiveItem = rememberMutableStateOf(true),
        setPlaying = {},
        playPreviousItem = {},
        playPreviousChapter = {},
        playNextItem = {},
        playNextChapter = {},
        setVoiceId = {},
        playFirstVisibleItem = {},
        scrollToActiveItem = {},
        setVoiceSpeed = {},
        setVoicePitch = {},
        setCustomSavedVoices = {},
        customSavedVoices = rememberMutableStateOf(value = listOf())
    )

    InternalTheme {
        Surface(color = Color.Black) {
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
                liveTranslationSettingData = liveTranslationSettingData,
                textToSpeechSettingData = textToSpeechSettingData,
                currentTheme = Themes.DARK,
                currentFollowSystem = true,
                selectableText = false,
                onSelectableTextChange = {},
                onFollowSystem = {},
                onThemeSelected = {},
            )
        }
    }
}
