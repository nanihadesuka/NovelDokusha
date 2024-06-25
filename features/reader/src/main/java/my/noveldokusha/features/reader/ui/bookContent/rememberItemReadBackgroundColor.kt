package my.noveldokusha.features.reader.ui.bookContent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import my.noveldokusha.features.reader.domain.ReaderItem
import my.noveldokusha.features.reader.features.TextSynthesis
import my.noveldokusha.texttospeech.Utterance

@Composable
internal fun rememberItemReadBackgroundColor(
    item: ReaderItem,
    currentSpeakerActiveItem: () -> TextSynthesis
): State<Color> {
    val textSynthesis = currentSpeakerActiveItem()

    return remember(item, textSynthesis) {
        val isReadingItem = item is ReaderItem.Position &&
                textSynthesis.itemPos.chapterIndex == item.chapterIndex &&
                textSynthesis.itemPos.chapterItemPosition == item.chapterItemPosition

        if (!isReadingItem) return@remember Color.Unspecified.let(::mutableStateOf)

        when (textSynthesis.playState) {
            Utterance.PlayState.PLAYING -> Color.Blue.copy(0.3f)
            Utterance.PlayState.LOADING -> Color.Cyan.copy(0.3f)
            Utterance.PlayState.FINISHED -> Color.Unspecified
        }.let(::mutableStateOf)
    }
}