package app.cleanlaunch.tv.data

import android.content.Context
import app.cleanlaunch.tv.sentence.Clauses
import java.security.MessageDigest

data class ClausePref(val id: String, val enabled: Boolean)

class EveningStore(context: Context) {
    private val prefs = context.getSharedPreferences("cleanlaunch", Context.MODE_PRIVATE)

    var schemeId: String
        get() {
            return prefs.getString(KEY_SCHEME, "cream") ?: "cream"
        }
        set(value) = prefs.edit().putString(KEY_SCHEME, value).apply()

    var templateMode: Boolean
        get() = prefs.getBoolean(KEY_TEMPLATE, false)
        set(value) = prefs.edit().putBoolean(KEY_TEMPLATE, value).apply()

    var template: String
        get() = prefs.getString(KEY_TEMPLATE_TEXT, DEFAULT_TEMPLATE) ?: DEFAULT_TEMPLATE
        set(value) = prefs.edit().putString(KEY_TEMPLATE_TEXT, value).apply()

    var customText: String
        get() = prefs.getString(KEY_CUSTOM, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM, value).apply()

    var idleMinutes: Int
        get() = prefs.getInt(KEY_IDLE, 2)
        set(value) = prefs.edit().putInt(KEY_IDLE, value).apply()

    var plate: Boolean
        get() = prefs.getBoolean(KEY_PLATE, false)
        set(value) = prefs.edit().putBoolean(KEY_PLATE, value).apply()

    var lastAppId: String?
        get() = prefs.getString(KEY_LAST, null)
        set(value) = prefs.edit().putString(KEY_LAST, value).apply()

    var showAv: Boolean
        get() = prefs.getBoolean(KEY_AV, false)
        set(value) = prefs.edit().putBoolean(KEY_AV, value).apply()

    var showTuner: Boolean
        get() = prefs.getBoolean(KEY_TUNER, false)
        set(value) = prefs.edit().putBoolean(KEY_TUNER, value).apply()

    var tickers: String
        get() = prefs.getString(KEY_TICKERS, DEFAULT_TICKERS) ?: DEFAULT_TICKERS
        set(value) = prefs.edit().putString(KEY_TICKERS, value).apply()

    var wave: Boolean
        get() = prefs.getBoolean(KEY_WAVE, true)
        set(value) = prefs.edit().putBoolean(KEY_WAVE, value).apply()

    var locationMode: String
        get() = prefs.getString(KEY_LOC_MODE, "auto") ?: "auto"
        set(value) = prefs.edit().putString(KEY_LOC_MODE, value).apply()

    var manualCity: String
        get() = prefs.getString(KEY_MANUAL_CITY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_MANUAL_CITY, value).apply()

    var ipFailedAt: Long
        get() = prefs.getLong(KEY_IP_FAIL, 0L)
        set(value) = prefs.edit().putLong(KEY_IP_FAIL, value).apply()

    var seededReal: Boolean
        get() = prefs.getBoolean(KEY_SEEDED, false)
        set(value) = prefs.edit().putBoolean(KEY_SEEDED, value).apply()

    fun clauses(): List<ClausePref> {
        val raw = prefs.getString(KEY_CLAUSES, null) ?: return defaultClauses()
        val parsed = raw.split(",").mapNotNull { part ->
            val bits = part.split(":")
            if (bits.size != 2) null else ClausePref(bits[0], bits[1] == "1")
        }
        val known = Clauses.all.map { it.id }.toSet()
        if (parsed.isEmpty() || parsed.any { it.id !in known }) return defaultClauses()
        val present = parsed.map { it.id }.toSet()
        val missing = Clauses.all.filter { it.id !in present }.map { ClausePref(it.id, false) }
        return parsed + missing
    }

    fun setClauses(clauses: List<ClausePref>) {
        val raw = clauses.joinToString(",") { "${it.id}:${if (it.enabled) 1 else 0}" }
        prefs.edit().putString(KEY_CLAUSES, raw).apply()
    }

    fun appOrder(): List<String> {
        val raw = prefs.getString(KEY_APPS, null) ?: return DEFAULT_APPS
        val ids = raw.split(",").filter { it.isNotBlank() }
        return ids.ifEmpty { DEFAULT_APPS }
    }

    fun setAppOrder(ids: List<String>) {
        prefs.edit().putString(KEY_APPS, ids.joinToString(",")).apply()
    }

    fun hidden(): Set<String> {
        return prefs.getStringSet(KEY_HIDDEN, emptySet()) ?: emptySet()
    }

