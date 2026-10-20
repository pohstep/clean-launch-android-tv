package app.cleanlaunch.tv.ui

import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.em
import androidx.compose.ui.viewinterop.AndroidView
import app.cleanlaunch.tv.theme.Design
import app.cleanlaunch.tv.theme.ReviewLayout
import app.cleanlaunch.tv.theme.SchemeTokens
import app.cleanlaunch.tv.theme.tvDp
import app.cleanlaunch.tv.theme.tvSp
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
class KeyGate {
    private val gate = OkGate()

    fun onKey(event: KeyEvent, onClick: () -> Unit, onLong: (() -> Unit)?): Boolean {
        val ok = event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter
        if (!ok) return false
        if (event.type == KeyEventType.KeyDown) {
            gate.onDown(
                first = event.nativeKeyEvent.repeatCount == 0,
                isLongPress = event.nativeKeyEvent.isLongPress,
                hasLong = onLong != null,
                onLong = { onLong?.invoke() },
            )
            return true
        }
        if (event.type == KeyEventType.KeyUp) {
            if (gate.onUp()) onClick()
            return true
        }
        return false
    }
}

/** False while a rename keyboard is up, so the page behind it cannot take the D-pad. */
val LocalPageFocus = staticCompositionLocalOf { true }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.tvFocus(
    requester: FocusRequester,
    onFocusChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLong: (() -> Unit)? = null,
    toLeft: FocusRequester? = null,
    toRight: FocusRequester? = null,
    toUp: FocusRequester? = null,
    toDown: FocusRequester? = null,
    onKey: ((KeyEvent) -> Boolean)? = null,
    enabled: Boolean = true,
): Modifier {
    val gate = remember { KeyGate() }
    val bring = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    val pageFocus = LocalPageFocus.current
    return this
        .bringIntoViewRequester(bring)
        .focusRequester(requester)
        .focusProperties {
            canFocus = pageFocus && enabled
            toLeft?.let { left = it }
            toRight?.let { right = it }
            toUp?.let { up = it }
            toDown?.let { down = it }
        }
        .onFocusChanged { focus ->
            onFocusChange(focus.isFocused)
            if (focus.isFocused) scope.launch { bring.bringIntoView() }
        }
        .onKeyEvent { event ->
            if (onKey?.invoke(event) == true) true else gate.onKey(event, onClick, onLong)
        }
        .focusable()
}

/** Mix [color] toward [dusk]. [remain] is how much of [color] is left at full dim. */
fun idleTone(color: Color, dusk: Color, dim: Float, remain: Float): Color {
    if (dim <= 0.001f) return color
    val keep = (1f - dim * (1f - remain)).coerceIn(0f, 1f)
    return lerp(dusk, color, keep)
}

/**
 * Text on the idle page. Dark ink on a dark dusk would vanish, so a colour
 * darker than the dusk fades toward the paper instead, at [remain] of that light.
 */
fun idleReadable(scheme: SchemeTokens, color: Color, dim: Float, remain: Float): Color {
    if (dim <= 0.001f) return color
    val anchor = if (luma(color) + 0.04f >= luma(scheme.idleDusk)) color else scheme.background
    return idleTone(anchor, scheme.idleDusk, dim, remain)
}

private fun luma(color: Color): Float {
    return 0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue
}

@Composable
fun PaperBackground(scheme: SchemeTokens, dim: Float = 0f) {
    val amount = dim.coerceIn(0f, 1f)
    val ground = lerp(scheme.background, scheme.idleDusk, amount)
    Box(Modifier.fillMaxSize().background(ground)) {
        if (!scheme.flatPaper && amount < 0.98f) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f * (1f - amount))),
                        center = Offset(size.width * 0.5f, size.height * 0.5f),
                        radius = size.maxDimension * 0.85f,
                    ),
                )
            }
        }
    }
}

/** Left-margin position mark: the cream pencil, or the night and harbour edge. */
@Composable
fun FocusChrome(focused: Boolean, scheme: SchemeTokens) {
    if (!focused) return
    val focus = scheme.focus
    val highlighter = focus.style == "highlighter"
    val width = if (highlighter) focus.pencilPx else focus.edgePx
    val color = if (highlighter) focus.pencil else focus.edge
    if (width <= 0) return
    Box(Modifier.width(width.tvDp()).fillMaxHeight().background(color))
}

