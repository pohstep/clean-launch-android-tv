package app.cleanlaunch.tv.data

import android.content.Context
import java.io.File

/** One row of the index, including apps hidden from the page. */
data class IndexRow(
    val packageName: String,
    val name: String,
    val order: Int,
    val hidden: Boolean,
    val favourite: Boolean,
)

/**
 * The index the Mac remote reads with adb.
 * Written to `/sdcard/Android/data/app.cleanlaunch.tv/files/index.json`.
 */
object IndexFile {
    const val FILE_NAME = "index.json"

    /** Visible rows keep page order. Hidden rows follow, by name. A package is listed once. */
    fun rows(visible: List<HomeApp>, hidden: List<HomeApp>): List<IndexRow> {
        val seen = LinkedHashSet<String>()
        val out = ArrayList<IndexRow>()
        fun add(app: HomeApp, hiddenFlag: Boolean) {
            val pkg = app.packageName ?: return
            if (!seen.add(pkg)) return
            out += IndexRow(
                packageName = pkg,
                name = app.label,
                order = out.size,
                hidden = hiddenFlag,
                favourite = app.pinned,
            )
        }
        visible.forEach { add(it, false) }
        hidden.sortedBy { it.label.lowercase() }.forEach { add(it, true) }
        return out
    }

    fun encode(rows: List<IndexRow>): String {
        val body = rows.joinToString(",\n") { row ->
            "    {\"package\":${json(row.packageName)},\"name\":${json(row.name)},\"order\":${row.order},\"hidden\":${row.hidden},\"favourite\":${row.favourite}}"
        }
        return "{\n  \"apps\": [\n$body\n  ]\n}\n"
    }

    fun write(context: Context, json: String): File? {
        val dir = context.getExternalFilesDir(null) ?: return null
        return write(dir, json)
    }

    fun write(dir: File, json: String): File {
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, FILE_NAME)
        val tmp = File(dir, "$FILE_NAME.tmp")
        tmp.writeText(json, Charsets.UTF_8)
        if (!tmp.renameTo(file)) {
            file.writeText(json, Charsets.UTF_8)
            tmp.delete()
        }
        return file
    }

    private fun json(value: String): String {
        val out = StringBuilder(value.length + 2)
        out.append('"')
        value.forEach { char ->
            when (char) {
                '\\' -> out.append("\\\\")
                '"' -> out.append("\\\"")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                else -> if (char < ' ') out.append("\\u%04x".format(char.code)) else out.append(char)
            }
        }
        out.append('"')
        return out.toString()
    }
}
