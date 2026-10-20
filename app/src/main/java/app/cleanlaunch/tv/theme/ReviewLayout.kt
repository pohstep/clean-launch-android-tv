package app.cleanlaunch.tv.theme

/**
 * Placeholder sizes from the 21 Oct review.
 * Folio's layout replaces these numbers. Behaviour code should read them from here
 * and not scatter pixel constants through the screens.
 */
object ReviewLayout {
    const val listTop = 190
    const val listBottom = 1026
    const val indexRowPx = 75
    val visibleRows: Int = (listBottom - listTop) / indexRowPx
    const val focusBandFirst = 3
    const val focusBandLast = 9
    const val pinCap = 6
    const val nameWidthPx = 520

    const val sourceTileW = 200
    const val sourceTileH = 88
    const val sourceGap = 24

    const val gridColumns = 6
    const val gridRows = 3
    const val gridTile = 160
    val gridPage: Int = gridColumns * gridRows

    const val menuWidth = 360
    const val menuRow = 64
    const val settingsRow = 72
    const val headingPx = 28
    const val rowTextPx = 26
    const val helpPx = 22
    const val metaPx = 22

    const val headerPx = 22
    const val editPx = 24
    const val bodyPx = 24
    const val headlinePx = 28
    const val glossPx = 24
    const val tickerPx = 24
    const val blockGapPx = 32
    const val maxHeadlines = 2
    const val maxTickers = 3

    const val focusScale = 1f
    const val focusScaleMs = 150
    const val focusOutlinePx = 3
    const val focusRadiusPx = 4

    const val fieldIdleFill = 0x14FFFFFF
    const val fieldStrokePx = 3

    const val idleTimePx = 96
    const val idleWeatherPx = 36

    val sections = listOf(
        "inputs" to "Inputs",
        "apps" to "Apps",
        "front" to "Front page",
        "look" to "Look",
        "saver" to "Screen saver",
        "about" to "About",
    )

    val idleChoices = listOf(0, 5, 10, 30)
}
