package app.cleanlaunch.tv.sentence

import kotlin.math.roundToInt

/**
 * One 1920×1080 grid. Outer margins match the design kit (96×54).
 * The left column is 5/8 of the space between them, then a 64px gutter, then the index.
 */
object PageFrame {
    const val screenW = 1920
    const val screenH = 1080
    const val marginX = 96
    const val marginY = 54
    const val columnGap = 64
    const val contentW = screenW - marginX * 2
    const val leftW = (contentW - columnGap) * 5 / 8
    const val indexW = contentW - columnGap - leftW
    const val indexX = marginX + leftW + columnGap
    /** The left zone splits into two equal columns with the same gutter. */
    const val leadW = (leftW - columnGap) / 2
    const val railW = leftW - columnGap - leadW
    const val railX = marginX + leadW + columnGap
    const val right = marginX + contentW
    const val bottom = screenH - marginY

    const val mastheadY = 48
    const val mastheadH = 84
    const val ruleY = 140
    const val ruleH = 2
    const val bodyTop = 178
    const val zoneGap = 36
    const val sourcesKicker = 934
    const val sourcesRow = 970
    const val sourceChipH = 56
    const val storyGap = 28
}

/**
 * Picks a type size for one front-page column so every English and Chinese line
 * fits between the masthead and Sources. Each column is measured on its own.
 * Sizes are 1080p pixels.
 */
object StoryFit {
    const val preferredEnglish = 44
    const val minimumEnglish = 16
    const val storyTop = PageFrame.bodyTop
    const val sourcesTop = PageFrame.sourcesKicker
    const val sourcesGap = PageFrame.storyGap
    const val leadWidth = PageFrame.leadW
    const val railWidth = PageFrame.railW
    const val sourcesSpan = PageFrame.leftW

    fun availableDesignPx(): Int = sourcesTop - storyTop - sourcesGap

    fun steps(): IntProgression = preferredEnglish downTo minimumEnglish step 2

    fun chinesePx(englishPx: Int): Int = (englishPx * 7 / 8).coerceAtLeast(16)

    /** Line box tall enough that a serif or CJK glyph is not cropped. */
    fun leading(px: Int): Int = (px * 1.45f).roundToInt().coerceAtLeast(px + 6)

    fun blockGap(englishPx: Int): Int = if (englishPx >= 28) 10 else 6

    /** Largest step whose measured height fits. The minimum is used when nothing larger does. */
    fun choose(availablePx: Int, heightAt: (Int) -> Int): Int {
        var chosen = minimumEnglish
        for (size in steps()) {
            chosen = size
            if (heightAt(size) <= availablePx) return size
        }
        return chosen
    }
}
