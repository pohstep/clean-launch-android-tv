package app.cleanlaunch.tv

import app.cleanlaunch.tv.location.IpFix
import app.cleanlaunch.tv.location.LocationCheck
import app.cleanlaunch.tv.location.LocationResolve
import app.cleanlaunch.tv.location.Places
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationTest {
    private val ashburn = IpFix("Ashburn", 39.0437, -77.4875, "US", "America/New_York")

    @Test
    fun disagreeingIpLeavesTheCityEmpty() {
        assertFalse(
            LocationCheck.agrees(
                deviceZone = "Europe/Lisbon",
                deviceCountry = "PT",
                ipZone = "America/New_York",
                ipCountry = "US",
                ipZoneCountry = "US",
            ),
        )
        val place = LocationResolve.resolve(
            mode = "auto",
            manualCity = "",
            deviceZone = "Europe/Lisbon",
            deviceCountry = "PT",
            ip = ashburn,
            ipZoneCountry = "US",
        )
        assertEquals("", place.city)
        assertEquals("none", place.source)
        assertTrue(place.latitude.isNaN())
    }

    @Test
    fun matchingIpKeepsItsOwnCity() {
        val ip = IpFix("Beijing", 39.9, 116.4, "CN", "Asia/Beijing")
        val place = LocationResolve.resolve("auto", "", "Asia/Beijing", "CN", ip, "CN")
        assertEquals("Beijing", place.city)
        assertEquals("北京", place.chinese)
        assertEquals("ip", place.source)
        assertEquals(39.9, place.latitude, 0.0001)
    }

    @Test
    fun countryMatchAcceptsADifferentZoneInTheSameCountry() {
        assertTrue(LocationCheck.agrees("Asia/Beijing", "CN", "Asia/Urumqi", "CN", "CN"))
    }

    @Test
    fun manualCityWinsOverTheIp() {
        val place = LocationResolve.resolve("manual", "Osaka", "Europe/Lisbon", "PT", ashburn, "US")
        assertEquals("Osaka", place.city)
        assertEquals("大阪", place.chinese)
        assertEquals("manual", place.source)
    }

    @Test
    fun unknownModeDoesNotInventACity() {
        val place = LocationResolve.resolve("default", "Osaka", "Europe/Lisbon", "PT", ashburn, "US")
        assertEquals("", place.city)
        assertEquals("none", place.source)
    }

    @Test
    fun missingIpLeavesTheCityEmpty() {
        assertNull(Places.forZone("Etc/UTC", "001"))
        assertEquals("London", Places.forZone("Europe/London", "GB")!!.city)
        val place = LocationResolve.resolve("auto", "", "Europe/Lisbon", "PT", null, null)
        assertEquals("", place.city)
        assertEquals("none", place.source)
        assertTrue(place.latitude.isNaN())
    }

    @Test
    fun unknownManualCityKeepsTheNameAndNoCoordinates() {
        val place = LocationResolve.resolve("manual", "Pudong", "Europe/Lisbon", "PT", ashburn, "US")
        assertEquals("Pudong", place.city)
        assertEquals("manual", place.source)
        assertTrue(place.latitude.isNaN())
        assertTrue(place.longitude.isNaN())
    }
}
