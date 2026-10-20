package app.cleanlaunch.tv.ui

import android.graphics.RuntimeShader
import app.cleanlaunch.tv.data.SkyMark
import app.cleanlaunch.tv.sentence.SkyMotion
import app.cleanlaunch.tv.sentence.SkyTone
import android.os.Build
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import android.graphics.RenderEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val WAVE_AGSL = """
    uniform float2 resolution;
    uniform float time;
    uniform float3 paper;
    uniform float3 drift;
    uniform float3 glow;

    half4 main(float2 frag) {
        float2 uv = frag / max(resolution, float2(1.0));
        float phase = time * 0.314159;
        float a = sin(uv.x * 2.4 + phase) * 0.5 + 0.5;
        float b = sin(uv.y * 1.7 - phase * 0.73 + uv.x) * 0.5 + 0.5;
        float mixAmt = (a * 0.62 + b * 0.38) * 0.22;
        float3 color = mix(paper, drift, mixAmt);
        float edge = smoothstep(0.2, 0.95, length(uv - float2(0.5, 0.45)));
        color = mix(color, glow, edge * 0.18);
        return half4(color.r, color.g, color.b, 1.0);
    }
"""

private data class WaveInk(val paper: Color, val drift: Color, val glow: Color)

private fun waveInk(schemeId: String): WaveInk = when (schemeId) {
    "night" -> WaveInk(Color(0xFF1A1510), Color(0xFF2A2218), Color(0xFF100C08))
    "harbour" -> WaveInk(Color(0xFF0E1A2A), Color(0xFF163044), Color(0xFF0A121C))
    else -> WaveInk(Color(0xFFF3EEE3), Color(0xFFE7D7B8), Color(0xFFF7F3EA))
}

/**
 * A slow, low-contrast field. The shader is evaluated at a small size and
 * scaled up, so a software GPU does not shade every screen pixel.
 * [running] is false while the page is idle, covered, or not in front.
 */
@Composable
fun WaveBackdrop(schemeId: String, running: Boolean, modifier: Modifier = Modifier) {
    if (Build.VERSION.SDK_INT >= 33) {
        ShaderWave(schemeId, running, modifier)
    } else {
        StillWave(schemeId, modifier)
    }
}

@Composable
private fun ShaderWave(schemeId: String, running: Boolean, modifier: Modifier) {
    val ink = waveInk(schemeId)
    val shader = remember(schemeId) {
        runCatching { RuntimeShader(WAVE_AGSL) }
            .onFailure { Log.i("CleanLaunch", "Wave shader unavailable", it) }
            .getOrNull()
    }
    if (shader == null) {
        StillWave(schemeId, modifier)
        return
    }
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(running, schemeId) {
        while (isActive && running) {
            time += 0.2f
            delay(200)
        }
    }
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxSize()) {
        val scaleX = if (maxWidth > 0.dp) maxWidth / 240.dp else 1f
        val scaleY = if (maxHeight > 0.dp) maxHeight / 135.dp else 1f
        Box(
            Modifier
                .size(240.dp, 135.dp)
                .graphicsLayer {
                    this.scaleX = scaleX
                    this.scaleY = scaleY
                    transformOrigin = TransformOrigin(0f, 0f)
                    clip = false
                    val widthPx = with(density) { 240.dp.toPx() }
                    val heightPx = with(density) { 135.dp.toPx() }
                    shader.setFloatUniform("resolution", widthPx, heightPx)
                    shader.setFloatUniform("time", time)
                    shader.setFloatUniform("paper", ink.paper.red, ink.paper.green, ink.paper.blue)
                    shader.setFloatUniform("drift", ink.drift.red, ink.drift.green, ink.drift.blue)
                    shader.setFloatUniform("glow", ink.glow.red, ink.glow.green, ink.glow.blue)
                    renderEffect = RenderEffect.createShaderEffect(shader).asComposeRenderEffect()
                }
                .drawWithContent {
                    val started = System.nanoTime()
                    drawContent()
                    val cost = (System.nanoTime() - started) / 1_000_000
                    if (cost > 40) Log.i("CleanLaunch", "Wave frame ${cost}ms")
                }
                .background(ink.paper),
        )
    }
}

@Composable
private fun StillWave(schemeId: String, modifier: Modifier) {
    val ink = waveInk(schemeId)
    Canvas(modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(ink.paper, ink.drift, ink.paper),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
        )
    }
}

/**
 * The day and weather wash, drawn small and scaled up.
 * [running] is false while the page is idle or this activity is not in front,
 * and the drift clock stops with it. A new hour or sky eases in over two seconds.
 */
@Composable
fun SkyWash(
    schemeId: String,
    hour: Int,
    sky: SkyMark?,
    running: Boolean,
    dim: Float,
    modifier: Modifier = Modifier,
) {
    val latest = rememberUpdatedState(SkyTone.tint(schemeId, hour, sky))
    var origin by remember { mutableStateOf(latest.value) }
    var shown by remember { mutableStateOf(latest.value) }
    var blend by remember { mutableFloatStateOf(1f) }
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(running) {
        Log.i("CleanLaunch", if (running) "Wash running" else "Wash paused")
        if (!running) return@LaunchedEffect
        var heading = latest.value
        while (isActive) {
            val dest = latest.value
            if (dest != heading || (blend >= 1f && shown != dest)) {
                origin = shown
                blend = 0f
                heading = dest
                Log.i("CleanLaunch", "Wash crossfade")
            }
            if (blend < 1f) {
                blend = (blend + SkyMotion.tickMs.toFloat() / SkyMotion.crossfadeMs).coerceAtMost(1f)
                shown = SkyMotion.mix(origin, dest, SkyMotion.ease(blend))
            }
            phase = (phase + SkyMotion.tickMs.toFloat() / SkyMotion.driftCycleMs) % 1f
            delay(SkyMotion.tickMs)
        }
    }
    val alpha = shown.alpha * (1f - dim).coerceIn(0f, 1f)
    if (alpha < 0.012f) return
    val sway = SkyMotion.driftOffset(phase)
    val packed = (0xFF000000L or shown.rgb.toLong()).toInt()
    val color = Color(packed).copy(alpha = alpha)
    BoxWithConstraints(modifier.fillMaxSize()) {
        val scaleX = if (maxWidth > 0.dp) maxWidth / 240.dp else 1f
        val scaleY = if (maxHeight > 0.dp) maxHeight / 135.dp else 1f
        Canvas(
            Modifier
                .size(240.dp, 135.dp)
                .graphicsLayer {
                    this.scaleX = scaleX
                    this.scaleY = scaleY
                    transformOrigin = TransformOrigin(0f, 0f)
                    clip = false
                },
        ) {
            val mid = (0.52f + sway * 0.08f).coerceIn(0.28f, 0.76f)
            drawRect(
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to color,
                        mid to color.copy(alpha = alpha * 0.35f),
                        1f to Color.Transparent,
                    ),
                    start = Offset(size.width * (0.5f + sway * 0.12f), 0f),
                    end = Offset(size.width * (0.5f - sway * 0.12f), size.height),
                ),
            )
        }
    }
}
