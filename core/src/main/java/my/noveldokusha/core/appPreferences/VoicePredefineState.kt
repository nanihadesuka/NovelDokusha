package my.noveldokusha.core.appPreferences

import kotlinx.serialization.Serializable

@Serializable
data class VoicePredefineState(
    val savedName: String,
    val voiceId: String,
    val pitch: Float,
    val speed: Float
)