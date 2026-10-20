package app.cleanlaunch.tv.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URI

/**
 * Bundled site shortcuts that sit in the app index.
 *
 * Child-safety rule: this home screen never bundles a shortcut to an adult
 * or explicit site. A bundled URL must be https, its host must be on the
 * small permit list, and neither the host nor the path may carry an adult
 * or explicit term. [bundled] is the only list, and it is checked on load.
 */
object SiteShortcuts {
    const val ID = "x"
    const val LABEL = "X"
    const val URL = "https://x.com"
    const val TV_BRO = "com.phlox.tvwebbrowser"
    const val INDEX_PACKAGE = "shortcut.x"

    data class Bundled(
        val id: String,
        val label: String,
        val url: String,
        val preferredPackage: String,
    )

    val bundled: List<Bundled> = listOf(
        Bundled(ID, LABEL, URL, TV_BRO),
    )

    private val permittedHosts = setOf("x.com", "www.x.com")

    private val blockedFragments = listOf(
        "porn",
        "xxx",
        "nsfw",
        "adult",
        "onlyfans",
        "xvideos",
        "pornhub",
        "xnxx",
        "redtube",
        "xhamster",
        "chaturbate",
        "youporn",
        "spankbang",
        "hentai",
        "erotic",
    )

    private val blockedTokens = setOf("sex", "explicit")

    init {
        check(bundled.all { isPermittedBundle(it.url) }) {
            "A bundled site shortcut failed the child-safety rule."
        }
    }

    fun find(id: String): Bundled? = bundled.firstOrNull { it.id == id }

    /** True when the URL is a normal https page and not an adult or explicit site. */
    fun passesChildSafety(url: String): Boolean {
        val uri = parse(url) ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        val host = uri.host?.lowercase()?.trim('.') ?: return false
        if (host.isEmpty() || host == "localhost" || host.endsWith(".local")) return false
        val path = (uri.path ?: "").lowercase()
        val haystack = "$host$path"
        if (blockedFragments.any { it in haystack }) return false
        val tokens = host.split('.') + path.split('/', '-', '_', '.').filter { it.isNotBlank() }
        return tokens.none { it in blockedTokens }
    }

    /** The closed set that may ship inside the app. Adult URLs fail even if someone adds the host. */
    fun isPermittedBundle(url: String): Boolean {
        if (!passesChildSafety(url)) return false
        val uri = parse(url) ?: return false
        val host = uri.host?.lowercase() ?: return false
        if (host !in permittedHosts) return false
        val path = uri.path ?: ""
        if (path.isNotEmpty() && path != "/") return false
        return uri.query == null && uri.fragment == null
    }

    /**
     * TV Bro when it is installed. Otherwise the first browser that can open
     * the URL. Null means the system should resolve ACTION_VIEW itself.
     */
    fun viewTarget(preferredPackage: String, installed: Set<String>, browsers: List<String>): String? {
        if (preferredPackage in installed) return preferredPackage
        return browsers.firstOrNull { it.isNotBlank() && it != preferredPackage }
    }

    fun row(names: Map<String, String>, pinned: Boolean): HomeApp? {
        val spec = find(ID) ?: return null
        if (!isPermittedBundle(spec.url)) return null
        val renamed = names[spec.id]?.takeIf { it.isNotBlank() }
        return HomeApp(
            id = spec.id,
            label = renamed ?: spec.label,
            wordmark = false,
            packageName = INDEX_PACKAGE,
            pinned = pinned,
            glyph = if (renamed == null || renamed == spec.label) "x" else null,
        )
    }

    /** Pins stay in front. The shortcut is the first of the other apps, unless it is hidden. */
    fun place(rows: List<HomeApp>, hidden: Set<String>, names: Map<String, String>): List<HomeApp> {
        if (ID in hidden || rows.any { it.id == ID }) return rows
        val pinned = rows.any { it.id == ID && it.pinned }
        val entry = row(names, pinned) ?: return rows
        val pins = rows.filter { it.pinned }
        val rest = rows.filter { !it.pinned }
        return pins + listOf(entry) + rest
    }

    fun open(context: Context, spec: Bundled): String? {
        if (!isPermittedBundle(spec.url)) return "That shortcut is not available."
        val view = Intent(Intent.ACTION_VIEW, Uri.parse(spec.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val manager = context.packageManager
        val installed = if (manager.resolveActivity(Intent(view).setPackage(spec.preferredPackage), 0) != null) {
            setOf(spec.preferredPackage)
        } else {
            emptySet()
        }
        val browsers = manager.queryIntentActivities(view, 0)
            .map { it.activityInfo.packageName }
            .filter { it != context.packageName && it != spec.preferredPackage }
            .distinct()
        val target = viewTarget(spec.preferredPackage, installed, browsers)
        val intent = if (target == null) view else Intent(view).setPackage(target)
        return try {
            context.startActivity(intent)
            null
        } catch (_: Throwable) {
            "Could not open ${spec.label}."
        }
    }

    private fun parse(url: String): URI? = runCatching { URI(url) }.getOrNull()?.takeIf { it.host != null }
}
