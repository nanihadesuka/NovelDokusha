package my.noveldokusha.tools

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.compose.runtime.derivedStateOf
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
    PLAYING, FINISHED, LOADING,
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
    private val scope = CoroutineScope(Dispatchers.Default)
    private val _queueList = mutableMapOf<String, TextSynthesis>()
    private val _currentTextSpeakFlow = MutableSharedFlow<TextSynthesis>()
    val availableVoices = mutableStateListOf<VoiceData>()
    val voiceSpeed = mutableStateOf<Float>(1f)
    val voicePitch = mutableStateOf<Float>(1f)
    val activeVoice = mutableStateOf<VoiceData?>(null)
    val serviceLoadedFlow = MutableSharedFlow<Unit>(replay = 1)

    val queueList = _queueList as Map<String, TextSynthesis>
    val currentTextSpeakFlow = _currentTextSpeakFlow.shareIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed()
    )

    val service = TextToSpeech(context) {
        when (it) {
            TextToSpeech.SUCCESS -> {
                listenToUtterances()
                availableVoices.addAll(getAvailableVoices())
                updateActiveVoice()
                scope.launch { serviceLoadedFlow.emit(Unit) }
            }
            TextToSpeech.ERROR -> Unit
            else -> Unit
        }
    }

    val currentActiveItemState = mutableStateOf(
        TextSynthesis(
            chapterIndex = -1,
            chapterItemIndex = 0,
            state = TextSynthesisState.FINISHED
        )
    )

    val isThereActiveItem = derivedStateOf { currentActiveItemState.value.chapterIndex != -1 }

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

    fun setCurrentSpeakState(textSynthesis: TextSynthesis) {
        currentActiveItemState.value = textSynthesis
        scope.launch { _currentTextSpeakFlow.emit(textSynthesis) }
    }

    fun trySetVoiceById(id: String): Boolean {
        val voice = service.voices.find { it.name == id } ?: return false
        service.voice = voice
        updateActiveVoice()
        return true
    }

    fun trySetVoicePitch(value: Float): Boolean {
        if (value < 0.1 || value > 5) return false
        val result = service.setPitch(value)
        if (result == TextToSpeech.SUCCESS) {
            voicePitch.value = value
            return true
        }
        return false
    }

    fun trySetVoiceSpeed(value: Float): Boolean {
        if (value < 0.1 || value > 5) return false
        val result = service.setSpeechRate(value)
        if (result == TextToSpeech.SUCCESS) {
            voiceSpeed.value = value
            return true
        }
        return false
    }

    private fun updateActiveVoice() {
        activeVoice.value = service.voice?.toVoiceData()
    }

    private fun Voice.toVoiceData() = VoiceData(
        id = name,
        language = locale.displayLanguage,
        needsInternet = isNetworkConnectionRequired,
        quality = quality
    )

    private fun getAvailableVoices(): List<VoiceData> {
        return service.voices.map { it.toVoiceData() }
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