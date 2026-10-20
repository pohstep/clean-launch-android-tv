package app.cleanlaunch.tv

import app.cleanlaunch.tv.sentence.ClauseBit
import app.cleanlaunch.tv.sentence.Clauses
import app.cleanlaunch.tv.sentence.EventLine
import app.cleanlaunch.tv.sentence.SentenceComposer
import app.cleanlaunch.tv.sentence.SentenceData
import app.cleanlaunch.tv.sentence.SentenceLayout
import app.cleanlaunch.tv.sentence.WeatherLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SentenceTest {
    private val sample = SentenceData(
        time = "21:40",
        date = "Tuesday 20 October",
        weather = WeatherLine(19, "clear for now", "rain arriving after three"),
        event = EventLine("Tomorrow", "the dentist", "09:30", "dinner at seven"),
        watchlist = "Your watchlist rose 0.6% today; BTC slipped 1.2% this evening.",
        nowPlaying = "Example Song",
        custom = "",
    )

    @Test
    fun defaultProseMatchesThePaper() {
        val text = SentenceComposer.prose(
            listOf("time", "weather", "event", "watchlist", "now"),
            sample,
        )
        assertEquals(
            "It is 21:40 and 19°, clear for now, with rain arriving after three. " +
                "Tomorrow opens with the dentist at 09:30, then dinner at seven. " +
                "Your watchlist rose 0.6% today; BTC slipped 1.2% this evening. " +
                "Example Song is playing quietly.",
            text,
        )
    }

    @Test
    fun templateSubstitutesTokens() {
        val text = SentenceComposer.applyTemplate(
            "It's {time}. {weather}. Next: {event}.",
            SentenceComposer.tokens(sample),
        )
        assertEquals(
            "It's 21:40. 19°, clear for now, with rain arriving after three. Next: the dentist at 09:30, then dinner at seven.",
            text,
        )
    }

    @Test
    fun noLineIsASingleWordWhenTheLineAboveCanSpareOne() {
        val lines = SentenceLayout.wrap("aa bb cc dd", maxWidth = 8f) { it.length.toFloat() }
        assertEquals(listOf("aa bb", "cc dd"), lines)
        assertTrue(lines.all { it.split(" ").size != 1 })
    }

    @Test
    fun dropsTheLowestPriorityClauseFirst() {
        val data = sample.copy(custom = "one two three four five six seven eight nine ten eleven twelve")
        val bits = Clauses.all.map { ClauseBit(it.id, it.label, it.priority) }
        val fitted = SentenceLayout.fitClauses(
            bits = bits,
            data = data,
            maxWidth = 28f,
            measure = { it.length.toFloat() },
            maxLines = 7,
        )
        assertTrue(fitted.lines.size <= 7)
        assertEquals("Custom line", fitted.dropped.first())
        assertFalse(fitted.lines.joinToString(" ").contains("twelve"))
        assertTrue(fitted.dropped.last() != "Time" || fitted.dropped.size == Clauses.all.size - 1)
        val kept = fitted.lines.joinToString(" ")
        if ("Time" !in fitted.dropped) {
            assertTrue(kept.contains("21:40"))
        }
    }

    @Test
    fun everyRenderedLineStaysInsideTheMeasure() {
        val bits = Clauses.all.map { ClauseBit(it.id, it.label, it.priority) }
        val fitted = SentenceLayout.fitClauses(bits, sample, maxWidth = 36f, measure = { it.length.toFloat() })
        assertTrue(fitted.lines.all { it.length <= 36 })
        assertTrue(fitted.lines.all { line -> line.split(" ").size != 1 || line.length > 36 })
    }
}
