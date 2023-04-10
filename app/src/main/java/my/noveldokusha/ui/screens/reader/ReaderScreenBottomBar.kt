package my.noveldokusha.ui.screens.reader

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import my.noveldokusha.ui.screens.reader.settingDialogs.MoreSettingDialog
import my.noveldokusha.ui.screens.reader.settingDialogs.StyleSettingDialog
import my.noveldokusha.ui.screens.reader.settingDialogs.TranslatorSettingDialog
import my.noveldokusha.ui.screens.reader.settingDialogs.VoiceReaderSettingDialog
import my.noveldokusha.ui.theme.Themes

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SettingsBottomBarDialog(
    settings: ReaderScreenState.Settings,
    onTextFontChanged: (String) -> Unit,
    onTextSizeChanged: (Float) -> Unit,
    onSelectableTextChange: (Boolean) -> Unit,
    onFollowSystem: (Boolean) -> Unit,
    onThemeSelected: (Themes) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(Modifier.padding(horizontal = 24.dp)) {
            AnimatedContent(targetState = settings.selectedSetting.value, label = "") { target ->
                Column {
                    when (target) {
                        ReaderScreenState.Settings.Type.LiveTranslation -> TranslatorSettingDialog(
                            state = settings.liveTranslation
                        )
                        ReaderScreenState.Settings.Type.TextToSpeech -> VoiceReaderSettingDialog(
                            state = settings.textToSpeech
                        )
                        ReaderScreenState.Settings.Type.Style -> {
                            StyleSettingDialog(
                                state = settings.style,
                                onFollowSystemChange = onFollowSystem,
                                onThemeSelectedChange = onThemeSelected,
                                onTextFontChange = onTextFontChanged,
                                onTextSizeChange = onTextSizeChanged,
                            )
                        }
                        ReaderScreenState.Settings.Type.More -> MoreSettingDialog(
                            allowTextSelection = settings.isTextSelectable.value,
                            onAllowTextSelectionChange = onSelectableTextChange
                        )
                        ReaderScreenState.Settings.Type.None -> Unit
                    }
                }
            }
        }
    }
}