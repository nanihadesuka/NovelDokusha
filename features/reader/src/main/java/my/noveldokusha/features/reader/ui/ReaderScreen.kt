package my.noveldokusha.features.reader.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import my.noveldoksuha.coreui.theme.InternalTheme
import my.noveldoksuha.coreui.theme.Themes
import my.noveldoksuha.coreui.theme.colorApp
import my.noveldoksuha.coreui.theme.rememberMutableStateOf
import my.noveldokusha.features.reader.domain.ReaderItem
import my.noveldokusha.features.reader.features.LiveTranslationSettingData
import my.noveldokusha.features.reader.features.TextSynthesis
import my.noveldokusha.features.reader.features.TextToSpeechSettingData
import my.noveldokusha.features.reader.ui.ReaderScreenState.Settings.Type
import my.noveldokusha.reader.R
import my.noveldokusha.text_translator.domain.TranslationModelState
import my.noveldokusha.tooling.texttospeech.Utterance
import my.noveldokusha.tooling.texttospeech.VoiceData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    state: ReaderScreenState,
    onSelectableTextChange: (Boolean) -> Unit,
    onKeepScreenOn: (Boolean) -> Unit,
    onFollowSystem: (Boolean) -> Unit,
    onThemeSelected: (Themes) -> Unit,
    onTextFontChanged: (String) -> Unit,
    onTextSizeChanged: (Float) -> Unit,
    onPressBack: () -> Unit,
    onOpenChapterInWeb: () -> Unit,
    readerContent: @Composable (paddingValues: PaddingValues) -> Unit,
) {
    // Capture back action when viewing info
    BackHandler(enabled = state.showReaderInfo.value) {
        state.showReaderInfo.value = false
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = state.showReaderInfo.value,
                enter = expandVertically(initialHeight = { 0 }, expandFrom = Alignment.Top)
                        + fadeIn(),
                exit = shrinkVertically(targetHeight = { 0 }, shrinkTowards = Alignment.Top)
                        + fadeOut(),
            ) {
                Surface(color = MaterialTheme.colorApp.tintedSurface) {
                    Column(modifier = Modifier.displayCutoutPadding()) {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorApp.tintedSurface,
                                scrolledContainerColor = MaterialTheme.colorApp.tintedSurface,
                            ),
                            title = {
                                Text(
                                    text = state.readerInfo.chapterTitle.value,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.animateContentSize()
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = onPressBack) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                                }
                            },
                            actions = {
                                IconButton(onClick = onOpenChapterInWeb) {
                                    Icon(Icons.Filled.Public, null)
                                }
                            }
                        )
                        Column(
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .padding(horizontal = 16.dp),
                        ) {
                            Text(
                                text = stringResource(
                                    id = R.string.chapter_x_over_n,
                                    state.readerInfo.chapterCurrentNumber.value,
                                    state.readerInfo.chaptersCount.value,
                                ),
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Text(
                                text = stringResource(
                                    id = R.string.progress_x_percentage,
                                    state.readerInfo.chapterPercentageProgress.value
                                ),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        Divider()
                    }
                }
            }
        },
        content = readerContent,
        bottomBar = {

            val toggleOrSet = { type: Type ->
                state.settings.selectedSetting.value = when (state.settings.selectedSetting.value) {
                    type -> Type.None
                    else -> type
                }
            }
            AnimatedVisibility(
                visible = state.showReaderInfo.value,
                enter = expandVertically(initialHeight = { 0 }) + fadeIn(),
                exit = shrinkVertically(targetHeight = { 0 }) + fadeOut(),
            ) {
                Column {
                    ReaderScreenBottomBarDialogs(
                        settings = state.settings,
                        onTextFontChanged = onTextFontChanged,
                        onTextSizeChanged = onTextSizeChanged,
                        onSelectableTextChange = onSelectableTextChange,
                        onFollowSystem = onFollowSystem,
                        onThemeSelected = onThemeSelected,
                        onKeepScreenOn = onKeepScreenOn,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    BottomAppBar(
                        modifier = Modifier.clip(
                            RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp
                            )
                        ),
                        containerColor = MaterialTheme.colorApp.tintedSurface,
                    ) {
                        if (state.settings.liveTranslation.isAvailable) SettingIconItem(
                            currentType = state.settings.selectedSetting.value,
                            settingType = Type.LiveTranslation,
                            onClick = toggleOrSet,
                            icon = Icons.Outlined.Translate,
                            textId = R.string.translator,
                        )
                        SettingIconItem(
                            currentType = state.settings.selectedSetting.value,
                            settingType = Type.TextToSpeech,
                            onClick = toggleOrSet,
                            icon = Icons.Filled.RecordVoiceOver,
                            textId = R.string.voice_reader,
                        )
                        SettingIconItem(
                            currentType = state.settings.selectedSetting.value,
                            settingType = Type.Style,
                            onClick = toggleOrSet,
                            icon = Icons.Outlined.ColorLens,
                            textId = R.string.style,
                        )
                        SettingIconItem(
                            currentType = state.settings.selectedSetting.value,
                            settingType = Type.More,
                            onClick = toggleOrSet,
                            icon = Icons.Outlined.MoreHoriz,
                            textId = R.string.more,
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun RowScope.SettingIconItem(
    currentType: Type,
    settingType: Type,
    @StringRes textId: Int,
    icon: ImageVector,
    onClick: (type: Type) -> Unit,
) {
    NavigationBarItem(
        selected = currentType == settingType,
        onClick = { onClick(settingType) },
        icon = { Icon(icon, null) },
        label = { Text(text = stringResource(id = textId)) }
    )
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ViewsPreview(
    @PreviewParameter(PreviewDataProvider::class) data: PreviewDataProvider.Data
) {

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
                    itemPos = ReaderItem.Title(
                        chapterUrl = "",
                        chapterIndex = 0,
                        chapterItemPosition = 1,
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

    val style = ReaderScreenState.Settings.StyleSettingsData(
        followSystem = remember { mutableStateOf(true) },
        currentTheme = remember { mutableStateOf(Themes.DARK) },
        textFont = remember { mutableStateOf("Arial") },
        textSize = remember { mutableFloatStateOf(20f) },
    )

    InternalTheme {
        Surface(color = Color.Black) {
            ReaderScreen(
                state = ReaderScreenState(
                    showReaderInfo = remember { mutableStateOf(true) },
                    readerInfo = ReaderScreenState.CurrentInfo(
                        chapterTitle = remember { mutableStateOf("Chapter title") },
                        chapterCurrentNumber = remember { mutableIntStateOf(2) },
                        chapterPercentageProgress = remember { mutableFloatStateOf(0.5f) },
                        chaptersCount = remember { mutableIntStateOf(255) },
                        chapterUrl = remember { mutableStateOf("Chapter url") },
                    ),
                    settings = ReaderScreenState.Settings(
                        isTextSelectable = remember { mutableStateOf(false) },
                        keepScreenOn = remember { mutableStateOf(false) },
                        textToSpeech = textToSpeechSettingData,
                        liveTranslation = liveTranslationSettingData,
                        style = style,
                        selectedSetting = remember { mutableStateOf(data.selectedSetting) },
                    ),
                    showInvalidChapterDialog = remember { mutableStateOf(false) }
                ),
                onTextSizeChanged = {},
                onTextFontChanged = {},
                onSelectableTextChange = {},
                onFollowSystem = {},
                onThemeSelected = {},
                onPressBack = {},
                onOpenChapterInWeb = {},
                readerContent = {},
                onKeepScreenOn = {},
            )
        }
    }
}


private class PreviewDataProvider : PreviewParameterProvider<PreviewDataProvider.Data> {
    data class Data(
        val selectedSetting: Type
    )

    override val values = sequenceOf(
        Data(selectedSetting = Type.None),
        Data(selectedSetting = Type.LiveTranslation),
        Data(selectedSetting = Type.TextToSpeech),
        Data(selectedSetting = Type.Style),
        Data(selectedSetting = Type.More),
    )
}
