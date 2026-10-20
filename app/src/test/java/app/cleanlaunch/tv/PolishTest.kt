package app.cleanlaunch.tv

import app.cleanlaunch.tv.data.AppCatalog
import app.cleanlaunch.tv.data.AppNames
import app.cleanlaunch.tv.data.ForecastWords
import app.cleanlaunch.tv.data.SkyMark
import app.cleanlaunch.tv.sentence.FrontPage
import app.cleanlaunch.tv.data.Quote
import app.cleanlaunch.tv.sentence.SkyMotion
import app.cleanlaunch.tv.sentence.SkyTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PolishTest {
    @Test
    fun weatherCodesMapToLineMarks() {
        assertEquals(SkyMark.SUN, ForecastWords.mark(0))
        assertEquals(SkyMark.SUN, ForecastWords.mark(1))
        assertEquals(SkyMark.CLOUD, ForecastWords.mark(2))
        assertEquals(SkyMark.FOG, ForecastWords.mark(45))
        assertEquals(SkyMark.DRIZZLE, ForecastWords.mark(53))
        assertEquals(SkyMark.DOWNPOUR, ForecastWords.mark(65))
        assertEquals(SkyMark.DOWNPOUR, ForecastWords.mark(61))
        assertEquals(SkyMark.SNOW, ForecastWords.mark(73))
        assertEquals(SkyMark.STORM, ForecastWords.mark(95))
        val rain = ForecastWords.fromCodes(21.0, 61, null)
        assertEquals(SkyMark.DOWNPOUR, rain.sky)
        assertNull(ForecastWords.unmapped(21.0, "strange").sky)
    }

    @Test
    fun quoteKeepsThePercentForTheArrow() {
        val line = FrontPage.quoteLine(Quote("AAPL", 194.59, -0.23))
        assertEquals(-0.23, line.change!!, 0.001)
        assertTrue(line.english.endsWith("-0.23%"))
    }

    @Test
    fun indexNamesAreEnglishUnlessRenamed() {
        assertEquals("Kumiao", AppNames.english("com.cibn.tv", "CIBN 酷喵"))
        assertEquals("Mango TV", AppNames.english(null, "MGTV 芒果TV"))
        assertEquals("NewTV 少儿", AppNames.english(null, "NewTV 少儿"))
        assertEquals("NewTV极光", AppNames.english("com.newtv.cboxtv", "NewTV极光"))
        assertEquals("Notifications", AppNames.english(null, "BRAVIA 通知"))
        assertEquals("TV Remote", AppNames.english(null, "Android TV Remote Service"))
        assertEquals("YouTube", AppNames.english("com.google.android.youtube.tv", "YouTube"))
        assertEquals("Something Else", AppNames.english("com.example.else", "Something Else"))
        assertEquals(
            "Kitchen",
            AppCatalog.displayName("pkg:com.cibn.tv", "com.cibn.tv", "CIBN 酷喵", null, mapOf("pkg:com.cibn.tv" to "Kitchen")),
        )
    }

    @Test
    fun theWashStaysFaintAndFollowsTheSky() {
        val sun = SkyTone.tint("cream", 21, SkyMark.SUN)
        val rain = SkyTone.tint("cream", 21, SkyMark.DOWNPOUR)
        val night = SkyTone.tint("night", 21, SkyMark.DOWNPOUR)
        assertTrue(sun.alpha < 0.25f)
        assertTrue(rain.alpha < 0.25f)
        assertTrue(night.alpha < 0.25f)
        assertTrue((sun.rgb shr 16) > (rain.rgb shr 16))
    }

    @Test
    fun theWashCrossfadesAndDriftsOnASlowClock() {
        assertTrue(SkyMotion.driftCycleMs in 30_000L..60_000L)
        assertEquals(2_000L, SkyMotion.crossfadeMs)
        assertEquals(200L, SkyMotion.tickMs)
        assertEquals(0f, SkyMotion.driftOffset(0f), 0.001f)
        assertEquals(1f, SkyMotion.driftOffset(0.25f), 0.001f)
        assertEquals(-1f, SkyMotion.driftOffset(0.75f), 0.001f)
        assertEquals(0f, SkyMotion.ease(0f), 0.001f)
        assertEquals(1f, SkyMotion.ease(1f), 0.001f)
        val sun = SkyTone.tint("cream", 15, SkyMark.SUN)
        val rain = SkyTone.tint("cream", 15, SkyMark.DOWNPOUR)
        assertEquals(sun, SkyMotion.mix(sun, rain, 0f))
        assertEquals(rain, SkyMotion.mix(sun, rain, 1f))
        val mid = SkyMotion.mix(sun, rain, 0.5f)
        val red = mid.rgb shr 16
        assertTrue(red < sun.rgb shr 16)
        assertTrue(red > rain.rgb shr 16)
    }
}
