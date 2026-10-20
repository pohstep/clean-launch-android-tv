package app.cleanlaunch.tv

import app.cleanlaunch.tv.input.HdmiNames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HdmiNamesTest {
    @Test
    fun sonyListOrderStillOpensPortNFromTileN() {
        val claims = listOf(
            HdmiNames.Claim("HW7", "HDMI 3 eARC", null, 7),
            HdmiNames.Claim("HW8", "HDMI 4", null, 8),
            HdmiNames.Claim("HW5", "HDMI 1", null, 5),
            HdmiNames.Claim("HW6", "HDMI 2", null, 6),
        )
        val map = HdmiNames.assign(claims)
        assertEquals("HW5", map[1]?.id)
        assertEquals("HW6", map[2]?.id)
        assertEquals("HW7", map[3]?.id)
        assertEquals("HW8", map[4]?.id)
    }

    @Test
    fun aCustomNameDoesNotStealThePort() {
        val port = HdmiNames.port("com.sony.dtv.tvinput/.hdmi.HdmiInputService/HW5", "Apple TV", 5)
        assertEquals(1, port)
        val map = HdmiNames.assign(
            listOf(HdmiNames.Claim("com.sony.dtv.tvinput/.hdmi.HdmiInputService/HW5", "Apple TV", "Apple TV", 5)),
        )
        assertEquals(1, map.keys.single())
        assertEquals("Apple TV", HdmiNames.friendly(1, "Apple TV", "Apple TV", null))
    }

    @Test
    fun anUnidentifiedPortStaysUnassigned() {
        assertNull(HdmiNames.port("com.example.input", "Living room", null))
        assertEquals("HDMI 3", HdmiNames.friendly(3, "", null, null))
        assertEquals("PS5", HdmiNames.friendly(2, "HDMI 2", null, "PS5"))
    }
}
