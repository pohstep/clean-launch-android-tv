package app.cleanlaunch.tv

import app.cleanlaunch.tv.data.HomeApp
import app.cleanlaunch.tv.data.IndexFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class IndexFileTest {
    @Test
    fun pageOrderThenHiddenWithTheNameAlreadyResolved() {
        val visible = listOf(
            HomeApp("pkg:com.example.star", "Kitchen", true, "com.example.star", pinned = true),
            HomeApp("pkg:com.google.android.youtube.tv", "YouTube", true, "com.google.android.youtube.tv"),
        )
        val hidden = listOf(
            HomeApp("pkg:com.example.quiet", "Quiet", true, "com.example.quiet"),
            HomeApp("pkg:com.google.android.youtube.tv", "YouTube", true, "com.google.android.youtube.tv"),
        )
        val rows = IndexFile.rows(visible, hidden)
        assertEquals(listOf("com.example.star", "com.google.android.youtube.tv", "com.example.quiet"), rows.map { it.packageName })
        assertEquals(listOf(0, 1, 2), rows.map { it.order })
        assertEquals("Kitchen", rows[0].name)
        assertTrue(rows[0].favourite)
        assertFalse(rows[0].hidden)
        assertTrue(rows[2].hidden)
        assertFalse(rows[2].favourite)
    }

    @Test
    fun encodeEscapesTheDisplayName() {
        val json = IndexFile.encode(
            listOf(
                app.cleanlaunch.tv.data.IndexRow("com.example.app", "Say \"hi\"\\", 0, false, false),
            ),
        )
        assertTrue(json.contains("\"package\":\"com.example.app\""))
        assertTrue(json.contains("\"name\":\"Say \\\"hi\\\"\\\\\""))
        assertTrue(json.contains("\"order\":0"))
        assertTrue(json.contains("\"hidden\":false"))
        assertTrue(json.contains("\"favourite\":false"))
        val dir = File.createTempFile("index", "dir")
        dir.delete()
        dir.mkdirs()
        val file = IndexFile.write(dir, json)
        assertEquals(json, file.readText())
        dir.deleteRecursively()
    }
}
