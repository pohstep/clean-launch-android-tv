package app.cleanlaunch.tv.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import app.cleanlaunch.tv.data.SkyMark

@Composable
fun ArrowIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.16f, size.height * 0.34f)
            lineTo(size.width * 0.52f, size.height * 0.34f)
            lineTo(size.width * 0.52f, size.height * 0.16f)
            lineTo(size.width * 0.88f, size.height * 0.50f)
            lineTo(size.width * 0.52f, size.height * 0.84f)
            lineTo(size.width * 0.52f, size.height * 0.66f)
            lineTo(size.width * 0.16f, size.height * 0.66f)
            close()
        }
        drawPath(path, color)
    }
}

@Composable
fun GearIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outer = size.minDimension * 0.46f
        val stroke = size.minDimension * 0.08f
        drawCircle(color, radius = outer * 0.62f, center = center, style = Stroke(stroke))
        drawCircle(color, radius = outer * 0.18f, center = center)
        repeat(8) { tooth ->
            rotate(tooth * 45f, center) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(center.x - stroke * 0.55f, center.y - outer),
                    size = Size(stroke * 1.1f, outer * 0.34f),
                    cornerRadius = CornerRadius(stroke * 0.3f, stroke * 0.3f),
                )
            }
        }
    }
}

@Composable
fun StarIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outer = size.minDimension / 2f
        val inner = outer * 0.42f
        for (point in 0 until 10) {
            val radius = if (point % 2 == 0) outer else inner
            val angle = Math.toRadians(-90.0 + point * 36.0)
            val x = cx + radius * kotlin.math.cos(angle).toFloat()
            val y = cy + radius * kotlin.math.sin(angle).toFloat()
            if (point == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, color)
    }
}

@Composable
fun RecordIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(color, radius = size.minDimension * 0.46f, center = center, style = Stroke(size.minDimension * 0.08f))
        drawCircle(color, radius = size.minDimension * 0.22f, center = center)
    }
}

@Composable
fun KeyboardIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.06f
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.06f, size.height * 0.18f),
            size = Size(size.width * 0.88f, size.height * 0.64f),
            cornerRadius = CornerRadius(size.minDimension * 0.08f, size.minDimension * 0.08f),
            style = Stroke(stroke),
        )
        val keys = listOf(0.18f to 0.36f, 0.38f to 0.36f, 0.58f to 0.36f, 0.78f to 0.36f, 0.28f to 0.56f, 0.48f to 0.56f, 0.68f to 0.56f)
        keys.forEach { (x, y) ->
            drawRoundRect(
                color = color,
                topLeft = Offset(size.width * x, size.height * y),
                size = Size(size.width * 0.1f, size.height * 0.12f),
                cornerRadius = CornerRadius(2f, 2f),
            )
        }
    }
}

@Composable
fun WifiIcon(state: String, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.08f
        val cx = size.width / 2f
        val base = size.height * 0.78f
        if (state == "offline") {
            drawLine(
                color,
                Offset(size.width * 0.22f, size.height * 0.22f),
                Offset(size.width * 0.78f, size.height * 0.78f),
                stroke,
            )
        } else {
            drawCircle(color, radius = stroke, center = Offset(cx, base))
        }
        val arcs = when (state) {
            "weak" -> 1
            "offline" -> 2
            else -> 3
        }
        for (level in 1..arcs) {
            val radius = size.minDimension * (0.18f + level * 0.16f)
            drawArc(
                color = color,
                startAngle = 205f,
                sweepAngle = 130f,
                useCenter = false,
                topLeft = Offset(cx - radius, base - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(stroke),
            )
        }
    }
}

@Composable
fun ScreenIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.08f
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.12f, size.height * 0.14f),
            size = Size(size.width * 0.76f, size.height * 0.52f),
            cornerRadius = CornerRadius(stroke, stroke),
            style = Stroke(stroke),
        )
        drawLine(
            color,
            Offset(size.width * 0.5f, size.height * 0.66f),
            Offset(size.width * 0.5f, size.height * 0.8f),
            stroke,
        )
        drawLine(
            color,
            Offset(size.width * 0.32f, size.height * 0.82f),
            Offset(size.width * 0.68f, size.height * 0.82f),
            stroke,
        )
    }
}

/** Two ink strokes. Same colour as the app names, no brand fill. */
@Composable
fun XGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.12f
        val inset = size.minDimension * 0.16f
        drawLine(
            color,
            Offset(inset, inset),
            Offset(size.width - inset, size.height - inset),
            stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(size.width - inset, inset),
            Offset(inset, size.height - inset),
            stroke,
            cap = StrokeCap.Round,
        )
    }
}

