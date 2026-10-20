package app.cleanlaunch.tv.sentence

data class WeatherLine(val celsius: Int, val condition: String, val outlook: String)

data class EventLine(val day: String, val title: String, val time: String, val also: String?)

data class SentenceData(
    val time: String,
    val date: String,
    val weather: WeatherLine?,
    val event: EventLine?,
    val watchlist: String?,
    val nowPlaying: String?,
    val custom: String,
)

data class ClauseMeta(val id: String, val label: String, val priority: Int)

object Clauses {
    val all: List<ClauseMeta> = listOf(
        ClauseMeta("time", "Time", 60),
        ClauseMeta("weather", "Weather", 50),
        ClauseMeta("event", "Next event", 40),
        ClauseMeta("watchlist", "Watchlist", 30),
        ClauseMeta("now", "Now playing", 20),
        ClauseMeta("custom", "Custom line", 10),
    )

    private val byId = all.associateBy { it.id }

    fun meta(id: String): ClauseMeta = byId.getValue(id)

    val tokenDropOrder = listOf("custom", "now_playing", "watchlist", "event", "weather", "date", "time")

    fun tokenLabel(token: String): String = when (token) {
        "time" -> "Time"
        "date" -> "Date"
        "weather" -> "Weather"
        "event" -> "Next event"
        "watchlist" -> "Watchlist"
        "now_playing" -> "Now playing"
        "custom" -> "Custom line"
        else -> token
    }
}

object SentenceComposer {
    fun prose(ids: List<String>, data: SentenceData): String {
        val parts = ArrayList<String>(ids.size)
        var index = 0
        while (index < ids.size) {
            val id = ids[index]
            val next = ids.getOrNull(index + 1)
            if (id == "time" && next == "weather" && data.weather != null) {
                val weather = data.weather
                parts += "It is ${data.time} and ${weather.celsius}°, ${weather.condition}, with ${weather.outlook}."
                index += 2
                continue
            }
            when (id) {
                "time" -> parts += "It is ${data.time}."
                "weather" -> data.weather?.let {
                    parts += "${it.celsius}°, ${it.condition}, with ${it.outlook}."
                }
                "event" -> data.event?.let { parts += eventSentence(it) }
                "watchlist" -> data.watchlist?.takeIf { it.isNotBlank() }?.let { parts += it }
                "now" -> data.nowPlaying?.takeIf { it.isNotBlank() }?.let {
                    parts += "$it is playing quietly."
                }
                "custom" -> customSentence(data.custom)?.let { parts += it }
            }
            index++
        }
        return parts.joinToString(" ")
    }

    fun tokens(data: SentenceData): Map<String, String> = mapOf(
        "time" to data.time,
        "date" to data.date,
        "weather" to data.weather?.let { "${it.celsius}°, ${it.condition}, with ${it.outlook}" }.orEmpty(),
        "event" to data.event?.let { eventPhrase(it) }.orEmpty(),
        "watchlist" to data.watchlist.orEmpty().trim().removeSuffix("."),
        "now_playing" to data.nowPlaying?.takeIf { it.isNotBlank() }?.let { "$it is playing quietly" }.orEmpty(),
        "custom" to data.custom.trim(),
    )

    fun applyTemplate(template: String, values: Map<String, String>): String {
        var text = template
        values.forEach { (key, value) ->
            text = text.replace("{$key}", value)
        }
        text = text.replace(Regex("[ \\t]{2,}"), " ")
        text = text.replace(Regex("\\s+([.,;:!?])"), "$1")
        text = text.replace(Regex("([.!?])\\s*[.]"), "$1")
        text = text.replace(Regex("\\s+"), " ")
        return text.trim()
    }

    private fun eventSentence(event: EventLine): String {
        val tail = if (event.also.isNullOrBlank()) "" else ", then ${event.also}"
        return "${event.day} opens with ${event.title} at ${event.time}$tail."
    }

    private fun eventPhrase(event: EventLine): String {
        val tail = if (event.also.isNullOrBlank()) "" else ", then ${event.also}"
        return "${event.title} at ${event.time}$tail"
    }

    private fun customSentence(raw: String): String? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        val last = text.last()
        return if (last == '.' || last == '!' || last == '?') text else "$text."
    }
}
