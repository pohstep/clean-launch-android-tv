package app.cleanlaunch.tv.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.media.tv.TvInputManager
import app.cleanlaunch.tv.theme.MarkDef

data class HomeApp(
    val id: String,
    val label: String,
    val wordmark: Boolean,
    val packageName: String?,
    val pinned: Boolean = false,
    val isNew: Boolean = false,
    val utility: Boolean = false,
    val glyph: String? = null,
)

data class InstalledApp(val packageName: String, val label: String)

object AppCatalog {
    private val preferred = listOf(
        "com.android.tv.settings",
        "com.android.settings",
        "com.android.tv",
    )

    fun idFor(packageName: String, marks: List<MarkDef>): String {
        val mark = marks.firstOrNull { packageName in it.packages }
        return mark?.id ?: "pkg:$packageName"
    }

    fun canonical(id: String, marks: List<MarkDef>): String {
        if (!id.startsWith("pkg:")) return id
        val pkg = id.removePrefix("pkg:")
        return marks.firstOrNull { pkg in it.packages }?.id ?: id
    }

    /** Installed launchable apps only. One italic row per package. Pins, then last opened, then the rest. */
    fun assemble(
        context: Context,
        marks: List<MarkDef>,
        hidden: Set<String>,
        pins: List<String>,
        recent: List<String>,
        names: Map<String, String> = emptyMap(),
        shelfOrder: List<String> = emptyList(),
    ): List<HomeApp> {
        val blocked = inputPackages(context)
        val installed = LinkedHashMap<String, String>()
        for (resolve in launchables(context)) {
            val pkg = resolve.activityInfo.packageName
            if (pkg == context.packageName || pkg in installed) continue
            val label = resolve.loadLabel(context.packageManager)?.toString() ?: pkg
            if (isTvInput(pkg, label, blocked)) continue
            installed[pkg] = label
        }
        val hiddenIds = hidden.map { canonical(it, marks) }.toSet()
        val rows = installed.map { (pkg, label) ->
            val mark = marks.firstOrNull { pkg in it.packages }
            val id = mark?.id ?: "pkg:$pkg"
            HomeApp(
                id = id,
                label = displayName(id, pkg, label, mark, names),
                wordmark = true,
                packageName = pkg,
                pinned = false,
                isNew = false,
                utility = Shelf.isUtility(pkg, label),
            )
        }.distinctBy { it.id }.filter { canonical(it.id, marks) !in hiddenIds }
        val byId = rows.associateBy { it.id }
        // The X shortcut is not an installed package. Resolve a pinned one in pin
        // order here; an unpinned one stays where SiteShortcuts.place inserts it.
        val pinLookup = byId.toMutableMap()
        SiteShortcuts.row(names, pinned = false)?.takeIf { it.id !in hiddenIds }?.let { shortcut ->
            pinLookup.putIfAbsent(shortcut.id, shortcut)
        }
        val pinRows = pins.map { canonical(it, marks) }.mapNotNull { pinLookup[it] }.distinctBy { it.id }
            .map { it.copy(pinned = true) }
        val pinIds = (pinRows.map { it.id } + pins.map { canonical(it, marks) }).toSet()
        val pool = rows.filter { it.id !in pinIds }
        val ordered = if (shelfOrder.isEmpty()) {
            val recentRows = recent.map { canonical(it, marks) }.mapNotNull { byId[it] }
                .filter { it.id !in pinIds }
                .distinctBy { it.id }
            val used = recentRows.map { it.id }.toSet()
            val rest = pool.filter { it.id !in used }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
            recentRows + rest
        } else {
            val poolById = pool.associateBy { it.id }
            val listed = shelfOrder.map { canonical(it, marks) }.mapNotNull { poolById[it] }.distinctBy { it.id }
            val have = listed.map { it.id }.toSet()
            listed + pool.filter { it.id !in have }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
        }
        val placed = SiteShortcuts.place(pinRows + ordered, hiddenIds, names)
        val pinnedShortcut = SiteShortcuts.ID in pinIds
        return disambiguate(
            if (!pinnedShortcut) placed else placed.map { if (it.id == SiteShortcuts.ID) it.copy(pinned = true) else it },
        )
    }

    /** When two rows share a label, keep the system title and add the package's last part. */
    fun disambiguate(rows: List<HomeApp>): List<HomeApp> {
        val counts = rows.groupingBy { it.label }.eachCount()
        return rows.map { row ->
            if ((counts[row.label] ?: 0) < 2) {
                row
            } else {
                val tail = row.packageName?.substringAfterLast('.')?.takeIf { it.isNotBlank() }
                if (tail == null || row.label.endsWith("· $tail")) row else row.copy(label = "${row.label} · $tail")
            }
        }
    }

