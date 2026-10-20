package app.cleanlaunch.tv.data

import app.cleanlaunch.tv.theme.ReviewLayout

/** Which apps belong on the front, and how the front list windows its rows. */
object Shelf {
    val defaultPackages: List<List<String>> = emptyList()

    fun defaultPinIds(installed: Set<String>, idFor: (String) -> String): List<String> {
        return defaultPackages.mapNotNull { options ->
            options.firstOrNull { it in installed }?.let(idFor)
        }.distinct().take(ReviewLayout.pinCap)
    }

    fun isUtility(packageName: String, label: String): Boolean {
        val pkg = packageName.lowercase()
        if (pkg in UTILITY_PACKAGES) return true
        if (pkg.contains("clash")) return true
        val name = label.lowercase()
        if ("tv remote" in name || "remote service" in name) return true
        if ("projectivy" in name) return true
        if ("通知" in label || name == "notifications") return true
        if ("应用助手" in label || "應用助手" in label) return true
        if ("计时" in label || "計時" in label || "时钟" in label || "時鐘" in label) return true
        return false
    }

    /** Focus stays on rows 3–9 of the window. The window moves. */
    fun windowStart(count: Int, focus: Int): Int {
        val visible = ReviewLayout.visibleRows
        if (count <= visible) return 0
        val bandEnd = ReviewLayout.focusBandLast - 1
        val maxStart = count - visible
        return (focus - bandEnd).coerceIn(0, maxStart)
    }

    /** "12–26 ↓" when rows sit below the window. Null when the last row is on screen. */
    fun moreBelow(start: Int, count: Int): String? {
        val firstHidden = start + ReviewLayout.visibleRows
        if (firstHidden >= count) return null
        return "${firstHidden + 1}–$count ↓"
    }

    fun pressesFromTop(index: Int): Int = index.coerceAtLeast(0)

    private val UTILITY_PACKAGES = setOf(
        "com.google.android.tv.remote.service",
        "com.google.android.tv.remote",
        "com.spocky.projengmenu",
        "com.sony.dtv.notificationcenter",
        "com.sony.dtv.bravialibrary",
        "com.android.deskclock",
        "com.google.android.deskclock",
    )
}
