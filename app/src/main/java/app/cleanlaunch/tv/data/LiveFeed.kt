package app.cleanlaunch.tv.data

import android.util.Log
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.Charset
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import kotlin.math.roundToInt

/** Last price and the day's percent change. Nothing else is kept. */
data class Quote(val symbol: String, val price: Double, val changePercent: Double)

data class Headline(val english: String, val chinese: String)

enum class SkyMark { SUN, CLOUD, DRIZZLE, DOWNPOUR, STORM, SNOW, FOG }

data class LiveForecast(
    val celsius: Int,
    val short: String,
    val english: String,
    val chinese: String,
    val sky: SkyMark? = null,
)

object TickerList {
    private val token = Regex("[A-Z]{1,5}")

    fun parse(raw: String): List<String> {
        return raw.uppercase(Locale.US)
            .split(',', ' ', '\n', '\t')
            .map { it.trim() }
            .filter { token.matches(it) }
            .distinct()
            .take(8)
    }

    /** Unknown symbols stay English. This build ships no preset names. */
    fun chinese(symbol: String): String? = null
}

object MarketHours {
    private val newYork: ZoneId = ZoneId.of("America/New_York")
    private val open: LocalTime = LocalTime.of(9, 30)
    private val close: LocalTime = LocalTime.of(16, 0)

    fun isOpen(now: ZonedDateTime = ZonedDateTime.now()): Boolean {
        val ny = now.withZoneSameInstant(newYork)
        if (ny.dayOfWeek == DayOfWeek.SATURDAY || ny.dayOfWeek == DayOfWeek.SUNDAY) return false
        val clock = ny.toLocalTime()
        return !clock.isBefore(open) && !clock.isAfter(close)
    }
}

object LiveParse {
    fun tencent(body: String): List<Quote> {
        val out = ArrayList<Quote>()
        Regex("""v_us([A-Za-z]{1,5})\s*=\s*"([^"]*)"""").findAll(body).forEach { match ->
            val symbol = match.groupValues[1].uppercase(Locale.US)
            val fields = match.groupValues[2].split('~')
            val price = fields.getOrNull(3)?.toDoubleOrNull() ?: return@forEach
            val percent = fields.getOrNull(32)?.toDoubleOrNull() ?: return@forEach
            if (price > 0.0) out += Quote(symbol, price, percent)
        }
        return out
    }

    fun sina(body: String): List<Quote> {
        val out = ArrayList<Quote>()
        Regex("""hq_str_gb_([A-Za-z]{1,5})\s*=\s*"([^"]*)"""").findAll(body).forEach { match ->
            val symbol = match.groupValues[1].uppercase(Locale.US)
            val fields = match.groupValues[2].split(',')
            val price = fields.getOrNull(1)?.toDoubleOrNull() ?: return@forEach
            val percent = fields.getOrNull(2)?.toDoubleOrNull() ?: return@forEach
            if (price > 0.0) out += Quote(symbol, price, percent)
        }
        return out
    }

    fun openMeteo(json: String, city: String = "", chineseCity: String = ""): LiveForecast? {
        val current = Regex(""""current"\s*:\s*\{([^{}]*)\}""").find(json)?.groupValues?.get(1) ?: return null
        val temp = Regex(""""temperature_2m"\s*:\s*(-?\d+(?:\.\d+)?)""")
            .find(current)?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
        val code = Regex(""""weather_code"\s*:\s*(-?\d+)""")
            .find(current)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        val tomorrow = Regex(""""weather_code"\s*:\s*\[\s*-?\d+\s*,\s*(-?\d+)""")
            .find(json)?.groupValues?.get(1)?.toIntOrNull()
        return ForecastWords.fromCodes(temp, code, tomorrow, city, chineseCity)
    }

