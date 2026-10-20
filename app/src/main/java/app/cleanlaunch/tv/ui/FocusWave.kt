package app.cleanlaunch.tv.ui

import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.PorterDuff
import android.graphics.Shader
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import app.cleanlaunch.tv.theme.FolioPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

data class FocusWaveChoice(val enabled: Boolean, val id: String)

val LocalFocusWave = staticCompositionLocalOf { FocusWaveChoice(true, "dusk-sweep") }

/**
 * Two text-clipped gradients. The base dusk sweep runs 2200 ms.
 * The glint runs 1600 ms and starts later. Then the glyphs settle to solid ink.
 */
object FocusWave {
    const val BASE_MS = FolioPalette.baseMs
    const val GLINT_MS = FolioPalette.glintMs
    const val BASE_DELAY_MS = FolioPalette.baseDelayMs
    const val GLINT_DELAY_MS = FolioPalette.glintDelayMs

    fun ease(curve: FloatArray, t: Float): Float = cubicBezier(curve[0], curve[1], curve[2], curve[3], t)

    fun reducedMotion(scale: Float): Boolean = scale == 0f

    private fun cubicBezier(x1: Float, y1: Float, x2: Float, y2: Float, x: Float): Float {
        if (x <= 0f) return 0f
        if (x >= 1f) return 1f
        var t = x
        repeat(8) {
            val dx = bezierSlope(t, x1, x2)
            val xt = bezierSample(t, x1, x2) - x
            if (abs(dx) < 1e-4f) return bezierSample(t, y1, y2)
            t = (t - xt / dx).coerceIn(0f, 1f)
        }
        return bezierSample(t, y1, y2)
    }

    private fun bezierSample(t: Float, a: Float, b: Float): Float {
        val u = 1f - t
        return 3f * u * u * t * a + 3f * u * t * t * b + t * t * t
    }

    private fun bezierSlope(t: Float, a: Float, b: Float): Float {
        val u = 1f - t
        return 3f * u * u * a + 6f * u * t * (b - a) + 3f * t * t * (1f - b)
    }
}

private class TwoLayerBrush(
    private val schemeId: String,
    private val elapsedMs: Long,
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val wave = FolioPalette.wave(schemeId)
        val w = size.width.coerceAtLeast(1f)
        val h = size.height.coerceAtLeast(1f)
        val baseT = progress(elapsedMs, FolioPalette.baseDelayMs.toLong(), FolioPalette.baseMs.toLong())
        val glintT = progress(elapsedMs, FolioPalette.glintDelayMs.toLong(), FolioPalette.glintMs.toLong())
        val base = band(wave.base, w, h, angle = 90f, span = 3f, travel = 2f, t = FocusWave.ease(FolioPalette.baseEase, baseT))
        val glintStops = listOf(
            FolioPalette.Stop(wave.glint, 0.40f),
            FolioPalette.Stop(wave.glint, 0.49f),
            FolioPalette.Stop(wave.glint, 0.58f),
        )
        val glint = glintBand(glintStops, w, h, t = FocusWave.ease(FolioPalette.glintEase, glintT))
        return ComposeShader(base, glint, PorterDuff.Mode.SRC_OVER)
    }
}

private fun progress(elapsed: Long, delay: Long, duration: Long): Float {
    if (elapsed <= delay) return 0f
    return ((elapsed - delay).toFloat() / duration.coerceAtLeast(1L)).coerceIn(0f, 1f)
}

private fun band(
    stops: List<FolioPalette.Stop>,
    width: Float,
    height: Float,
    angle: Float,
    span: Float,
    travel: Float,
    t: Float,
): Shader {
    val rad = Math.toRadians((angle - 90.0))
    val dx = cos(rad).toFloat()
    val dy = sin(rad).toFloat()
    val shift = (1f - t) * width * travel
    val length = width * span
    val colors = IntArray(stops.size) { 0xFF000000.toInt() or stops[it].rgb }
    val positions = FloatArray(stops.size) { stops[it].at }
    return LinearGradient(
        -dx * shift,
        height / 2f - dy * shift,
        -dx * shift + dx * length,
        height / 2f - dy * shift + dy * length,
        colors,
        positions,
        Shader.TileMode.CLAMP,
    )
}

private fun glintBand(stops: List<FolioPalette.Stop>, width: Float, height: Float, t: Float): Shader {
    val rad = Math.toRadians((100.0 - 90.0))
    val dx = cos(rad).toFloat()
    val dy = sin(rad).toFloat()
    val shift = (1f - t) * width * 3f
    val length = width * 4f
    val colors = intArrayOf(
        0x00000000,
        0x00000000,
        0xFF000000.toInt() or stops[1].rgb,
        0x00000000,
        0x00000000,
    )
    val positions = floatArrayOf(0f, 0.40f, 0.49f, 0.58f, 1f)
    return LinearGradient(
        -dx * shift,
        height / 2f - dy * shift,
        -dx * shift + dx * length,
        height / 2f - dy * shift + dy * length,
        colors,
        positions,
        Shader.TileMode.CLAMP,
    )
}

@Composable
fun FocusText(
    text: String,
    focused: Boolean,
    rest: Color,
    dark: Boolean,
    style: TextStyle,
    modifier: Modifier = Modifier,
    schemeId: String = if (dark) "night" else "cream",
) {
    val choice = LocalFocusWave.current
    val context = LocalContext.current
    val reduce = remember {
        val scale = runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
        FocusWave.reducedMotion(scale)
    }
    val play = focused && choice.enabled && !reduce
    var elapsed by remember { mutableFloatStateOf(-1f) }
    LaunchedEffect(play, schemeId) {
        if (!play) {
            elapsed = -1f
            return@LaunchedEffect
        }
        val start = SystemClock.elapsedRealtime()
        val end = (FolioPalette.baseDelayMs + FolioPalette.baseMs).toLong()
        while (isActive) {
            val now = SystemClock.elapsedRealtime() - start
            elapsed = now.toFloat()
            if (now >= end) break
            delay(32)
        }
        elapsed = -1f
    }
    val brush = if (play && elapsed >= 0f) TwoLayerBrush(schemeId, elapsed.toLong()) else null
    BasicText(
        text = text,
        modifier = modifier,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = if (brush == null) style.copy(color = rest) else style.copy(brush = brush),
    )
}
