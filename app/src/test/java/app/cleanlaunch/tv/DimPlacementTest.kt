package app.cleanlaunch.tv

import app.cleanlaunch.tv.ui.DimPlacement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DimPlacementTest {
    @Test
    fun overscanIsFivePercentAndAtLeastThe1080pMargin() {
        assertEquals(96, DimPlacement.insetPx(1920, 96))
        assertEquals(54, DimPlacement.insetPx(1080, 54))
        assertEquals(96, DimPlacement.insetPx(1600, 96))
        assertTrue(DimPlacement.insetPx(1920, 0) >= (1920 * 0.05).toInt())
        assertTrue(DimPlacement.insetPx(1080, 0) >= (1080 * 0.05).toInt())
    }

    @Test
    fun blockBottomStaysInsideTheSafeAreaEvenWhenTheCjkLineGrows() {
        val screen = 1080
        val bottom = DimPlacement.insetPx(screen, 54)
        val available = screen - bottom - DimPlacement.insetPx(screen, 54)
        val measured = 300 + 64 + 90
        val scale = DimPlacement.fitScale(measured, available)
        val top = DimPlacement.bottomAlignedTop(available, measured, scale)
        val used = (measured * scale).toInt()
        assertEquals(1f, scale)
        assertTrue(top + used <= available)
        assertEquals(available, top + used)
    }

    @Test
    fun aLargeFontScaleShrinksInsteadOfCrossingTheMargin() {
        val available = 1080 - 54 - 54
        val measured = 1400
        val scale = DimPlacement.fitScale(measured, available)
        val top = DimPlacement.bottomAlignedTop(available, measured, scale)
        assertTrue(scale < 1f)
        assertEquals(0, top)
        assertTrue((measured * scale).toInt() <= available + 1)
    }
}
