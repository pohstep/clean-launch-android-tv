package app.cleanlaunch.tv

import app.cleanlaunch.tv.data.Headline
import app.cleanlaunch.tv.data.LiveForecast
import app.cleanlaunch.tv.data.Quote
import app.cleanlaunch.tv.sentence.CustomCopy
import app.cleanlaunch.tv.sentence.FrontPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrontPageTest {
    private val forecast = LiveForecast(
        celsius = 21,
        short = "21° drizzle",
        english = "In Lisbon it's 21° and drizzling, with showers tomorrow.",
        chinese = "里斯本現在21°，毛毛雨，明天有陣雨。",
    )
    private val quotes = listOf(
        Quote("AAPL", 374.36, 1.71),
        Quote("MSFT", 194.59, -0.23),
    )
    private val headlines = listOf(
        Headline("A lab maps a new reef", "實驗室繪出一座新礁"),
        Headline("Talks stall overnight", ""),
    )

    @Test
    fun failedSourcesStayOffThePage() {
        val lines = FrontPage.page(
            order = FrontPage.ids,
            enabled = FrontPage.ids.toSet(),
            forecast = null,
            quotes = emptyList(),
            headlines = emptyList(),
            customs = emptyList(),
        )
        assertTrue(lines.isEmpty())
    }

    @Test
    fun customsShowWithNoLiveData() {
        val lines = FrontPage.page(
            order = FrontPage.ids,
            enabled = setOf("weather"),
            forecast = null,
            quotes = emptyList(),
            headlines = emptyList(),
            customs = listOf(CustomCopy("Dinner is at seven", "七點吃飯"), CustomCopy("  ", "")),
        )
        assertEquals(1, lines.size)
        assertEquals("Dinner is at seven", lines[0].english)
        assertEquals("七點吃飯", lines[0].chinese)
    }

    @Test
    fun blocksFollowOrderAndSkipWhatIsHidden() {
        val lines = FrontPage.page(
            order = listOf("news", "weather", "stocks"),
            enabled = setOf("news", "stocks"),
            forecast = forecast,
            quotes = quotes,
            headlines = headlines,
            customs = listOf(CustomCopy("A note", "")),
        )
        assertEquals("A lab maps a new reef", lines[0].english)
        assertEquals("實驗室繪出一座新礁", lines[0].chinese)
        assertEquals("Talks stall overnight", lines[1].english)
        assertEquals("", lines[1].chinese)
        assertEquals("AAPL  374.36  +1.71%", lines[2].english)
        assertEquals("", lines[2].chinese)
        assertEquals("MSFT  194.59  -0.23%", lines[3].english)
        assertEquals("A note", lines[4].english)
        assertTrue(lines.none { it.english.contains("Lisbon") })
    }

    @Test
    fun quotesNeverCarryHoldings() {
        val line = FrontPage.quoteLine(Quote("IBM", 719.03, 0.09))
        assertEquals("IBM  719.03  +0.09%", line.english)
        assertEquals("", line.chinese)
        assertFalse(line.english.contains("share", ignoreCase = true))
        assertFalse(line.english.contains("15841459"))
    }
}