/** Full-row wash used by the glow schemes. Cream does not draw this. */
@Composable
fun GlowWash(scheme: SchemeTokens, modifier: Modifier = Modifier) {
    val focus = scheme.focus
    Box(
        modifier.background(
            Brush.horizontalGradient(
                listOf(focus.glow.copy(alpha = focus.glowOpacity), focus.glow.copy(alpha = 0f)),
            ),
        ),
    )
}

fun focusMargin(scheme: SchemeTokens): Int {
    val focus = scheme.focus
    return if (focus.style == "highlighter") focus.pencilPx else focus.edgePx
}

@Composable
fun FocusSlot(
    focused: Boolean,
    scheme: SchemeTokens,
    modifier: Modifier = Modifier,
    fill: Boolean = false,
    rowHeight: Dp? = null,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val sized = if (rowHeight != null) modifier.height(rowHeight) else modifier.height(IntrinsicSize.Min)
    val mark = focusMargin(scheme)
    val outline = if (scheme.focus.style == "highlighter") scheme.focus.pencil else scheme.focus.edge
    Box(
        sized.then(
            if (focused) {
                Modifier.border(
                    ReviewLayout.focusOutlinePx.tvDp(),
                    outline,
                    RoundedCornerShape(ReviewLayout.focusRadiusPx.tvDp()),
                )
            } else {
                Modifier
            },
        ),
    ) {
        Row(
            Modifier
                .fillMaxHeight()
                .then(if (fill) Modifier.fillMaxWidth() else Modifier),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(mark.tvDp()).fillMaxHeight()) {
                if (focused) FocusChrome(true, scheme)
            }
            if (mark > 0) Spacer(Modifier.width(12.tvDp()))
            content()
        }
    }
}

@Composable
private fun HighlighterBand(scheme: SchemeTokens, modifier: Modifier) {
    val swipe = scheme.focus.swipe
    Canvas(modifier.clipToBounds()) {
        rotate(degrees = -2f, pivot = Offset(size.width / 2f, size.height / 2f)) {
            drawRect(
                color = swipe,
                topLeft = Offset(-12f, size.height * 0.16f),
                size = androidx.compose.ui.geometry.Size(size.width + 24f, size.height * 0.70f),
            )
        }
    }
}

@Composable
fun NameText(
    text: String,
    focused: Boolean,
    scheme: SchemeTokens,
    design: Design,
    basePx: Int,
    serif: Boolean,
    italic: Boolean,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    grow: Boolean = true,
    underlineOnFocus: Boolean = false,
) {
    val focus = scheme.focus
    val scale by animateFloatAsState(
        targetValue = if (focused && grow) ReviewLayout.focusScale else 1f,
        animationSpec = tween(ReviewLayout.focusScaleMs),
        label = "focusScale",
    )
    val color = if (focused) focus.text else (tint ?: scheme.ink)
    val glow = if (focused && grow && focus.glowOpacity > 0f) {
        Shadow(color = focus.glow.copy(alpha = 0.55f), offset = Offset.Zero, blurRadius = 14f)
    } else {
        null
    }
    val face = when {
        focused && serif && italic -> design.faces.focusItalic
        focused && serif -> design.faces.focus
        serif && italic -> design.faces.headlineItalic
        serif -> design.faces.headline
        else -> design.faces.ui
    }
    val weight = when {
        focused && serif -> FontWeight.Medium
        serif -> FontWeight.Light
        else -> FontWeight.Normal
    }
    FocusText(
        text = text,
        focused = focused,
        rest = color,
        dark = scheme.id == "night" || scheme.id == "harbour",
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0f, 0.5f)
        },
        style = TextStyle(
            fontFamily = face,
            fontWeight = weight,
            fontStyle = if (italic && !serif) FontStyle.Italic else FontStyle.Normal,
            fontSize = basePx.tvSp(),
            lineHeight = (basePx * 1.15f).tvSp(),
            shadow = glow,
            textDecoration = if (focused && underlineOnFocus) TextDecoration.Underline else TextDecoration.None,
        ),
    )
}

