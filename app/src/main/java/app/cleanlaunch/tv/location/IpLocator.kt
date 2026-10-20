package app.cleanlaunch.tv.location

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Keyless lookups, in order: ip-api.com, then ipinfo.io, then api.ip.sb.
 * Each call is short so a dead service does not hold the page.
 */
class IpLocator {
    fun locate(): IpFix? {
        val attempts = listOf(
            "http://ip-api.com/json/?fields=status,country,countryCode,city,lat,lon,timezone" to ::ipApi,
            "https://ipinfo.io/json" to ::ipInfo,
            "https://api.ip.sb/geoip" to ::ipSb,
        )
        for ((url, parse) in attempts) {
            val body = fetch(url) ?: continue
            val fix = runCatching { parse(body) }.getOrNull()
            if (fix != null && fix.city.isNotBlank()) {
                Log.i(TAG, "IP lookup via $url -> ${fix.city} ${fix.country} ${fix.timezone}")
                return fix
            }
            Log.i(TAG, "IP lookup empty from $url")
        }
        return null
    }

    internal fun ipApi(body: String): IpFix? {
        val json = JSONObject(body)
        if (json.optString("status") != "success") return null
        val city = json.optString("city")
        if (city.isBlank() || !json.has("lat") || !json.has("lon")) return null
        return IpFix(
            city = city,
            latitude = json.getDouble("lat"),
            longitude = json.getDouble("lon"),
            country = json.optString("countryCode").ifBlank { null },
            timezone = json.optString("timezone").ifBlank { null },
        )
    }

    internal fun ipInfo(body: String): IpFix? {
        val json = JSONObject(body)
        if (json.optBoolean("bogon")) return null
        val city = json.optString("city")
        val loc = json.optString("loc").split(",")
        val latitude = loc.getOrNull(0)?.toDoubleOrNull() ?: return null
        val longitude = loc.getOrNull(1)?.toDoubleOrNull() ?: return null
        if (city.isBlank()) return null
        return IpFix(
            city = city,
            latitude = latitude,
            longitude = longitude,
            country = json.optString("country").ifBlank { null },
            timezone = json.optString("timezone").ifBlank { null },
        )
    }

    internal fun ipSb(body: String): IpFix? {
        val json = JSONObject(body)
        val city = json.optString("city")
        if (city.isBlank() || !json.has("latitude") || !json.has("longitude")) return null
        return IpFix(
            city = city,
            latitude = json.getDouble("latitude"),
            longitude = json.getDouble("longitude"),
            country = json.optString("country_code").ifBlank { null },
            timezone = json.optString("timezone").ifBlank { null },
        )
    }

    private fun fetch(url: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "CleanLaunch/1.0")
                setRequestProperty("Accept", "application/json")
            }
            val code = connection.responseCode
            if (code !in 200..299) {
                Log.i(TAG, "IP lookup $url answered $code")
                return null
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (error: Throwable) {
            Log.i(TAG, "IP lookup failed for $url", error)
            null
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        private const val TAG = "CleanLaunch"
    }
}
