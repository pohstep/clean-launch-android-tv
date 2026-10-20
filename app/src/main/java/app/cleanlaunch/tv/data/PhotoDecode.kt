package app.cleanlaunch.tv.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlin.math.max

/** Photographs are kept at television size, never at the camera's full frame. */
object PhotoDecode {
    const val FRAME_W = 1920
    const val FRAME_H = 1080
    const val THUMB_W = 320
    const val THUMB_H = 180

    fun frame(context: Context, uri: String): Bitmap? = decode(context, uri, FRAME_W, FRAME_H)

    fun thumb(context: Context, uri: String): Bitmap? = decode(context, uri, THUMB_W, THUMB_H)

    fun decode(context: Context, uri: String, maxW: Int, maxH: Int): Bitmap? {
        val parsed = runCatching { Uri.parse(uri) }.getOrNull() ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open(context, parsed)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > maxW * 2 || bounds.outHeight / sample > maxH * 2) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val raw = open(context, parsed)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        if (raw.width <= maxW && raw.height <= maxH) return raw
        val scale = minOf(maxW.toFloat() / raw.width, maxH.toFloat() / raw.height)
        val width = max(1, (raw.width * scale).toInt())
        val height = max(1, (raw.height * scale).toInt())
        val scaled = Bitmap.createScaledBitmap(raw, width, height, true)
        if (scaled != raw) raw.recycle()
        return scaled
    }

    private fun open(context: Context, uri: Uri) = runCatching {
        context.contentResolver.openInputStream(uri)
    }.getOrNull()
}
