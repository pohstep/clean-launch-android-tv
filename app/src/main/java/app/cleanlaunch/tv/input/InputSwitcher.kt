package app.cleanlaunch.tv.input

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.util.Log

data class SourceSlot(
    val id: String,
    val label: String,
    val inputId: String?,
    val detail: String,
    val port: Int = 0,
    val shown: Boolean = true,
)

/**
 * Home shows HDMI 1–4. AV and the TV tuner stay off the page until Settings
 * asks for them. Each slot is wired to a real port when [TvInputManager]
 * reports one. Switching tries the passthrough channel first, then the system
 * input list, then a TV app if one is installed.
 */
class InputSwitcher(private val context: Context) {
    private val manager: TvInputManager? = context.getSystemService(TvInputManager::class.java)

    /** The slot whose port TvInputManager currently reports as tuned, if the API exists. */
    fun tunedSlotId(slots: List<SourceSlot>): String? {
        val tuned = tunedInputIds()
        if (tuned.isEmpty()) return null
        return slots.firstOrNull { it.inputId != null && it.inputId in tuned }?.id
    }

    fun slots(
        showAv: Boolean = false,
        showTuner: Boolean = false,
        names: Map<String, String> = emptyMap(),
        hidden: Set<String> = emptySet(),
    ): List<SourceSlot> {
        val inputs = manager?.tvInputList.orEmpty()
        val hdmi = inputs.filter { it.type == TvInputInfo.TYPE_HDMI }
        val claims = hdmi.map { info ->
            HdmiNames.Claim(
                id = info.id,
                systemLabel = info.loadLabel(context)?.toString().orEmpty(),
                customLabel = info.loadCustomLabel(context)?.toString()?.takeIf { it.isNotBlank() },
                devicePort = devicePort(info),
            )
        }
        val byPort = HdmiNames.assign(claims)
        val av = inputs.filter {
            it.type == TvInputInfo.TYPE_COMPOSITE ||
                it.type == TvInputInfo.TYPE_COMPONENT ||
                it.type == TvInputInfo.TYPE_SCART
        }
        val tuner = inputs.filter { it.type == TvInputInfo.TYPE_TUNER }
        return buildList {
            for (port in 1..4) {
                val claim = byPort[port]
                val info = claim?.let { picked -> hdmi.firstOrNull { it.id == picked.id } }
                add(hdmiSlot(port, info, claim, names, hidden))
            }
            add(other("av", "AV", av.firstOrNull(), showAv, names))
            add(other("tuner", "TV tuner", tuner.firstOrNull(), showTuner, names))
        }
    }

    fun switch(slot: SourceSlot): String {
        if (slot.inputId != null) {
            val info = manager?.getTvInputInfo(slot.inputId)
            if (info != null && info.isPassthroughInput) {
                val uri = TvContract.buildChannelUriForPassthroughInput(slot.inputId)
                if (tryStart(Intent(Intent.ACTION_VIEW, uri))) {
                    return "Opened ${slot.label}."
                }
                Log.i(TAG, "Passthrough view refused for ${slot.inputId}")
            } else if (info != null && openTuner(slot.inputId)) {
                return "Opened ${slot.label}."
            }
        }
        val fallback = openFallback()
        return if (fallback) {
            if (slot.inputId == null) {
                "No ${slot.label} reported. Opened the TV app instead."
            } else {
                "Could not switch ${slot.label} directly. Opened the TV app instead."
            }
        } else if (slot.inputId == null) {
            "No ${slot.label} on this device."
        } else {
            "This device blocked a switch to ${slot.label}."
        }
    }

    private fun tunedInputIds(): Set<String> {
        val tv = manager ?: return emptySet()
        return try {
            val method = tv.javaClass.getMethod("getCurrentTunedInfos")
            val list = method.invoke(tv) as? List<*> ?: return emptySet()
            list.mapNotNull { info ->
                val read = info?.javaClass?.methods?.firstOrNull { it.name == "getInputId" && it.parameterCount == 0 }
                read?.invoke(info) as? String
            }.toSet()
        } catch (_: Throwable) {
            emptySet()
        }
    }

    private fun hdmiSlot(
        port: Int,
        info: TvInputInfo?,
        claim: HdmiNames.Claim?,
        names: Map<String, String>,
        hidden: Set<String>,
    ): SourceSlot {
        val id = "hdmi$port"
        val label = HdmiNames.friendly(port, claim?.systemLabel.orEmpty(), claim?.customLabel, names[id])
        return SourceSlot(
            id = id,
            label = label,
            inputId = info?.id,
            detail = HdmiNames.detail(port, label, info != null),
            port = port,
            shown = id !in hidden,
        )
    }

    private fun other(
        id: String,
        fallback: String,
        info: TvInputInfo?,
        shown: Boolean,
        names: Map<String, String>,
    ): SourceSlot {
        val reported = info?.loadLabel(context)?.toString().orEmpty()
        val label = names[id]?.takeIf { it.isNotBlank() } ?: reported.ifBlank { fallback }
        val detail = if (info == null) "No input reported" else reported.ifBlank { fallback }
        return SourceSlot(id, label, info?.id, detail, port = 0, shown = shown)
    }

    /** HdmiDeviceInfo is a hidden API. The id and the system label are the public path. */
    private fun devicePort(info: TvInputInfo): Int? {
        if (android.os.Build.VERSION.SDK_INT < 28) return null
        return runCatching {
            val hdmi = info.javaClass.getMethod("getHdmiDeviceInfo").invoke(info) ?: return null
            hdmi.javaClass.getMethod("getPortId").invoke(hdmi) as? Int
        }.getOrNull()
    }

    private fun openTuner(inputId: String): Boolean {
        val uri = TvContract.buildChannelsUriForInput(inputId)
        return try {
            context.contentResolver.query(uri, arrayOf(TvContract.Channels._ID), null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return false
                val channelId = cursor.getLong(0)
                tryStart(Intent(Intent.ACTION_VIEW, TvContract.buildChannelUri(channelId)))
            } ?: false
        } catch (error: SecurityException) {
            Log.i(TAG, "Tuner query refused", error)
            false
        }
    }

    private fun openFallback(): Boolean {
        val setup = Intent("android.media.tv.action.SETUP_INPUTS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (tryStart(setup)) return true
        val packages = listOf(
            "com.android.tv",
            "com.google.android.tv",
            "com.sony.dtv.tvx",
            "com.sony.dtv.smartmediaapp",
            "com.mediatek.wwtv.tvcenter",
        )
        val manager = context.packageManager
        for (pkg in packages) {
            val intent = manager.getLeanbackLaunchIntentForPackage(pkg)
                ?: manager.getLaunchIntentForPackage(pkg)
                ?: continue
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (tryStart(intent)) return true
        }
        return false
    }

    private fun tryStart(intent: Intent): Boolean {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val resolved = intent.resolveActivity(context.packageManager) ?: return false
        return try {
            context.startActivity(intent)
            Log.i(TAG, "Started $resolved")
            true
        } catch (error: Throwable) {
            Log.i(TAG, "Start failed for $intent", error)
            false
        }
    }

    companion object {
        private const val TAG = "CleanLaunch"
    }
}
