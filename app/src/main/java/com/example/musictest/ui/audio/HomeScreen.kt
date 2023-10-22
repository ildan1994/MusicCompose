package com.example.musictest.ui.audio

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOn
import androidx.compose.material.icons.filled.RepeatOneOn
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import com.example.musictest.data.local.model.Audio
import com.example.musictest.media.service.MediaPlayerService
import kotlin.math.floor

const val TAG = "HomeScreen"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    audioViewModel: AudioViewModel = viewModel(),
) {
    val context = LocalContext.current

    val onItemClick = fun(index: Int) {
        if (!context.isServiceRunning<MediaPlayerService>()) {
            Log.d(TAG, " service is running ")
            val intent = Intent(context, MediaPlayerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        audioViewModel.onUiEvent(UIEvents.SelectAudioChange(index))
    }
    Scaffold(
        bottomBar = {
            BottomBarPlayer(
                progressProducer = audioViewModel::progress,
//                onProgressChange = { progress ->
//                    audioViewModel.onUiEvent(UIEvents.SeekTo(progress))
//                },
                onProgressChange = audioViewModel::onUiEvent,
                audio = audioViewModel.currentSelectedAudio,
                isAudioPlaying = audioViewModel.isPlaying,
                currentRepeatMode = audioViewModel.repeatMode,
                onStart = audioViewModel::onUiEvent,
//                onNext = { audioViewModel.onUiEvent(UIEvents.SeekToNext) },
                onNext = audioViewModel::onUiEvent,
                onRepeatClick = { audioViewModel.onUiEvent(UIEvents.ChangeRepeat) },
            )
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues
        ) {
//            itemsIndexed(audioViewModel.audioList) { index, audio ->
//                val isSelected = audio.id == audioViewModel.currentSelectedAudio.id
//                AudioItem(
//                    audio = audio,
//                    isSelected = isSelected
//                ) {
//                    onItemClick.invoke(index)
//                }
//            }
            audioViewModel.categoryList.forEach { category ->
                stickyHeader {
                    CategoryHeader(text = category.text)
                }
                items(category.audios) { audio ->
                    val isSelected = audio.id == audioViewModel.currentSelectedAudio.id
                    AudioItem(audio = audio, isSelected = isSelected) {
                        onItemClick.invoke(audio.index)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.inversePrimary)
            .padding(12.dp)
    )
}

@Composable
fun AudioItem(
    audio: Audio,
    isSelected: Boolean,
    onItemClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clickable {
                onItemClick.invoke()
            },
        colors = if (isSelected) CardDefaults.cardColors(contentColor = Color.Blue) else CardDefaults.cardColors()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = audio.title,
                    style = MaterialTheme.typography.titleLarge,
                    overflow = TextOverflow.Clip,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = audio.artist,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
            Text(text = timeStampToDuration(audio.duration.toLong()))
            Spacer(modifier = Modifier.size(8.dp))
        }
    }
}

@Composable
fun BottomBarPlayer(
    modifier: Modifier = Modifier,
    progressProducer: () -> Float,
    onProgressChange: (UIEvents.SeekTo) -> Unit,
    audio: Audio,
    isAudioPlaying: Boolean,
    onStart: (UIEvents.PlayPause) -> Unit,
    onNext: (UIEvents.SeekToNext) -> Unit,
    onRepeatClick: (UIEvents.ChangeRepeat) -> Unit,
    currentRepeatMode: Int,
) {
    BottomAppBar(
        content = {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ArtistInfo(
                        audio = audio,
                        modifier = Modifier.weight(1f)
                    )
                    MediaPlayerController(
                        modifier = modifier,
                        isAudioPlaying = isAudioPlaying,
                        onStart = onStart,
                        onNext = onNext,
                        onRepeatClick = onRepeatClick,
                        currentRepeatMode = currentRepeatMode
                    )
                }
                Slider(
                    value = progressProducer.invoke(),
                    onValueChange = { progress ->
                        onProgressChange(UIEvents.SeekTo(progress))
                    },
                    valueRange = 0f..100f
                )
            }
        }
    )
}


@Preview(showBackground = true)
@Composable
fun BottomBarPlayerPrev() {
    BottomBarPlayer(
        progressProducer = { 50f },
        onProgressChange = {},
        audio = Audio(
            "".toUri(),
            "The Dan",
            123,
            "Van",
            "",
            180,
            "hahaha"
        ),
        isAudioPlaying = true,
        onStart = {},
        onNext = {},
        currentRepeatMode = Player.REPEAT_MODE_ALL,
        onRepeatClick = {},
    )
}

private fun timeStampToDuration(position: Long): String {
    val totalSeconds = floor(position / 1E3).toInt()
    val minutes = totalSeconds / 60
    val remainingSeconds = totalSeconds - (minutes * 60)
    return if (position < 0) "--:--"
    else "%d:%02d".format(minutes, remainingSeconds)
}

@Composable
fun MediaPlayerController(
    modifier: Modifier = Modifier,
    isAudioPlaying: Boolean,
    onStart: (UIEvents.PlayPause) -> Unit,
    onNext: (UIEvents.SeekToNext) -> Unit,
    currentRepeatMode: Int,
    onRepeatClick: (UIEvents.ChangeRepeat) -> Unit,
) {

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(56.dp)
            .padding(4.dp)
    ) {
        PlayerIconItem(
            icon = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
        ) {
            onStart.invoke(UIEvents.PlayPause)
        }
        Spacer(modifier = Modifier.width(4.dp))
//        Icon(
//            imageVector = Icons.Default.SkipNext,
//            contentDescription = null,
//            modifier = modifier.clickable {
//                onNext(UIEvents.SeekToNext)
//            }
//        )
        PlayerIconItem(icon = Icons.Default.SkipNext) {
            onNext(UIEvents.SeekToNext)
        }
        Spacer(modifier = Modifier.width(4.dp))
        PlayerIconItem(
            icon = when (currentRepeatMode) {
                Player.REPEAT_MODE_OFF -> Icons.Default.Repeat
                Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOneOn
                else -> Icons.Default.RepeatOn
            },
            onClick = { onRepeatClick(UIEvents.ChangeRepeat) }
        )


    }
}

@Composable
fun ArtistInfo(
    modifier: Modifier = Modifier,
    audio: Audio,
) {
    Row(
        modifier = modifier.padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayerIconItem(
            icon = Icons.Default.MusicNote,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface
            ),
        ) {}
        Spacer(modifier = Modifier.size(4.dp))

        Column(modifier = Modifier.weight(1f)) {

            Text(
                text = audio.title,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge,
                overflow = TextOverflow.Clip,
                maxLines = 1,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = audio.artist,
                fontWeight = FontWeight.Normal,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Clip,
                maxLines = 1
            )
        }
    }
}

@Composable
fun PlayerIconItem(
    icon: ImageVector,
    border: BorderStroke? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        border = border,
        modifier = Modifier
            .clip(CircleShape)
            .clickable {
                onClick.invoke()
            },
        contentColor = color,
        color = backgroundColor
    ) {
        Box(
            modifier = Modifier.padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
        }
    }
}


inline fun <reified T> Context.isServiceRunning() =
    (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
        .getRunningServices(Integer.MAX_VALUE)
        .any { it.service.className == T::class.java.name }


