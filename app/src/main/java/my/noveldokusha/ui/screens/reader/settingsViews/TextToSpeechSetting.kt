package my.noveldokusha.ui.screens.reader.settingsViews

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import my.noveldokusha.R
import my.noveldokusha.tools.VoiceData
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.InternalThemeObject
import my.noveldokusha.uiViews.MyButton

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TextToSpeechSetting(
    isPlaying: Boolean,
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

        if (openVoicesDialog) Dialog(onDismissRequest = { openVoicesDialog = false }) {
            LazyColumn {
                items(availableVoices) {
                    MyButton(
                        text = "Name: ${it.id}  Lang:${it.language}  Internet:${it.needsInternet}  Quality: ${it.quality}/500",
                        onClick = {
                            openVoicesDialog = false
                            onSelectVoice(it)
                        },
                    )
                }
            }
        }
    }
}

@Preview(group = "setting")
@Composable
fun TextToSpeechSettingPreview() {
    InternalThemeObject {
        TextToSpeechSetting(
            isPlaying = true,
            setPlaying = {},
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