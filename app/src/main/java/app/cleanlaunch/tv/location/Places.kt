package app.cleanlaunch.tv.location

data class Place(
    val city: String,
    val chinese: String,
    val latitude: Double,
    val longitude: Double,
    val country: String,
)

data class IpFix(
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val country: String?,
    val timezone: String?,
)

data class ResolvedPlace(
    val city: String,
    val chinese: String,
    val latitude: Double,
    val longitude: Double,
    val source: String,
)

/**
 * A public IP often exits a VPN in another country. Accept it only when its
 * timezone or its country agrees with the television. Otherwise weather stays
 * off until the network agrees or a city is typed. A manual city always wins.
 */
object LocationCheck {
    fun agrees(
        deviceZone: String,
        deviceCountry: String?,
        ipZone: String?,
        ipCountry: String?,
        ipZoneCountry: String?,
    ): Boolean {
        val device = deviceZone.trim()
        val fromIp = ipZone?.trim().orEmpty()
        if (device.isNotEmpty() && fromIp.equals(device, ignoreCase = true)) return true
        val home = country(deviceCountry)
        val seen = country(ipCountry)
        val zoneHome = country(ipZoneCountry)
        if (home != null && seen != null && home == seen) return true
        if (home != null && zoneHome != null && home == zoneHome) return true
        return false
    }

    fun country(raw: String?): String? {
        val code = raw?.trim()?.uppercase() ?: return null
        if (code.length != 2 || code == "ZZ") return null
        return code
    }
}

object Places {
    private val all = listOf(
        place("Shanghai", "上海", 31.2304, 121.4737, "CN"),
        place("Beijing", "北京", 39.9042, 116.4074, "CN"),
        place("Shenzhen", "深圳", 22.5431, 114.0579, "CN"),
        place("Guangzhou", "廣州", 23.1291, 113.2644, "CN"),
        place("Hangzhou", "杭州", 30.2741, 120.1551, "CN"),
        place("Chengdu", "成都", 30.5728, 104.0668, "CN"),
        place("Nanjing", "南京", 32.0603, 118.7969, "CN"),
        place("Wuhan", "武漢", 30.5928, 114.3055, "CN"),
        place("Xi'an", "西安", 34.3416, 108.9398, "CN"),
        place("Suzhou", "蘇州", 31.2989, 120.5853, "CN"),
        place("Chongqing", "重慶", 29.4316, 106.9123, "CN"),
        place("Tianjin", "天津", 39.3434, 117.3616, "CN"),
        place("Xiamen", "廈門", 24.4798, 118.0894, "CN"),
        place("Qingdao", "青島", 36.0671, 120.3826, "CN"),
        place("Dalian", "大連", 38.9140, 121.6147, "CN"),
        place("Shenyang", "瀋陽", 41.8057, 123.4315, "CN"),
        place("Harbin", "哈爾濱", 45.8038, 126.5340, "CN"),
        place("Kunming", "昆明", 25.0389, 102.7183, "CN"),
        place("Changsha", "長沙", 28.2282, 112.9388, "CN"),
        place("Fuzhou", "福州", 26.0745, 119.2965, "CN"),
        place("Hefei", "合肥", 31.8206, 117.2272, "CN"),
        place("Zhengzhou", "鄭州", 34.7466, 113.6254, "CN"),
        place("Jinan", "濟南", 36.6512, 117.1201, "CN"),
        place("Ningbo", "寧波", 29.8683, 121.5440, "CN"),
        place("Wuxi", "無錫", 31.4912, 120.3119, "CN"),
        place("Dongguan", "東莞", 23.0205, 113.7518, "CN"),
        place("Foshan", "佛山", 23.0215, 113.1214, "CN"),
        place("Urumqi", "烏魯木齊", 43.8256, 87.6168, "CN"),
        place("Hong Kong", "香港", 22.3193, 114.1694, "HK"),
        place("Macau", "澳門", 22.1987, 113.5439, "MO"),
        place("Taipei", "台北", 25.0330, 121.5654, "TW"),
        place("Kaohsiung", "高雄", 22.6273, 120.3014, "TW"),
        place("Tokyo", "東京", 35.6762, 139.6503, "JP"),
        place("Osaka", "大阪", 34.6937, 135.5023, "JP"),
        place("Seoul", "首爾", 37.5665, 126.9780, "KR"),
        place("Singapore", "新加坡", 1.3521, 103.8198, "SG"),
        place("London", "倫敦", 51.5074, -0.1278, "GB"),
        place("Paris", "巴黎", 48.8566, 2.3522, "FR"),
        place("Berlin", "柏林", 52.5200, 13.4050, "DE"),
        place("Amsterdam", "阿姆斯特丹", 52.3676, 4.9041, "NL"),
        place("New York", "紐約", 40.7128, -74.0060, "US"),
        place("Los Angeles", "洛杉磯", 34.0522, -118.2437, "US"),
        place("San Francisco", "舊金山", 37.7749, -122.4194, "US"),
        place("Seattle", "西雅圖", 47.6062, -122.3321, "US"),
        place("Chicago", "芝加哥", 41.8781, -87.6298, "US"),
        place("Denver", "丹佛", 39.7392, -104.9903, "US"),
        place("Toronto", "多倫多", 43.6532, -79.3832, "CA"),
        place("Vancouver", "溫哥華", 49.2827, -123.1207, "CA"),
        place("Sydney", "雪梨", -33.8688, 151.2093, "AU"),
        place("Melbourne", "墨爾本", -37.8136, 144.9631, "AU"),
    )