    fun setHidden(ids: Set<String>) {
        prefs.edit().putStringSet(KEY_HIDDEN, ids).apply()
    }

    fun pins(): List<String> {
        return prefs.getString(KEY_PINS, null).orEmpty().split(",").filter { it.isNotBlank() }
    }

    fun setPins(ids: List<String>) {
        prefs.edit().putString(KEY_PINS, ids.joinToString(",")).apply()
    }

    fun shelfOrder(): List<String> {
        return prefs.getString(KEY_SHELF, null).orEmpty().split(",").filter { it.isNotBlank() }
    }

    fun setShelfOrder(ids: List<String>) {
        prefs.edit().putString(KEY_SHELF, ids.joinToString(",")).apply()
    }

    var shelfSeeded: Boolean
        get() = prefs.getBoolean(KEY_SHELF_SEEDED, false)
        set(value) = prefs.edit().putBoolean(KEY_SHELF_SEEDED, value).apply()

    var settingsSection: String
        get() = prefs.getString(KEY_SECTION, "inputs")?.takeIf { it.isNotBlank() } ?: "inputs"
        set(value) = prefs.edit().putString(KEY_SECTION, value).apply()

    var layoutVersion: Int
        get() = prefs.getInt(KEY_LAYOUT, 0)
        set(value) = prefs.edit().putInt(KEY_LAYOUT, value).apply()

    var tickerChinese: Boolean
        get() = prefs.getBoolean(KEY_TICKER_ZH, false)
        set(value) = prefs.edit().putBoolean(KEY_TICKER_ZH, value).apply()

    var frontPhoto: String
        get() = prefs.getString(KEY_FRONT_PHOTO, "") ?: ""
        set(value) = prefs.edit().putString(KEY_FRONT_PHOTO, value).apply()

    var slotSeconds: Int
        get() = prefs.getInt(KEY_SLOT_SEC, SlotPhotos.DEFAULT_SECONDS).let {
            if (it in SlotPhotos.INTERVALS) it else SlotPhotos.DEFAULT_SECONDS
        }
        set(value) = prefs.edit().putInt(KEY_SLOT_SEC, value).apply()

    fun inputNames(): Map<String, String> = decodeMap(prefs.getString(KEY_INPUT_NAMES, null))

    fun setInputName(id: String, value: String) {
        val next = inputNames().toMutableMap()
        if (value.isBlank()) next.remove(id) else next[id] = value
        prefs.edit().putString(KEY_INPUT_NAMES, encodeMap(next)).apply()
    }

    fun hiddenInputs(): Set<String> = prefs.getStringSet(KEY_INPUT_HIDDEN, emptySet()) ?: emptySet()

    fun setHiddenInputs(ids: Set<String>) {
        prefs.edit().putStringSet(KEY_INPUT_HIDDEN, ids).apply()
    }

    private fun decodeMap(raw: String?): Map<String, String> {
        return raw.orEmpty().split('\u001e').mapNotNull { part ->
            val bits = part.split('\u001f')
            if (bits.size != 2 || bits[0].isBlank() || bits[1].isBlank()) null else bits[0] to bits[1]
        }.toMap()
    }

    private fun encodeMap(map: Map<String, String>): String {
        return map.entries.joinToString("\u001e") { "${it.key}\u001f${it.value}" }
    }

    fun opened(): Set<String> = prefs.getStringSet(KEY_OPENED, emptySet()) ?: emptySet()

    fun markOpened(id: String) {
        prefs.edit().putStringSet(KEY_OPENED, opened() + id).apply()
    }

    fun front(): List<FrontPref> {
        val raw = prefs.getString(KEY_FRONT, null) ?: return defaultFront()
        val parsed = raw.split(",").mapNotNull { part ->
            val bits = part.split(":")
            if (bits.size != 2) null else FrontPref(bits[0], bits[1] == "1")
        }.filter { it.id in FRONT_IDS }
        if (parsed.distinctBy { it.id }.size != parsed.size || parsed.isEmpty()) return defaultFront()
        val missing = FRONT_IDS.filter { id -> parsed.none { it.id == id } }.map { FrontPref(it, it == "weather") }
        return parsed + missing
    }

    fun setFront(items: List<FrontPref>) {
        val raw = items.joinToString(",") { "${it.id}:${if (it.enabled) 1 else 0}" }
        prefs.edit().putString(KEY_FRONT, raw).apply()
    }

    fun customEnglish(index: Int): String = prefs.getString("frontEn$index", "") ?: ""

    fun customChinese(index: Int): String = prefs.getString("frontCn$index", "") ?: ""

