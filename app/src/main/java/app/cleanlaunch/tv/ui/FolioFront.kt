package app.cleanlaunch.tv.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import app.cleanlaunch.tv.EveningViewModel
import app.cleanlaunch.tv.Overlay
import app.cleanlaunch.tv.UiState
import app.cleanlaunch.tv.data.HomeApp
import app.cleanlaunch.tv.data.SlotPhotos
import app.cleanlaunch.tv.input.SourceSlot
import app.cleanlaunch.tv.sentence.FrontPage
import app.cleanlaunch.tv.theme.Design
import app.cleanlaunch.tv.theme.FolioPalette
import app.cleanlaunch.tv.theme.ReviewLayout
import app.cleanlaunch.tv.theme.SchemeTokens
import app.cleanlaunch.tv.theme.tvDp
import app.cleanlaunch.tv.theme.tvSp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

private const val APP_ROWS = 7
private const val APP_COLS = 2
private const val APP_VISIBLE = APP_ROWS * APP_COLS

@Composable
fun FolioFront(
    model: EveningViewModel,
    state: UiState,
    design: Design,
    scheme: SchemeTokens,
) {
    val starred = state.apps.filter { it.pinned }.take(ReviewLayout.pinCap)
    val others = state.apps.filter { app -> starred.none { it.id == app.id } }
    val ports = state.sources.filter { it.port in 1..4 && it.shown }
    val edit = remember { FocusRequester() }
    val starFocus = remember(starred.map { it.id }) { starred.associate { it.id to FocusRequester() } }
    val portFocus = remember(ports.map { it.id }) { ports.associate { it.id to FocusRequester() } }
    val forecast = state.forecast
    val quotes = state.quotes.take(ReviewLayout.maxTickers)
    var appIndex by remember(others.map { it.id }) { mutableStateOf(0) }
    val window = gridStart(others.size, appIndex)
    val visible = others.drop(window).take(APP_VISIBLE)
    val appFocus = remember(visible.map { it.id }, window) { visible.associate { it.id to FocusRequester() } }
    val resting = state.debugFocus == "rest"
    LaunchedEffect(state.focusToken, state.overlay, state.dimmed, starred, others, state.debugFocus) {
        if (state.dimmed || resting || state.overlay != Overlay.None) return@LaunchedEffect
        delay(40)
        val target = starFocus.values.firstOrNull()
            ?: appFocus.values.firstOrNull()
            ?: portFocus.values.firstOrNull()
            ?: edit
        runCatching { target.requestFocus() }
    }
    Box(Modifier.fillMaxSize()) {
        Header(model, state, design, scheme, edit, starFocus.values.firstOrNull() ?: appFocus.values.firstOrNull() ?: edit, resting)
        Box(
            Modifier
                .offset(96.tvDp(), 176.tvDp())
                .width(1728.tvDp())
                .height(2.tvDp())
                .background(scheme.ink.copy(alpha = FolioPalette.hairlineAlpha(scheme.id))),
        )
        WeatherColumn(state, design, scheme, forecast?.english.orEmpty(), forecast?.chinese.orEmpty())
        FrontPhoto(state)
        SourcesRow(model, state, design, scheme, ports, portFocus, edit, starFocus.values.firstOrNull(), resting)
        Box(
            Modifier
                .offset(872.tvDp(), 176.tvDp())
                .width(2.tvDp())
                .height(850.tvDp())
                .background(scheme.ink.copy(alpha = FolioPalette.hairlineAlpha(scheme.id))),
        )
        StarredColumn(
            model, state, design, scheme, starred, starFocus, edit,
            portFocus.values.firstOrNull(), appFocus.values.firstOrNull(), resting,
        )
        if (quotes.isNotEmpty()) {
            TickerBlock(quotes, design, scheme, starred.size)
        }
        AllAppsColumn(
            model, state, design, scheme, others, visible, window, appIndex, appFocus,
            starFocus.values.firstOrNull(), resting,
            onMove = { next -> appIndex = next.coerceIn(0, (others.size - 1).coerceAtLeast(0)) },
        )
    }
}

