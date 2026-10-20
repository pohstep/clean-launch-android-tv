package app.cleanlaunch.tv.sentence

import app.cleanlaunch.tv.data.SkyMark
import kotlin.math.roundToInt
import kotlin.math.sin

/** A low-alpha wash. Weather moves it warm or cool; the hour only nudges it. */
data class SkyTint(val rgb: Int, val alpha: Float)

/**
 * The wash clock. Drift is one slow cycle. A sky or hour change eases across
 * in [crossfadeMs]. Both advance on the same [tickMs] step as the wave.
 */
object SkyMotion {
    const val driftCycleMs = 45_000L
    const val crossfadeMs = 2_000L
    const val tickMs = 200L

    fun driftOffset(phase: Float): Float {
        val turns = (phase - phase.toInt()) * 2.0 * Math.PI
        return sin(turns).toFloat()
    }

    fun ease(t: Float): Float {
        val u = t.coerceIn(0f, 1f)
        return u * u * (3f - 2f * u)
    }

    fun mix(from: SkyTint, to: SkyTint, t: Float): SkyTint {
        val u = t.coerceIn(0f, 1f)
        fun channel(shift: Int): Int {
            val start = (from.rgb shr shift) and 0xFF
            val end = (to.rgb shr shift) and 0xFF
            return (start + (end - start) * u).roundToInt().coerceIn(0, 255)
        }
        val rgb = (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
        return SkyTint(rgb, from.alpha + (to.alpha - from.alpha) * u)
    }
}

object SkyTone {
    fun tint(schemeId: String, hour: Int, sky: SkyMark?): SkyTint {
        val dark = schemeId == "night" || schemeId == "harbour"
        val coolHour = hour < 6 || hour >= 20
        val warmSky = sky == SkyMark.SUN
        val coolSky = sky == SkyMark.DRIZZLE || sky == SkyMark.DOWNPOUR ||
            sky == SkyMark.STORM || sky == SkyMark.SNOW || sky == SkyMark.FOG
        val warm = if (dark) 0x6A4A30 else 0xE7C49A
        val cool = if (schemeId == "harbour") 0x2A4560 else if (dark) 0x3A4E62 else 0xA9BFD0
        val neutral = if (dark) 0x4A4038 else 0xE4D3BC
        val (rgb, alpha) = when {
            warmSky && coolSky.not() -> warm to if (dark) 0.18f else 0.14f
            coolSky -> cool to if (dark) 0.20f else 0.15f
            coolHour -> cool to if (dark) 0.08f else 0.06f
            else -> neutral to if (dark) 0.06f else 0.05f
        }
        return SkyTint(rgb, alpha)
    }
}