    fun setCustomEnglish(index: Int, value: String) {
        prefs.edit().putString("frontEn$index", value).apply()
    }

    fun setCustomChinese(index: Int, value: String) {
        prefs.edit().putString("frontCn$index", value).apply()
    }

    fun translation(english: String): String? = prefs.getString(translationKey(english), null)

    fun saveTranslation(english: String, chinese: String) {
        if (english.isBlank() || chinese.isBlank()) return
        prefs.edit().putString(translationKey(english), chinese).apply()
    }

    fun names(): Map<String, String> {
        return prefs.getString(KEY_NAMES, null).orEmpty().split('\u001e').mapNotNull { part ->
            val bits = part.split('\u001f')
            if (bits.size != 2 || bits[0].isBlank() || bits[1].isBlank()) null else bits[0] to bits[1]
        }.toMap()
    }

    fun setAppName(id: String, value: String) {
        val next = names().toMutableMap()
        if (value.isBlank()) next.remove(id) else next[id] = value
        val raw = next.entries.joinToString("\u001e") { "${it.key}\u001f${it.value}" }
        prefs.edit().putString(KEY_NAMES, raw).apply()
    }

    var lastSourceId: String?
        get() = prefs.getString(KEY_SOURCE, null)?.takeIf { it.isNotBlank() }
        set(value) = prefs.edit().putString(KEY_SOURCE, value).apply()

    var photoMode: String
        get() = prefs.getString(KEY_PHOTO_MODE, "off")?.takeIf { it in PhotoLibrary.modes } ?: "off"
        set(value) = prefs.edit().putString(KEY_PHOTO_MODE, value).apply()

    var photoMinutes: Int
        get() = prefs.getInt(KEY_PHOTO_MIN, 15).let { if (it in PhotoLibrary.rotationMinutes) it else 15 }
        set(value) = prefs.edit().putInt(KEY_PHOTO_MIN, value).apply()

    var photoSeconds: Int
        get() = prefs.getInt(KEY_PHOTO_SEC, 20).let { if (it in PhotoLibrary.carouselSeconds) it else 20 }
        set(value) = prefs.edit().putInt(KEY_PHOTO_SEC, value).apply()

    var photoFixedId: String
        get() = prefs.getString(KEY_PHOTO_FIXED, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PHOTO_FIXED, value).apply()

    var photoAsked: Boolean
        get() = prefs.getBoolean(KEY_PHOTO_ASKED, false)
        set(value) = prefs.edit().putBoolean(KEY_PHOTO_ASKED, value).apply()

    fun photos(): List<PhotoEntry> = decodePhotos(prefs.getString(KEY_PHOTOS, null))

    fun savePhotos(entries: List<PhotoEntry>) {
        val raw = entries.joinToString("\u001e") { "${it.id}\u001f${it.uri}\u001f${it.name}\u001f${it.source}" }
        prefs.edit().putString(KEY_PHOTOS, raw).apply()
    }

    fun photoHidden(): Set<String> = prefs.getStringSet(KEY_PHOTO_HIDDEN, emptySet()) ?: emptySet()

    fun hidePhoto(id: String) {
        prefs.edit().putStringSet(KEY_PHOTO_HIDDEN, photoHidden() + id).apply()
    }

    private fun decodePhotos(raw: String?): List<PhotoEntry> {
        return raw.orEmpty().split('\u001e').mapNotNull { part ->
            val bits = part.split('\u001f')
            if (bits.size != 4 || bits[0].isBlank() || bits[1].isBlank()) null
            else PhotoEntry(bits[0], bits[1], bits[2].ifBlank { "Photo" }, bits[3].ifBlank { "picker" })
        }
    }

    fun recent(): List<String> = prefs.getString(KEY_RECENT, null).orEmpty().split(",").filter { it.isNotBlank() }

    fun noteRecent(id: String) {
        val next = (listOf(id) + recent().filter { it != id }).take(40)
        prefs.edit().putString(KEY_RECENT, next.joinToString(",")).apply()
    }

    fun cachedIp(): CachedIp? {
        val bits = prefs.getString(KEY_IP, null)?.split("\u001f") ?: return null
        if (bits.size != 6) return null
        return CachedIp(
            at = bits[0].toLongOrNull() ?: return null,
            city = bits[1],
            latitude = bits[2].toDoubleOrNull() ?: return null,
            longitude = bits[3].toDoubleOrNull() ?: return null,
            country = bits[4],
            timezone = bits[5],
        )
    }

