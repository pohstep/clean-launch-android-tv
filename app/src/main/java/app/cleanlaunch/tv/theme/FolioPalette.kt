package app.cleanlaunch.tv.theme

/**
 * Folio v6 colours for the three schemes.
 * Cream and Night follow SPEC.md. Harbour uses the same roles
 * (background, ink, muted, rule, pencil, wave, dim) in the late-edition blue.
 * Wave stops are the colours drawn on the glyphs. Resting ink is separate.
 */
object FolioPalette {
    data class Stop(val rgb: Int, val at: Float)

    data class Wave(
        val base: List<Stop>,
        val glint: Int,
        val lightGlyphs: Boolean,
    )

    data class Dim(
        val core: Int,
        val mid: Int,
        val edge: Int,
        val glow: Int,
    )

    const val baseMs = 2200
    const val glintMs = 1600
    const val baseDelayMs = 120
    const val glintDelayMs = 420
    val baseEase = floatArrayOf(0.33f, 0f, 0.2f, 1f)
    val glintEase = floatArrayOf(0.45f, 0f, 0.25f, 1f)

    fun wave(schemeId: String): Wave = when (schemeId) {
        "harbour" -> harbourWave
        "night" -> nightWave
        else -> creamWave
    }

    fun dim(schemeId: String): Dim = if (schemeId == "harbour") harbourDim else paperDim

    fun card(schemeId: String): Int = when (schemeId) {
        "harbour" -> 0x163044
        "night" -> 0x221b14
        else -> 0xebe4d5
    }

    fun scrim(schemeId: String): Long = when (schemeId) {
        "harbour" -> 0xD6081018
        "night" -> 0xD6100C09
        else -> 0xD1F3EEE3
    }

    fun keyFill(schemeId: String): Float = if (schemeId == "cream") 0.06f else 0.07f

    fun highlighterAlpha(schemeId: String): Float = if (schemeId == "cream") 1f else 0.26f

    fun hairlineAlpha(schemeId: String): Float = if (schemeId == "cream") 0.22f else 0.20f

    /** Resting ink and paper, used by the contrast check. */
    fun resting(schemeId: String): Pair<Int, Int> = when (schemeId) {
        "harbour" -> 0xe4ebee to 0x0e1a2a
        "night" -> 0xefe3c8 to 0x1a1510
        else -> 0x1c1a17 to 0xf3eee3
    }

    fun muted(schemeId: String): Int = when (schemeId) {
        "harbour" -> 0xc6d3d8
        "night" -> 0xd8c9ab
        else -> 0x524f4a
    }

    private val creamWave = Wave(
        base = listOf(
            Stop(0x1c1a17, 0f),
            Stop(0x1c1a17, 0.25f),
            Stop(0x5e2a14, 0.36f),
            Stop(0x7a2f12, 0.46f),
            Stop(0x6a2240, 0.56f),
            Stop(0x1c1a17, 0.70f),
            Stop(0x1c1a17, 1f),
        ),
        glint = 0x8a3a10,
        lightGlyphs = false,
    )

    /** Night mids sit further from the ivory rest so the sweep reads, and stay light on the brown paper. */
    private val nightWave = Wave(
        base = listOf(
            Stop(0xfff6e2, 0f),
            Stop(0xfff6e2, 0.25f),
            Stop(0xffbf70, 0.36f),
            Stop(0xff9d45, 0.46f),
            Stop(0xff8a72, 0.56f),
            Stop(0xfff6e2, 0.70f),
            Stop(0xfff6e2, 1f),
        ),
        glint = 0xfff1c4,
        lightGlyphs = true,
    )

    private val harbourWave = Wave(
        base = listOf(
            Stop(0xf2fbf6, 0f),
            Stop(0xf2fbf6, 0.25f),
            Stop(0x9fe0cf, 0.36f),
            Stop(0x4eae9a, 0.46f),
            Stop(0x7eb8e6, 0.56f),
            Stop(0xf2fbf6, 0.70f),
            Stop(0xf2fbf6, 1f),
        ),
        glint = 0xd8fff0,
        lightGlyphs = true,
    )

    private val paperDim = Dim(core = 0x3a2a1f, mid = 0x1a1520, edge = 0x07080d, glow = 0x1A8CA0C8)
    private val harbourDim = Dim(core = 0x1a3344, mid = 0x0c1824, edge = 0x060910, glow = 0x1F8EBED2)
}