/** Stroke-only sky marks. One weight, the same ink as the sentence. */
@Composable
fun WeatherIcon(sky: SkyMark, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = Stroke(size.minDimension * 0.07f)
        when (sky) {
            SkyMark.SUN -> sun(color, stroke)
            SkyMark.CLOUD -> cloud(color, stroke, 0.52f)
            SkyMark.DRIZZLE -> {
                cloud(color, stroke, 0.40f)
                drops(color, stroke, long = false)
            }
            SkyMark.DOWNPOUR -> {
                cloud(color, stroke, 0.40f)
                drops(color, stroke, long = true)
            }
            SkyMark.STORM -> {
                cloud(color, stroke, 0.40f)
                bolt(color, stroke)
            }
            SkyMark.SNOW -> {
                cloud(color, stroke, 0.40f)
                flakes(color, stroke)
            }
            SkyMark.FOG -> fog(color, stroke)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.sun(color: Color, stroke: Stroke) {
    val c = Offset(size.width / 2f, size.height / 2f)
    val radius = size.minDimension * 0.18f
    drawCircle(color, radius, c, style = stroke)
    val inner = radius + size.minDimension * 0.08f
    val outer = size.minDimension * 0.44f
    for (i in 0 until 8) {
        val angle = Math.toRadians(i * 45.0)
        val cos = kotlin.math.cos(angle).toFloat()
        val sin = kotlin.math.sin(angle).toFloat()
        drawLine(color, Offset(c.x + cos * inner, c.y + sin * inner), Offset(c.x + cos * outer, c.y + sin * outer), stroke.width)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.cloud(color: Color, stroke: Stroke, cy: Float) {
    val y = size.height * cy
    drawCircle(color, size.minDimension * 0.16f, Offset(size.width * 0.38f, y), style = stroke)
    drawCircle(color, size.minDimension * 0.22f, Offset(size.width * 0.58f, y - size.minDimension * 0.06f), style = stroke)
    drawLine(color, Offset(size.width * 0.22f, y + size.minDimension * 0.08f), Offset(size.width * 0.78f, y + size.minDimension * 0.08f), stroke.width)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drops(color: Color, stroke: Stroke, long: Boolean) {
    val len = if (long) 0.22f else 0.12f
    listOf(0.32f, 0.50f, 0.68f).forEach { x ->
        drawLine(
            color,
            Offset(size.width * x, size.height * 0.62f),
            Offset(size.width * (x - 0.06f), size.height * (0.62f + len)),
            stroke.width,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.bolt(color: Color, stroke: Stroke) {
    val path = Path().apply {
        moveTo(size.width * 0.58f, size.height * 0.58f)
        lineTo(size.width * 0.42f, size.height * 0.78f)
        lineTo(size.width * 0.52f, size.height * 0.78f)
        lineTo(size.width * 0.40f, size.height * 0.96f)
    }
    drawPath(path, color, style = stroke)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.flakes(color: Color, stroke: Stroke) {
    listOf(0.34f, 0.52f, 0.70f).forEach { x ->
        val cx = size.width * x
        val cy = size.height * 0.78f
        val r = size.minDimension * 0.06f
        drawLine(color, Offset(cx - r, cy), Offset(cx + r, cy), stroke.width)
        drawLine(color, Offset(cx, cy - r), Offset(cx, cy + r), stroke.width)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.fog(color: Color, stroke: Stroke) {
    listOf(0.32f, 0.50f, 0.68f).forEachIndexed { index, y ->
        val inset = if (index == 1) 0.18f else 0.12f
        drawLine(
            color,
            Offset(size.width * inset, size.height * y),
            Offset(size.width * (1f - inset), size.height * y),
            stroke.width,
        )
    }
}

/** A short sparkline with an arrow head. Up is green, down is red, from the caller. */
@Composable
fun ZigzagIcon(up: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = Stroke(size.minDimension * 0.1f)
        val path = Path()
        if (up) {
            path.moveTo(size.width * 0.12f, size.height * 0.72f)
            path.lineTo(size.width * 0.38f, size.height * 0.46f)
            path.lineTo(size.width * 0.54f, size.height * 0.60f)
            path.lineTo(size.width * 0.78f, size.height * 0.22f)
        } else {
            path.moveTo(size.width * 0.12f, size.height * 0.28f)
            path.lineTo(size.width * 0.38f, size.height * 0.54f)
            path.lineTo(size.width * 0.54f, size.height * 0.40f)
            path.lineTo(size.width * 0.78f, size.height * 0.78f)
        }
        drawPath(path, color, style = stroke)
        val tip = if (up) Offset(size.width * 0.78f, size.height * 0.22f) else Offset(size.width * 0.78f, size.height * 0.78f)
        val wing = size.minDimension * 0.16f
        drawLine(color, tip, Offset(tip.x - wing, tip.y + if (up) wing * 0.15f else -wing * 0.15f), stroke.width)
        drawLine(color, tip, Offset(tip.x - wing * 0.15f, tip.y + if (up) wing else -wing), stroke.width)
    }
}

@Composable
fun BatteryIcon(percent: Int, charging: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = Stroke(size.minDimension * 0.08f)
        val body = Size(size.width * 0.62f, size.height * 0.42f)
        val top = Offset(size.width * 0.14f, size.height * 0.29f)
        drawRoundRect(color, top, body, CornerRadius(stroke.width, stroke.width), style = stroke)
        drawRoundRect(
            color,
            Offset(top.x + body.width - stroke.width * 0.2f, size.height * 0.40f),
            Size(size.width * 0.08f, size.height * 0.20f),
            CornerRadius(2f, 2f),
        )
        val fill = (percent.coerceIn(0, 100) / 100f) * (body.width - stroke.width * 3)
        if (fill > 1f) {
            drawRoundRect(
                color,
                Offset(top.x + stroke.width * 1.4f, top.y + stroke.width * 1.3f),
                Size(fill, body.height - stroke.width * 2.6f),
                CornerRadius(2f, 2f),
            )
        }
        if (charging) {
            drawLine(color, Offset(size.width * 0.48f, size.height * 0.36f), Offset(size.width * 0.40f, size.height * 0.52f), stroke.width)
            drawLine(color, Offset(size.width * 0.40f, size.height * 0.52f), Offset(size.width * 0.50f, size.height * 0.52f), stroke.width)
            drawLine(color, Offset(size.width * 0.50f, size.height * 0.52f), Offset(size.width * 0.42f, size.height * 0.66f), stroke.width)
        }
    }
}
