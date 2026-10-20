package app.cleanlaunch.tv

import app.cleanlaunch.tv.data.LiveParse
import app.cleanlaunch.tv.data.MarketHours
import app.cleanlaunch.tv.data.TickerList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class LiveFeedTest {
    private val tencent = """
        v_usAAPL="200~Apple~AAPL.OQ~373.55~368.06~370.28~15841459~0~0~0~0~0~0~0~0~0~0~0~0~0~0~0~0~0~0~0~0~0~0~~2026-10-20 11:34:19~5.49~1.49~377.59~363.64~USD~15841459~5890122294";
        v_usMSFT="200~Microsoft~MSFT.OQ~194.58~194.15~192.44~6769263~0~0~0~0~0~0~0~0~0~0~0~0~0~0~0~0~0~0~0~0~0~0~~2026-10-20 11:34:19~0.43~0.22~196.11~189.42~USD~6769263";
    """.trimIndent()

    @Test
    fun tencentKeepsPriceAndPercentOnly() {
        val quotes = LiveParse.tencent(tencent)
        assertEquals(2, quotes.size)
        assertEquals("AAPL", quotes[0].symbol)
        assertEquals(373.55, quotes[0].price, 0.001)
        assertEquals(1.49, quotes[0].changePercent, 0.001)
        assertEquals("MSFT", quotes[1].symbol)
        assertEquals(0.22, quotes[1].changePercent, 0.001)
        val rendered = quotes.joinToString(" ") { "${it.symbol} ${it.price} ${it.changePercent}" }
        assertFalse(rendered.contains("15841459"))
        assertFalse(rendered.contains("6769263"))
    }

    @Test
    fun sinaReadsTheSameTwoFields() {
        val body = """var hq_str_gb_aapl="Apple,373.5100,1.48,2026-10-20,5.45,370.28,15873105";"""
        val quotes = LiveParse.sina(body)
        assertEquals("AAPL", quotes.single().symbol)
        assertEquals(373.51, quotes.single().price, 0.001)
        assertEquals(1.48, quotes.single().changePercent, 0.001)
    }

    @Test
    fun openMeteoBuildsTheNamedCity() {
        val json = """
            {"current":{"time":"2026-10-20T23:30","interval":900,"temperature_2m":21.2,"weather_code":55},
            "daily":{"time":["2026-10-20","2026-10-21"],"weather_code":[55,80]}}
        """.trimIndent()
        val forecast = LiveParse.openMeteo(json, "Lisbon", "里斯本")!!
        assertEquals(21, forecast.celsius)
        assertEquals("21° drizzle", forecast.short)
        assertEquals("In Lisbon it's 21° and drizzling, with showers tomorrow.", forecast.english)
        assertEquals("里斯本現在21°，毛毛雨，明天有陣雨。", forecast.chinese)
    }

    @Test
    fun wttrWithoutACityOrAKnownWordKeepsTheTemperature() {
        val json = """{"current_condition":[{"temp_C":"21","weatherDesc":[{"value":"Humid"}]}]}"""
        val forecast = LiveParse.wttr(json)!!
        assertEquals("It's 21° and humid.", forecast.english)
        assertEquals("現在21°。", forecast.chinese)
    }

    @Test
    fun rssTitleAndTranslationFailures() {
        val xml = """<rss><channel><item><title><![CDATA[Reef &amp; tide]]></title></item></channel></rss>"""
        assertEquals("Reef & tide", LiveParse.firstTitle(xml))
        val ok = """{"responseData":{"translatedText":"礁與潮"},"quotaFinished":false}"""
        assertEquals("礁與潮", LiveParse.translation(ok, "Reef & tide"))
        assertEquals("", LiveParse.translation("""{"responseData":{"translatedText":"MYMEMORY WARNING: you used all"}}""", "Reef"))
        assertEquals("", LiveParse.translation("""{"responseData":{"translatedText":"Reef & tide"}}""", "Reef & tide"))
        assertEquals("", LiveParse.translation("{}", "Reef"))
    }

    @Test
    fun marketHoursFollowNewYork() {
        val zone = ZoneId.of("America/New_York")
        assertTrue(MarketHours.isOpen(ZonedDateTime.of(2026, 10, 20, 11, 30, 0, 0, zone)))
        assertTrue(MarketHours.isOpen(ZonedDateTime.of(2026, 10, 20, 9, 30, 0, 0, zone)))
        assertFalse(MarketHours.isOpen(ZonedDateTime.of(2026, 10, 20, 9, 29, 0, 0, zone)))
        assertFalse(MarketHours.isOpen(ZonedDateTime.of(2026, 10, 20, 16, 1, 0, 0, zone)))
        assertFalse(MarketHours.isOpen(ZonedDateTime.of(2026, 10, 24, 12, 0, 0, 0, zone)))
    }

    @Test
    fun tickersStaySymbols() {
        assertEquals(listOf("AAPL", "MSFT", "IBM", "ORCL"), TickerList.parse("aapl, msft ibm\norcl, shares, AAPL"))
        assertNull(TickerList.chinese("AAPL"))
        assertNull(TickerList.chinese("MSFT"))
    }
}
