package app.cleanlaunch.tv

import app.cleanlaunch.tv.nav.BackState
import app.cleanlaunch.tv.nav.reduceBack
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackNavTest {
    @Test
    fun backFromSettingsReturnsHomeWithoutDimming() {
        val result = reduceBack(BackState(overlay = true, onLastUsed = false))
        assertFalse(result.overlay)
        assertFalse(result.dimmed)
        assertTrue(result.focusHome)
    }

    @Test
    fun backClosesAMenuBeforeLeavingTheScreen() {
        val result = reduceBack(BackState(menu = true, overlay = true))
        assertFalse(result.menu)
        assertTrue(result.overlay)
        assertFalse(result.dimmed)
        assertFalse(result.focusHome)
    }

    @Test
    fun backOnHomeMovesToTheLastUsedApp() {
        val result = reduceBack(BackState(onLastUsed = false))
        assertTrue(result.focusHome)
        assertFalse(result.dimmed)
    }

    @Test
    fun secondBackOnHomeDimsThePage() {
        val result = reduceBack(BackState(onLastUsed = true))
        assertTrue(result.dimmed)
        assertFalse(result.focusHome)
    }
}
