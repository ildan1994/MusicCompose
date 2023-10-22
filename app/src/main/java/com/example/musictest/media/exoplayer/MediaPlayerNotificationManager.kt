package com.example.musictest.media.exoplayer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager
//import android.support.v4.media.session.MediaSessionCompat
import com.example.musictest.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject


private const val PLAYBACK_NOTIFICATION_CHANNEL_ID = "playback_notification_channel_id"
private const val PLAYBACK_NOTIFICATION_CHANNEL_NAME = "playback_notification_channel_name"

private const val PLAYBACK_NOTIFICATION_ID = 12

class MediaPlayerNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exoPlayer: ExoPlayer,
) {
    private val notificationManager: NotificationManagerCompat =
        NotificationManagerCompat.from(context)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(androidx.media3.common.util.UnstableApi::class)
    fun  startNotificationService(
        mediaSessionService: MediaSessionService,
        mediaSession: MediaSession,
    ) {
        buildNotification(mediaSession)
        startForeGroundNotificationService(mediaSessionService)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startForeGroundNotificationService(mediaSessionService: MediaSessionService) {
        val notification = Notification.Builder(context, PLAYBACK_NOTIFICATION_CHANNEL_ID)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        mediaSessionService.startForeground(PLAYBACK_NOTIFICATION_ID, notification)
    }
    @UnstableApi
    private fun buildNotification(mediaSession: MediaSession) {
        PlayerNotificationManager.Builder(
            context,
            PLAYBACK_NOTIFICATION_ID,
            PLAYBACK_NOTIFICATION_CHANNEL_ID
        )
            .setMediaDescriptionAdapter(
                AudioNotificationAdapter(
                    context,
                    pendingIntent = mediaSession.sessionActivity
                )
            )
            .setSmallIconResourceId(R.drawable.ic_launcher_foreground)
            .build()
            .also {
                it.setMediaSessionToken(mediaSession.sessionCompatToken)
                it.setUseFastForwardActionInCompactView(true)
                it.setUseRewindActionInCompactView(true)
                it.setUseNextActionInCompactView(true)
                it.setPriority(NotificationCompat.PRIORITY_LOW)
                it.setPlayer(exoPlayer)
            }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            PLAYBACK_NOTIFICATION_CHANNEL_ID,
            PLAYBACK_NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }
}