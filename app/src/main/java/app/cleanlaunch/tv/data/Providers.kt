package app.cleanlaunch.tv.data

import app.cleanlaunch.tv.sentence.EventLine
import app.cleanlaunch.tv.sentence.WeatherLine

/** Plug a real forecast in here. The example is not connected, so the page can hide it. */
interface WeatherProvider {
    val sourceName: String
    val connected: Boolean
    fun current(latitude: Double, longitude: Double): WeatherLine
}

/** Plug a real watchlist in here. The example is not connected, so the page can hide it. */
interface WatchlistProvider {
    val sourceName: String
    val connected: Boolean
    fun current(): String
}

interface CalendarProvider {
    val sourceName: String
    val connected: Boolean
    fun next(): EventLine?
}

interface NowPlayingProvider {
    val sourceName: String
    val connected: Boolean
    fun current(): String?
}

class ExampleWeatherProvider : WeatherProvider {
    override val sourceName = "Example weather"
    override val connected = false
    var latitude: Double = Double.NaN
        private set
    var longitude: Double = Double.NaN
        private set

    override fun current(latitude: Double, longitude: Double): WeatherLine {
        this.latitude = latitude
        this.longitude = longitude
        return WeatherLine(19, "clear for now", "rain arriving after three")
    }
}

class ExampleWatchlistProvider : WatchlistProvider {
    override val sourceName = "Example watchlist"
    override val connected = false
    override fun current() = "Your watchlist rose 0.6% today; BTC slipped 1.2% this evening."
}

class ExampleCalendarProvider : CalendarProvider {
    override val sourceName = "Example calendar"
    override val connected = false
    override fun next() = EventLine("Tomorrow", "the dentist", "09:30", "dinner at seven")
}

class ExampleNowPlayingProvider : NowPlayingProvider {
    override val sourceName = "Example now playing"
    override val connected = false
    override fun current() = "Example Song"
}

data class ProviderLine(val name: String, val source: String, val slot: String)

object ExampleSources {
    val weather: WeatherProvider = ExampleWeatherProvider()
    val watchlist: WatchlistProvider = ExampleWatchlistProvider()
    val calendar: CalendarProvider = ExampleCalendarProvider()
    val nowPlaying: NowPlayingProvider = ExampleNowPlayingProvider()

    val lines = listOf(
        ProviderLine("Weather", weather.sourceName, "WeatherProvider"),
        ProviderLine("Watchlist", watchlist.sourceName, "WatchlistProvider"),
        ProviderLine("Calendar", calendar.sourceName, "CalendarProvider"),
        ProviderLine("Now playing", nowPlaying.sourceName, "NowPlayingProvider"),
    )
}
