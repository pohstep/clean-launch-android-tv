package app.cleanlaunch.tv

import app.cleanlaunch.tv.data.PhotoEntry
import app.cleanlaunch.tv.data.PhotoLibrary
import app.cleanlaunch.tv.data.PhotoScrim
import app.cleanlaunch.tv.data.SkyMark
import app.cleanlaunch.tv.sentence.SkyTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoTest {
    @Test
    fun reorderMovesOneStepAndMergeKeepsPickerRows() {
        val a = PhotoEntry("folder:a.jpg", "file://a", "a.jpg", "folder")
        val b = PhotoEntry("folder:b.jpg", "file://b", "b.jpg", "folder")
        val picked = PhotoEntry("pick:1", "content://picked", "Kept", "picker")
        val moved = PhotoLibrary.reorder(listOf(a, b, picked), "folder:b.jpg", -1)
        assertEquals(listOf("folder:b.jpg", "folder:a.jpg", "pick:1"), moved.map { it.id })
        val found = listOf(b, PhotoEntry("folder:c.jpg", "file://c", "c.jpg", "folder"))
        val merged = PhotoLibrary.merge(listOf(picked, a, b), found, hidden = setOf("folder:a.jpg"))
        assertEquals(listOf("pick:1", "folder:b.jpg", "folder:c.jpg"), merged.map { it.id })
        assertEquals("fixed", PhotoLibrary.nextMode("off"))
        assertEquals(30, PhotoLibrary.nextIn(PhotoLibrary.rotationMinutes, 15))
        assertEquals(10, PhotoLibrary.nextIn(PhotoLibrary.carouselSeconds, 30))
    }

    @Test
    fun theScrimKeepsInkReadableOnABlackOrWhitePhotograph() {
        val schemes = listOf(
            "cream" to (0xF3EEE3 to 0x1C1A17),
            "night" to (0x1A1510 to 0xEFE3C8),
            "harbour" to (0x0E1A2A to 0xE4EBEE),
        )
        schemes.forEach { (id, colors) ->
            val (paper, ink) = colors
            val washes = listOf(
                SkyTone.tint(id, 12, SkyMark.SUN),
                SkyTone.tint(id, 12, SkyMark.DOWNPOUR),
                SkyTone.tint(id, 21, null),
            )
            val alpha = PhotoScrim.cover(paper, ink, washes)
            assertTrue("$id scrim $alpha", alpha < 1f)
            assertTrue("$id scrim $alpha", PhotoScrim.holds(paper, ink, alpha, washes))
            assertTrue(PhotoScrim.contrast(ink, paper) >= PhotoScrim.minimumContrast)
        }
    }
}
