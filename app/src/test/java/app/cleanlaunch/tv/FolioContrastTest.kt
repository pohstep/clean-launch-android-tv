package app.cleanlaunch.tv

import app.cleanlaunch.tv.theme.FolioPalette
import app.cleanlaunch.tv.ui.FocusWave
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class FolioContrastTest {
    @Test
    fun restingInkAndMutedClearSevenToOne() {
        for (id in listOf("cream", "night", "harbour")) {
            val (ink, paper) = FolioPalette.resting(id)
            assertTrue("$id ink ${contrast(ink, paper)}", contrast(ink, paper) >= 7.0)
            assertTrue("$id muted ${contrast(FolioPalette.muted(id), paper)}", contrast(FolioPalette.muted(id), paper) >= 7.0)
        }
    }

    @Test
    fun midWaveStopsClearFourPointFive() {
        for (id in listOf("cream", "night", "harbour")) {
            val paper = FolioPalette.resting(id).second
            val wave = FolioPalette.wave(id)
            val mids = wave.base.filter { it.at in 0.30f..0.65f }
            assertEquals(3, mids.size)
            for (stop in mids) {
                assertTrue("$id #${stop.rgb.toString(16)} ${contrast(stop.rgb, paper)}", contrast(stop.rgb, paper) >= 4.5)
            }
            assertTrue("$id glint", contrast(wave.glint, paper) >= 4.5)
        }
    }

    @Test
    fun dimClockStaysReadableOnTheLightestStop() {
        val paper = FolioPalette.dim("cream")
        val harbour = FolioPalette.dim("harbour")
        assertTrue(contrast(0xffffff, paper.core) >= 7.0)
        assertTrue(contrast(0xf2ede4, paper.core) >= 7.0)
        assertTrue(contrast(0xcfc8bd, paper.core) >= 7.0)
        assertTrue(contrast(0xffffff, harbour.core) >= 7.0)
        assertTrue(contrast(0xcfc8bd, harbour.core) >= 7.0)
        assertEquals(paper.core, FolioPalette.dim("night").core)
    }

    @Test
    fun theTwoBrushesKeepTheSpecTimings() {
        assertEquals(2200, FocusWave.BASE_MS)
        assertEquals(1600, FocusWave.GLINT_MS)
        assertEquals(120, FocusWave.BASE_DELAY_MS)
        assertEquals(420, FocusWave.GLINT_DELAY_MS)
        assertEquals(0f, FocusWave.ease(FolioPalette.baseEase, 0f), 0.001f)
        assertEquals(1f, FocusWave.ease(FolioPalette.baseEase, 1f), 0.001f)
        assertEquals(0f, FocusWave.ease(FolioPalette.glintEase, 0f), 0.001f)
        assertEquals(1f, FocusWave.ease(FolioPalette.glintEase, 1f), 0.001f)
        assertTrue(FolioPalette.wave("harbour").lightGlyphs)
        assertTrue(FolioPalette.wave("night").lightGlyphs)
        assertTrue(!FolioPalette.wave("cream").lightGlyphs)
    }

    private fun contrast(fg: Int, bg: Int): Double {
        val lighter = maxOf(luminance(fg), luminance(bg))
        val darker = minOf(luminance(fg), luminance(bg))
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun luminance(rgb: Int): Double {
        fun channel(value: Int): Double {
            val s = value / 255.0
            return if (s <= 0.04045) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        val r = channel((rgb shr 16) and 0xff)
        val g = channel((rgb shr 8) and 0xff)
        val b = channel(rgb and 0xff)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }
}
