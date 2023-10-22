package com.example.musictest.ui.audio

import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.example.musictest.data.local.model.Audio
import com.example.musictest.data.local.model.AudioCategory
import com.example.musictest.data.repository.AudioRepository
import com.example.musictest.media.service.AudioServiceHandler
import com.example.musictest.media.service.AudioPlayerState
import com.example.musictest.media.service.PlayerEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject


private val audioDummy = Audio("".toUri(), "", 0L, "", "", 0, "")

@OptIn(SavedStateHandleSaveableApi::class)
@HiltViewModel
class AudioViewModel @Inject constructor(
    private val audioServiceHandler: AudioServiceHandler,
    private val repository: AudioRepository,
    savedStateHandle: SavedStateHandle,
//    private val audioServiceHandler: JetAudioServiceHandler
) : ViewModel() {


    private var duration by savedStateHandle.saveable { mutableLongStateOf(0L) }
    var progress by savedStateHandle.saveable { mutableFloatStateOf(0f) }
    private var progressString by savedStateHandle.saveable { mutableStateOf("00:00") }
    var isPlaying by savedStateHandle.saveable { mutableStateOf(false) }
    var currentSelectedAudio by savedStateHandle.saveable { mutableStateOf(audioDummy) }
    var audioList by savedStateHandle.saveable { mutableStateOf(listOf<Audio>()) }
    val categoryList
        get() = audioList.groupBy { it.title.first() }.toSortedMap().map {
            AudioCategory(
                text = it.key.toString(),
                audios = it.value
            )
        }
    var repeatMode by savedStateHandle.saveable { mutableIntStateOf(Player.REPEAT_MODE_OFF) }

    private val _uiState = MutableStateFlow<UIState>(UIState.Initial)

    init {
        loadAudioData()
    }


    init {
        viewModelScope.launch {
            audioServiceHandler.audioPlayerState.collectLatest { audioPlayerState: AudioPlayerState ->
                when (audioPlayerState) {
                    AudioPlayerState.Initial -> _uiState.value = UIState.Initial
                    is AudioPlayerState.Buffering -> calculateProgressValue(audioPlayerState.progress)
                    is AudioPlayerState.Playing -> isPlaying = audioPlayerState.isPlaying
                    is AudioPlayerState.Progress -> calculateProgressValue(audioPlayerState.progress)
                    is AudioPlayerState.CurrentPlaying -> currentSelectedAudio =
                        audioList[audioPlayerState.mediaItemIndex]

                    is AudioPlayerState.CurrentRepeatMode -> repeatMode =
                        audioPlayerState.repeatMode

                    is AudioPlayerState.Ready -> {
                        duration = audioPlayerState.duration
                        _uiState.value = UIState.Ready
                    }
                }
            }
        }
    }

    private fun loadAudioData() {
        viewModelScope.launch {
            val audio = repository.getAudioData()
            audioList = audio
            setMediaItems()
        }
    }


    private suspend fun setMediaItems() = withContext<Unit>(Dispatchers.Default) {
        audioList.map { audio ->
            MediaItem.Builder()
                .setUri(audio.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setAlbumArtist(audio.artist)
                        .setDisplayTitle(audio.title)
                        .setSubtitle(audio.displayName)
                        .build()
                )
                .build()
        }.also {
            withContext(Dispatchers.Main) {
                audioServiceHandler.setMediaItemList(it)
            }
        }
    }

    private fun calculateProgressValue(currentProgress: Long) {
        progress =
            if (currentProgress > 0) currentProgress.toFloat() / duration.toFloat() * 100f
            else 0f
        progressString = formatDuration(currentProgress)
    }

    private fun formatDuration(duration: Long): String {
        val minute = TimeUnit.MINUTES.convert(duration, TimeUnit.MICROSECONDS)
        val seconds = minute - minute * TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES)
        return String.format("%02d:%02d", minute, seconds)
    }

    fun onUiEvent(uiEvents: UIEvents) {
        viewModelScope.launch {
            when (uiEvents) {
                UIEvents.Backward -> audioServiceHandler.onPlayerEvents(PlayerEvent.Backward)
                UIEvents.Forward -> audioServiceHandler.onPlayerEvents(PlayerEvent.Forward)
                UIEvents.SeekToNext -> audioServiceHandler.onPlayerEvents(PlayerEvent.SeekToNext)
                is UIEvents.PlayPause -> {
                    audioServiceHandler.onPlayerEvents(
                        PlayerEvent.PlayPause
                    )
                }

                is UIEvents.SeekTo -> {
                    audioServiceHandler.onPlayerEvents(
                        PlayerEvent.SeekTo,
                        seekPosition = (duration * uiEvents.position / 100f).toLong()
                    )
                }

                is UIEvents.SelectAudioChange -> {
                    audioServiceHandler.onPlayerEvents(
                        playerEvent = PlayerEvent.SelectedAudioChange,
                        selectedAudioIndex = uiEvents.index
                    )
                }

                is UIEvents.ChangeRepeat -> {
                    audioServiceHandler.onPlayerEvents(
                        playerEvent = PlayerEvent.ChangeRepeat
                    )
                }
            }
        }
    }

    override fun onCleared() {
        Log.d(TAG, "onCleared: called")
        super.onCleared()
        viewModelScope.launch(Dispatchers.Default) {
            audioServiceHandler.onPlayerEvents(PlayerEvent.Stop)
        }

    }
}

sealed class UIEvents {
    data object PlayPause : UIEvents()
    data class SelectAudioChange(val index: Int) : UIEvents()
    data class SeekTo(val position: Float) : UIEvents()
    data object SeekToNext : UIEvents()
    data object Backward : UIEvents()
    data object Forward : UIEvents()
    data object ChangeRepeat : UIEvents()
//    data class UpdateProgress(val newProgress: Float) : UIEvents()

}

sealed class UIState {
    data object Initial : UIState()
    data object Ready : UIState()

}
