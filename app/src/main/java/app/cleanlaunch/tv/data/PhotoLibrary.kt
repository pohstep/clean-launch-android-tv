package app.cleanlaunch.tv.data

import app.cleanlaunch.tv.sentence.SkyTint
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

data class PhotoEntry(
    val id: String,
    val uri: String,
    val name: String,
    val source: String,
)

/** Ordered list of photographs, plus the scrim that keeps type readable on top of them. */
object PhotoLibrary {
    const val FOLDER = "/sdcard/Pictures/CleanLaunch"
    val rotationMinutes = listOf(5, 15, 30, 60)
    val carouselSeconds = listOf(10, 20, 30)
    val modes = listOf("off", "fixed", "rotation", "carousel")

    fun modeLabel(mode: String): String = when (mode) {
        "fixed" -> "Fixed"
        "rotation" -> "Rotation"
        "carousel" -> "Carousel"
        else -> "Off"
    }

    fun pickId(uri: String): String = "pick:" + sha(uri)

    fun folderId(name: String): String = "folder:$name"

    fun nextMode(mode: String): String {
        val index = modes.indexOf(mode).let { if (it < 0) 0 else it }
        return modes[(index + 1) % modes.size]
    }

    fun nextIn(options: List<Int>, current: Int): Int {
        val index = options.indexOf(current).let { if (it < 0) 0 else it }
        return options[(index + 1) % options.size]
    }

    fun addPicked(current: List<PhotoEntry>, incoming: List<PhotoEntry>): List<PhotoEntry> {
        val have = current.map { it.id }.toSet()
        return current + incoming.filter { it.id !in have && it.uri.isNotBlank() }
    }

    fun reorder(current: List<PhotoEntry>, id: String, direction: Int): List<PhotoEntry> {
        if (direction == 0) return current
        val index = current.indexOfFirst { it.id == id }
        if (index < 0) return current
        val target = (index + direction).coerceIn(0, current.lastIndex)
        if (target == index) return current
        val next = current.toMutableList()
        val item = next.removeAt(index)
        next.add(target, item)
        return next
    }

    /**
     * Folder files that disappeared drop off the page. New ones append.
     * A hidden id stays off the page. Picker rows are kept until removed.
     */
    fun merge(saved: List<PhotoEntry>, folderNow: List<PhotoEntry>, hidden: Set<String>): List<PhotoEntry> {
        val live = folderNow.associateBy { it.id }
        val kept = saved.mapNotNull { entry ->
            if (entry.id in hidden) return@mapNotNull null
            if (entry.source == "folder") live[entry.id] else entry
        }
        val have = kept.map { it.id }.toSet()
        val added = folderNow.filter { it.id !in have && it.id !in hidden }
        return kept + added
    }

    private fun sha(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return digest.take(6).joinToString("") { "%02x".format(it) }
    }
}

/** Paper (or night paper) laid over a photograph. Alpha is the lightest scrim that still clears the type. */
object PhotoScrim {
    const val minimumContrast = 4.5f

    fun cover(paper: Int, ink: Int, washes: List<SkyTint> = emptyList()): Float {
        if (contrast(ink, paper) < minimumContrast) return 1f
        var low = 0f
        var high = 1f
        var best = 1f
        repeat(14) {
            val mid = (low + high) / 2f
            if (holds(paper, ink, mid, washes)) {
                best = mid
                high = mid
            } else {
                low = mid
            }
        }
        return best
    }

    fun holds(paper: Int, ink: Int, alpha: Float, washes: List<SkyTint>): Boolean {
        val samples = if (washes.isEmpty()) listOf(SkyTint(0, 0f)) else washes
        return listOf(0xFFFFFF, 0x000000).all { photo ->
            samples.all { wash ->
                val base = composite(paper, photo, alpha)
                val seen = composite(wash.rgb, base, wash.alpha)
                contrast(ink, seen) >= minimumContrast
            }
        }
    }

    /** [top] laid over [bottom] at [alpha]. */
    fun composite(top: Int, bottom: Int, alpha: Float): Int {
        val cover = alpha.coerceIn(0f, 1f)
        fun channel(shift: Int): Int {
            val a = (top shr shift) and 0xFF
            val b = (bottom shr shift) and 0xFF
            return (a * cover + b * (1f - cover)).toInt().coerceIn(0, 255)
        }
        return (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }

    fun contrast(first: Int, second: Int): Float {
        val a = luminance(first)
        val b = luminance(second)
        return (max(a, b) + 0.05f) / (min(a, b) + 0.05f)
    }

    private fun luminance(rgb: Int): Float {
        fun channel(value: Int): Float {
            val s = value / 255f
            return if (s <= 0.04045f) s / 12.92f else ((s + 0.055f) / 1.055f).pow(2.4f)
        }
        val r = channel((rgb shr 16) and 0xFF)
        val g = channel((rgb shr 8) and 0xFF)
        val b = channel(rgb and 0xFF)
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }
}
