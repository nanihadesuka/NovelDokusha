package my.noveldokusha.services.narratorMediaControls

import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import androidx.media3.common.*
import androidx.media3.common.Player.PLAYBACK_SUPPRESSION_REASON_NONE
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.text.CueGroup
import my.noveldokusha.ui.screens.reader.manager.ReaderSession
import timber.log.Timber

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun createNarratorMediaControlsPlayer(readerSession: ReaderSession): Player = object : Player {

    val speakerSession = readerSession.readerTextToSpeech

    val readerAvailableCommands = Player.Commands.Builder()
        .add(Player.COMMAND_PLAY_PAUSE)
        .add(Player.COMMAND_SEEK_TO_NEXT) // Next chapter
        .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM) // Next chapter item
        .add(Player.COMMAND_SEEK_TO_PREVIOUS) // Previous chapter
        .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM) // Previous chapter item
        .add(Player.COMMAND_STOP) // Close notification
        .build()

    override fun getApplicationLooper(): Looper = Looper.getMainLooper()

    override fun addListener(listener: Player.Listener) {}
    override fun removeListener(listener: Player.Listener) {}
    override fun setMediaItems(mediaItems: MutableList<MediaItem>) {}
    override fun setMediaItems(mediaItems: MutableList<MediaItem>, resetPosition: Boolean) {}
    override fun setMediaItems(items: MutableList<MediaItem>, index: Int, posMs: Long) {}
    override fun setMediaItem(mediaItem: MediaItem) {}
    override fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long) {}
    override fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean) {}
    override fun addMediaItem(mediaItem: MediaItem) {}
    override fun addMediaItem(index: Int, mediaItem: MediaItem) {}
    override fun addMediaItems(mediaItems: MutableList<MediaItem>) {}
    override fun addMediaItems(index: Int, mediaItems: MutableList<MediaItem>) {}
    override fun moveMediaItem(currentIndex: Int, newIndex: Int) {}
    override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int) {}
    override fun removeMediaItem(index: Int) {}
    override fun removeMediaItems(fromIndex: Int, toIndex: Int) {}
    override fun clearMediaItems() {}

    override fun isCommandAvailable(command: Int): Boolean = true
    override fun canAdvertiseSession(): Boolean = true
    override fun getAvailableCommands(): Player.Commands = readerAvailableCommands

    override fun prepare() {}

    override fun getPlaybackState(): Int = when {
        speakerSession.isAtLastItem.value -> Player.STATE_ENDED
        speakerSession.settings.isLoadingChapter.value -> Player.STATE_BUFFERING
        else -> Player.STATE_READY
    }

    override fun getPlaybackSuppressionReason(): Int = PLAYBACK_SUPPRESSION_REASON_NONE
    override fun isPlaying(): Boolean = speakerSession.isSpeaking.value
    override fun getPlayerError(): PlaybackException? = null

    override fun play() = speakerSession.settings.setPlaying(true)
    override fun pause() = speakerSession.settings.setPlaying(false)
    override fun setPlayWhenReady(playWhenReady: Boolean) =
        speakerSession.settings.setPlaying(playWhenReady)

    override fun getPlayWhenReady(): Boolean = true
    override fun setRepeatMode(repeatMode: Int) {}
    override fun getRepeatMode(): Int = REPEAT_MODE_OFF

    override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {}
    override fun getShuffleModeEnabled(): Boolean = false

    override fun isLoading(): Boolean = speakerSession.settings.isLoadingChapter.value

    override fun seekToDefaultPosition() {
        // noop
    }

    override fun seekToDefaultPosition(mediaItemIndex: Int) {
        // noop
    }

    override fun seekTo(positionMs: Long) {
        // noop
    }

    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        // noop
    }

    override fun getSeekBackIncrement(): Long {
        return 1 // This should equals to one chapter paragraph
    }

    override fun seekBack() {
        // noop ???
    }

    override fun getSeekForwardIncrement(): Long {
        return 1 // This should equals to one chapter paragraph
    }

    override fun seekForward() {
        // noop ???
    }

    override fun hasPrevious(): Boolean = !speakerSession.isAtFirstItem.value
    override fun hasPreviousWindow(): Boolean = !speakerSession.isAtFirstItem.value
    override fun hasPreviousMediaItem(): Boolean = !speakerSession.isAtFirstItem.value

    override fun previous() = speakerSession.settings.playPreviousItem()
    override fun seekToPreviousWindow() = speakerSession.settings.playPreviousItem()
    override fun seekToPreviousMediaItem() = speakerSession.settings.playPreviousItem()

    override fun getMaxSeekToPreviousPosition(): Long {
        return 10
    }

    override fun seekToPrevious() = speakerSession.settings.playPreviousChapter()

    override fun hasNext(): Boolean = !speakerSession.isAtLastItem.value
    override fun hasNextWindow(): Boolean = !speakerSession.isAtLastItem.value
    override fun hasNextMediaItem(): Boolean = !speakerSession.isAtLastItem.value

    override fun next() = speakerSession.settings.playNextItem()
    override fun seekToNextWindow() = speakerSession.settings.playNextItem()
    override fun seekToNextMediaItem() = speakerSession.settings.playNextItem()

    override fun seekToNext() = speakerSession.settings.playNextChapter()

    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) {
        // noop
    }

    override fun setPlaybackSpeed(speed: Float) {
        // noop
    }

    override fun getPlaybackParameters(): PlaybackParameters {
        return PlaybackParameters.DEFAULT
    }

    override fun stop() {
        // TODO
        Timber.d("STOP")
    }

    override fun stop(reset: Boolean) {
        // TODO
        Timber.d("STOP $reset")
    }

    override fun release() {
        // TODO
        Timber.d("RELEASE")
    }

    override fun getCurrentTracks(): Tracks {
        return Tracks.EMPTY
    }

    override fun getTrackSelectionParameters(): TrackSelectionParameters {
        return TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT
    }

    override fun setTrackSelectionParameters(parameters: TrackSelectionParameters) {}

    override fun getMediaMetadata(): MediaMetadata {
        val chapterUrl = speakerSession.currentTextPlaying.value.itemPos.chapterUrl
        val stats = readerSession.readerChaptersLoader.chaptersStats[chapterUrl]
        return MediaMetadata.Builder()
            .setAlbumArtist(stats?.chapter?.title ?: "Current chapter")
            .setAlbumTitle(readerSession.bookTitle ?: "Book reader")
            .build()
    }

    override fun getPlaylistMetadata(): MediaMetadata {
        val chapterUrl = speakerSession.currentTextPlaying.value.itemPos.chapterUrl
        val stats = readerSession.readerChaptersLoader.chaptersStats[chapterUrl]
        return MediaMetadata.Builder()
            .setAlbumArtist(stats?.chapter?.title ?: "Current chapter")
            .setAlbumTitle(readerSession.bookTitle ?: "Book reader")
            .build()
    }

    override fun setPlaylistMetadata(mediaMetadata: MediaMetadata) {
        // noop
    }

    override fun getCurrentManifest(): Any? {

        return null
    }

    override fun getCurrentTimeline(): Timeline {
        return Timeline.EMPTY
    }

    override fun getCurrentPeriodIndex(): Int {
        // noop
        return 0
    }

    override fun getCurrentWindowIndex(): Int {
        return 1
    }

    override fun getCurrentMediaItemIndex(): Int {
        // noop
        return 1
    }

    override fun getNextWindowIndex(): Int {
        return 2
    }

    override fun getNextMediaItemIndex(): Int {
        return 2
    }

    override fun getPreviousWindowIndex(): Int {
        return 0
    }

    override fun getPreviousMediaItemIndex(): Int {
        return 0
    }

    override fun getCurrentMediaItem(): MediaItem? {
        return MediaItem.EMPTY
    }

    override fun getMediaItemCount(): Int {
        return 3
    }

    override fun getMediaItemAt(index: Int): MediaItem {
        return MediaItem.EMPTY
    }

    override fun getDuration(): Long {
        // noop
        return 1
    }

    override fun getCurrentPosition(): Long {
        // noop
        return 1
    }

    override fun getBufferedPosition(): Long {
        // noop
        return 1
    }

    override fun getBufferedPercentage(): Int {
        return 100
    }

    override fun getTotalBufferedDuration(): Long {
        // noop
        return 1
    }

    override fun isCurrentWindowDynamic(): Boolean = false
    override fun isCurrentMediaItemDynamic(): Boolean = false

    override fun isCurrentWindowLive(): Boolean = true
    override fun isCurrentMediaItemLive(): Boolean = true

    override fun getCurrentLiveOffset(): Long {
        return 0
    }

    override fun isCurrentWindowSeekable(): Boolean = true
    override fun isCurrentMediaItemSeekable(): Boolean = true

    override fun isPlayingAd(): Boolean = false

    override fun getCurrentAdGroupIndex(): Int {
        return -1
    }

    override fun getCurrentAdIndexInAdGroup(): Int {
        return -1
    }

    override fun getContentDuration(): Long {
        return 10
    }

    override fun getContentPosition(): Long {
        return 1
    }

    override fun getContentBufferedPosition(): Long {
        return 1
    }

    override fun getAudioAttributes(): AudioAttributes {
        return AudioAttributes.DEFAULT
    }

    override fun setVolume(volume: Float) {}
    override fun getVolume(): Float = 1f
    override fun clearVideoSurface() {}
    override fun clearVideoSurface(surface: Surface?) {}
    override fun setVideoSurface(surface: Surface?) {}
    override fun setVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {}
    override fun clearVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {}
    override fun setVideoSurfaceView(surfaceView: SurfaceView?) {}
    override fun clearVideoSurfaceView(surfaceView: SurfaceView?) {}
    override fun setVideoTextureView(textureView: TextureView?) {}
    override fun clearVideoTextureView(textureView: TextureView?) {}
    override fun getVideoSize(): VideoSize = VideoSize.UNKNOWN
    override fun getCurrentCues(): CueGroup = CueGroup.EMPTY
    override fun getDeviceInfo(): DeviceInfo = DeviceInfo.UNKNOWN
    override fun getDeviceVolume(): Int = 100
    override fun isDeviceMuted(): Boolean = false
    override fun setDeviceVolume(volume: Int) {}
    override fun increaseDeviceVolume() {}
    override fun decreaseDeviceVolume() {}
    override fun setDeviceMuted(muted: Boolean) {}
}
