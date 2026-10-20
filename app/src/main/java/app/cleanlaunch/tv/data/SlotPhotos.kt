package app.cleanlaunch.tv.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.media.ExifInterface
import java.io.File
import kotlin.math.max
import kotlin.random.Random

/**
 * Photographs for the front-page slot. They live in the app's own external
 * files directory, so an adb push needs no storage permission.
 */
object SlotPhotos {
    const val FOLDER = "photos"
    const val SLOT_W = 660
    const val SLOT_H = 384
    const val FADE_MS = 600
    const val DEFAULT_SECONDS = 5
    val INTERVALS = listOf(5, 10, 30, 60, 300)
    const val PUSH_PATH = "/sdcard/Android/data/app.cleanlaunch.tv/files/photos/"
    const val SAMPLE_CAPTION = "Fronalpstock, CC0"

    private val extensions = setOf("jpg", "jpeg", "png", "webp", "heic", "heif")

    data class Deck(val order: List<String>, val cursor: Int) {
        val current: String? get() = order.getOrNull(cursor)
    }

    fun directory(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), FOLDER)
        if (!dir.isDirectory) dir.mkdirs()
        return dir
    }

    fun names(dir: File): List<String> {
        return dir.listFiles()
            ?.filter { file -> file.isFile && file.extension.lowercase() in extensions }
            ?.map { it.name }
            ?.sortedBy { it.lowercase() }
            .orEmpty()
    }

    fun intervalLabel(seconds: Int): String = when (seconds) {
        60 -> "1 min"
        300 -> "5 min"
        else -> "${seconds}s"
    }

    /** Smoothstep so the crossfade eases in and out. */
    fun fade(elapsedMs: Long, durationMs: Int = FADE_MS): Float {
        val linear = (elapsedMs.toFloat() / durationMs.coerceAtLeast(1)).coerceIn(0f, 1f)
        return linear * linear * (3f - 2f * linear)
    }

    fun fresh(names: List<String>, random: Random): Deck {
        if (names.isEmpty()) return Deck(emptyList(), 0)
        return Deck(names.shuffled(random), 0)
    }

    /**
     * Keep the photo on screen, drop files that vanished, and append anything
     * new to the unseen part of this pass.
     */
    fun reconcile(deck: Deck, names: List<String>, random: Random): Deck {
        if (names.isEmpty()) return Deck(emptyList(), 0)
        val available = names.toSet()
        val current = deck.current?.takeIf { it in available }
        if (current == null) return fresh(names, random)
        val shown = deck.order.take(deck.cursor).filter { it in available && it != current }
        val pending = deck.order.drop(deck.cursor + 1).filter { it in available && it != current }
        val known = (shown + current + pending).toSet()
        val added = names.filter { it !in known }.shuffled(random)
        return Deck(shown + current + pending + added, shown.size)
    }

    /** Step to the next unseen photo. A finished pass is shuffled again, and does not open on the photo just shown. */
    fun advance(deck: Deck, names: List<String>, random: Random): Deck {
        val synced = reconcile(deck, names, random)
        if (synced.order.size <= 1) return synced
        if (synced.cursor + 1 < synced.order.size) return synced.copy(cursor = synced.cursor + 1)
        val last = synced.current
        val fresh = names.shuffled(random).toMutableList()
        if (fresh.size > 1 && fresh.first() == last) {
            val swap = (1 until fresh.size).random(random)
            val hold = fresh[0]
            fresh[0] = fresh[swap]
            fresh[swap] = hold
        }
        return Deck(fresh, 0)
    }

    fun decode(context: Context, name: String, maxW: Int, maxH: Int): Bitmap? {
        if (name.isBlank()) {
            return context.assets.open("photos/sample.jpg").use { stream ->
                val raw = BitmapFactory.decodeStream(stream) ?: return null
                centerCrop(raw, maxW, maxH)
            }
        }
        val file = File(directory(context), name)
        if (!file.isFile) return null
        return decodeFile(file, maxW, maxH)
    }

    fun decodeFile(file: File, maxW: Int, maxH: Int): Bitmap? {
        val degrees = exifDegrees(file)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val turned = degrees == 90 || degrees == 270
        val rawW = if (turned) bounds.outHeight else bounds.outWidth
        val rawH = if (turned) bounds.outWidth else bounds.outHeight
        var sample = 1
        while (rawW / (sample * 2) >= maxW && rawH / (sample * 2) >= maxH) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return null
        return centerCrop(rotate(decoded, degrees), maxW, maxH)
    }

    private fun exifDegrees(file: File): Int {
        return runCatching {
            val exif = ExifInterface(file.absolutePath)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_TRANSPOSE -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270, ExifInterface.ORIENTATION_TRANSVERSE -> 270
                else -> 0
            }
        }.getOrDefault(0)
    }

    private fun rotate(src: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return src
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val turned = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        if (turned != src) src.recycle()
        return turned
    }

    private fun centerCrop(src: Bitmap, width: Int, height: Int): Bitmap {
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val scale = max(width.toFloat() / src.width, height.toFloat() / src.height)
        val drawnW = src.width * scale
        val drawnH = src.height * scale
        val left = (width - drawnW) / 2f
        val top = (height - drawnH) / 2f
        Canvas(out).drawBitmap(src, null, RectF(left, top, left + drawnW, top + drawnH), null)
        if (src != out) src.recycle()
        return out
    }
}
