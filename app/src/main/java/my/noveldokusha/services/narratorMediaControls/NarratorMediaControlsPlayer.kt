package my.noveldokusha.services.narratorMediaControls

import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import androidx.media3.common.*
import androidx.media3.common.text.CueGroup
import timber.log.Timber

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun createNarratorMediaControlsPlayer(): Player = object : BasePlayer() {

    val readerAvailableCommands = Player.Commands.Builder()
        .add(Player.COMMAND_PLAY_PAUSE)
        .add(Player.COMMAND_SEEK_TO_NEXT) // Next chapter
        .add(Player.COMMAND_SEEK_TO_PREVIOUS) // Previous chapter
        .build()

    override fun getApplicationLooper(): Looper {
        return Looper.getMainLooper()
    }

    override fun addListener(listener: Player.Listener) {
        // noop
    }

    override fun removeListener(listener: Player.Listener) {
        // noop
    }

    override fun setMediaItems(mediaItems: MutableList<MediaItem>, resetPosition: Boolean) {
        // noop
    }

    override fun setMediaItems(
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ) {
        // noop
    }

    override fun addMediaItems(index: Int, mediaItems: MutableList<MediaItem>) {
        // noop
    }

    override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int) {
        // noop
    }

    override fun removeMediaItems(fromIndex: Int, toIndex: Int) {
        // noop
    }

    override fun getAvailableCommands(): Player.Commands {
        return readerAvailableCommands
    }

    override fun prepare() {
        // TODO
    }

    override fun getPlaybackState(): Int {
        return Player.STATE_READY
    }

    override fun getPlaybackSuppressionReason(): Int {
        return PLAYBACK_SUPPRESSION_REASON_NONE
    }

    override fun getPlayerError(): PlaybackException? {
        return null
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        // TODO
    }

    override fun getPlayWhenReady(): Boolean {
        return true
    }

    override fun setRepeatMode(repeatMode: Int) {
        // noop
    }

    override fun getRepeatMode(): Int {
        return REPEAT_MODE_OFF
    }

    override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {
        // noop
    }

    override fun getShuffleModeEnabled(): Boolean {
        return false
    }

    override fun isLoading(): Boolean {
        return false
    }

    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        // noop
    }

    override fun getSeekBackIncrement(): Long {
        return 1 // This should equals to one chapter paragraph
    }

    override fun getSeekForwardIncrement(): Long {
        return 1 // This should equals to one chapter paragraph
    }

    override fun getMaxSeekToPreviousPosition(): Long {
        return 1
    }

    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) {
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

    override fun setTrackSelectionParameters(parameters: TrackSelectionParameters) {
        // noop
    }

    override fun getMediaMetadata(): MediaMetadata {
        return MediaMetadata.EMPTY
    }

    override fun getPlaylistMetadata(): MediaMetadata {
        return MediaMetadata.EMPTY
    }

    override fun setPlaylistMetadata(mediaMetadata: MediaMetadata) {
        // noop
    }

    override fun getCurrentTimeline(): Timeline {
        return Timeline.EMPTY
    }

    override fun getCurrentPeriodIndex(): Int {
        // noop
        return 0
    }

    override fun getCurrentMediaItemIndex(): Int {
        // noop
        return 0
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

    override fun getTotalBufferedDuration(): Long {
        // noop
        return 1
    }

    override fun isPlayingAd(): Boolean {
        return false
    }

    override fun getCurrentAdGroupIndex(): Int {
        return -1
    }

    override fun getCurrentAdIndexInAdGroup(): Int {
        return -1
    }

    override fun getContentPosition(): Long {
        // noop
        return 1
    }

    override fun getContentBufferedPosition(): Long {
        // noop
        return 1
    }

    override fun getAudioAttributes(): AudioAttributes {
        return AudioAttributes.DEFAULT
    }

    override fun setVolume(volume: Float) {
        // noop
    }

    override fun getVolume(): Float {
        return 1f
    }

    override fun clearVideoSurface() {
        // noop
    }

    override fun clearVideoSurface(surface: Surface?) {
        // noop
    }

    override fun setVideoSurface(surface: Surface?) {
        // noop
    }

    override fun setVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {
        // noop
    }

    override fun clearVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {
        // noop
    }

    override fun setVideoSurfaceView(surfaceView: SurfaceView?) {
        // noop
    }

    override fun clearVideoSurfaceView(surfaceView: SurfaceView?) {
        // noop
    }

    override fun setVideoTextureView(textureView: TextureView?) {
        // noop
    }

    override fun clearVideoTextureView(textureView: TextureView?) {
        // noop
    }

    override fun getVideoSize(): VideoSize {
        return VideoSize.UNKNOWN
    }

    override fun getCurrentCues(): CueGroup {
        return CueGroup.EMPTY
    }

    override fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo.UNKNOWN
    }

    override fun getDeviceVolume(): Int {
        return 100
    }

    override fun isDeviceMuted(): Boolean {
        return false
    }

    override fun setDeviceVolume(volume: Int) {
        // noop
    }

    override fun increaseDeviceVolume() {
        // noop
    }

    override fun decreaseDeviceVolume() {
        // noop
    }

    override fun setDeviceMuted(muted: Boolean) {
        // noop
    }
}
