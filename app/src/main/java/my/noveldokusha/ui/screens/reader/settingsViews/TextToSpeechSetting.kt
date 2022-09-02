package my.noveldokusha.ui.screens.reader.settingsViews

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.withContext
import my.noveldokusha.R
import my.noveldokusha.tools.VoiceData
import my.noveldokusha.ui.screens.reader.roundedOutline
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.InternalTheme
import my.noveldokusha.ui.theme.InternalThemeObject
import my.noveldokusha.ui.theme.Themes
import my.noveldokusha.utils.ifCase

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TextToSpeechSetting(
    isPlaying: Boolean,
    isLoadingChapter: Boolean,
    currentVoice: VoiceData?,
    setPlaying: (Boolean) -> Unit,
    playPreviousItem: () -> Unit,
    playPreviousChapter: () -> Unit,
    playNextItem: () -> Unit,
    playNextChapter: () -> Unit,
    availableVoices: List<VoiceData>,
    onSelectVoice: (VoiceData) -> Unit,
    playFirstVisibleItem: () -> Unit,
    scrollToActiveItem: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {

        AnimatedVisibility(visible = isLoadingChapter) {
            CircularProgressIndicator(
                strokeWidth = 6.dp,
                color = ColorAccent,
                modifier = Modifier.background(
                    MaterialTheme.colors.surface.copy(alpha = 0.7f),
                    CircleShape
                )
            )
        }
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(4.dp)
                .background(MaterialTheme.colors.primaryVariant.copy(alpha = 0.8f), CircleShape)
                .padding(4.dp)
        ) {
            IconButton(onClick = playPreviousChapter) {
                Icon(
                    imageVector = Icons.Rounded.FastRewind,
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .background(ColorAccent, CircleShape),
                    tint = Color.White,
                )
            }
            IconButton(onClick = playPreviousItem) {
                Icon(
                    imageVector = Icons.Rounded.NavigateBefore,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(38.dp)
                        .background(ColorAccent, CircleShape),
                )
            }
            IconButton(onClick = { setPlaying(!isPlaying) }) {
                AnimatedContent(
                    targetState = isPlaying,
                    modifier = Modifier
                        .size(56.dp)
                        .background(ColorAccent, CircleShape)
                ) { target ->
                    when (target) {
                        true -> Icon(
                            Icons.Rounded.Pause,
                            contentDescription = null,
                            tint = Color.White,
                        )
                        false -> Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                }

            }
            IconButton(onClick = playNextItem) {
                Icon(
                    Icons.Rounded.NavigateNext,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(38.dp)
                        .background(ColorAccent, CircleShape),
                )
            }
            IconButton(onClick = playNextChapter) {
                Icon(
                    Icons.Rounded.FastForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(32.dp)
                        .background(ColorAccent, CircleShape),
                )
            }
        }

        var openVoicesDialog by rememberSaveable { mutableStateOf(false) }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(8.dp)
        ) {

            Surface(
                color = MaterialTheme.colors.primary,
                modifier = Modifier
                    .roundedOutline()
                    .clickable { playFirstVisibleItem() }
                    .widthIn(min = 76.dp)
            ) {
                Text(
                    text = stringResource(R.string.start_here),
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center
                )
            }
            Surface(
                color = MaterialTheme.colors.primary,
                modifier = Modifier
                    .roundedOutline()
                    .clickable { scrollToActiveItem() }
                    .widthIn(min = 76.dp)
            ) {
                Text(
                    text = stringResource(R.string.focus),
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center
                )
            }
            Surface(
                color = MaterialTheme.colors.primary,
                modifier = Modifier
                    .roundedOutline()
                    .clickable { openVoicesDialog = !openVoicesDialog }
                    .widthIn(min = 76.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.voices),
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        VoiceSelectorDialog(
            availableVoices = availableVoices,
            currentVoice = currentVoice,
            inputTextFilter = rememberSaveable { mutableStateOf("") },
            setVoice = onSelectVoice,
            isDialogOpen = openVoicesDialog,
            setDialogOpen = { openVoicesDialog = it }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VoiceSelectorDialog(
    availableVoices: List<VoiceData>,
    currentVoice: VoiceData?,
    inputTextFilter: MutableState<String>,
    setVoice: (VoiceData) -> Unit,
    isDialogOpen: Boolean,
    setDialogOpen: (Boolean) -> Unit,
) {
    val voicesSorted = remember { mutableStateListOf<VoiceData>() }
    LaunchedEffect(availableVoices) {
        withContext(Dispatchers.Default) {
            availableVoices.sortedWith(
                compareBy<VoiceData> { it.language }
                    .thenByDescending { it.quality }
                    .thenBy { it.needsInternet }
            )
        }.let { voicesSorted.addAll(it) }
    }

    val voicesFiltered =
        remember { mutableStateListOf<VoiceData>().apply { addAll(availableVoices) } }

    LaunchedEffect(Unit) {
        snapshotFlow { inputTextFilter.value }
            .debounce(200)
            .collectLatest {
                val items = withContext(Dispatchers.Default) {
                    if (inputTextFilter.value.isEmpty()) {
                        voicesSorted
                    } else {
                        voicesSorted.filter { voice ->
                            voice.language.contains(it, ignoreCase = true)
                        }
                    }
                }
                voicesFiltered.clear()
                voicesFiltered.addAll(items)
            }
    }

    val listState = rememberLazyListState()

    if (isDialogOpen) Dialog(onDismissRequest = { setDialogOpen(false) }) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(bottom = 8.dp)
                .background(MaterialTheme.colors.background, MaterialTheme.shapes.large)
                .padding(8.dp),
        ) {
            stickyHeader {
                Surface(color = MaterialTheme.colors.background) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(bottom = 2.dp),
                    ) {
                        OutlinedTextField(
                            value = inputTextFilter.value,
                            onValueChange = { inputTextFilter.value = it },
                            singleLine = true,
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.search_voice_by_language),
                                    modifier = Modifier.alpha(0.7f),
                                    style = MaterialTheme.typography.subtitle2
                                )
                            },
                            modifier = Modifier
                                .heightIn(min = 42.dp)
                                .fillMaxWidth(),
                            shape = CircleShape,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = MaterialTheme.colors.onPrimary
                            )
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.language),
                                modifier = Modifier
                                    .widthIn(min = 84.dp)
                                    .padding(start = 20.dp)
                            )
                            Text(
                                text = stringResource(R.string.quality),
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                    }
                }
            }

            if (voicesFiltered.isEmpty()) item {
                Text(
                    text = stringResource(R.string.no_matches),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                )
            }

            items(voicesFiltered) {
                val selected = it.id == currentVoice?.id
                Row(
                    modifier = Modifier
                        .heightIn(min = 54.dp)
                        .background(
                            if (selected) MaterialTheme.colors.secondaryVariant else MaterialTheme.colors.secondaryVariant,
                            CircleShape
                        )
                        .clip(CircleShape)
                        .clickable(enabled = !selected) { setVoice(it) }
                        .ifCase(selected) { border(2.dp, ColorAccent, CircleShape) }
                        .padding(horizontal = 16.dp)
                        .padding(4.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = it.language,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.widthIn(min = 84.dp)
                    )
                    Row {
                        for (star in 0..4) {
                            Icon(
                                imageVector = Icons.Default.StarRate,
                                contentDescription = null,
                                tint = if (it.quality > star * 100) {
                                    Color.Yellow
                                } else {
                                    LocalContentColor.current.copy(
                                        alpha = LocalContentAlpha.current
                                    )
                                },
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.wrapContentHeight()
                    ) {
                        Text(
                            text = it.id,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colors.primary,
                                    MaterialTheme.shapes.medium
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.body1,
                            fontSize = 10.sp,
                        )
                        if (it.needsInternet) {
                            Text(
                                text = stringResource(R.string.needs_internet),
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colors.primary,
                                        MaterialTheme.shapes.medium
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                style = MaterialTheme.typography.body1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(group = "dialog")
@Composable
fun VoiceSelectorDialogContentPreview() {
    InternalTheme {
        VoiceSelectorDialog(
            availableVoices = (0..7).map {
                VoiceData(
                    id = "$it",
                    language = "lang${it / 2}",
                    needsInternet = (it % 2) == 0,
                    quality = (it * 100) % 501
                )
            },
            setVoice = {},
            inputTextFilter = remember { mutableStateOf("hello") },
            currentVoice = VoiceData(id = "2", language = "", needsInternet = false, quality = 100),
            setDialogOpen = {},
            isDialogOpen = true
        )
    }
}

@Preview(group = "setting")
@Composable
fun TextToSpeechSettingPreview() {
    InternalThemeObject(Themes.DARK) {
        TextToSpeechSetting(
            isPlaying = true,
            isLoadingChapter = true,
            setPlaying = {},
            currentVoice = VoiceData(id = "", language = "", needsInternet = false, quality = 100),
            playPreviousItem = {},
            playPreviousChapter = {},
            playNextItem = {},
            playNextChapter = {},
            onSelectVoice = {},
            availableVoices = (0..7).map {
                VoiceData(
                    id = "$it",
                    language = "lang$it",
                    needsInternet = (it % 2) == 0,
                    quality = it
                )
            },
            scrollToActiveItem = {},
            playFirstVisibleItem = {},
        )
    }
}