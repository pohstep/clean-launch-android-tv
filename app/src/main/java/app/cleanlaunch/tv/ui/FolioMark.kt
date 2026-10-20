package app.cleanlaunch.tv.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import app.cleanlaunch.tv.theme.Design
import app.cleanlaunch.tv.theme.FolioPalette
import app.cleanlaunch.tv.theme.SchemeTokens
import app.cleanlaunch.tv.theme.tvDp
import app.cleanlaunch.tv.theme.tvSp

@Composable
fun FolioGlyph(
    text: String,
    focused: Boolean,
    scheme: SchemeTokens,
    design: Design,
    px: Int,
    restingFace: FontFamily,
    restingWeight: FontWeight = FontWeight.Light,
    color: Color = scheme.ink,
    stroke: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val show = focused
    val face = if (show) design.faces.focus else restingFace
    val weight = if (show) FontWeight.Medium else restingWeight
    val ink = if (show && scheme.focus.text != Color.Unspecified) {
        if (scheme.id == "cream") scheme.ink else scheme.focus.text
    } else {
        color
    }
    val mark = scheme.focus.swipe.copy(alpha = FolioPalette.highlighterAlpha(scheme.id))
    val style = TextStyle(
        fontFamily = face,
        fontWeight = weight,
        fontSize = px.tvSp(),
        lineHeight = (px * 1.15f).tvSp(),
    )
    val measured = rememberTextMeasurer().measure(
        text = text,
        style = style,
        overflow = TextOverflow.Clip,
        maxLines = 1,
        softWrap = false,
    )
    val glyphWidth = measured.size.width.toFloat()
    val glyphHeight = measured.size.height.toFloat()
    FocusText(
        text = text,
        focused = show,
        rest = ink,
        dark = FolioPalette.wave(scheme.id).lightGlyphs,
        schemeId = scheme.id,
        modifier = modifier.drawBehind {
            if (!show || !stroke || glyphWidth <= 0f) return@drawBehind
            val band = Highlighter.band(size.width, size.height, glyphWidth, glyphHeight)
            rotate(degrees = Highlighter.TILT, pivot = Offset(band.pivotX, band.pivotY)) {
                drawRect(
                    color = mark,
                    topLeft = Offset(band.left, band.top),
                    size = Size(band.width, band.height),
                )
            }
        },
        style = style,
    )
}

/**
 * The marker sits under the glyphs only. A full-row width, tilted, reads as a
 * bar cutting across the focused name.
 */
internal object Highlighter {
    const val TILT = -4f
    private const val OVERHANG = 10f

    fun band(layoutWidth: Float, layoutHeight: Float, glyphWidth: Float, glyphHeight: Float): Band {
        val width = glyphWidth.coerceIn(0f, layoutWidth.coerceAtLeast(0f))
        val height = glyphHeight.coerceAtLeast(1f).coerceAtMost(layoutHeight.coerceAtLeast(1f))
        val y0 = ((layoutHeight - height) / 2f).coerceAtLeast(0f)
        val top = y0 + height * 0.49f
        val thick = height * 0.45f
        return Band(
            left = -OVERHANG,
            top = top,
            width = width + OVERHANG * 2f,
            height = thick,
            pivotX = width / 2f,
            pivotY = top + thick / 2f,
        )
    }

    data class Band(
        val left: Float,
        val top: Float,
        val width: Float,
        val height: Float,
        val pivotX: Float,
        val pivotY: Float,
    )
}

@Composable
fun PencilTick(scheme: SchemeTokens, modifier: Modifier = Modifier) {
    val color = scheme.focus.pencil
    Canvas(modifier.width(6.tvDp()).fillMaxHeight()) {
        val band = size.height * 0.56f
        val top = (size.height - band) / 2f
        drawRoundRect(
            color = color,
            topLeft = Offset(0f, top),
            size = Size(size.width, band),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f),
        )
    }
}

@Composable
fun FolioHairline(scheme: SchemeTokens, modifier: Modifier = Modifier) {
    val color = scheme.ink.copy(alpha = FolioPalette.hairlineAlpha(scheme.id))
    Box(modifier.height(2.tvDp()).drawBehind { drawRect(color) })
}

fun editionTitle(scheme: SchemeTokens): String = scheme.masthead
