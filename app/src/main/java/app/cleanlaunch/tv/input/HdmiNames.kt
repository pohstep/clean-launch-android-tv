package app.cleanlaunch.tv.input

/**
 * Tile N is physical port N. The TvInputManager list order is not the port order.
 * A custom name such as "Apple TV" is never read as a port number.
 */
object HdmiNames {
    data class Claim(
        val id: String,
        val systemLabel: String,
        val customLabel: String?,
        val devicePort: Int?,
    )

    fun assign(claims: List<Claim>): Map<Int, Claim> {
        val chosen = linkedMapOf<Int, Pair<Boolean, Claim>>()
        for (claim in claims) {
            val port = port(claim.id, claim.systemLabel, claim.devicePort) ?: continue
            val explicit = hdmiNumber(claim.id) != null || hdmiNumber(claim.systemLabel) != null
            val current = chosen[port]
            if (current == null || (explicit && !current.first)) {
                chosen[port] = explicit to claim
            }
        }
        return chosen.mapValues { it.value.second }
    }

    fun port(id: String, systemLabel: String, devicePort: Int?): Int? {
        hdmiNumber(id)?.let { return it }
        hdmiNumber(systemLabel)?.let { return it }
        hwPort(id)?.let { return it }
        return when {
            devicePort == null -> null
            devicePort in 1..4 -> devicePort
            devicePort in 5..8 -> devicePort - 4
            else -> null
        }
    }

    fun friendly(port: Int, systemLabel: String, customLabel: String?, userLabel: String?): String {
        userLabel?.takeIf { it.isNotBlank() }?.let { return it }
        customLabel?.takeIf { it.isNotBlank() }?.let { return it }
        val system = systemLabel.trim()
        if (system.isNotBlank()) return system
        return "HDMI $port"
    }

    fun detail(port: Int, label: String, reported: Boolean): String {
        if (!reported) return "No input reported"
        val portName = "HDMI $port"
        return if (label.equals(portName, ignoreCase = true)) portName else "$portName · $label"
    }

    private fun hwPort(id: String): Int? {
        val number = Regex("(?i)(?:^|[^A-Z0-9])HW(\\d+)(?:[^0-9]|$)").find(id)?.groupValues?.get(1)?.toInt()
            ?: return null
        return when (number) {
            in 1..4 -> number
            in 5..8 -> number - 4
            else -> null
        }
    }

    private fun hdmiNumber(text: String): Int? {
        return Regex("(?i)hdmi\\s*[_-]?([1-4])").find(text)?.groupValues?.get(1)?.toInt()
    }
}