@Composable
private fun Header(
    model: EveningViewModel,
    state: UiState,
    design: Design,
    scheme: SchemeTokens,
    edit: FocusRequester,
    down: FocusRequester,
    resting: Boolean,
) {
    Row(
        Modifier.offset(96.tvDp(), 66.tvDp()).width(1728.tvDp()).height(84.tvDp()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            state.timeLabel,
            style = TextStyle(
                color = scheme.ink,
                fontFamily = design.faces.display,
                fontWeight = FontWeight.Light,
                fontSize = 84.tvSp(),
            ),
        )
        Spacer(Modifier.width(28.tvDp()))
        Column {
            BasicText(
                state.dateLabel,
                style = TextStyle(color = scheme.ink, fontFamily = design.faces.ui, fontWeight = FontWeight.Medium, fontSize = 28.tvSp()),
            )
            BasicText(
                state.dateChinese,
                style = TextStyle(color = scheme.ink, fontFamily = design.faces.chinese, fontSize = 28.tvSp()),
            )
        }
        Spacer(Modifier.weight(1f))
        EditLink(model, state, design, scheme, edit, down, resting)
    }
}

@Composable
private fun EditLink(
    model: EveningViewModel,
    state: UiState,
    design: Design,
    scheme: SchemeTokens,
    edit: FocusRequester,
    down: FocusRequester,
    resting: Boolean,
) {
    var focused by remember { mutableStateOf(false) }
    val show = focused && !resting
    Row(
        Modifier.tvFocus(edit, { focused = it }, { model.openSettings() }, toDown = down),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (show) PencilTick(scheme, Modifier.height(36.tvDp()))
        if (show) Spacer(Modifier.width(12.tvDp()))
        FolioGlyph(
            "${editionTitle(scheme)} · Edit page",
            show,
            scheme,
            design,
            28,
            design.faces.ui,
            FontWeight.Medium,
            stroke = true,
        )
    }
}

@Composable
private fun WeatherColumn(state: UiState, design: Design, scheme: SchemeTokens, english: String, chinese: String) {
    if (state.place.city.isBlank()) {
        Column(Modifier.offset(96.tvDp(), 200.tvDp()).width(660.tvDp())) {
            BasicText(
                "WEATHER",
                style = TextStyle(
                    color = scheme.ink,
                    fontFamily = design.faces.ui,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 28.tvSp(),
                    letterSpacing = 0.12.em,
                ),
            )
            Spacer(Modifier.height(16.tvDp()))
            BasicText(
                "No city yet. Set one under Settings, or leave it on automatic.",
                style = TextStyle(
                    color = scheme.ink,
                    fontFamily = design.faces.display,
                    fontWeight = FontWeight.Light,
                    fontSize = 46.tvSp(),
                    lineHeight = (46 * 1.18f).tvSp(),
                ),
            )
        }
        return
    }
    val kicker = buildAnnotatedString {
        withStyle(SpanStyle(fontFamily = design.faces.ui, fontWeight = FontWeight.SemiBold, letterSpacing = 0.12.em)) {
            append(state.place.city.uppercase(Locale.UK))
            append(" · ")
        }
        withStyle(SpanStyle(fontFamily = design.faces.chinese)) {
            append(state.place.chinese)
        }
    }
    Column(Modifier.offset(96.tvDp(), 200.tvDp()).width(660.tvDp())) {
        BasicText(kicker, style = TextStyle(color = scheme.ink, fontSize = 28.tvSp()))
        if (english.isNotBlank()) {
            Spacer(Modifier.height(16.tvDp()))
            BasicText(
                english,
                style = TextStyle(
                    color = scheme.ink,
                    fontFamily = design.faces.display,
                    fontWeight = FontWeight.Light,
                    fontSize = 46.tvSp(),
                    lineHeight = (46 * 1.18f).tvSp(),
                ),
            )
        }
        if (chinese.isNotBlank()) {
            Spacer(Modifier.height(10.tvDp()))
            BasicText(
                chinese,
                style = TextStyle(
                    color = scheme.ink,
                    fontFamily = design.faces.chinese,
                    fontSize = 34.tvSp(),
                    lineHeight = 42.tvSp(),
                ),
            )
        }
    }
}

