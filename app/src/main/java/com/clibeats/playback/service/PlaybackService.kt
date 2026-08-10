// ForbiddenImport: data/playback imports are legitimate
@file:Suppress("ForbiddenImport", "MagicNumber")

package com.clibeats.playback.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.clibeats.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground media session service. Started by [com.clibeats.playback.PlayerAdapter]
 * whenever playback begins so audio keeps running in the background and media
 * controls appear in the notification shade / lock screen.
 *
 * The media notification is built and posted manually here because the app
 * drives the player through its own [PlayerAdapter] (no MediaController
 * connects to the session), which would otherwise leave MediaSessionService
 * without an active session to auto-post a notification — and never calling
 * [startForeground] crashes the app within seconds.
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {
    @Inject
    lateinit var player: ExoPlayer

    private var mediaSession: MediaSession? = null

    private val notificationManager: NotificationManager by lazy {
        getSystemService(NotificationManager::class.java)
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        mediaSession = MediaSession.Builder(this, player).build()
        player.addListener(notificationUpdater)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        handleAction(intent)
        startForeground(NOTIFICATION_ID, buildMediaNotification())
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        player.removeListener(notificationUpdater)
        // The ExoPlayer is a DI singleton owned by the app (PlayerAdapter), not
        // by this service — releasing it here would kill playback permanently.
        mediaSession?.run {
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private fun handleAction(intent: Intent?) {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> if (player.isPlaying) player.pause() else player.play()
            ACTION_NEXT -> player.seekToNextMediaItem()
            ACTION_PREVIOUS -> player.seekToPreviousMediaItem()
        }
    }

    private fun createChannel() {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "Playback",
                NotificationManager.IMPORTANCE_LOW,
            )
        notificationManager.createNotificationChannel(channel)
    }

    private val notificationUpdater =
        object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                refreshNotification()
            }

            override fun onMediaItemTransition(
                mediaItem: MediaItem?,
                reason: Int,
            ) {
                refreshNotification()
            }
        }

    @android.annotation.SuppressLint("MissingPermission", "NotificationPermission")
    private fun refreshNotification() {
        notificationManager.notify(NOTIFICATION_ID, buildMediaNotification())
    }

    @OptIn(androidx.media3.common.util.UnstableApi::class)
    @android.annotation.SuppressLint("UnsafeOptInUsageError")
    private fun buildMediaNotification(): Notification {
        val session = mediaSession
        val sessionPlayer = session?.player ?: player
        val metadata: MediaMetadata =
            sessionPlayer.currentMediaItem?.mediaMetadata ?: MediaMetadata.EMPTY
        val title = metadata.title?.toString()?.ifBlank { null } ?: "CLIBeats"
        val artist = metadata.artist?.toString().orEmpty()
        val isPlaying = sessionPlayer.isPlaying

        val playPauseIcon =
            if (isPlaying) {
                android.R.drawable.ic_media_pause
            } else {
                android.R.drawable.ic_media_play
            }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_music_note)
            .setContentTitle(title)
            .setContentText(artist)
            .setOngoing(isPlaying)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setStyle(
                MediaStyle()
                    .setMediaSession(session?.sessionCompatToken)
                    .setShowActionsInCompactView(1),
            )
            .addAction(action(android.R.drawable.ic_media_previous, ACTION_PREVIOUS, "Previous"))
            .addAction(action(playPauseIcon, ACTION_PLAY_PAUSE, if (isPlaying) "Pause" else "Play"))
            .addAction(action(android.R.drawable.ic_media_next, ACTION_NEXT, "Next"))
            .build()
    }

    private fun action(
        iconRes: Int,
        action: String,
        label: String,
    ): NotificationCompat.Action {
        val intent = Intent(this, PlaybackService::class.java).setAction(action)
        val pendingIntent =
            PendingIntent.getService(
                this,
                action.hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        return NotificationCompat.Action.Builder(iconRes, label, pendingIntent).build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "playback"
        private const val ACTION_PLAY_PAUSE = "com.clibeats.action.PLAY_PAUSE"
        private const val ACTION_NEXT = "com.clibeats.action.NEXT"
        private const val ACTION_PREVIOUS = "com.clibeats.action.PREVIOUS"
    }
}
