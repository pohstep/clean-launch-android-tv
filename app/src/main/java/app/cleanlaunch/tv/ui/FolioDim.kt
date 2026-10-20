package app.cleanlaunch.tv.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.em
import app.cleanlaunch.tv.UiState
import app.cleanlaunch.tv.theme.Design
import app.cleanlaunch.tv.theme.FolioPalette
import app.cleanlaunch.tv.theme.SchemeTokens
import app.cleanlaunch.tv.theme.tvDp
import app.cleanlaunch.tv.theme.tvSp
import kotlin.math.max
import kotlin.math.roundToInt

/** Where the dim clock sits: inside the overscan margin, bottom-aligned to its real height. */
internal object DimPlacement {
    const val SAFE_FRACTION = 0.05f

    fun insetPx(spanPx: Int, minimumPx: Int): Int {
        val overscan = (spanPx * 5 + 99) / 100
        return max(overscan, minimumPx)
    }

    fun fitScale(blockPx: Int, availablePx: Int): Float {
        if (blockPx <= 0 || availablePx <= 0) return 1f
        return if (blockPx <= availablePx) 1f else availablePx.toFloat() / blockPx
    }

    fun bottomAlignedTop(availablePx: Int, blockPx: Int, scale: Float): Int {
        val used = (blockPx * scale).roundToInt()
        return (availablePx - used).coerceAtLeast(0)
    }
}

@Composable
fun FolioDim(state: UiState, design: Design, scheme: SchemeTokens) {
    val wash = FolioPalette.dim(scheme.id)
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(solid(wash.edge))
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(solid(wash.core), solid(wash.mid), solid(wash.edge)),
                    center = Offset(size.width * 0.22f, size.height * 0.78f),
                    radius = size.width * 0.7f,
                ),
            )
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val left = safeInset(maxWidth, design.layout.safeHorizontalPx.tvDp())
            val vertical = safeInset(maxHeight, design.layout.safeVerticalPx.tvDp())
            AnchoredDimCopy(
                Modifier.padding(start = left, end = left, top = vertical, bottom = vertical).fillMaxSize(),
            ) {
                DimCopy(state, design)
            }
        }
    }
}

@Composable
private fun safeInset(span: Dp, minimum: Dp): Dp = if (span * DimPlacement.SAFE_FRACTION > minimum) span * DimPlacement.SAFE_FRACTION else minimum

@Composable
private fun DimCopy(state: UiState, design: Design) {
    val line = dimLine(state)
    val cjk = line.second.any { it.code > 0x2E80 }
    Column(Modifier.fillMaxWidth()) {
        BasicText(
            state.timeLabel,
            style = dimStyle(Color.White, design.faces.display, FontWeight.Light, 300, 1.15f),
        )
        if (line.first.isNotBlank()) {
            BasicText(
                line.first,
                style = dimStyle(Color(0xFFF2EDE4), design.faces.ui, FontWeight.Medium, 44, 1.45f),
            )
        }
        if (line.second.isNotBlank()) {
            BasicText(
                line.second,
                style = dimStyle(
                    Color(0xFFCFC8BD),
                    if (cjk) design.faces.chinese else design.faces.ui,
                    FontWeight.Medium,
                    44,
                    if (cjk) 1.6f else 1.45f,
                ),
            )
        }
    }
}

@Composable
private fun dimStyle(color: Color, face: FontFamily, weight: FontWeight, px: Int, lineEm: Float) = TextStyle(
    color = color,
    fontFamily = face,
    fontWeight = weight,
    fontSize = px.tvSp(),
    lineHeight = lineEm.em,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    ),
    platformStyle = PlatformTextStyle(includeFontPadding = true),
)

/** Measures the clock and both weather lines, then keeps the bottom of that box inside the safe area. */
@Composable
private fun AnchoredDimCopy(modifier: Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val placeable = measurables.first().measure(
            constraints.copy(minWidth = 0, minHeight = 0, maxHeight = Constraints.Infinity),
        )
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val scale = DimPlacement.fitScale(placeable.height, height)
        val top = DimPlacement.bottomAlignedTop(height, placeable.height, scale)
        layout(width, height) {
            placeable.placeWithLayer(0, top) {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }
}

private fun solid(rgb: Int): Color = Color(0xFF000000L or (rgb.toLong() and 0xFFFFFF))

private fun dimLine(state: UiState): Pair<String, String> {
    val forecast = state.forecast
    val first = forecast?.short?.takeIf { it.isNotBlank() } ?: state.weatherShort
    val english = forecast?.english.orEmpty()
    val second = when {
        "tomorrow" in english.lowercase() -> english.substringAfterLast(",").trim().replaceFirstChar { it.uppercase() }
        forecast?.chinese?.isNotBlank() == true -> forecast.chinese
        else -> ""
    }
    return first to second
}
