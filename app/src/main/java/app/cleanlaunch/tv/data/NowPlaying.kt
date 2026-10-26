package app.cleanlaunch.tv.data

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.LruCache
import java.net.HttpURLConnection
import java.net.URL

/** What the home page shows for the media session in front. */
data class NowPlayingInfo(
    val packageName: String,
    val title: String,
    val artist: String,
    val art: Bitmap?,
    val playing: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    /** SystemClock.elapsedRealtime() when [positionMs] was true. */
    val positionAt: Long,
    val speed: Float,
)

/** Which session to show. Kept free of Android types so it can be unit tested. */
object NowPlayingPick {
    data class Candidate(val live: Boolean, val playing: Boolean, val lastPlayAt: Long)

    /** Index of the session to show, or null when none is live. Ties keep the system's own order. */
    fun choose(items: List<Candidate>): Int? {
        var best: Int? = null
        items.forEachIndexed { index, item ->
            if (!item.live) return@forEachIndexed
            val current = best?.let { items[it] }
            if (current == null || better(item, current)) best = index
        }
        return best
    }

    private fun better(a: Candidate, b: Candidate): Boolean {
        if (a.playing != b.playing) return a.playing
        return a.lastPlayAt > b.lastPlayAt
    }

    /** Position now, from the last report. */
    fun position(positionMs: Long, positionAt: Long, now: Long, speed: Float, playing: Boolean, durationMs: Long): Long {
        var p = positionMs
        if (playing) p += ((now - positionAt) * speed).toLong()
        if (durationMs > 0) p = p.coerceAtMost(durationMs)
        return p.coerceAtLeast(0L)
    }

    fun live(state: Int): Boolean = when (state) {
        PlaybackState.STATE_PLAYING,
        PlaybackState.STATE_PAUSED,
        PlaybackState.STATE_BUFFERING,
        PlaybackState.STATE_CONNECTING,
        PlaybackState.STATE_FAST_FORWARDING,
        PlaybackState.STATE_REWINDING,
        PlaybackState.STATE_SKIPPING_TO_NEXT,
        PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
        PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM,
        -> true
        else -> false
    }
}

/**
 * Follows the active media sessions of every app, through the notification-access grant.
 * Nothing here throws when access is missing: [start] returns false and the page shows nothing.
 */