    private val byZone = mapOf(
        "Asia/Beijing" to all.first { it.city == "Beijing" },
        "Asia/Urumqi" to all.first { it.city == "Urumqi" },
        "Asia/Hong_Kong" to all.first { it.city == "Hong Kong" },
        "Asia/Macau" to all.first { it.city == "Macau" },
        "Asia/Taipei" to all.first { it.city == "Taipei" },
        "Asia/Tokyo" to all.first { it.city == "Tokyo" },
        "Asia/Seoul" to all.first { it.city == "Seoul" },
        "Asia/Singapore" to all.first { it.city == "Singapore" },
        "Europe/London" to all.first { it.city == "London" },
        "Europe/Paris" to all.first { it.city == "Paris" },
        "Europe/Berlin" to all.first { it.city == "Berlin" },
        "Europe/Amsterdam" to all.first { it.city == "Amsterdam" },
        "America/New_York" to all.first { it.city == "New York" },
        "America/Los_Angeles" to all.first { it.city == "Los Angeles" },
        "America/Denver" to all.first { it.city == "Denver" },
        "America/Chicago" to all.first { it.city == "Chicago" },
        "America/Toronto" to all.first { it.city == "Toronto" },
        "America/Vancouver" to all.first { it.city == "Vancouver" },
        "Australia/Sydney" to all.first { it.city == "Sydney" },
        "Australia/Melbourne" to all.first { it.city == "Melbourne" },
    )

    private val byCountry = mapOf(
        "HK" to all.first { it.city == "Hong Kong" },
        "MO" to all.first { it.city == "Macau" },
        "TW" to all.first { it.city == "Taipei" },
        "JP" to all.first { it.city == "Tokyo" },
        "KR" to all.first { it.city == "Seoul" },
        "SG" to all.first { it.city == "Singapore" },
        "GB" to all.first { it.city == "London" },
        "FR" to all.first { it.city == "Paris" },
        "DE" to all.first { it.city == "Berlin" },
        "NL" to all.first { it.city == "Amsterdam" },
        "US" to all.first { it.city == "New York" },
        "CA" to all.first { it.city == "Toronto" },
        "AU" to all.first { it.city == "Sydney" },
    )

    private val byName: Map<String, Place> = buildMap {
        all.forEach { place ->
            put(place.city.lowercase(), place)
            put(place.chinese, place)
        }
        put("xi'an", all.first { it.city == "Xi'an" })
        put("xian", all.first { it.city == "Xi'an" })
        put("hongkong", all.first { it.city == "Hong Kong" })
        put("macao", all.first { it.city == "Macau" })
    }

    fun forZone(zone: String, country: String?): Place? {
        byZone[zone]?.let { return it }
        val code = LocationCheck.country(country)
        if (code != null) byCountry[code]?.let { return it }
        return null
    }

    fun none(): ResolvedPlace = ResolvedPlace("", "", Double.NaN, Double.NaN, "none")

    fun match(text: String): Place? = byName[text.trim().lowercase()] ?: byName[text.trim()]

    private fun place(city: String, chinese: String, latitude: Double, longitude: Double, country: String) =
        Place(city, chinese, latitude, longitude, country)
}

object LocationResolve {
    fun resolve(
        mode: String,
        manualCity: String,
        deviceZone: String,
        deviceCountry: String?,
        ip: IpFix?,
        ipZoneCountry: String?,
    ): ResolvedPlace {
        if (mode == "manual") return manual(manualCity)
        if (ip != null && ip.city.isNotBlank() && LocationCheck.agrees(
                deviceZone,
                deviceCountry,
                ip.timezone,
                ip.country,
                ipZoneCountry,
            )
        ) {
            val known = Places.match(ip.city)
            return ResolvedPlace(
                city = known?.city ?: ip.city,
                chinese = known?.chinese ?: ip.city,
                latitude = ip.latitude,
                longitude = ip.longitude,
                source = "ip",
            )
        }
        return Places.none()
    }

    private fun manual(typed: String): ResolvedPlace {
        val text = typed.trim()
        if (text.isEmpty()) return Places.none()
        val known = Places.match(text)
        if (known != null) return from(known, "manual")
        return ResolvedPlace(text, text, Double.NaN, Double.NaN, "manual")
    }

    private fun from(place: Place, source: String) = ResolvedPlace(
        place.city,
        place.chinese,
        place.latitude,
        place.longitude,
        source,
    )
}
