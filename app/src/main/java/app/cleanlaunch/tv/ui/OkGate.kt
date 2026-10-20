package app.cleanlaunch.tv.ui

/**
 * OK opens on release, unless that press was a long-press.
 * The release after a long-press must not activate whatever gained focus.
 */
class OkGate {
    private var sawDown = false
    private var longPressed = false

    fun onDown(first: Boolean, isLongPress: Boolean, hasLong: Boolean, onLong: () -> Unit) {
        if (first) {
            longPressed = false
            sawDown = true
        }
        if (hasLong && isLongPress && !longPressed) {
            longPressed = true
            onLong()
        }
    }

    /** @return true when this release should run the click. */
    fun onUp(): Boolean {
        val click = sawDown && !longPressed
        sawDown = false
        longPressed = false
        return click
    }
}
