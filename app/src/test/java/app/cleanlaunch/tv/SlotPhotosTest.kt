package app.cleanlaunch.tv

import app.cleanlaunch.tv.data.SlotPhotos
import app.cleanlaunch.tv.ui.Highlighter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SlotPhotosTest {
    private val names = listOf("a.jpg", "b.jpg", "c.jpg")

    @Test
    fun onePassShowsEveryPhotoOnce() {
        val random = Random(1)
        var deck = SlotPhotos.fresh(names, random)
        val seen = mutableListOf(deck.current)
        repeat(names.size - 1) {
            deck = SlotPhotos.advance(deck, names, random)
            seen += deck.current
        }
        assertEquals(names.toSet(), seen.filterNotNull().toSet())
        assertEquals(names.size, seen.distinct().size)
    }

    @Test
    fun theNextPassDoesNotOpenOnThePhotoJustShown() {
        val random = Random(2)
        var deck = SlotPhotos.fresh(names, random)
        repeat(names.size - 1) { deck = SlotPhotos.advance(deck, names, random) }
        val last = deck.current
        val next = SlotPhotos.advance(deck, names, random)
        assertTrue(next.current != last)
        assertEquals(names.toSet(), next.order.toSet())
    }

    @Test
    fun aNewFileJoinsTheUnseenPartOfThisPass() {
        val deck = SlotPhotos.Deck(listOf("a.jpg", "b.jpg", "c.jpg"), 0)
        val next = SlotPhotos.reconcile(deck, names + "d.jpg", Random(3))
        assertEquals("a.jpg", next.current)
        assertTrue(next.order.drop(1).contains("d.jpg"))
        assertEquals(4, next.order.distinct().size)
    }

    @Test
    fun aMissingCurrentPhotoIsReplaced() {
        val deck = SlotPhotos.reconcile(SlotPhotos.Deck(listOf("gone.jpg", "b.jpg"), 0), listOf("b.jpg"), Random(4))
        assertEquals("b.jpg", deck.current)
    }

    @Test
    fun anEmptyFolderHasNothingToShow() {
        val deck = SlotPhotos.advance(SlotPhotos.Deck(listOf("a.jpg"), 0), emptyList(), Random(5))
        assertEquals(null, deck.current)
        assertEquals("5s", SlotPhotos.intervalLabel(SlotPhotos.DEFAULT_SECONDS))
        assertEquals("10s", SlotPhotos.intervalLabel(10))
        assertEquals("30s", SlotPhotos.intervalLabel(30))
        assertEquals("1 min", SlotPhotos.intervalLabel(60))
        assertEquals("5 min", SlotPhotos.intervalLabel(300))
        assertEquals(0f, SlotPhotos.fade(0), 0.001f)
        assertEquals(1f, SlotPhotos.fade(SlotPhotos.FADE_MS.toLong()), 0.001f)
        assertTrue(SlotPhotos.fade(SlotPhotos.FADE_MS / 2L) in 0.4f..0.6f)
    }
}

class HighlighterTest {
    @Test
    fun theMarkStaysUnderTheGlyphs() {
        val band = Highlighter.band(layoutWidth = 446f, layoutHeight = 78f, glyphWidth = 140f, glyphHeight = 62f)
        assertEquals(160f, band.width, 0.01f)
        assertEquals(-10f, band.left, 0.01f)
        assertEquals(70f, band.pivotX, 0.01f)
        assertTrue(band.top > 20f)
        assertTrue(band.top + band.height < 78f)
    }
}
