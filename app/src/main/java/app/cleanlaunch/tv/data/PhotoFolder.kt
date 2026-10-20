package app.cleanlaunch.tv.data

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.File

/** Pictures dropped in [PhotoLibrary.FOLDER], including an `adb push` from a computer. */
object PhotoFolder {
    fun directory(): File {
        val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        return File(pictures, "CleanLaunch")
    }

    fun permission(sdk: Int = Build.VERSION.SDK_INT): String {
        return if (sdk >= 33) android.Manifest.permission.READ_MEDIA_IMAGES
        else android.Manifest.permission.READ_EXTERNAL_STORAGE
    }

    fun canRead(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, permission()) == PackageManager.PERMISSION_GRANTED
    }

    fun scan(context: Context): List<PhotoEntry> {
        val fromDisk = disk(directory())
        val fromStore = runCatching { mediaStore(context) }.getOrElse {
            Log.i("CleanLaunch", "Photo store failed: ${it.message}")
            emptyList()
        }
        Log.i("CleanLaunch", "Photo folder ${directory().absolutePath} disk ${fromDisk.size} store ${fromStore.size}")
        val byName = LinkedHashMap<String, PhotoEntry>()
        (fromDisk + fromStore).forEach { entry -> byName.putIfAbsent(entry.id, entry) }
        return byName.values.toList()
    }

    fun displayName(context: Context, uri: Uri): String {
        val projection = arrayOf(android.provider.OpenableColumns.DISPLAY_NAME)
        val queried = runCatching {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
        return queried?.takeIf { it.isNotBlank() } ?: uri.lastPathSegment ?: "Photo"
    }

    fun release(context: Context, uri: String) {
        val parsed = runCatching { Uri.parse(uri) }.getOrNull() ?: return
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                parsed,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }

    private fun disk(dir: File): List<PhotoEntry> {
        val files = dir.listFiles() ?: return emptyList()
        return files.filter { it.isFile && it.name.isImage() }.sortedBy { it.name.lowercase() }.map { file ->
            PhotoEntry(
                id = PhotoLibrary.folderId(file.name),
                uri = Uri.fromFile(file).toString(),
                name = file.name,
                source = "folder",
            )
        }
    }

    private fun mediaStore(context: Context): List<PhotoEntry> {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val nameColumn = MediaStore.Images.Media.DISPLAY_NAME
        val idColumn = MediaStore.Images.Media._ID
        val projection = mutableListOf(idColumn, nameColumn)
        val relative = if (Build.VERSION.SDK_INT >= 29) MediaStore.Images.Media.RELATIVE_PATH else null
        if (relative != null) projection += relative
        val data = MediaStore.Images.Media.DATA
        projection += data
        val selection = if (relative != null) {
            "$relative LIKE ? OR $data LIKE ?"
        } else {
            "$data LIKE ?"
        }
        val args = if (relative != null) {
            arrayOf("Pictures/CleanLaunch/%", "%/Pictures/CleanLaunch/%")
        } else {
            arrayOf("%/Pictures/CleanLaunch/%")
        }
        val found = ArrayList<PhotoEntry>()
        context.contentResolver.query(collection, projection.toTypedArray(), selection, args, "$nameColumn ASC")
            ?.use { cursor ->
                val idAt = cursor.getColumnIndexOrThrow(idColumn)
                val nameAt = cursor.getColumnIndexOrThrow(nameColumn)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameAt) ?: continue
                    if (!name.isImage()) continue
                    val uri = ContentUris.withAppendedId(collection, cursor.getLong(idAt))
                    found += PhotoEntry(PhotoLibrary.folderId(name), uri.toString(), name, "folder")
                }
            }
        return found
    }

    private fun String.isImage(): Boolean {
        val lower = lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
            lower.endsWith(".webp") || lower.endsWith(".gif")
    }
}