    fun wttr(json: String, city: String = "", chineseCity: String = ""): LiveForecast? {
        val temp = Regex(""""temp_C"\s*:\s*"(-?\d+(?:\.\d+)?)"""")
            .find(json)?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
        val desc = Regex(""""weatherDesc"\s*:\s*\[\s*\{\s*"value"\s*:\s*"([^"]+)"""")
            .find(json)?.groupValues?.get(1)?.trim().orEmpty()
        val code = ForecastWords.codeForWords(desc) ?: return ForecastWords.unmapped(temp, desc, city, chineseCity)
        return ForecastWords.fromCodes(temp, code, null, city, chineseCity)
    }

    fun firstTitle(xml: String): String? {
        val item = Regex("(?is)<item\\b[^>]*>(.*?)</item>").find(xml)?.groupValues?.get(1) ?: return null
        val raw = Regex("(?is)<title\\b[^>]*>(.*?)</title>").find(item)?.groupValues?.get(1) ?: return null
        val text = unescape(strip(raw)).trim()
        return text.ifBlank { null }
    }

    /** Empty when the service failed, warned, or echoed the source. Never invents a line. */
    fun translation(json: String, source: String): String {
        if (json.contains("MYMEMORY WARNING") || json.contains("QUERY LENGTH")) return ""
        val raw = Regex(""""translatedText"\s*:\s*"((?:\\.|[^"\\])*)"""")
            .find(json)?.groupValues?.get(1) ?: return ""
        val text = unescape(raw).trim()
        if (text.isEmpty() || text == source.trim()) return ""
        return text
    }

    private fun strip(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("<![CDATA[") && text.endsWith("]]>")) {
            text = text.removePrefix("<![CDATA[").removeSuffix("]]>")
        }
        return text.replace(Regex("<[^>]+>"), "")
    }

    private fun unescape(raw: String): String {
        val unicode = Regex("""\\u([0-9a-fA-F]{4})""").replace(raw) {
            it.groupValues[1].toInt(16).toChar().toString()
        }
        return unicode
            .replace("\\n", " ")
            .replace("\\\"", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
    }
}

private data class Sky(val short: String, val now: String, val chinese: String, val later: String, val laterChinese: String)

object ForecastWords {
    fun fromCodes(
        celsius: Double,
        nowCode: Int,
        tomorrowCode: Int?,
        city: String = "",
        chineseCity: String = "",
    ): LiveForecast {
        val degrees = celsius.roundToInt()
        val now = sky(nowCode)
        val tomorrow = tomorrowCode?.let { sky(it) }
        val same = tomorrow == null || tomorrow.short == now.short
        val where = city.trim()
        val english = if (same) {
            if (where.isEmpty()) "It's $degrees° and ${now.now}." else "In $where it's $degrees° and ${now.now}."
        } else {
            val later = tomorrow!!.later
            if (where.isEmpty()) "It's $degrees° and ${now.now}, with $later tomorrow."
            else "In $where it's $degrees° and ${now.now}, with $later tomorrow."
        }
        val zh = chineseCity.trim().ifBlank { where }
        val chinese = if (zh.isEmpty()) {
            if (same) "現在${degrees}°，${now.chinese}。" else "現在${degrees}°，${now.chinese}，明天${tomorrow!!.laterChinese}。"
        } else if (same) {
            "${zh}現在${degrees}°，${now.chinese}。"
        } else {
            "${zh}現在${degrees}°，${now.chinese}，明天${tomorrow!!.laterChinese}。"
        }
        return LiveForecast(degrees, "$degrees° ${now.short}", english, chinese, mark(nowCode))
    }

    fun mark(code: Int): SkyMark = when (code) {
        0, 1 -> SkyMark.SUN
        2, 3 -> SkyMark.CLOUD
        45, 48 -> SkyMark.FOG
        51, 53, 55 -> SkyMark.DRIZZLE
        65, 80, 81, 82 -> SkyMark.DOWNPOUR
        61, 63 -> SkyMark.DOWNPOUR
        71, 73, 75 -> SkyMark.SNOW
        95, 96, 99 -> SkyMark.STORM
        else -> SkyMark.CLOUD
    }

    /** Temperature only in Chinese when the English words are not a known condition. */
    fun unmapped(celsius: Double, description: String, city: String = "", chineseCity: String = ""): LiveForecast {
        val degrees = celsius.roundToInt()
        val words = description.trim().ifBlank { "unsettled" }
        val where = city.trim()
        val zh = chineseCity.trim().ifBlank { where }
        return LiveForecast(
            celsius = degrees,
            short = "$degrees°",
            english = if (where.isEmpty()) "It's $degrees° and ${words.lowercase(Locale.UK)}."
            else "In $where it's $degrees° and ${words.lowercase(Locale.UK)}.",
            chinese = if (zh.isEmpty()) "現在${degrees}°。" else "${zh}現在${degrees}°。",
        )
    }

    fun codeForWords(description: String): Int? {
        val text = description.lowercase(Locale.UK)
        return when {
            text.contains("thunder") || text.contains("storm") -> 95
            text.contains("snow") || text.contains("sleet") -> 71
            text.contains("drizzle") -> 51
            text.contains("shower") -> 80
            text.contains("rain") -> 61
            text.contains("fog") || text.contains("mist") -> 45
            text.contains("overcast") -> 3
            text.contains("cloud") -> 2
            text.contains("clear") || text.contains("sunny") -> 0
            else -> null
        }
    }

    private fun sky(code: Int): Sky = when (code) {
        0 -> Sky("clear", "clear", "晴朗", "clear skies", "晴朗")
        1 -> Sky("clear", "mainly clear", "大致晴朗", "mainly clear skies", "大致晴朗")
        2 -> Sky("cloudy", "partly cloudy", "多雲", "clouds", "多雲")
        3 -> Sky("overcast", "overcast", "陰天", "overcast skies", "陰天")
        45, 48 -> Sky("fog", "foggy", "有霧", "fog", "有霧")
        51, 53, 55 -> Sky("drizzle", "drizzling", "毛毛雨", "drizzle", "有毛毛雨")
        61, 63, 65 -> Sky("rain", "raining", "下雨", "rain", "有雨")
        71, 73, 75 -> Sky("snow", "snowing", "下雪", "snow", "有雪")
        80, 81, 82 -> Sky("showers", "showery", "有陣雨", "showers", "有陣雨")
        95, 96, 99 -> Sky("storms", "stormy", "有雷雨", "storms", "有雷雨")
        else -> Sky("unsettled", "unsettled", "天氣多變", "unsettled weather", "天氣多變")
    }
}

private object Net {
    fun get(url: String, gbk: Boolean = false, referer: String? = null): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 9_000
            readTimeout = 9_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "CleanLaunch/1.0")
            if (referer != null) setRequestProperty("Referer", referer)
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("HTTP $code for $url")
            val bytes = connection.inputStream.use { it.readBytes() }
            val charset = if (gbk) Charset.forName("GBK") else Charsets.UTF_8
            return String(bytes, charset)
        } finally {
            connection.disconnect()
        }
    }
}