    fun resolve(
        context: Context,
        marks: List<MarkDef>,
        order: List<String>,
        names: Map<String, String> = emptyMap(),
    ): List<HomeApp> {
        val installed = installedPackages(context)
        val blocked = inputPackages(context)
        return order.mapNotNull { id ->
            val mark = marks.firstOrNull { it.id == id }
            if (mark != null) {
                val pkg = mark.packages.firstOrNull { it in installed } ?: return@mapNotNull null
                val label = labelOf(context, pkg) ?: mark.label
                if (isTvInput(pkg, label, blocked)) return@mapNotNull null
                HomeApp(id = mark.id, label = displayName(mark.id, pkg, label, mark, names), wordmark = true, packageName = pkg)
            } else if (id.startsWith("pkg:")) {
                val pkg = id.removePrefix("pkg:")
                if (pkg !in installed) return@mapNotNull null
                val label = labelOf(context, pkg) ?: return@mapNotNull null
                if (isTvInput(pkg, label, blocked)) return@mapNotNull null
                HomeApp(id = id, label = displayName(id, pkg, label, null, names), wordmark = true, packageName = pkg)
            } else {
                null
            }
        }
    }

    fun hiddenApps(
        context: Context,
        marks: List<MarkDef>,
        hidden: Set<String>,
        names: Map<String, String> = emptyMap(),
    ): List<HomeApp> {
        return resolve(context, marks, hidden.toList(), names)
    }

    /** Custom rename, then a designed mark, then the English alias, then the system label. */
    fun displayName(
        id: String,
        packageName: String,
        systemLabel: String,
        mark: MarkDef?,
        names: Map<String, String>,
    ): String {
        names[id]?.takeIf { it.isNotBlank() }?.let { return it }
        if (mark != null) return mark.label
        return AppNames.english(packageName, systemLabel)
    }

    fun installedApps(context: Context): List<InstalledApp> {
        val blocked = inputPackages(context)
        return launchables(context).map { resolve ->
            val pkg = resolve.activityInfo.packageName
            InstalledApp(pkg, resolve.loadLabel(context.packageManager)?.toString() ?: pkg)
        }.distinctBy { it.packageName }
            .filter { it.packageName != context.packageName && !isTvInput(it.packageName, it.label, blocked) }
            .sortedBy { it.label.lowercase() }
    }

    fun preferredReal(context: Context): String? {
        val installed = installedPackages(context)
        return preferred.firstOrNull { it in installed && launchIntent(context, it) != null }
    }

    fun launch(context: Context, app: HomeApp): String? {
        SiteShortcuts.find(app.id)?.let { return SiteShortcuts.open(context, it) }
        val pkg = app.packageName ?: return "${app.label} is not installed."
        val intent = launchIntent(context, pkg) ?: return "${app.label} has no screen to open."
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            null
        } catch (_: Throwable) {
            "Could not open ${app.label}."
        }
    }

    private fun launchIntent(context: Context, packageName: String): Intent? {
        val manager = context.packageManager
        return manager.getLeanbackLaunchIntentForPackage(packageName)
            ?: manager.getLaunchIntentForPackage(packageName)
    }

    private fun labelOf(context: Context, packageName: String): String? {
        return try {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(info)?.toString()
        } catch (_: Throwable) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun launchables(context: Context): List<ResolveInfo> {
        val sony = sonyApplicationList(context)
        if (sony.isNotEmpty()) return sony
        val manager = context.packageManager
        val leanback = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return manager.queryIntentActivities(leanback, 0) + manager.queryIntentActivities(launcher, 0)
    }

    /** Optional Sony list. Other televisions keep the PackageManager queries. */
    private fun sonyApplicationList(context: Context): List<ResolveInfo> {
        val manager = context.packageManager
        val method = manager.javaClass.methods.firstOrNull {
            it.name == "getApplicationList" && it.parameterTypes.isEmpty()
        } ?: return emptyList()
        val raw = runCatching { method.invoke(manager) }.getOrNull() ?: return emptyList()
        return (raw as? List<*>)?.filterIsInstance<ResolveInfo>().orEmpty()
    }

    private fun installedPackages(context: Context): Set<String> {
        return launchables(context).map { it.activityInfo.packageName }.toSet()
    }

    private fun inputPackages(context: Context): Set<String> {
        return try {
            val manager = context.getSystemService(TvInputManager::class.java) ?: return emptySet()
            manager.tvInputList.mapNotNull { it.serviceInfo?.packageName }.toSet()
        } catch (_: Throwable) {
            emptySet()
        }
    }

    private fun isTvInput(packageName: String, label: String, reported: Set<String>): Boolean {
        if (packageName in reported) return true
        val pkg = packageName.lowercase()
        if (pkg in INPUT_PACKAGES) return true
        if (pkg.startsWith("com.sony.dtv.") && (pkg.contains("input") || pkg.contains("hdmi") || pkg.contains("tvx"))) {
            return true
        }
        val name = label.trim().lowercase()
        if (name.matches(Regex("^hdmi\\s*\\d+$"))) return true
        return name == "av" || name == "tv tuner" || name == "tuner"
    }

    private val INPUT_PACKAGES = setOf(
        "com.sony.dtv.tvx",
        "com.sony.dtv.smartmediaapp",
        "com.android.tv",
        "com.google.android.tv",
        "com.mediatek.wwtv.tvcenter",
    )
}
