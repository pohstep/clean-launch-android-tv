package app.cleanlaunch.tv.nav

/**
 * Back always walks toward the home screen.
 * On the home screen, with the highlight already on the last-used app, Back dims the page.
 * A key while the page is dim wakes it and puts the highlight back on that app.
 */
data class BackState(
    val dimmed: Boolean = false,
    val menu: Boolean = false,
    val overlay: Boolean = false,
    val onLastUsed: Boolean = false,
    val rename: Boolean = false,
    val moving: Boolean = false,
    val allApps: Boolean = false,
    val settingsPanel: Boolean = false,
)

data class BackResult(
    val dimmed: Boolean,
    val menu: Boolean,
    val overlay: Boolean,
    val focusHome: Boolean,
    val rename: Boolean = false,
    val moving: Boolean = false,
    val allApps: Boolean = false,
    val settingsPanel: Boolean = false,
)

fun reduceBack(state: BackState): BackResult {
    if (state.dimmed) {
        return BackResult(dimmed = false, menu = false, overlay = false, focusHome = true)
    }
    if (state.rename) return kept(state, rename = false)
    if (state.moving) return kept(state, moving = false)
    if (state.menu) return kept(state, menu = false)
    if (state.allApps) return kept(state, allApps = false)
    if (state.settingsPanel) return kept(state, settingsPanel = false)
    if (state.overlay) {
        return BackResult(dimmed = false, menu = false, overlay = false, focusHome = true)
    }
    if (!state.onLastUsed) {
        return BackResult(dimmed = false, menu = false, overlay = false, focusHome = true)
    }
    return BackResult(dimmed = true, menu = false, overlay = false, focusHome = false)
}

private fun kept(
    state: BackState,
    rename: Boolean = state.rename,
    moving: Boolean = state.moving,
    menu: Boolean = state.menu,
    allApps: Boolean = state.allApps,
    settingsPanel: Boolean = state.settingsPanel,
): BackResult {
    return BackResult(
        dimmed = false,
        menu = menu,
        overlay = state.overlay,
        focusHome = false,
        rename = rename,
        moving = moving,
        allApps = allApps,
        settingsPanel = settingsPanel,
    )
}

fun wake(): BackResult = BackResult(dimmed = false, menu = false, overlay = false, focusHome = true)
