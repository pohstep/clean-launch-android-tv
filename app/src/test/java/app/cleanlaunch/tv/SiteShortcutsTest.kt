package app.cleanlaunch.tv

import app.cleanlaunch.tv.data.HomeApp
import app.cleanlaunch.tv.data.SiteShortcuts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SiteShortcutsTest {
    @Test
    fun bundledShortcutIsOnlyXAndOpensInTvBro() {
        assertEquals(listOf("x"), SiteShortcuts.bundled.map { it.id })
        val spec = SiteShortcuts.bundled.single()
        assertEquals("X", spec.label)
        assertEquals("https://x.com", spec.url)
        assertEquals("com.phlox.tvwebbrowser", spec.preferredPackage)
        assertTrue(SiteShortcuts.isPermittedBundle(spec.url))
    }

    @Test
    fun childSafetyRejectsAdultAndExplicitShortcuts() {
        assertFalse(SiteShortcuts.passesChildSafety("http://x.com"))
        assertFalse(SiteShortcuts.passesChildSafety("https://example.com/adult"))
        assertFalse(SiteShortcuts.passesChildSafety("https://pornhub.com"))
        assertFalse(SiteShortcuts.passesChildSafety("https://www.xvideos.com/video"))
        assertFalse(SiteShortcuts.passesChildSafety("https://onlyfans.com/someone"))
        assertFalse(SiteShortcuts.passesChildSafety("https://news.example/explicit"))
        assertFalse(SiteShortcuts.passesChildSafety("https://sex.example"))
        assertFalse(SiteShortcuts.isPermittedBundle("https://example.com"))
        assertFalse(SiteShortcuts.isPermittedBundle("https://x.com/home"))
        assertTrue(SiteShortcuts.passesChildSafety("https://example.com/forecast"))
        assertTrue(SiteShortcuts.bundled.all { SiteShortcuts.isPermittedBundle(it.url) })
    }

    @Test
    fun tvBroWinsAndAnyBrowserIsTheFallback() {
        assertEquals(
            "com.phlox.tvwebbrowser",
            SiteShortcuts.viewTarget("com.phlox.tvwebbrowser", setOf("com.phlox.tvwebbrowser"), listOf("com.example.browser")),
        )
        assertEquals(
            "com.example.browser",
            SiteShortcuts.viewTarget("com.phlox.tvwebbrowser", emptySet(), listOf("com.example.browser")),
        )
        assertNull(SiteShortcuts.viewTarget("com.phlox.tvwebbrowser", emptySet(), emptyList()))
    }

    @Test
    fun shortcutSitsWithTheAppsUnlessHidden() {
        val apps = listOf(
            HomeApp("netflix", "Netflix", true, "com.netflix.ninja", pinned = true),
            HomeApp("pkg:com.example.notes", "Notes", true, "com.example.notes"),
        )
        val placed = SiteShortcuts.place(apps, emptySet(), emptyMap())
        assertEquals(listOf("netflix", "x", "pkg:com.example.notes"), placed.map { it.id })
        assertEquals("X", placed[1].label)
        assertEquals("x", placed[1].glyph)
        assertEquals("shortcut.x", placed[1].packageName)
        assertFalse(placed[1].pinned)
        assertEquals(apps.map { it.id }, SiteShortcuts.place(apps, setOf("x"), emptyMap()).map { it.id })
    }
}