@Composable
fun BodyText(
    text: String,
    color: Color,
    design: Design,
    px: Int,
    serif: Boolean = false,
    italic: Boolean = false,
    modifier: Modifier = Modifier,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = color,
            fontFamily = when {
                serif && italic -> design.faces.headlineItalic
                serif -> design.faces.headline
                else -> design.faces.ui
            },
            fontWeight = if (serif) FontWeight.Light else FontWeight.Normal,
            fontSize = px.tvSp(),
            lineHeight = (px * 1.35f).tvSp(),
        ),
    )
}

@Composable
fun Hairline(scheme: SchemeTokens, design: Design) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(design.layout.rulePx.tvDp())
            .background(scheme.rule),
    )
}

@Composable
fun OkMark(scheme: SchemeTokens, design: Design) {
    val color = if (scheme.focus.style == "highlighter") scheme.focus.text else scheme.accent
    Row(verticalAlignment = Alignment.CenterVertically) {
        BasicText(
            text = design.copy.ok,
            style = TextStyle(color = color, fontFamily = design.faces.ui, fontSize = design.type.footerPx.tvSp()),
        )
        Spacer(Modifier.width(8.tvDp()))
        ArrowIcon(color, Modifier.size(design.type.footerPx.tvDp()))
    }
}

@Composable
fun Kicker(text: String, scheme: SchemeTokens, design: Design) {
    BasicText(
        text = text,
        style = TextStyle(
            color = scheme.accent,
            fontFamily = design.faces.ui,
            fontWeight = FontWeight.Normal,
            fontSize = design.type.kickerPx.tvSp(),
            letterSpacing = design.type.kickerTracking.em,
        ),
    )
}

@Composable
fun Pill(text: String, scheme: SchemeTokens, design: Design) {
    BasicText(
        text = text,
        style = TextStyle(
            color = scheme.accent,
            fontFamily = design.faces.ui,
            fontSize = design.type.footerPx.tvSp(),
        ),
        modifier = Modifier
            .border(design.layout.rulePx.tvDp(), scheme.accent, RoundedCornerShape(50))
            .padding(horizontal = 16.tvDp(), vertical = 6.tvDp()),
    )
}

@Composable
fun TvField(
    value: String,
    onValue: (String) -> Unit,
    scheme: SchemeTokens,
    design: Design,
    hint: String,
    modifier: Modifier = Modifier,
) {
    val latest = rememberUpdatedState(onValue)
    val latestValue = rememberUpdatedState(value)
    val density = LocalDensity.current
    val textPx = with(density) { design.type.uiPx.tvSp().toPx() }
    val ink = scheme.ink.toArgb()
    val accent = scheme.accent.toArgb()
    val muted = scheme.muted.toArgb()
    AndroidView(
        modifier = modifier.fillMaxWidth().height(64.tvDp()),
        factory = { context ->
            EditText(context).apply {
                setText(latestValue.value)
                setTextColor(ink)
                setHint(hint)
                setHintTextColor(muted)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx)
                isSingleLine = true
                setPadding(28, 16, 28, 16)
                fun paint(hasFocus: Boolean) {
                    background = GradientDrawable().apply {
                        cornerRadius = 16f
                        if (hasFocus) {
                            setStroke(ReviewLayout.fieldStrokePx, accent)
                            setColor(android.graphics.Color.TRANSPARENT)
                        } else {
                            setStroke(0, accent)
                            setColor(ReviewLayout.fieldIdleFill)
                        }
                    }
                }
                paint(false)
                setOnFocusChangeListener { _, hasFocus -> paint(hasFocus) }
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                    override fun afterTextChanged(s: Editable?) {
                        val next = s?.toString().orEmpty()
                        if (next != latestValue.value) latest.value(next)
                    }
                })
            }
        },
        update = { field ->
            if (field.text.toString() != value) {
                field.setText(value)
                field.setSelection(field.text.length)
            }
            field.setTextColor(ink)
            field.setHintTextColor(muted)
        },
    )
}

fun roman(number: Int): String {
    val values = listOf(10 to "x", 9 to "ix", 5 to "v", 4 to "iv", 1 to "i")
    var left = number
    val out = StringBuilder()
    for ((amount, glyph) in values) {
        while (left >= amount) {
            out.append(glyph)
            left -= amount
        }
    }
    return out.toString()
}
