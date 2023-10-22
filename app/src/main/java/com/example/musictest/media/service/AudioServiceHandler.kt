package com.example.musictest.media.service

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.RepeatMode
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


class AudioServiceHandler @Inject constructor(
    private val exoPlayer: ExoPlayer,
) : Player.Listener {

    companion object {
        const val TAG = "AudioServiceHandler"
    }
    private val _audioPlayerState = MutableStateFlow<AudioPlayerState>(AudioPlayerState.Initial)
    val audioPlayerState: StateFlow<AudioPlayerState>
        get() = _audioPlayerState.asStateFlow()
    private val job: Job = Job()
    private val scope = CoroutineScope(job + Dispatchers.Main)

    init {
        exoPlayer.addListener(this)
    }

    fun setMediaItemList(mediaItems: List<MediaItem>) {
        exoPlayer.setMediaItems(mediaItems)
        exoPlayer.prepare()
    }

    suspend fun onPlayerEvents(
        playerEvent: PlayerEvent,
        selectedAudioIndex: Int = -1,
        seekPosition: Long = 0,
    ) {
        when (playerEvent) {
            PlayerEvent.Backward -> exoPlayer.seekBack()
            PlayerEvent.Forward -> exoPlayer.seekForward()
            PlayerEvent.SeekToNext -> exoPlayer.seekToNext()
            PlayerEvent.PlayPause -> exoPlayer.playOrPause()
            PlayerEvent.SeekTo -> exoPlayer.seekTo(seekPosition)
            PlayerEvent.SelectedAudioChange -> {
                when (selectedAudioIndex) {
                    exoPlayer.currentMediaItemIndex -> {
                        playOrPause()
                    }

                    else -> {
                        exoPlayer.seekToDefaultPosition(selectedAudioIndex)
                        _audioPlayerState.value = AudioPlayerState.Playing(
                            isPlaying = true
                        )
                        exoPlayer.playWhenReady = true
                        startProgressUpdate()
                    }
                }
            }

            PlayerEvent.Stop -> stopProgressUpdate()
            is PlayerEvent.UpdateProgress -> {
                exoPlayer.seekTo(
                    (exoPlayer.duration * playerEvent.newProgress).toLong()
                )
            }

            PlayerEvent.ChangeRepeat -> {
                exoPlayer.repeatMode = when (exoPlayer.repeatMode) {
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_OFF
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_ALL
                }
            }
            PlayerEvent.Close -> {

            }
        }
    }


    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        super.onMediaItemTransition(mediaItem, reason)
        _audioPlayerState.value = AudioPlayerState.CurrentPlaying(exoPlayer.currentMediaItemIndex)
    }
    override fun onRepeatModeChanged(repeatMode: Int) {
        super.onRepeatModeChanged(repeatMode)
        Log.d(TAG, "current repeatMode : $repeatMode")
        _audioPlayerState.value = AudioPlayerState.CurrentRepeatMode(repeatMode)
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)
        when (playbackState) {
            ExoPlayer.STATE_BUFFERING -> _audioPlayerState.value =
                AudioPlayerState.Buffering(exoPlayer.currentPosition)

            ExoPlayer.STATE_READY -> _audioPlayerState.value =
                AudioPlayerState.Ready(exoPlayer.duration)

            else -> {}
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        _audioPlayerState.value = AudioPlayerState.Playing(isPlaying = isPlaying)
        _audioPlayerState.value = AudioPlayerState.CurrentPlaying(exoPlayer.currentMediaItemIndex)
        if (isPlaying) {
            scope.launch {
                startProgressUpdate()
            }
        } else {
            stopProgressUpdate()
        }
    }



    private suspend fun ExoPlayer.playOrPause() {
        if (isPlaying) {
            pause()
            stopProgressUpdate()
        } else {
            play()
            _audioPlayerState.value = AudioPlayerState.Playing(
                isPlaying = true
            )
            startProgressUpdate()
        }
    }

    private suspend fun playOrPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            stopProgressUpdate()
        } else {
            exoPlayer.play()
            _audioPlayerState.value = AudioPlayerState.Playing(
                isPlaying = true
            )
            startProgressUpdate()
        }
    }

    private suspend fun startProgressUpdate() {
        while (true) {
            delay(500)
            _audioPlayerState.value = AudioPlayerState.Progress(exoPlayer.currentPosition)
        }
    }


    private fun stopProgressUpdate() {
        job.cancel()
        _audioPlayerState.value = AudioPlayerState.Playing(isPlaying = false)
    }
}

sealed class PlayerEvent {
    data object PlayPause : PlayerEvent()
    data object SelectedAudioChange : PlayerEvent()
    data object Backward : PlayerEvent()

    data object ChangeRepeat : PlayerEvent()
    data object SeekToNext : PlayerEvent()
    data object Forward : PlayerEvent()
    data object SeekTo : PlayerEvent()
    data object Close: PlayerEvent()

    data object Stop : PlayerEvent()
    data class UpdateProgress(val newProgress: Float) : PlayerEvent()

}

sealed class AudioPlayerState {
    data object Initial : AudioPlayerState()
    data class Ready(val duration: Long) : AudioPlayerState()
    data class Progress(val progress: Long) : AudioPlayerState()
    data class Buffering(val progress: Long) : AudioPlayerState()
    data class Playing(val isPlaying: Boolean) : AudioPlayerState()
    data class CurrentPlaying(val mediaItemIndex: Int) : AudioPlayerState()
    data class CurrentRepeatMode(val repeatMode: Int): AudioPlayerState()
}
