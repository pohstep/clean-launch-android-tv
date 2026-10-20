package app.cleanlaunch.tv.sentence

import app.cleanlaunch.tv.data.Headline
import app.cleanlaunch.tv.data.LiveForecast
import app.cleanlaunch.tv.data.Quote
import app.cleanlaunch.tv.data.SkyMark
import app.cleanlaunch.tv.data.TickerList
import java.util.Locale

enum class LineRole { WEATHER, DATE, NOTE, QUOTE, NEWS }

data class PageLine(
    val english: String,
    val chinese: String,
    val sky: SkyMark? = null,
    val change: Double? = null,
    val role: LineRole = LineRole.NOTE,
)

/** Weather and the dateline on the left; quotes and a short wire on the right. */
object PaperColumns {
    const val maxHeadlines = 2
    const val maxQuotes = 3

    fun lead(lines: List<PageLine>, date: PageLine): List<PageLine> {
        val weather = lines.filter { it.role == LineRole.WEATHER }
        val notes = lines.filter { it.role == LineRole.NOTE }
        val dated = if (date.english.isBlank() && date.chinese.isBlank()) emptyList() else listOf(date)
        return weather + dated + notes
    }

    fun rail(lines: List<PageLine>): List<PageLine> {
        val quotes = lines.filter { it.role == LineRole.QUOTE }.take(maxQuotes)
        val news = lines.filter { it.role == LineRole.NEWS }.take(maxHeadlines)
        return quotes + news
    }
}

data class CustomCopy(val english: String, val chinese: String)

object FrontPage {
    val ids = listOf("weather", "stocks", "news")

    fun label(id: String): String = when (id) {
        "weather" -> "Weather"
        "stocks" -> "Stocks"
        "news" -> "News"
        else -> id
    }

    /**
     * Only blocks with a real result. A failed source is omitted.
     * Customs are the caller's own text, so they stay.
     */
    fun page(
        order: List<String>,
        enabled: Set<String>,
        forecast: LiveForecast?,
        quotes: List<Quote>,
        headlines: List<Headline>,
        customs: List<CustomCopy>,
    ): List<PageLine> {
        val out = ArrayList<PageLine>()
        val seen = HashSet<String>()
        for (id in order) {
            if (!seen.add(id) || id !in enabled) continue
            when (id) {
                "weather" -> forecast?.let {
                    out += PageLine(it.english, it.chinese, sky = it.sky, role = LineRole.WEATHER)
                }
                "stocks" -> quotes.forEach { out += quoteLine(it) }
                "news" -> headlines.forEach { item ->
                    if (item.english.isNotBlank()) {
                        out += PageLine(item.english, item.chinese, role = LineRole.NEWS)
                    }
                }
            }
        }
        customs.forEach { copy ->
            val english = copy.english.trim()
            val chinese = copy.chinese.trim()
            if (english.isNotEmpty() || chinese.isNotEmpty()) out += PageLine(english, chinese)
        }
        return out
    }

    fun quoteLine(quote: Quote): PageLine {
        val english = String.format(Locale.US, "%s  %.2f  %+.2f%%", quote.symbol, quote.price, quote.changePercent)
        val name = TickerList.chinese(quote.symbol)
        val chinese = if (name == null) {
            ""
        } else {
            String.format(Locale.US, "%s  %.2f  %+.2f%%", name, quote.price, quote.changePercent)
        }
        return PageLine(english, chinese, change = quote.changePercent, role = LineRole.QUOTE)
    }
}