class NowPlayingSource(
    private val context: Context,
    private val onChange: (NowPlayingInfo?) -> Unit,
) {
    private class Tracked(
        val controller: MediaController,
        var metadata: MediaMetadata?,
        var state: PlaybackState?,
        var lastPlayAt: Long,
        var art: Bitmap?,
        var artKey: String,
    ) {
        var callback: MediaController.Callback? = null
    }

    private val handler = Handler(Looper.getMainLooper())
    private val manager: MediaSessionManager? = context.getSystemService(MediaSessionManager::class.java)
    private val component = ComponentName(context, NowPlayingListener::class.java)
    private val tracked = LinkedHashMap<MediaSession.Token, Tracked>()
    private val uriArt = LruCache<String, Bitmap>(6)
    private val uriLoading = HashSet<String>()
    private var running = false
    private var order: List<MediaSession.Token> = emptyList()
    private var shown: Tracked? = null
    private var last: NowPlayingInfo? = null

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        sync(list.orEmpty())
    }

    fun start(): Boolean {
        if (running) return true
        val mgr = manager ?: return false
        if (!hasAccess(context)) return false
        val list = try {
            mgr.getActiveSessions(component)
        } catch (_: SecurityException) {
            return false
        } catch (_: RuntimeException) {
            return false
        }
        try {
            mgr.addOnActiveSessionsChangedListener(sessionsListener, component, handler)
        } catch (_: RuntimeException) {
            return false
        }
        running = true
        sync(list)
        return true
    }

    fun stop() {
        if (running) {
            runCatching { manager?.removeOnActiveSessionsChangedListener(sessionsListener) }
        }
        running = false
        tracked.values.forEach { t -> t.callback?.let { runCatching { t.controller.unregisterCallback(it) } } }
        tracked.clear()
        order = emptyList()
        shown = null
        publish()
    }

    private fun sync(list: List<MediaController>) {
        val tokens = list.map { it.sessionToken }
        val gone = tracked.keys.filter { it !in tokens }
        gone.forEach { token ->
            tracked.remove(token)?.let { t -> t.callback?.let { runCatching { t.controller.unregisterCallback(it) } } }
        }
        for (controller in list) {
            val token = controller.sessionToken
            if (tracked.containsKey(token)) continue
            val state = runCatching { controller.playbackState }.getOrNull()
            val t = Tracked(
                controller,
                runCatching { controller.metadata }.getOrNull(),
                state,
                if (state?.state == PlaybackState.STATE_PLAYING) SystemClock.elapsedRealtime() else 0L,
                null,
                "",
            )
            val callback = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) {
                    val wasPlaying = t.state?.state == PlaybackState.STATE_PLAYING
                    t.state = state
                    if (!wasPlaying && state?.state == PlaybackState.STATE_PLAYING) {
                        t.lastPlayAt = SystemClock.elapsedRealtime()
                    }
                    publish()
                }

                override fun onMetadataChanged(metadata: MediaMetadata?) {
                    t.metadata = metadata
                    publish()
                }

                override fun onSessionDestroyed() {
                    runCatching {
                        tracked.remove(controller.sessionToken)
                        publish()
                    }
                }
            }
            t.callback = callback
            runCatching { controller.registerCallback(callback, handler) }
            tracked[token] = t
        }
        order = tokens
        publish()
    }

    private fun publish() {
        val items = order.mapNotNull { tracked[it] }
        val candidates = items.map {
            val state = it.state?.state ?: PlaybackState.STATE_NONE
            val hasText = !(it.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).isNullOrBlank() &&
                it.metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE).isNullOrBlank())
            NowPlayingPick.Candidate(
                live = NowPlayingPick.live(state) && hasText,
                playing = state == PlaybackState.STATE_PLAYING,
                lastPlayAt = it.lastPlayAt,
            )
        }
        val index = NowPlayingPick.choose(candidates)
        val pick = index?.let { items[it] }
        shown = pick
        val info = pick?.let { describe(it) }
        if (info != last) {
            last = info
            onChange(info)
        }
    }

    private fun describe(t: Tracked): NowPlayingInfo {
        val md = t.metadata
        val state = t.state
        val title = md?.getString(MediaMetadata.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }
            ?: md?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE).orEmpty()
        val artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }
            ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)?.takeIf { it.isNotBlank() }
            ?: md?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE).orEmpty()
        return NowPlayingInfo(
            packageName = t.controller.packageName.orEmpty(),
            title = title,
            artist = artist,
            art = artFor(t, md),
            playing = state?.state == PlaybackState.STATE_PLAYING,
            positionMs = state?.position?.takeIf { it >= 0 } ?: 0L,
            durationMs = md?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.takeIf { it > 0 } ?: 0L,
            positionAt = state?.lastPositionUpdateTime?.takeIf { it > 0 } ?: SystemClock.elapsedRealtime(),
            speed = state?.playbackSpeed?.takeIf { it > 0f } ?: 1f,
        )
    }

    private fun artFor(t: Tracked, md: MediaMetadata?): Bitmap? {
        if (md == null) return null
        val bitmap = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        if (bitmap != null) {
            // The system hands back a fresh copy each time. Keep the first one per picture size.
            val key = "bmp:${bitmap.width}x${bitmap.height}:${md.getString(MediaMetadata.METADATA_KEY_TITLE)}"
            if (t.artKey != key || t.art == null) {
                t.artKey = key
                t.art = small(bitmap)
            }
            return t.art
        }
        val uri = listOf(
            MediaMetadata.METADATA_KEY_ALBUM_ART_URI,
            MediaMetadata.METADATA_KEY_ART_URI,
            MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI,
        ).firstNotNullOfOrNull { md.getString(it)?.takeIf { s -> s.isNotBlank() } } ?: return null
        uriArt.get(uri)?.let { return it }
        loadUri(uri)
        return null
    }

    private fun small(src: Bitmap): Bitmap {
        val side = maxOf(src.width, src.height)
        if (side <= 256) return src
        val scale = 256f / side
        return Bitmap.createScaledBitmap(src, (src.width * scale).toInt().coerceAtLeast(1), (src.height * scale).toInt().coerceAtLeast(1), true)
    }

    private fun loadUri(uri: String) {
        if (!uriLoading.add(uri)) return
        Thread {
            var bm: Bitmap? = null
            runCatching {
                val parsed = Uri.parse(uri)
                val stream = when (parsed.scheme) {
                    "http", "https" -> {
                        val c = URL(uri).openConnection() as HttpURLConnection
                        c.connectTimeout = 6000
                        c.readTimeout = 6000
                        c.inputStream
                    }
                    "content", "file" -> context.contentResolver.openInputStream(parsed)
                    else -> null
                }
                stream?.use { bm = android.graphics.BitmapFactory.decodeStream(it) }
            }
            handler.post {
                uriLoading.remove(uri)
                bm?.let {
                    uriArt.put(uri, small(it))
                    if (running) publish()
                }
            }
        }.start()
    }

    fun previous() {
        shown?.controller?.transportControls?.skipToPrevious()
    }

    fun next() {
        shown?.controller?.transportControls?.skipToNext()
    }

    fun toggle() {
        val t = shown ?: return
        if (t.state?.state == PlaybackState.STATE_PLAYING) t.controller.transportControls.pause()
        else t.controller.transportControls.play()
    }

    companion object {
        /** True once the user has granted this app notification access. */
        fun hasAccess(context: Context): Boolean {
            val flat = runCatching {
                Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            }.getOrNull().orEmpty()
            val name = ComponentName(context, NowPlayingListener::class.java)
            return flat.split(':').any { part ->
                val c = ComponentName.unflattenFromString(part)
                c != null && c == name
            }
        }

        fun listenerId(context: Context): String =
            ComponentName(context, NowPlayingListener::class.java).flattenToString()
    }
}
