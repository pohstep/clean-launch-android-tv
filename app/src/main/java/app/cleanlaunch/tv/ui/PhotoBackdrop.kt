package app.cleanlaunch.tv.ui

import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import app.cleanlaunch.tv.data.PhotoDecode
import app.cleanlaunch.tv.data.PhotoEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * A still, a slow crossfade, or a short slide with a Ken Burns pan.
 * [running] is false while the page is idle, covered, or not in front.
 */
@Composable
fun PhotoBackdrop(
    entries: List<PhotoEntry>,
    mode: String,
    fixedId: String,
    minutes: Int,
    seconds: Int,
    running: Boolean,
    scrim: Color,
    scrimAlpha: Float,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty() || mode == "off") return
    LaunchedEffect(scrimAlpha, mode) {
        Log.i("CleanLaunch", "Photo scrim ${"%.2f".format(scrimAlpha)} mode $mode")
    }
    val context = LocalContext.current
    val ids = entries.map { it.id }
    val chosen = entries.indexOfFirst { it.id == fixedId }.let { if (it < 0) 0 else it }
    var index by remember(mode, fixedId, ids) { mutableIntStateOf(chosen) }
    var phase by remember { mutableFloatStateOf(0f) }
    var slide by remember { mutableFloatStateOf(1f) }
    var current by remember { mutableStateOf<ImageBitmap?>(null) }
    var outgoing by remember { mutableStateOf<ImageBitmap?>(null) }
    val cache = remember { LinkedHashMap<String, ImageBitmap>() }
    val liveRunning = rememberUpdatedState(running)
    val liveEntries = rememberUpdatedState(entries)
    val liveMode = rememberUpdatedState(mode)
    val liveMinutes = rememberUpdatedState(minutes)
    val liveSeconds = rememberUpdatedState(seconds)

    LaunchedEffect(entries, index) {
        val list = entries
        val entry = list.getOrNull(index) ?: return@LaunchedEffect
        val shown = load(cache, context, entry) ?: return@LaunchedEffect
        if (current != shown) {
            outgoing = current
            current = shown
        }
        val upcoming = list.getOrNull((index + 1) % list.size)
        if (upcoming != null) load(cache, context, upcoming)
        while (cache.size > 3) {
            val drop = cache.keys.firstOrNull { it != entry.id && it != upcoming?.id } ?: break
            cache.remove(drop)
        }
    }

    LaunchedEffect(Unit) {
        var last = SystemClock.elapsedRealtime()
        var accum = 0L
        while (isActive) {
            delay(100)
            val now = SystemClock.elapsedRealtime()
            val dt = now - last
            last = now
            if (!liveRunning.value) continue
            val kind = liveMode.value
            if (slide < 1f) {
                val fade = if (kind == "rotation") 1600f else 1200f
                slide = (slide + dt / fade).coerceAtMost(1f)
            }
            val list = liveEntries.value
            if (list.size < 2 || kind == "fixed" || kind == "off") continue
            val span = if (kind == "rotation") liveMinutes.value * 60_000L else liveSeconds.value * 1_000L
            accum += dt
            phase = (accum.toFloat() / span.toFloat()).coerceIn(0f, 1f)
            if (accum >= span) {
                accum = 0L
                phase = 0f
                slide = 0f
                index = (index + 1) % list.size
            }
        }
    }

    val kind = mode
    val travel = 1f - slide
    val ken = if (kind == "carousel") phase else 0f
    val direction = if (index % 2 == 0) 1f else -1f
    Box(modifier.fillMaxSize().clipToBounds()) {
        val gone = outgoing
        if (gone != null && slide < 1f) {
            Still(
                bitmap = gone,
                alpha = if (kind == "carousel") 1f - slide * 0.35f else 1f - slide,
                shift = if (kind == "carousel") -0.08f * travel else 0f,
                zoom = 1f,
                panX = 0f,
                panY = 0f,
            )
        }
        val shown = current
        if (shown != null) {
            Still(
                bitmap = shown,
                alpha = if (kind == "rotation" && gone != null && slide < 1f) slide else 1f,
                shift = if (kind == "carousel") 0.08f * travel else 0f,
                zoom = 1f + 0.06f * ken,
                panX = 0.03f * ken * direction,
                panY = -0.02f * ken * direction,
            )
        }
        Box(Modifier.fillMaxSize().background(scrim.copy(alpha = scrimAlpha.coerceIn(0f, 1f))))
    }
}

private suspend fun load(
    cache: LinkedHashMap<String, ImageBitmap>,
    context: android.content.Context,
    entry: PhotoEntry,
): ImageBitmap? {
    cache[entry.id]?.let { return it }
    val bitmap = withContext(Dispatchers.IO) { PhotoDecode.frame(context, entry.uri) } ?: return null
    val image = bitmap.asImageBitmap()
    cache[entry.id] = image
    return image
}

@Composable
private fun Still(
    bitmap: ImageBitmap,
    alpha: Float,
    shift: Float,
    zoom: Float,
    panX: Float,
    panY: Float,
) {
    Image(
        bitmap = bitmap,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                this.alpha = alpha.coerceIn(0f, 1f)
                scaleX = zoom
                scaleY = zoom
                translationX = size.width * (shift + panX)
                translationY = size.height * panY
                transformOrigin = TransformOrigin.Center
            },
    )
}