object QuoteClient {
    fun fetch(symbols: List<String>): List<Quote> {
        if (symbols.isEmpty()) return emptyList()
        val tencent = runCatching {
            val query = symbols.joinToString(",") { "us$it" }
            LiveParse.tencent(Net.get("https://qt.gtimg.cn/q=$query", gbk = true))
        }.getOrDefault(emptyList())
        if (tencent.isNotEmpty()) return order(symbols, tencent)
        val sina = runCatching {
            val query = symbols.joinToString(",") { "gb_${it.lowercase(Locale.US)}" }
            LiveParse.sina(
                Net.get(
                    "https://hq.sinajs.cn/list=$query",
                    gbk = true,
                    referer = "https://finance.sina.com.cn",
                ),
            )
        }.getOrDefault(emptyList())
        return order(symbols, sina)
    }

    private fun order(symbols: List<String>, quotes: List<Quote>): List<Quote> {
        val bySymbol = quotes.associateBy { it.symbol }
        return symbols.mapNotNull { bySymbol[it] }
    }
}

object ForecastClient {
    fun fetch(latitude: Double, longitude: Double, city: String, chineseCity: String = city): LiveForecast? {
        if (city.isBlank() || latitude.isNaN() || longitude.isNaN()) return null
        val meteoUrl = "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude" +
            "&current=temperature_2m,weather_code&daily=weather_code&timezone=auto&forecast_days=2"
        val meteo = readForecast("Open-Meteo", meteoUrl) { LiveParse.openMeteo(it, city, chineseCity) }
        if (meteo != null) return meteo
        val encoded = URLEncoder.encode(city, "UTF-8")
        return readForecast("wttr.in", "https://wttr.in/$encoded?format=j1") { LiveParse.wttr(it, city, chineseCity) }
    }

    private fun readForecast(name: String, url: String, parse: (String) -> LiveForecast?): LiveForecast? {
        val body = try {
            Net.get(url)
        } catch (error: Throwable) {
            Log.i("CleanLaunch", "$name failed: ${error.message}")
            return null
        }
        val forecast = parse(body)
        if (forecast == null) Log.i("CleanLaunch", "$name unparsed: ${body.take(160)}")
        return forecast
    }
}

object HeadlineClient {
    private val science = listOf(
        "https://feeds.bbci.co.uk/news/science_and_environment/rss.xml",
        "https://www.sciencedaily.com/rss/top/science.xml",
        "https://www.theguardian.com/science/rss",
    )
    private val world = listOf(
        "https://feeds.bbci.co.uk/news/world/rss.xml",
        "https://rss.dw.com/rdf/rss-en-world",
        "https://www.theguardian.com/world/rss",
    )
    private val technology = listOf(
        "https://feeds.bbci.co.uk/news/technology/rss.xml",
        "https://www.theguardian.com/technology/rss",
    )

    fun fetch(cached: (String) -> String?, save: (String, String) -> Unit): List<Headline> {
        return listOf(science, world, technology).mapNotNull { chain ->
            val title = firstWorking(chain) ?: return@mapNotNull null
            val chinese = translate(title, cached, save)
            Headline(title, chinese)
        }
    }

    private fun firstWorking(urls: List<String>): String? {
        for (url in urls) {
            val title = runCatching { LiveParse.firstTitle(Net.get(url)) }.getOrNull()
            if (!title.isNullOrBlank()) return title
        }
        return null
    }

    private fun translate(english: String, cached: (String) -> String?, save: (String, String) -> Unit): String {
        val known = cached(english)
        if (!known.isNullOrBlank()) return known
        val translated = runCatching {
            val query = URLEncoder.encode(english.take(400), "UTF-8")
            LiveParse.translation(
                Net.get("https://api.mymemory.translated.net/get?q=$query&langpair=en%7Czh-TW"),
                english,
            )
        }.getOrDefault("")
        if (translated.isNotBlank()) save(english, translated)
        return translated
    }
}
