package com.example.musictest.media.service

import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.musictest.media.exoplayer.MediaPlayerNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MediaPlayerService : MediaSessionService() {
    companion object {
        const val TAG = "MediaPlayerService"
    }

    @Inject
    lateinit var mediaSession: MediaSession


    @Inject
    lateinit var notificationManager: MediaPlayerNotificationManager

    override fun onCreate() {
        Log.d(TAG, "onCreate: called")
        super.onCreate()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.startNotificationService(
                mediaSession = mediaSession,
                mediaSessionService = this
            )
        }
        return super.onStartCommand(intent, flags, startId)
    }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {

        return mediaSession

    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: Called")
        super.onDestroy()
        mediaSession.apply {
            release()
            if (player.playbackState != Player.STATE_IDLE) {
                player.seekTo(0)
                player.playWhenReady = false
                player.stop()
            }
            player.release()
        }
    }


}