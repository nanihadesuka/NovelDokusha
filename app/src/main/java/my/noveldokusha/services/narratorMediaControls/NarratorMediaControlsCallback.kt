package my.noveldokusha.services.narratorMediaControls

import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import android.view.KeyEvent
import ReaderTextToSpeech

class NarratorMediaControlsCallback(
    private val readerTextToSpeech: ReaderTextToSpeech
) : MediaSessionCompat.Callback() {

    override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
        val keyEvent =
            mediaButtonEvent?.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                ?: return super.onMediaButtonEvent(mediaButtonEvent)

        if (keyEvent.action == KeyEvent.ACTION_DOWN) {
            when (keyEvent.keyCode) {
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                    onSkipToPrevious()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_REWIND -> {
                    onRewind()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    onPause()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY -> {
                    onPlay()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    val isPlaying = readerTextToSpeech.isSpeaking.value
                    if (isPlaying) onPause() else onPlay()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                    onFastForward()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    onSkipToNext()
                    return true
                }
            }
        }
        return super.onMediaButtonEvent(mediaButtonEvent)
    }

    override fun onPlay() {
        readerTextToSpeech.state.setPlaying(true)
    }

    override fun onPause() {
        readerTextToSpeech.state.setPlaying(false)
    }

    override fun onSkipToNext() {
        readerTextToSpeech.state.playNextChapter()
    }

    override fun onSkipToPrevious() {
        readerTextToSpeech.state.playPreviousChapter()
    }

    override fun onRewind() {
        readerTextToSpeech.state.playPreviousItem()
    }

    override fun onFastForward() {
        readerTextToSpeech.state.playNextItem()
    }
}