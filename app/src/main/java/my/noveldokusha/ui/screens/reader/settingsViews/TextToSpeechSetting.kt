package my.noveldokusha.ui.screens.reader.settingsViews

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.InternalTheme
import my.noveldokusha.ui.theme.InternalThemeObject
import my.noveldokusha.uiViews.MyButton
import my.noveldokusha.utils.ifCase

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TextToSpeechSetting(
    isPlaying: Boolean,
    currentVoice: VoiceData?,
    setPlaying: (Boolean) -> Unit,
    playPreviousItem: () -> Unit,
    playPreviousChapter: () -> Unit,
    playNextItem: () -> Unit,
    playNextChapter: () -> Unit,
    availableVoices: List<VoiceData>,
    onSelectVoice: (VoiceData) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(4.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            MaterialTheme.colors.primaryVariant,
                            MaterialTheme.colors.primaryVariant.copy(alpha = 0.5f)
                        )
                    )
                )
        ) {
            IconButton(onClick = playPreviousChapter) {
                Icon(
                    imageVector = Icons.Outlined.FastRewind,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .background(ColorAccent, CircleShape),
                    tint = Color.White,
                )
            }
            IconButton(onClick = playPreviousItem) {
                Icon(
                    imageVector = Icons.Filled.NavigateBefore,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(32.dp)
                        .background(ColorAccent, CircleShape),
                )
            }
            IconButton(onClick = { setPlaying(!isPlaying) }) {
                AnimatedContent(
                    targetState = isPlaying,
                    modifier = Modifier
                        .size(46.dp)
                        .background(ColorAccent, CircleShape)
                ) { target ->
                    when (target) {
                        true -> Icon(
                            Icons.Filled.Pause,
                            contentDescription = null,
                            tint = Color.White,
                        )
                        false -> Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                }

            }
            IconButton(onClick = playNextItem) {
                Icon(
                    Icons.Filled.NavigateNext,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(32.dp)
                        .background(ColorAccent, CircleShape),
                )
            }
            IconButton(onClick = playNextChapter) {
                Icon(
                    Icons.Outlined.FastForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .background(ColorAccent, CircleShape),
                )
            }
        }

        var openVoicesDialog by remember { mutableStateOf(false) }
        MyButton(
            text = stringResource(R.string.voices),
            onClick = { openVoicesDialog = !openVoicesDialog }
        )

        val inputTextFilter = remember { mutableStateOf("") }

        VoiceSelectorDialog(
            availableVoices = availableVoices,
            currentVoice = currentVoice,
            inputTextFilter = inputTextFilter,
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
                .padding(vertical = 8.dp)
                .background(MaterialTheme.colors.background, MaterialTheme.shapes.large)
                .padding(8.dp),
        ) {
            stickyHeader {
                Surface(color = MaterialTheme.colors.background) {
                    OutlinedTextField(
                        value = inputTextFilter.value,
                        onValueChange = { inputTextFilter.value = it },
                        maxLines = 1,
                        placeholder = {
                            Text(
                                text = "Search voice by language",
                                modifier = Modifier.alpha(0.7f),
                                style = MaterialTheme.typography.subtitle2
                            )
                        },
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .heightIn(min = 42.dp)
                            .fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    )
                }
            }
            items(voicesFiltered) {
                val selected = it.id == currentVoice?.id
                Row(
                    modifier = Modifier
                        .heightIn(min = 54.dp)
                        .background(
                            if (selected) MaterialTheme.colors.secondaryVariant else MaterialTheme.colors.secondaryVariant,
                            MaterialTheme.shapes.large
                        )
                        .clip(MaterialTheme.shapes.large)
                        .clickable(enabled = !selected) { setVoice(it) }
                        .ifCase(selected) { border(2.dp, ColorAccent, MaterialTheme.shapes.large) }
                        .padding(horizontal = 8.dp)
                        .padding(4.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = it.language,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .widthIn(min = 64.dp)
                    )
                    Text(
                        text = "${it.quality}/500",
                        modifier = Modifier
                            .background(MaterialTheme.colors.primary, MaterialTheme.shapes.medium)
                            .padding(4.dp),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.overline
                    )
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
                            style = MaterialTheme.typography.overline,
                            fontSize = 8.sp,
                        )
                        if (it.needsInternet) {
                            Text(
                                text = "Needs Internet",
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colors.primary,
                                        MaterialTheme.shapes.medium
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                fontSize = 8.sp,
                                style = MaterialTheme.typography.overline
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
    InternalThemeObject {
        TextToSpeechSetting(
            isPlaying = true,
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
            }
        )
    }
}