    fun saveIp(city: String, latitude: Double, longitude: Double, country: String, timezone: String, at: Long) {
        val raw = listOf(
            at.toString(),
            city.replace("\u001f", " "),
            latitude.toString(),
            longitude.toString(),
            country,
            timezone,
        ).joinToString("\u001f")
        prefs.edit().putString(KEY_IP, raw).apply()
    }

    /** First launch records every installed package as already known, so nothing is tagged New. */
    fun touchSeen(packages: Set<String>): Map<String, Long> {
        val map = decodeSeen().toMutableMap()
        val now = System.currentTimeMillis()
        if (!prefs.getBoolean(KEY_READY, false)) {
            packages.forEach { map.putIfAbsent(it, 0L) }
            prefs.edit().putBoolean(KEY_READY, true).apply()
        }
        packages.forEach { map.putIfAbsent(it, now) }
        prefs.edit().putString(KEY_SEEN, map.entries.joinToString(";") { "${it.key}=${it.value}" }).apply()
        return map
    }

    private fun decodeSeen(): Map<String, Long> {
        return prefs.getString(KEY_SEEN, null).orEmpty().split(";").mapNotNull { part ->
            val bits = part.split("=")
            if (bits.size != 2) null else bits[0] to (bits[1].toLongOrNull() ?: return@mapNotNull null)
        }.toMap()
    }

    private fun defaultFront() = FRONT_IDS.map { FrontPref(it, it == "weather") }

    private fun translationKey(english: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(english.toByteArray())
        return "tr" + digest.take(8).joinToString("") { "%02x".format(it) }
    }

    private fun defaultClauses() = listOf(
        ClausePref("time", true),
        ClausePref("weather", true),
        ClausePref("event", true),
        ClausePref("watchlist", true),
        ClausePref("now", true),
        ClausePref("custom", false),
    )

    companion object {
        const val DEFAULT_TEMPLATE = "It's {time}. {weather}. Next: {event}."
        const val DEFAULT_TICKERS = ""
        val DEFAULT_APPS = emptyList<String>()
        private const val KEY_SCHEME = "scheme"
        private const val KEY_TEMPLATE = "templateMode"
        private const val KEY_TEMPLATE_TEXT = "template"
        private const val KEY_CUSTOM = "custom"
        private const val KEY_IDLE = "idle"
        private const val KEY_PLATE = "plate"
        private const val KEY_LAST = "lastApp"
        private const val KEY_SEEDED = "seededReal"
        private const val KEY_CLAUSES = "clauses"
        private const val KEY_APPS = "apps"
        private const val KEY_HIDDEN = "hidden"
        private const val KEY_PINS = "pins"
        private const val KEY_OPENED = "opened"
        private const val KEY_SEEN = "seen"
        private const val KEY_READY = "catalogReady"
        private const val KEY_AV = "showAv"
        private const val KEY_TUNER = "showTuner"
        private const val KEY_TICKERS = "tickers"
        private const val KEY_WAVE = "wave"
        private const val KEY_FRONT = "front"
        private const val KEY_RECENT = "recent"
        private const val KEY_NAMES = "appNames"
        private const val KEY_SOURCE = "lastSource"
        private const val KEY_LOC_MODE = "locationMode"
        private const val KEY_MANUAL_CITY = "manualCity"
        private const val KEY_IP = "ipFix"
        private const val KEY_IP_FAIL = "ipFailedAt"
        private const val KEY_PHOTO_MODE = "photoMode"
        private const val KEY_PHOTO_MIN = "photoMinutes"
        private const val KEY_PHOTO_SEC = "photoSeconds"
        private const val KEY_PHOTO_FIXED = "photoFixed"
        private const val KEY_PHOTOS = "photos"
        private const val KEY_PHOTO_HIDDEN = "photoHidden"
        private const val KEY_PHOTO_ASKED = "photoAsked"
        private const val KEY_SHELF = "shelfOrder"
        private const val KEY_SHELF_SEEDED = "shelfSeeded"
        private const val KEY_SECTION = "settingsSection"
        private const val KEY_LAYOUT = "layoutVersion"
        private const val KEY_TICKER_ZH = "tickerChinese"
        private const val KEY_FRONT_PHOTO = "frontPhoto"
        private const val KEY_SLOT_SEC = "slotSeconds"
        private const val KEY_INPUT_NAMES = "inputNames"
        private const val KEY_INPUT_HIDDEN = "inputHidden"
        private val FRONT_IDS = listOf("weather", "stocks", "news")
    }
}

data class FrontPref(val id: String, val enabled: Boolean)

data class CachedIp(
    val at: Long,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val country: String,
    val timezone: String,
)
