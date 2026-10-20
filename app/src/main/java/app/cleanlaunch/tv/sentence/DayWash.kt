package app.cleanlaunch.tv.sentence

import kotlin.math.abs

/** Time-of-day paper colours. The evening cream stays close to the printed page. */
object DayWash {
    data class Ink(val paper: Int, val drift: Int, val glow: Int)

    const val cycleMs = 40_000L

    fun ink(schemeId: String, hour: Int): Ink {
        val h = ((hour % 24) + 24) % 24
        return when (schemeId) {
            "night" -> when (h) {
                in 5..8 -> Ink(0x241C16, 0x3A2C20, 0x140E0A)
                in 9..16 -> Ink(0x1E1812, 0x32261C, 0x100C08)
                else -> Ink(0x1A1510, 0x2A2218, 0x100C08)
            }
            "harbour" -> when (h) {
                in 5..8 -> Ink(0x163044, 0x1E4060, 0x0A121C)
                in 9..16 -> Ink(0x12283C, 0x1A3854, 0x0A121C)
                else -> Ink(0x0E1A2A, 0x163044, 0x0A121C)
            }
            else -> when (h) {
                in 5..8 -> Ink(0xF4EFE6, 0xE4D8C6, 0xF7F3EA)
                in 9..15 -> Ink(0xF7F3EA, 0xE7D7B8, 0xF3EEE3)
                in 16..20 -> Ink(0xF3EEE3, 0xE7C9A4, 0xF7F1E4)
                else -> Ink(0xEFE4D4, 0xDCC3A4, 0xF3EEE3)
            }
        }
    }

    fun spread(ink: Ink): Int {
        fun channel(shift: Int): Int = abs(((ink.paper shr shift) and 0xFF) - ((ink.drift shr shift) and 0xFF))
        return maxOf(channel(16), channel(8), channel(0))
    }
}
