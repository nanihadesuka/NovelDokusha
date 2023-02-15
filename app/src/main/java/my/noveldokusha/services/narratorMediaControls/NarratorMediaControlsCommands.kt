package my.noveldokusha.services.narratorMediaControls

import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionResult
import my.noveldokusha.ui.screens.reader.ReaderSession
import timber.log.Timber

fun createNarratorMediaControlsCommands(readerSession: ReaderSession) =
    object : MediaSession.Callback {
        override fun onPlayerCommandRequest(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            playerCommand: Int
        ): Int {
            Timber.d("playerCommand: $playerCommand")
            return when (playerCommand) {
                Player.COMMAND_PLAY_PAUSE -> {
                    val isPlaying = readerSession.readerSpeaker.settings.isPlaying.value
                    readerSession.readerSpeaker.settings.setPlaying(!isPlaying)
                    SessionResult.RESULT_SUCCESS
                }
                Player.COMMAND_SEEK_TO_NEXT -> {
                    readerSession.readerSpeaker.settings.playNextChapter()
                    SessionResult.RESULT_SUCCESS
                }
                Player.COMMAND_SEEK_TO_PREVIOUS -> {
                    readerSession.readerSpeaker.settings.playPreviousChapter()
                    SessionResult.RESULT_SUCCESS
                }
                else -> super.onPlayerCommandRequest(session, controller, playerCommand)
            }
        }
    }