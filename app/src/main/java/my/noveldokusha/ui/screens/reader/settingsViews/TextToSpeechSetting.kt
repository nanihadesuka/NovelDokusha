package my.noveldokusha.ui.screens.reader.settingsViews

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.withContext
import my.noveldokusha.R
import my.noveldokusha.VoicePredefineState
import my.noveldokusha.composableActions.debouncedAction
import my.noveldokusha.tools.VoiceData
import my.noveldokusha.ui.composeViews.*
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.InternalTheme
import my.noveldokusha.ui.theme.InternalThemeObject
import my.noveldokusha.ui.theme.Themes
import my.noveldokusha.utils.backgroundRounded
import my.noveldokusha.utils.ifCase
import my.noveldokusha.utils.outlineRounded
import my.noveldokusha.utils.rememberMutableStateOf


@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterialApi::class)
@Composable
fun TextToSpeechSetting(
    isPlaying: Boolean,
    isLoadingChapter: Boolean,
    currentVoice: VoiceData?,
    isActive: Boolean,
    voiceSpeed: Float,
    voicePitch: Float,
    playCurrent: (Boolean) -> Unit,
    playPreviousItem: () -> Unit,
    playPreviousChapter: () -> Unit,
    playNextItem: () -> Unit,
    playNextChapter: () -> Unit,
    playFirstVisibleItem: () -> Unit,
    scrollToActiveItem: () -> Unit,
    availableVoices: List<VoiceData>,
    customSavedVoicesStates: List<VoicePredefineState>,
    setVoice: (voiceId: String) -> Unit,
    setVoiceSpeed: (Float) -> Unit,
    setVoicePitch: (Float) -> Unit,
    setCustomSavedVoices: (List<VoicePredefineState>) -> Unit,
) {
    var openVoicesDialog by rememberSaveable { mutableStateOf(false) }
    val dropdownCustomSavedVoicesExpanded = rememberSaveable { mutableStateOf(false) }

    Column {
        AnimatedVisibility(visible = isLoadingChapter) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    strokeWidth = 6.dp,
                    color = ColorAccent,
                    modifier = Modifier.background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        CircleShape
                    )
                )
            }
        }
        Section {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background, CircleShape)
                ) {
                    val alpha by animateFloatAsState(targetValue = if (isActive) 1f else 0.5f)
                    IconButton(
                        onClick = debouncedAction(waitMillis = 1000) { playPreviousChapter() },
                        enabled = isActive,
                        modifier = Modifier.alpha(alpha),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FastRewind,
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .background(ColorAccent, CircleShape),
                            tint = Color.White,
                        )
                    }
                    IconButton(
                        onClick = debouncedAction(waitMillis = 100) { playPreviousItem() },
                        enabled = isActive,
                        modifier = Modifier.alpha(alpha),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NavigateBefore,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(38.dp)
                                .background(ColorAccent, CircleShape),
                        )
                    }
                    IconButton(onClick = { playCurrent(!isPlaying) }) {
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
                    IconButton(
                        onClick = debouncedAction(waitMillis = 100) { playNextItem() },
                        enabled = isActive,
                        modifier = Modifier.alpha(alpha),
                    ) {
                        Icon(
                            Icons.Rounded.NavigateNext,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(38.dp)
                                .background(ColorAccent, CircleShape),
                        )
                    }
                    IconButton(
                        onClick = debouncedAction(waitMillis = 1000) { playNextChapter() },
                        enabled = isActive,
                        modifier = Modifier.alpha(alpha),
                    ) {
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

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MyButton(
                        text = stringResource(id = R.string.start_here),
                        onClick = debouncedAction { playFirstVisibleItem() },
                        borderWidth = 1.dp,
                        borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        outerPadding = 0.dp,
                    )
                    MyButton(
                        text = stringResource(id = R.string.focus),
                        onClick = debouncedAction { scrollToActiveItem() },
                        borderWidth = 1.dp,
                        borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        outerPadding = 0.dp,
                    )
                    MyButton(
                        text = stringResource(id = R.string.voices),
                        onClick = { openVoicesDialog = !openVoicesDialog },
                        borderWidth = 1.dp,
                        borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        outerPadding = 0.dp,
                    )
                    MyIconButton(
                        icon = Icons.Outlined.FormatListBulleted,
                        onClick = {
                            dropdownCustomSavedVoicesExpanded.let {
                                it.value = !it.value
                            }
                        },
                        contentDescription = stringResource(R.string.custom_predefined_voices),
                        borderWidth = 1.dp,
                        borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        outerPadding = 0.dp,
                    )
                    Box {
                        DropdownCustomSavedVoices(
                            expanded = dropdownCustomSavedVoicesExpanded,
                            list = customSavedVoicesStates,
                            currentVoice = currentVoice,
                            currentVoiceSpeed = voiceSpeed,
                            currentVoicePitch = voicePitch,
                            onPredefinedSelected = {
                                setVoiceSpeed(it.speed)
                                setVoicePitch(it.pitch)
                                setVoice(it.voiceId)
                            },
                            setCustomSavedVoices = setCustomSavedVoices
                        )
                        VoiceSelectorDialog(
                            availableVoices = availableVoices,
                            currentVoice = currentVoice,
                            inputTextFilter = rememberSaveable { mutableStateOf("") },
                            setVoice = setVoice,
                            isDialogOpen = openVoicesDialog,
                            setDialogOpen = { openVoicesDialog = it }
                        )
                    }
                }

                MySlider(
                    value = voicePitch,
                    valueRange = 0.1f..5f,
                    onValueChange = setVoicePitch,
                    text = stringResource(R.string.voice_pitch) + ": %.2f".format(voicePitch),
                )

                MySlider(
                    value = voiceSpeed,
                    valueRange = 0.1f..5f,
                    onValueChange = setVoiceSpeed,
                    text = stringResource(R.string.voice_speed) + ": %.2f".format(voiceSpeed),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VoiceSelectorDialog(
    availableVoices: List<VoiceData>,
    currentVoice: VoiceData?,
    inputTextFilter: MutableState<String>,
    setVoice: (voiceId: String) -> Unit,
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

    val voicesFiltered = remember {
        mutableStateListOf<VoiceData>().apply { addAll(availableVoices) }
    }

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

    val inputFocusRequester = remember { FocusRequester() }

    if (isDialogOpen) Dialog(
        onDismissRequest = { setDialogOpen(false) }
    ) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(4.dp)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.large)
        ) {
            stickyHeader {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column {
                        MyOutlinedTextField(
                            value = inputTextFilter.value,
                            onValueChange = { inputTextFilter.value = it },
                            placeHolderText = stringResource(R.string.search_voice_by_language),
                            modifier = Modifier
                                .padding(12.dp)
                                .focusRequester(inputFocusRequester)
                        )
                        Divider(Modifier.padding(top = 0.dp))
                    }
                }
                LaunchedEffect(Unit) {
                    delay(300)
                    inputFocusRequester.requestFocus()
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
                        .padding(horizontal = 4.dp)
                        .heightIn(min = 54.dp)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            CircleShape
                        )
                        .clip(CircleShape)
                        .clickable(enabled = !selected) { setVoice(it.id) }
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
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.shapes.medium
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 10.sp,
                        )
                        if (it.needsInternet) {
                            Text(
                                text = stringResource(R.string.needs_internet),
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.shapes.medium
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownCustomSavedVoices(
    expanded: MutableState<Boolean>,
    list: List<VoicePredefineState>,
    currentVoice: VoiceData?,
    currentVoiceSpeed: Float,
    currentVoicePitch: Float,
    onPredefinedSelected: (VoicePredefineState) -> Unit,
    setCustomSavedVoices: (List<VoicePredefineState>) -> Unit,
) {

    var expandedAddNextEntry by rememberMutableStateOf(false)
    DropdownMenu(
        expanded = expanded.value,
        onDismissRequest = { expanded.value = !expanded.value },
    ) {
        MyButton(
            text = stringResource(R.string.save_current_voice),
            onClick = { expandedAddNextEntry = true },
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(8.dp),
            outerPadding = 0.dp,
            shape = MaterialTheme.shapes.medium,
            borderWidth = 1.dp,
            borderColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
        )
        Column(
            Modifier
                .padding(horizontal = 8.dp)
                .backgroundRounded()
                .outlineRounded(width = Dp.Hairline)
                .widthIn(min = 128.dp)
        ) {
            list.forEachIndexed { index, predefinedVoice ->
                var deleteEntryExpand by rememberMutableStateOf(false)
                MyButton(
                    text = predefinedVoice.savedName,
                    onClick = { onPredefinedSelected(predefinedVoice) },
                    onLongClick = { deleteEntryExpand = true },
                    borderWidth = Dp.Unspecified,
                    textAlign = TextAlign.Center,
                    outerPadding = 0.dp,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                )
                if (deleteEntryExpand) AlertDialog(
                    onDismissRequest = { deleteEntryExpand = false },
                    content = {
                        Column {
                            Text(
                                text = predefinedVoice.savedName,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                textAlign = TextAlign.Center
                            )
                            Box(Modifier.padding(8.dp)) {
                                MyButton(
                                    text = stringResource(R.string.delete),
                                    onClick = {
                                        deleteEntryExpand = false
                                        setCustomSavedVoices(
                                            list.toMutableList().also { it.removeAt(index) }
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    if (expandedAddNextEntry) Dialog(
        onDismissRequest = { expandedAddNextEntry = !expandedAddNextEntry }
    ) {
        var name by rememberMutableStateOf(value = "")
        val focusRequester = remember { FocusRequester() }
        Surface(
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.save_current_voice_parameters),
                    textAlign = TextAlign.Center
                )
                MyOutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeHolderText = stringResource(R.string.name),
                    modifier = Modifier.focusRequester(focusRequester)
                )
                MyButton(
                    text = stringResource(id = R.string.save),
                    onClick = click@{
                        val voice = currentVoice ?: return@click
                        val state = VoicePredefineState(
                            savedName = name,
                            voiceId = voice.id,
                            speed = currentVoiceSpeed,
                            pitch = currentVoicePitch
                        )
                        setCustomSavedVoices(list + state)
                        expandedAddNextEntry = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    outerPadding = 0.dp,
                    textAlign = TextAlign.Center
                )
            }
        }
        LaunchedEffect(Unit) {
            delay(300)
            focusRequester.requestFocus()
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
            currentVoice = VoiceData(
                id = "2",
                language = "",
                needsInternet = false,
                quality = 100
            ),
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
            playCurrent = {},
            currentVoice = VoiceData(
                id = "",
                language = "",
                needsInternet = false,
                quality = 100
            ),
            voiceSpeed = 1f,
            voicePitch = 1f,
            isActive = false,
            playPreviousItem = {},
            playPreviousChapter = {},
            playNextItem = {},
            playNextChapter = {},
            setVoice = {},
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
            setVoicePitch = {},
            setVoiceSpeed = {},
            customSavedVoicesStates = listOf(),
            setCustomSavedVoices = {},
        )
    }
}