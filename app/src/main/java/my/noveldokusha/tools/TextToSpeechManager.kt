package my.noveldokusha.tools

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TextSynthesisState {
    PLAYING, FINISHED,
}

data class TextSynthesis(
    val chapterItemIndex: Int,
    val chapterIndex: Int,
    val state: TextSynthesisState
) {
    val utteranceId = "$chapterItemIndex-$chapterIndex"
}

data class VoiceData(
    val id: String,
    val language: String,
    val needsInternet: Boolean,
    val quality: Int,
)

class TextToSpeechManager @Inject constructor(
    val context: Context,
) {
    private val _queueList = mutableMapOf<String, TextSynthesis>()
    val queueList = _queueList as Map<String, TextSynthesis>
    val availableVoices = mutableStateListOf<VoiceData>()

    private val scope = CoroutineScope(Dispatchers.Default)

    val service = TextToSpeech(context) {
        when (it) {
            TextToSpeech.SUCCESS -> {
                listenToUtterances()
                availableVoices.addAll(getAvailableVoices())
            }
            TextToSpeech.ERROR -> Unit
            else -> Unit
        }
    }

    private val _currentTextSpeakFlow = MutableSharedFlow<TextSynthesis>()
    val currentTextSpeakFlow = _currentTextSpeakFlow.shareIn(
        scope = CoroutineScope(Dispatchers.Default),
        started = SharingStarted.WhileSubscribed()
    )

    var currentActiveItemState = mutableStateOf(
        TextSynthesis(
            chapterIndex = -1,
            chapterItemIndex = 0,
            state = TextSynthesisState.FINISHED
        )
    )

    fun setVoiceById(id: String) {
        service
            .voices
            .find { it.name == id }
            ?.let { service.setVoice(it) }
    }

    fun getAvailableVoices(): List<VoiceData> {
        return service.voices.map {
            VoiceData(
                id = it.name,
                language = it.locale.displayLanguage,
                needsInternet = it.isNetworkConnectionRequired,
                quality = it.quality
            )
        }
    }

    fun stop() {
        service.stop()
        _queueList.clear()
    }

    fun speak(text: String, textSynthesis: TextSynthesis) {
        _queueList[textSynthesis.utteranceId] = textSynthesis
        val bundle = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, textSynthesis.utteranceId)
        }
        service.speak(text, TextToSpeech.QUEUE_ADD, bundle, textSynthesis.utteranceId)
    }

    private fun listenToUtterances() {
        service.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                if (utteranceId == null) return
                val res = _queueList[utteranceId]
                    ?.copy(state = TextSynthesisState.PLAYING)
                    ?: return
                currentActiveItemState.value = res
                scope.launch { _currentTextSpeakFlow.emit(res) }
            }

            override fun onDone(utteranceId: String?) {
                if (utteranceId == null) return
                val res = _queueList.remove(utteranceId)
                    ?.copy(state = TextSynthesisState.FINISHED)
                    ?: return
                currentActiveItemState.value = res
                scope.launch { _currentTextSpeakFlow.emit(res) }
            }

            override fun onError(utteranceId: String?) {
                if (utteranceId == null) return
                val res = _queueList.remove(utteranceId)
                    ?.copy(state = TextSynthesisState.FINISHED)
                    ?: return
                currentActiveItemState.value = res
                scope.launch { _currentTextSpeakFlow.emit(res) }
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                super.onRangeStart(utteranceId, start, end, frame)
            }
        })
    }
}