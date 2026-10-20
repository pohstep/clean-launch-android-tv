package app.cleanlaunch.tv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Cream focus is a pale apricot swipe, an 8px pencil line, and larger high-contrast
 * type. Night and Harbour stay on the glow. The numbers come from tokens.json.
 */
class CreamFocusTest {
    @Test
    fun creamFocusMeetsContrastAndIsNotABlock() {
        val json = tokensJson()
        val evening = slice(json, "\"id\": \"cream\"", "\"id\": \"night\"")
        val night = slice(json, "\"id\": \"night\"", "\"id\": \"harbour\"")
        val harbour = slice(json, "\"id\": \"harbour\"", null)

        assertEquals("highlighter", field(evening, "style"))
        assertEquals("glow", field(night, "style"))
        assertEquals("glow", field(harbour, "style"))
        assertEquals("The Late Edition", field(harbour, "masthead"))
        assertEquals("page dims", field(evening, "idle"))
        assertEquals(8, number(evening, "pencilPx").toInt())
        assertEquals(1.19f, number(evening, "nameScale"), 0.001f)
        assertEquals(1.19f, number(night, "nameScale"), 0.001f)
        assertEquals(1.19f, number(harbour, "nameScale"), 0.001f)
        assertEquals(1f, number(evening, "swipeOpacity"))
        assertEquals(0f, number(evening, "glowOpacity"))
        assertTrue(number(night, "glowOpacity") > 0f)
        assertTrue(number(harbour, "glowOpacity") > 0f)

        val paper = field(evening, "background")
        val swipe = field(evening, "swipe")
        val pencil = field(evening, "pencil")
        val text = field(evening, "text")
        val opacity = number(evening, "swipeOpacity")
        val composite = blend(paper, swipe, opacity)
        val onSwipe = contrast(text, composite)
        val onPaper = contrast(text, paper)
        val pencilOnPaper = contrast(pencil, paper)

        assertTrue(
            "focused text on the apricot swipe was ${"%.2f".format(onSwipe)}:1, want about 11:1 (composite $composite)",
            onSwipe in 10.5f..12.5f,
        )
        assertTrue(
            "focused text on the paper was ${"%.2f".format(onPaper)}:1",
            onPaper >= 11f,
        )
        assertTrue(
            "pencil on the paper was ${"%.2f".format(pencilOnPaper)}:1, need at least 6:1",
            pencilOnPaper >= 6f,
        )
    }

    private fun tokensJson(): String {
        val candidates = listOf(
            File("src/main/assets/design/tokens.json"),
            File("app/src/main/assets/design/tokens.json"),
        )
        return candidates.first { it.exists() }.readText()
    }

    private fun slice(json: String, start: String, end: String?): String {
        val from = json.indexOf(start)
        check(from >= 0) { start }
        val to = if (end == null) json.length else json.indexOf(end, from + start.length)
        check(to > from)
        return json.substring(from, to)
    }

    private fun field(block: String, name: String): String {
        val match = Regex(""""$name"\s*:\s*"([^"]+)"""").find(block)
        return checkNotNull(match) { "$name missing" }.groupValues[1]
    }

    private fun number(block: String, name: String): Float {
        val match = Regex(""""$name"\s*:\s*(-?[0-9.]+)""").find(block)
        return checkNotNull(match) { "$name missing" }.groupValues[1].toFloat()
    }

    private fun blend(background: String, foreground: String, alpha: Float): String {
        fun channel(index: Int): Int {
            val fg = foreground.removePrefix("#").substring(index, index + 2).toInt(16)
            val bg = background.removePrefix("#").substring(index, index + 2).toInt(16)
            return (fg * alpha + bg * (1f - alpha)).toInt()
        }
        return "#%02X%02X%02X".format(channel(0), channel(2), channel(4))
    }

    private fun contrast(first: String, second: String): Float {
        fun luminance(hex: String): Float {
            fun linear(channel: Int): Float {
                val c = channel / 255f
                return if (c <= 0.04045f) c / 12.92f else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
            }
            val clean = hex.removePrefix("#")
            val r = linear(clean.substring(0, 2).toInt(16))
            val g = linear(clean.substring(2, 4).toInt(16))
            val b = linear(clean.substring(4, 6).toInt(16))
            return 0.2126f * r + 0.7152f * g + 0.0722f * b
        }
        val a = luminance(first)
        val b = luminance(second)
        val hi = maxOf(a, b)
        val lo = minOf(a, b)
        return (hi + 0.05f) / (lo + 0.05f)
    }
}
