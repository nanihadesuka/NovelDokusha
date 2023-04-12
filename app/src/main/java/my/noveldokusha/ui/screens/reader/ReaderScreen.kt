package my.noveldokusha.ui.screens.reader

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import my.noveldokusha.R
import my.noveldokusha.ui.screens.reader.ReaderScreenState.Settings.Type
import my.noveldokusha.ui.theme.Themes
import my.noveldokusha.ui.theme.colorApp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    state: ReaderScreenState,
    onSelectableTextChange: (Boolean) -> Unit,
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
                                    Icon(Icons.Filled.ArrowBack, null)
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

//@Preview(
//    showBackground = true,
//    widthDp = 360
//)
//@Composable
//private fun ViewsPreview() {
//
//    val liveTranslationSettingData = LiveTranslationSettingData(
//        isAvailable = true,
//        enable = remember { mutableStateOf(true) },
//        listOfAvailableModels = remember { mutableStateListOf() },
//        source = remember {
//            mutableStateOf(
//                TranslationModelState(
//                    language = "fr",
//                    available = true,
//                    downloading = false,
//                    downloadingFailed = false
//                )
//            )
//        },
//        target = remember {
//            mutableStateOf(
//                TranslationModelState(
//                    language = "en",
//                    available = true,
//                    downloading = false,
//                    downloadingFailed = false
//                )
//            )
//        },
//        onTargetChange = {},
//        onEnable = {},
//        onSourceChange = {},
//        onDownloadTranslationModel = {}
//    )
//
//    val textToSpeechSettingData = TextToSpeechSettingData(
//        isPlaying = rememberMutableStateOf(false),
//        isLoadingChapter = rememberMutableStateOf(false),
//        voicePitch = rememberMutableStateOf(1f),
//        voiceSpeed = rememberMutableStateOf(1f),
//        availableVoices = remember { mutableStateListOf() },
//        activeVoice = remember {
//            mutableStateOf(
//                VoiceData(
//                    id = "",
//                    language = "",
//                    quality = 100,
//                    needsInternet = true
//                )
//            )
//        },
//        currentActiveItemState = remember {
//            mutableStateOf(
//                TextSynthesis(
//                    playState = Utterance.PlayState.PLAYING,
//                    itemPos = ReaderItem.Title(
//                        chapterUrl = "",
//                        chapterIndex = 0,
//                        chapterItemPosition = 1,
//                        text = ""
//                    )
//                )
//            )
//        },
//        isThereActiveItem = rememberMutableStateOf(true),
//        setPlaying = {},
//        playPreviousItem = {},
//        playPreviousChapter = {},
//        playNextItem = {},
//        playNextChapter = {},
//        setVoiceId = {},
//        playFirstVisibleItem = {},
//        scrollToActiveItem = {},
//        setVoiceSpeed = {},
//        setVoicePitch = {},
//        setCustomSavedVoices = {},
//        customSavedVoices = rememberMutableStateOf(value = listOf())
//    )
//
//    InternalTheme {
//        Surface(color = Color.Black) {
//            ReaderInfoView(
//                chapterTitle = "Chapter title",
//                chapterCurrentNumber = 64,
//                chapterPercentageProgress = 20f,
//                chaptersTotalSize = 540,
//                textFont = "serif",
//                textSize = 15f,
//                onTextSizeChanged = {},
//                onTextFontChanged = {},
//                visible = true,
//                liveTranslationSettingData = liveTranslationSettingData,
//                textToSpeechSettingData = textToSpeechSettingData,
//                currentTheme = Themes.DARK,
//                currentFollowSystem = true,
//                selectableText = false,
//                onSelectableTextChange = {},
//                onFollowSystem = {},
//                onThemeSelected = {},
//            )
//        }
//    }
//}
