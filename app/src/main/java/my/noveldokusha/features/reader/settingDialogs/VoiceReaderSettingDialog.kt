package my.noveldokusha.features.reader.settingDialogs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.withContext
import my.noveldokusha.R
import my.noveldokusha.VoicePredefineState
import my.noveldokusha.composableActions.debouncedAction
import my.noveldokusha.features.reader.features.TextToSpeechSettingData
import my.noveldokusha.tools.VoiceData
import my.noveldokusha.ui.composeViews.MyOutlinedTextField
import my.noveldokusha.ui.composeViews.MySlider
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.ColorNotice
import my.noveldokusha.ui.theme.InternalTheme
import my.noveldokusha.ui.theme.colorApp
import my.noveldokusha.utils.rememberMutableStateOf


@OptIn(ExperimentalAnimationApi::class, ExperimentalLayoutApi::class)
@Composable
fun VoiceReaderSettingDialog(
    state: TextToSpeechSettingData
) {
    var openVoicesDialog by rememberSaveable { mutableStateOf(false) }
    val dropdownCustomSavedVoicesExpanded = rememberSaveable { mutableStateOf(false) }

    Column {
        AnimatedVisibility(visible = state.isLoadingChapter.value) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    strokeWidth = 6.dp,
                    color = ColorAccent,
                    modifier = Modifier.background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        CircleShape
                    )
                )
            }
        }
        ElevatedCard(
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                // Player voice parameters
                MySlider(
                    value = state.voicePitch.value,
                    valueRange = 0.1f..5f,
                    onValueChange = state.setVoicePitch,
                    text = stringResource(R.string.voice_pitch) + ": %.2f".format(state.voicePitch.value),
                )
                MySlider(
                    value = state.voiceSpeed.value,
                    valueRange = 0.1f..5f,
                    onValueChange = state.setVoiceSpeed,
                    text = stringResource(R.string.voice_speed) + ": %.2f".format(state.voiceSpeed.value),
                )

                // Player settings buttons
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AssistChip(
                        label = { Text(text = stringResource(id = R.string.start_here)) },
                        onClick = debouncedAction { state.playFirstVisibleItem() },
                        leadingIcon = { Icon(Icons.Filled.CenterFocusWeak, null) },
                        colors = AssistChipDefaults.assistChipColors(
                            leadingIconContentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledLeadingIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                    AssistChip(
                        label = { Text(text = stringResource(id = R.string.focus)) },
                        onClick = debouncedAction { state.scrollToActiveItem() },
                        leadingIcon = { Icon(Icons.Filled.CenterFocusStrong, null) },
                        colors = AssistChipDefaults.assistChipColors(
                            leadingIconContentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledLeadingIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                    AssistChip(
                        label = { Text(text = stringResource(id = R.string.voices)) },
                        onClick = { openVoicesDialog = !openVoicesDialog },
                        leadingIcon = { Icon(Icons.Filled.RecordVoiceOver, null) },
                        colors = AssistChipDefaults.assistChipColors(
                            leadingIconContentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledLeadingIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                    AssistChip(
                        label = { Text(text = stringResource(R.string.saved_voices)) },
                        onClick = {
                            dropdownCustomSavedVoicesExpanded.let {
                                it.value = !it.value
                            }
                        },
                        leadingIcon = { Icon(Icons.Filled.Bookmarks, null) },
                        colors = AssistChipDefaults.assistChipColors(
                            leadingIconContentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledLeadingIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                    Box {
                        DropdownCustomSavedVoices(
                            expanded = dropdownCustomSavedVoicesExpanded,
                            list = state.customSavedVoices.value,
                            currentVoice = state.activeVoice.value,
                            currentVoiceSpeed = state.voiceSpeed.value,
                            currentVoicePitch = state.voicePitch.value,
                            onPredefinedSelected = {
                                state.setVoiceSpeed(it.speed)
                                state.setVoicePitch(it.pitch)
                                state.setVoiceId(it.voiceId)
                            },
                            setCustomSavedVoices = state.setCustomSavedVoices
                        )
                        VoiceSelectorDialog(
                            availableVoices = state.availableVoices,
                            currentVoice = state.activeVoice.value,
                            inputTextFilter = rememberSaveable { mutableStateOf("") },
                            setVoice = state.setVoiceId,
                            isDialogOpen = openVoicesDialog,
                            setDialogOpen = { openVoicesDialog = it }
                        )
                    }
                }

                // Player playback buttons
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val alpha by animateFloatAsState(
                        targetValue = if (state.isThereActiveItem.value) 1f else 0.5f,
                        label = ""
                    )
                    IconButton(
                        onClick = debouncedAction(waitMillis = 1000) { state.playPreviousChapter() },
                        enabled = state.isThereActiveItem.value,
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
                        onClick = debouncedAction(waitMillis = 100) { state.playPreviousItem() },
                        enabled = state.isThereActiveItem.value,
                        modifier = Modifier.alpha(alpha),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.NavigateBefore,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(38.dp)
                                .background(ColorAccent, CircleShape),
                        )
                    }
                    IconButton(onClick = { state.setPlaying(!state.isPlaying.value) }) {
                        AnimatedContent(
                            targetState = state.isPlaying.value,
                            modifier = Modifier
                                .size(56.dp)
                                .background(ColorAccent, CircleShape), label = ""
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
                        onClick = debouncedAction(waitMillis = 100) { state.playNextItem() },
                        enabled = state.isThereActiveItem.value,
                        modifier = Modifier.alpha(alpha),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.NavigateNext,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(38.dp)
                                .background(ColorAccent, CircleShape),
                        )
                    }
                    IconButton(
                        onClick = debouncedAction(waitMillis = 1000) { state.playNextChapter() },
                        enabled = state.isThereActiveItem.value,
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
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, FlowPreview::class)
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
            modifier = Modifier
                .shadow(10.dp, MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large)
        ) {
            stickyHeader {
                Surface(color = MaterialTheme.colorApp.tintedSurface) {
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
                val selected by remember { derivedStateOf { it.id == currentVoice?.id } }
                Row(
                    modifier = Modifier
                        .heightIn(min = 54.dp)
                        .background(
                            if (selected) MaterialTheme.colorApp.tintedSelectedSurface
                            else MaterialTheme.colorScheme.primary
                        )
                        .clickable(enabled = !selected) { setVoice(it.id) }
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
                            val yay = it.quality > star * 100
                            Icon(
                                imageVector = if (yay) Icons.Filled.StarRate else Icons.Outlined.StarBorder,
                                contentDescription = null,
                                tint = if (yay) ColorNotice else MaterialTheme.colorScheme.onPrimary,
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
                                    MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium
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


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DropdownCustomSavedVoices(
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
        ListItem(
            headlineContent = {
                Text(text = stringResource(R.string.save_current_voice))
            },
            leadingContent = {
                Icon(
                    Icons.Outlined.Save,
                    null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            },
            modifier = Modifier.clickable { expandedAddNextEntry = true }
        )
        Divider()
        if (list.isEmpty()) {
            Text(text = stringResource(R.string.no_voices_saved), Modifier.padding(16.dp))
        }
        list.forEachIndexed { index, predefinedVoice ->
            var deleteEntryExpand by rememberMutableStateOf(false)
            ListItem(
                headlineContent = {
                    Text(text = predefinedVoice.savedName)
                },
                modifier = Modifier.combinedClickable(
                    enabled = true,
                    onClick = { onPredefinedSelected(predefinedVoice) },
                    onLongClick = { deleteEntryExpand = true },
                )
            )
            if (deleteEntryExpand) AlertDialog(
                onDismissRequest = { deleteEntryExpand = false },
                title = { Text(text = stringResource(R.string.delete_voice)) },
                text = {
                    Text(
                        text = predefinedVoice.savedName,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    FilledTonalButton(onClick = {
                        deleteEntryExpand = false
                        setCustomSavedVoices(
                            list.toMutableList().also { it.removeAt(index) }
                        )
                    }) {
                        Text(text = stringResource(id = R.string.delete))
                    }
                },
                dismissButton = {
                    Button(onClick = { deleteEntryExpand = false }) {
                        Text(text = stringResource(R.string.cancel))
                    }
                },
            )
        }
    }


    if (expandedAddNextEntry) {
        var name by rememberMutableStateOf(value = "")
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            delay(300)
            focusRequester.requestFocus()
        }
        AlertDialog(
            tonalElevation = 24.dp,
            onDismissRequest = { expandedAddNextEntry = false },
            title = { Text(text = stringResource(R.string.save_current_voice_parameters)) },
            text = {
                MyOutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeHolderText = stringResource(R.string.name),
                    modifier = Modifier.focusRequester(focusRequester)
                )
            },
            confirmButton = {
                FilledTonalButton(onClick = onClick@{
                    val voice = currentVoice ?: return@onClick
                    val state = VoicePredefineState(
                        savedName = name,
                        voiceId = voice.id,
                        speed = currentVoiceSpeed,
                        pitch = currentVoicePitch
                    )
                    setCustomSavedVoices(list + state)
                    expandedAddNextEntry = false
                }) {
                    Text(text = stringResource(R.string.save_voice))
                }
            },
            dismissButton = {
                Button(onClick = { expandedAddNextEntry = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Preview(group = "dialog")
@Composable
private fun VoiceSelectorDialogContentPreview() {
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