@Composable
private fun FrontPhoto(state: UiState) {
    val context = LocalContext.current
    val name = state.slotName
    var current by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var previous by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var fade by remember { mutableStateOf(1f) }
    LaunchedEffect(name, state.slotToken) {
        val bitmap = withContext(Dispatchers.IO) {
            SlotPhotos.decode(context, name, SlotPhotos.SLOT_W, SlotPhotos.SLOT_H)
        }?.asImageBitmap() ?: return@LaunchedEffect
        if (current == null) {
            current = bitmap
            fade = 1f
            return@LaunchedEffect
        }
        previous = current
        current = bitmap
        val start = android.os.SystemClock.uptimeMillis()
        while (true) {
            fade = SlotPhotos.fade(android.os.SystemClock.uptimeMillis() - start)
            if (fade >= 1f) break
            delay(16)
        }
        previous = null
    }
    Box(
        Modifier
            .offset(96.tvDp(), 500.tvDp())
            .width(660.tvDp())
            .height(384.tvDp())
            .clip(RoundedCornerShape(6.tvDp()))
            .background(Color(0xFF1A1612)),
    ) {
        previous?.let {
            Image(
                it,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 1f - fade },
                contentScale = ContentScale.Crop,
            )
        }
        current?.let {
            Image(
                it,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = fade },
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun SourcesRow(
    model: EveningViewModel,
    state: UiState,
    design: Design,
    scheme: SchemeTokens,
    ports: List<SourceSlot>,
    focus: Map<String, FocusRequester>,
    up: FocusRequester,
    right: FocusRequester?,
    resting: Boolean,
) {
    Row(
        Modifier.offset(96.tvDp(), 900.tvDp()),
        horizontalArrangement = Arrangement.spacedBy(12.tvDp()),
    ) {
        ports.forEachIndexed { index, slot ->
            val requester = focus[slot.id] ?: return@forEachIndexed
            SourceTile(
                slot, model, design, scheme, requester, resting,
                toLeft = ports.getOrNull(index - 1)?.let { focus[it.id] },
                toRight = ports.getOrNull(index + 1)?.let { focus[it.id] } ?: right,
                toUp = up,
            )
        }
    }
}

@Composable
private fun SourceTile(
    slot: SourceSlot,
    model: EveningViewModel,
    design: Design,
    scheme: SchemeTokens,
    requester: FocusRequester,
    resting: Boolean,
    toLeft: FocusRequester?,
    toRight: FocusRequester?,
    toUp: FocusRequester,
) {
    var focused by remember { mutableStateOf(false) }
    val show = focused && !resting
    val port = "HDMI ${slot.port}"
    val device = if (slot.label.equals(port, true)) "—" else slot.label
    val line = scheme.ink.copy(alpha = FolioPalette.hairlineAlpha(scheme.id))
    Column(
        Modifier
            .width(150.tvDp())
            .height(92.tvDp())
            .clip(RoundedCornerShape(6.tvDp()))
            .border(2.tvDp(), if (show) scheme.focus.pencil else line, RoundedCornerShape(6.tvDp()))
            .tvFocus(requester, {
                focused = it
                if (it) model.reportFocus("source", null)
            }, { model.switchSource(slot) }, toLeft = toLeft, toRight = toRight, toUp = toUp)
            .padding(horizontal = 12.tvDp(), vertical = 8.tvDp()),
        verticalArrangement = Arrangement.Center,
    ) {
        FolioGlyph(port, show, scheme, design, 28, design.faces.ui, FontWeight.Bold, stroke = true)
        FolioGlyph(device, false, scheme, design, 28, design.faces.display, color = scheme.muted)
    }
}

@Composable
private fun StarredColumn(
    model: EveningViewModel,
    state: UiState,
    design: Design,
    scheme: SchemeTokens,
    starred: List<HomeApp>,
    focus: Map<String, FocusRequester>,
    up: FocusRequester,
    left: FocusRequester?,
    right: FocusRequester?,
    resting: Boolean,
) {
    Column(Modifier.offset(930.tvDp(), 200.tvDp()).width(446.tvDp())) {
        Kicker("Starred", "常用", design, scheme)
        Spacer(Modifier.height(8.tvDp()))
        starred.forEachIndexed { index, app ->
            val requester = focus[app.id] ?: return@forEachIndexed
            AppNameRow(
                app.label, state, design, scheme, requester, resting, 54,
                glyph = app.glyph,
                onClick = { model.launch(app) },
                onLong = { model.longPressApp(app.id) },
                toUp = if (index == 0) up else focus[starred[index - 1].id],
                toDown = starred.getOrNull(index + 1)?.let { focus[it.id] },
                toLeft = left,
                toRight = right,
                onFocused = { if (it) model.reportFocus("app", app.id) },
            )
        }
    }
}

@Composable
private fun TickerBlock(
    quotes: List<app.cleanlaunch.tv.data.Quote>,
    design: Design,
    scheme: SchemeTokens,
    starredCount: Int,
) {
    val top = 200 + 48 + starredCount * 78 + 28
    Column(Modifier.offset(956.tvDp(), top.tvDp()).width(400.tvDp())) {
        quotes.forEach { quote ->
            val line = FrontPage.quoteLine(quote)
            BasicText(
                line.english,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = scheme.ink,
                    fontFamily = design.faces.ui,
                    fontWeight = FontWeight.Medium,
                    fontSize = 28.tvSp(),
                    lineHeight = 36.tvSp(),
                ),
            )
        }
    }
}

@Composable
private fun AllAppsColumn(
    model: EveningViewModel,
    state: UiState,
    design: Design,
    scheme: SchemeTokens,
    others: List<HomeApp>,
    visible: List<HomeApp>,
    window: Int,
    focusIndex: Int,
    focus: Map<String, FocusRequester>,
    left: FocusRequester?,
    resting: Boolean,
    onMove: (Int) -> Unit,
) {
    Column(Modifier.offset(1400.tvDp(), 200.tvDp()).width(424.tvDp())) {
        Kicker("All apps", "全部", design, scheme)
        Spacer(Modifier.height(8.tvDp()))
        Column {
            for (row in 0 until APP_ROWS) {
                Row(horizontalArrangement = Arrangement.spacedBy(28.tvDp())) {
                    for (col in 0 until APP_COLS) {
                        val local = row * APP_COLS + col
                        val app = visible.getOrNull(local)
                        val requester = app?.let { focus[it.id] }
                        Box(Modifier.width(198.tvDp()).height(78.tvDp())) {
                            if (app != null && requester != null) {
                                val absolute = window + local
                                AppNameRow(
                                    app.label, state, design, scheme, requester, resting, 32,
                                    glyph = app.glyph,
                                    hairline = true,
                                    onClick = { model.launch(app) },
                                    onLong = { model.longPressApp(app.id) },
                                    toLeft = if (col == 0) left else visible.getOrNull(local - 1)?.let { focus[it.id] },
                                    toRight = visible.getOrNull(local + 1)?.let { focus[it.id] },
                                    toUp = visible.getOrNull(local - APP_COLS)?.let { focus[it.id] },
                                    toDown = visible.getOrNull(local + APP_COLS)?.let { focus[it.id] },
                                    onFocused = { if (it) model.reportFocus("app", app.id) },
                                    onMove = { delta -> onMove(absolute + delta) },
                                    canLeaveDown = local + APP_COLS >= visible.size && absolute + APP_COLS < others.size,
                                    canLeaveUp = local < APP_COLS && absolute >= APP_COLS,
                                )
                            }
                        }
                    }
                }
            }
        }
        val more = moreBelow(window, others.size)
        if (more != null) {
            Spacer(Modifier.height(8.tvDp()))
            BasicText(
                more,
                style = TextStyle(color = scheme.muted, fontFamily = design.faces.ui, fontSize = 28.tvSp()),
            )
        }
    }
}

@Composable
private fun AppNameRow(
    label: String,
    state: UiState,
    design: Design,
    scheme: SchemeTokens,
    requester: FocusRequester,
    resting: Boolean,
    px: Int,
    glyph: String? = null,
    hairline: Boolean = false,
    onClick: () -> Unit,
    onLong: () -> Unit,
    toUp: FocusRequester? = null,
    toDown: FocusRequester? = null,
    toLeft: FocusRequester? = null,
    toRight: FocusRequester? = null,
    onFocused: (Boolean) -> Unit = {},
    onMove: ((Int) -> Unit)? = null,
    canLeaveDown: Boolean = false,
    canLeaveUp: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    val show = focused && !resting && !state.dimmed
    Column(
        Modifier
            .fillMaxWidth()
            .height(78.tvDp())
            .tvFocus(
                requester,
                {
                    focused = it
                    onFocused(it)
                },
                onClick,
                onLong,
                toLeft = toLeft,
                toRight = toRight,
                toUp = toUp,
                toDown = toDown,
                onKey = { event ->
                    if (event.type != KeyEventType.KeyDown || onMove == null) return@tvFocus false
                    when (event.key) {
                        Key.DirectionDown -> if (canLeaveDown) {
                            onMove(APP_COLS)
                            true
                        } else false
                        Key.DirectionUp -> if (canLeaveUp) {
                            onMove(-APP_COLS)
                            true
                        } else false
                        else -> false
                    }
                },
            ),
    ) {
        if (hairline) {
            Box(Modifier.fillMaxWidth().height(2.tvDp()).background(scheme.ink.copy(alpha = FolioPalette.hairlineAlpha(scheme.id))))
        }
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            if (show) PencilTick(scheme, Modifier.height(48.tvDp())) else Spacer(Modifier.width(6.tvDp()))
            Spacer(Modifier.width(20.tvDp()))
            if (glyph == "x") {
                val ink = if (show && scheme.id != "cream" && scheme.focus.text != Color.Unspecified) {
                    scheme.focus.text
                } else {
                    scheme.ink
                }
                XGlyph(ink, Modifier.size(px.tvDp()))
            } else {
                FolioGlyph(label, show, scheme, design, px, design.faces.display, stroke = true, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Kicker(english: String, chinese: String, design: Design, scheme: SchemeTokens) {
    val text = buildAnnotatedString {
        withStyle(
            SpanStyle(
                fontFamily = design.faces.ui,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.12.em,
            ),
        ) {
            append(english.uppercase(Locale.UK))
            append(" · ")
        }
        withStyle(SpanStyle(fontFamily = design.faces.chinese)) {
            append(chinese)
        }
    }
    BasicText(text, style = TextStyle(color = scheme.ink, fontSize = 28.tvSp()))
}

private fun gridStart(count: Int, focus: Int): Int {
    val visible = APP_VISIBLE
    if (count <= visible) return 0
    val row = focus / APP_COLS
    val startRow = (row - (APP_ROWS - 1)).coerceAtLeast(0)
    val maxStart = count - visible
    return (startRow * APP_COLS).coerceAtMost(maxStart)
}

private fun moreBelow(start: Int, count: Int): String? {
    val first = start + APP_VISIBLE
    if (first >= count) return null
    val left = count - first
    return "${first + 1}–$count ↓ · 还有${left}个"
}
