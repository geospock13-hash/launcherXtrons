package com.artefgo.metro.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class NowPlaying(
    val title: String,
    val artist: String,
    val appName: String,
    val packageName: String,
    val art: ImageBitmap?,
    val isPlaying: Boolean,
    val durationMs: Long,
    val positionMs: Long,
    val positionUpdateTime: Long,
    val speed: Float
) {
    fun currentPosition(): Long {
        if (!isPlaying) return positionMs
        val p = positionMs + ((SystemClock.elapsedRealtime() - positionUpdateTime) * speed).toLong()
        return if (durationMs > 0) p.coerceIn(0, durationMs) else p
    }
}

/**
 * Lee la canción de cualquier app con sesión multimedia: Spotify, YouTube Music, Poweramp,
 * Tidal, Deezer, Apple Music, la app de música Bluetooth de la radio, etc.
 * Requiere que el usuario active el "acceso a notificaciones" para este launcher.
 */
object MediaHub {
    private val _now = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _now
    private val _hasAccess = MutableStateFlow(false)
    val hasAccess: StateFlow<Boolean> = _hasAccess

    private var appContext: Context? = null
    private var msm: MediaSessionManager? = null
    private var registered = false
    private val controllers = mutableListOf<MediaController>()
    private var current: MediaController? = null
    private val main = Handler(Looper.getMainLooper())

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        setControllers(list ?: emptyList())
    }

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = pick()
        override fun onMetadataChanged(metadata: MediaMetadata?) = pick()
        override fun onSessionDestroyed() {
            val m = msm
            val c = appContext
            if (m != null && c != null) setControllers(sessions(m, c)) else pick()
        }
    }

    fun hasNotificationAccess(ctx: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)

    fun start(ctx: Context) {
        val c = ctx.applicationContext
        appContext = c
        _hasAccess.value = hasNotificationAccess(c)
        if (!_hasAccess.value) return
        val m = msm ?: c.getSystemService(MediaSessionManager::class.java)?.also { msm = it } ?: return
        if (!registered) {
            try {
                m.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent(c), main)
                registered = true
            } catch (_: SecurityException) {
                return
            }
        }
        setControllers(sessions(m, c))
    }

    private fun listenerComponent(c: Context) = ComponentName(c, MediaListenerService::class.java)

    private fun sessions(m: MediaSessionManager, c: Context): List<MediaController> = try {
        m.getActiveSessions(listenerComponent(c))
    } catch (_: SecurityException) {
        emptyList()
    }

    private fun setControllers(list: List<MediaController>) {
        controllers.forEach { runCatching { it.unregisterCallback(callback) } }
        controllers.clear()
        controllers.addAll(list)
        controllers.forEach { runCatching { it.registerCallback(callback, main) } }
        pick()
    }

    private fun pick() {
        val playing = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        val keep = current?.let { cur -> controllers.firstOrNull { it.sessionToken == cur.sessionToken } }
        current = playing ?: keep ?: controllers.firstOrNull()
        publish()
    }

    private fun publish() {
        val c = current
        val ctx = appContext
        if (c == null || ctx == null) {
            _now.value = null
            return
        }
        val md = c.metadata
        val st = c.playbackState
        val title = md?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: md?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) ?: ""
        val artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: md?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE) ?: ""
        val bmp = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val appName = runCatching {
            val pm = ctx.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(c.packageName, 0)).toString()
        }.getOrDefault(c.packageName)
        _now.value = NowPlaying(
            title = title,
            artist = artist,
            appName = appName,
            packageName = c.packageName,
            art = runCatching { bmp?.asImageBitmap() }.getOrNull(),
            isPlaying = st?.state == PlaybackState.STATE_PLAYING,
            durationMs = md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
            positionMs = st?.position ?: 0L,
            positionUpdateTime = st?.lastPositionUpdateTime ?: SystemClock.elapsedRealtime(),
            speed = st?.playbackSpeed ?: 1f
        )
    }

    fun playPause() {
        val c = current ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause()
        else c.transportControls.play()
    }

    fun next() {
        current?.transportControls?.skipToNext()
    }

    fun previous() {
        current?.transportControls?.skipToPrevious()
    }

    fun openCurrentApp(ctx: Context) {
        val pkg = current?.packageName ?: return
        val i = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return
        runCatching { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}

class MediaListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        MediaHub.start(applicationContext)
    }
}
