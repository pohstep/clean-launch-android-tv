package app.cleanlaunch.tv.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import app.cleanlaunch.tv.BuildConfig
import app.cleanlaunch.tv.EveningViewModel
import app.cleanlaunch.tv.UiState
import app.cleanlaunch.tv.data.HomeApp
import app.cleanlaunch.tv.data.PhotoLibrary
import app.cleanlaunch.tv.data.SlotPhotos
import app.cleanlaunch.tv.input.SourceSlot
import app.cleanlaunch.tv.sentence.FrontPage
import app.cleanlaunch.tv.theme.Design
import app.cleanlaunch.tv.theme.FolioPalette
import app.cleanlaunch.tv.theme.SchemeTokens
import app.cleanlaunch.tv.theme.tvDp
import app.cleanlaunch.tv.theme.tvSp

private val sections = listOf(
    "inputs" to ("Inputs" to "输入"),
    "apps" to ("Apps" to "应用"),
    "front" to ("Front page" to "首页"),
    "look" to ("Look" to "外观"),
    "saver" to ("Screen saver" to "屏保"),
    "about" to ("About" to "关于"),
)

@Composable
fun SettingsScreen(model: EveningViewModel, state: UiState, design: Design, schemeIn: SchemeTokens) {
    val scheme = if (state.settingsSection == "look") {
        model.design.scheme(state.previewSchemeId ?: state.schemeId)
    } else {
        schemeIn
    }
    val menu = remember { sections.associate { it.first to FocusRequester() } }
    val appFocus = remember { mutableMapOf<String, FocusRequester>() }
    LaunchedEffect(state.renameKind) {
        if (state.renameKind != null) return@LaunchedEffect
        delay(60)
        runCatching { menu.getValue(state.settingsSection).requestFocus() }
    }
    LaunchedEffect(state.focusHoldId) {
        val id = state.focusHoldId ?: return@LaunchedEffect
        delay(90)
        appFocus[id]?.let { runCatching { it.requestFocus() } }
        model.releaseFocusHold()
    }
    Box(Modifier.fillMaxSize().background(scheme.background)) {
        CompositionLocalProvider(LocalPageFocus provides (state.renameKind == null)) {
        SettingsHeader(state, design, scheme)
        Box(
            Modifier.offset(96.tvDp(), 150.tvDp()).width(1728.tvDp()).height(2.tvDp())
                .background(scheme.ink.copy(alpha = FolioPalette.hairlineAlpha(scheme.id))),
        )
        Column(Modifier.offset(122.tvDp(), 180.tvDp()).width(330.tvDp())) {
            BasicText(
                "Settings",
                style = TextStyle(color = scheme.ink, fontFamily = design.faces.display, fontWeight = FontWeight.Light, fontSize = 60.tvSp()),
            )
            BasicText(
                "设置",
                style = TextStyle(color = scheme.ink, fontFamily = design.faces.chinese, fontSize = 42.tvSp()),
            )
            Spacer(Modifier.height(18.tvDp()))
            sections.forEachIndexed { index, (id, labels) ->
                MenuRow(
                    labels.first, labels.second, id == state.settingsSection, model, design, scheme,
                    menu.getValue(id),
                    toUp = sections.getOrNull(index - 1)?.let { menu[it.first] },
                    toDown = sections.getOrNull(index + 1)?.let { menu[it.first] },
                    enabled = state.focusHoldId == null,
                )
            }
        }
        Box(
            Modifier.offset(500.tvDp(), 180.tvDp()).width(2.tvDp()).height(820.tvDp())
                .background(scheme.ink.copy(alpha = FolioPalette.hairlineAlpha(scheme.id))),
        )
        Column(
            Modifier
                .offset(560.tvDp(), 180.tvDp())
                .width(1264.tvDp())
                .height(820.tvDp())
                .verticalScroll(rememberScrollState()),
        ) {
            when (state.settingsSection) {
                "inputs" -> InputsPanel(model, state, design, scheme, menu.getValue("inputs"))
                "apps" -> AppsPanel(model, state, design, scheme, menu.getValue("apps"), appFocus)
                "front" -> FrontPanel(model, state, design, scheme, menu.getValue("front"))
                "look" -> LookPanel(model, state, design, scheme, menu.getValue("look"))
                "saver" -> SaverPanel(model, state, design, scheme, menu.getValue("saver"))
                else -> AboutPanel(design, scheme, menu.getValue("about"))
            }
        }
        }
        if (state.renameKind != null) {
            RenameKeyboard(model, state, design, scheme)
        }
    }
}

@Composable
private fun SettingsHeader(state: UiState, design: Design, scheme: SchemeTokens) {
    Row(Modifier.offset(96.tvDp(), 54.tvDp()), verticalAlignment = Alignment.CenterVertically) {
        BasicText(
            state.timeLabel,
            style = TextStyle(color = scheme.ink, fontFamily = design.faces.display, fontWeight = FontWeight.Light, fontSize = 64.tvSp()),
        )
        Spacer(Modifier.width(28.tvDp()))
        Column {
            BasicText(state.dateLabel, style = TextStyle(color = scheme.ink, fontFamily = design.faces.ui, fontWeight = FontWeight.Medium, fontSize = 28.tvSp()))
            BasicText(state.dateChinese, style = TextStyle(color = scheme.ink, fontFamily = design.faces.chinese, fontSize = 28.tvSp()))
        }
    }
}

@Composable
private fun MenuRow(
    english: String,
    chinese: String,
    selected: Boolean,
    model: EveningViewModel,
    design: Design,
    scheme: SchemeTokens,
    requester: FocusRequester,
    toUp: FocusRequester?,
    toDown: FocusRequester?,
    enabled: Boolean = true,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .height(72.tvDp())
            .tvFocus(requester, {
                focused = it
                if (it) {
                    model.selectSettings(sections.first { row -> row.second.first == english }.first)
                    model.noteSettingsMenu()
                }
            }, { model.enterSettingsPanel() }, toUp = toUp, toDown = toDown, enabled = enabled),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (focused || selected) PencilTick(scheme, Modifier.height(40.tvDp())) else Spacer(Modifier.width(6.tvDp()))
        Spacer(Modifier.width(16.tvDp()))
        BasicText(
            english,
            style = TextStyle(
                color = scheme.ink,
                fontFamily = if (focused) design.faces.focus else design.faces.display,
                fontWeight = if (focused) FontWeight.Medium else FontWeight.Light,
                fontSize = 40.tvSp(),
            ),
        )
        Spacer(Modifier.weight(1f))
        BasicText(chinese, style = TextStyle(color = scheme.muted, fontFamily = design.faces.chinese, fontSize = 30.tvSp()))
    }
}

@Composable
private fun PanelTitle(english: String, chinese: String, help: String, design: Design, scheme: SchemeTokens) {
    BasicText(english, style = TextStyle(color = scheme.ink, fontFamily = design.faces.display, fontWeight = FontWeight.Light, fontSize = 50.tvSp()))
    BasicText(chinese, style = TextStyle(color = scheme.ink, fontFamily = design.faces.chinese, fontSize = 38.tvSp()))
    Spacer(Modifier.height(8.tvDp()))
    BasicText(help, maxLines = 1, style = TextStyle(color = scheme.muted, fontFamily = design.faces.ui, fontSize = 28.tvSp()))
    Spacer(Modifier.height(16.tvDp()))
}

@Composable
private fun InputsPanel(model: EveningViewModel, state: UiState, design: Design, scheme: SchemeTokens, menu: FocusRequester) {
    PanelTitle("Inputs", "输入", "Tile 1 opens port 1. OK renames, shows, or tests the port.", design, scheme)
    state.sources.forEachIndexed { index, slot ->
        val strip = state.menuAppId == "input:${slot.id}"
        SettingRow(
            title = inputTitle(slot),
            meta = "",
            value = "tile ${slot.port.coerceAtLeast(1)} · ${if (slot.shown) "Shown" else "Hidden"}",
            design = design,
            scheme = scheme,
            menu = menu,
            strip = if (strip) listOf(
                "Rename" to { model.beginRename("input", slot.id) },
                (if (slot.shown) "Hide" else "Show") to { model.toggleInputShown(slot); model.closeMenu() },
                "Test" to { model.switchSource(slot); model.closeMenu() },
            ) else emptyList(),
            onClick = { model.openInputActions(slot.id) },
        )
        if (index != state.sources.lastIndex) Hair(scheme)
    }
}

private fun inputTitle(slot: SourceSlot): String {
    val port = if (slot.port in 1..4) "HDMI ${slot.port}" else slot.label
    return if (slot.label.equals(port, true)) port else "$port — ${slot.label}"
}

@Composable
private fun AppsPanel(
    model: EveningViewModel,
    state: UiState,
    design: Design,
    scheme: SchemeTokens,
    menu: FocusRequester,
    appFocus: MutableMap<String, FocusRequester>,
) {
    PanelTitle("Apps", "应用", "OK on a row: Pin, Rename, Hide, or Move.", design, scheme)
    val pinned = state.apps.filter { it.pinned }
    val page = state.apps.filter { !it.pinned }
    GroupLabel("PINNED · ${pinned.size}", design, scheme)
    pinned.forEach { app ->
        key(app.id) { AppSettingRow(app, true, model, state, design, scheme, menu, appFocus) }
    }
    GroupLabel("ON THE PAGE · ${page.size}", design, scheme)
    page.forEach { app ->
        key(app.id) { AppSettingRow(app, true, model, state, design, scheme, menu, appFocus) }
    }
    if (state.hidden.isNotEmpty()) {
        GroupLabel("HIDDEN · ${state.hidden.size}", design, scheme)
        state.hidden.forEach { app ->
            key(app.id) { AppSettingRow(app, false, model, state, design, scheme, menu, appFocus) }
        }
    }
    SettingRow("Add an installed app", "", "", design, scheme, menu, emptyList(), { model.openPicker() })
}

@Composable
private fun AppSettingRow(
    app: HomeApp,
    shown: Boolean,
    model: EveningViewModel,
    state: UiState,
    design: Design,
    scheme: SchemeTokens,
    menu: FocusRequester,
    appFocus: MutableMap<String, FocusRequester>,
) {
    val moving = state.movingAppId == app.id
    val strip = state.menuAppId == app.id
    val row = appFocus.getOrPut(app.id) { FocusRequester() }
    SettingRow(
        title = if (app.pinned) "★  ${app.label}" else app.label,
        meta = packageTail(app.packageName),
        value = when {
            moving -> "Moving"
            shown -> "Shown"
            else -> "Hidden"
        },
        design = design,
        scheme = scheme,
        menu = menu,
        strip = if (strip) listOf(
            (if (app.pinned) "Unpin" else "Pin") to { model.togglePin(app.id) },
            "Rename" to { model.beginRename("app", app.id) },
            (if (shown) "Hide" else "Show") to { if (shown) model.hideApp(app.id) else model.restore(app.id) },
            "Move" to { model.beginMove(app.id) },
        ) else emptyList(),
        onClick = { if (moving) model.finishMove() else model.openAppActions(app.id) },
        rowRequester = row,
        onKey = if (moving) { event ->
            if (event.type != KeyEventType.KeyDown) return@SettingRow false
            when (event.key) {
                Key.DirectionUp -> { model.moveApp(app.id, -1); true }
                Key.DirectionDown -> { model.moveApp(app.id, 1); true }
                else -> false
            }
        } else null,
    )
    Hair(scheme)
}

@Composable
private fun FrontPanel(model: EveningViewModel, state: UiState, design: Design, scheme: SchemeTokens, menu: FocusRequester) {
    PanelTitle("Front page", "首页", "OK on City types a place. OK on Photographs sets 5s, 10s, 30s, 1 min, or 5 min.", design, scheme)
    val cityValue = when {
        state.locationMode == "manual" && state.manualCity.isNotBlank() -> state.place.city.ifBlank { state.manualCity }
        state.place.source == "ip" && state.place.city.isNotBlank() -> "Automatic"
        else -> "Automatic"
    }
    SettingRow(
        "City",
        state.locationNote.ifBlank { "Blank returns to automatic." },
        cityValue,
        design, scheme, menu, emptyList(),
        { model.beginRename("city", null) },
    )
    Hair(scheme)
    val weatherOn = state.front.firstOrNull { it.id == "weather" }?.enabled == true
    val weatherValue = when {
        !weatherOn -> "off"
        state.place.city.isBlank() -> "empty"
        state.forecast != null -> "on"
        else -> "quiet"
    }
    SettingRow(
        "Weather",
        state.place.city,
        weatherValue,
        design, scheme, menu, emptyList(),
        { model.toggleFront("weather") },
    )
    Hair(scheme)
    SettingRow("Tickers", state.tickers, "3 on the page", design, scheme, menu, emptyList(), { model.beginRename("tickers", null) })
    Hair(scheme)
    val count = if (state.slotCount == 0) SlotPhotos.SAMPLE_CAPTION else "${state.slotCount} in the folder"
    SettingRow(
        "Photographs",
        count,
        SlotPhotos.intervalLabel(state.slotSeconds),
        design, scheme, menu,
        emptyList(),
        { model.cycleSlotInterval() },
    )
    BasicText(
        SlotPhotos.PUSH_PATH,
        maxLines = 1,
        style = TextStyle(color = scheme.muted, fontFamily = design.faces.ui, fontSize = 24.tvSp()),
    )
    Hair(scheme)
    state.customLines.forEachIndexed { index, copy ->
        val preview = listOf(copy.english, copy.chinese).filter { it.isNotBlank() }.joinToString(" / ").ifBlank { "Empty" }
        SettingRow("Custom line ${index + 1}", preview, "", design, scheme, menu, emptyList(), { model.beginRename("custom-en", index.toString()) })
        Hair(scheme)
    }
    state.front.forEach { pref ->
        if (pref.id == "news") return@forEach
        SettingRow(FrontPage.label(pref.id), "", if (pref.enabled) "Shown" else "Hidden", design, scheme, menu, emptyList(), {
            model.toggleFront(pref.id)
        })
        Hair(scheme)
    }
}

@Composable
private fun LookPanel(model: EveningViewModel, state: UiState, design: Design, scheme: SchemeTokens, menu: FocusRequester) {
    PanelTitle("Look", "外观", "Focus previews a scheme. OK keeps it.", design, scheme)
    design.schemes.forEach { item ->
        SettingRow(
            item.name,
            "",
            if (item.id == state.schemeId) "✓ In use" else "",
            design, scheme, menu, emptyList(),
            onClick = { model.commitScheme(item.id) },
            onFocus = { model.previewScheme(item.id) },
        )
        Hair(scheme)
    }
}

@Composable
private fun SaverPanel(model: EveningViewModel, state: UiState, design: Design, scheme: SchemeTokens, menu: FocusRequester) {
    PanelTitle("Screen saver", "屏保", "The dim clock shows the time and the weather.", design, scheme)
    val idle = when (state.idleMinutes) {
        0 -> "Off"
        else -> "${state.idleMinutes} min"
    }
    SettingRow("Dim after", "", idle, design, scheme, menu, emptyList(), { model.cycleIdle() })
    Hair(scheme)
    SettingRow("Background", "", PhotoLibrary.modeLabel(state.photoMode), design, scheme, menu, emptyList(), { model.cyclePhotoMode() })
    Hair(scheme)
    SettingRow("Background folder", "", if (state.photoReadable) "Folder" else "Off", design, scheme, menu, emptyList(), { model.requestPhotoRead() })
}

@Composable
private fun AboutPanel(design: Design, scheme: SchemeTokens, menu: FocusRequester) {
    PanelTitle("About", "关于", "Clean Launch, a home screen for Android TV.", design, scheme)
    BasicText(
        "Version ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
        style = TextStyle(color = scheme.ink, fontFamily = design.faces.ui, fontSize = 28.tvSp()),
    )
    Spacer(Modifier.height(12.tvDp()))
    BasicText(
        "Boska, Smiley Sans, and Familjen Grotesk.",
        style = TextStyle(color = scheme.muted, fontFamily = design.faces.ui, fontSize = 28.tvSp()),
    )
    Spacer(Modifier.height(12.tvDp()))
    SettingRow("About this", "D-pad, then OK. Back leaves the panel, then the page.", "", design, scheme, menu, emptyList(), {})
}

@Composable
private fun GroupLabel(text: String, design: Design, scheme: SchemeTokens) {
    Spacer(Modifier.height(12.tvDp()))
    BasicText(text, style = TextStyle(color = scheme.muted, fontFamily = design.faces.ui, fontWeight = FontWeight.SemiBold, fontSize = 22.tvSp(), letterSpacing = 0.08.em))
    Spacer(Modifier.height(6.tvDp()))
}

@Composable
private fun Hair(scheme: SchemeTokens) {
    Box(Modifier.fillMaxWidth().height(2.tvDp()).background(scheme.ink.copy(alpha = FolioPalette.hairlineAlpha(scheme.id))))
}

@Composable
private fun SettingRow(
    title: String,
    meta: String,
    value: String,
    design: Design,
    scheme: SchemeTokens,
    menu: FocusRequester,
    strip: List<Pair<String, () -> Unit>>,
    onClick: () -> Unit,
    onFocus: () -> Unit = {},
    onKey: ((androidx.compose.ui.input.key.KeyEvent) -> Boolean)? = null,
    rowRequester: FocusRequester? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val open = strip.isNotEmpty()
    // The title is the only focusable on this row. The option chips are siblings,
    // not children: a focusable parent swallows the D-pad and the next Enter
    // hits the row again instead of Pin, Rename, Hide, or Move.
    val rememberedRow = remember { FocusRequester() }
    val row = rowRequester ?: rememberedRow
    val chips = remember(strip.size) { List(strip.size) { FocusRequester() } }
    val scope = rememberCoroutineScope()
    LaunchedEffect(open, strip.size) {
        if (!open || chips.isEmpty()) return@LaunchedEffect
        // The opening OK has already finished. Wait until the chips are attached,
        // then land on the first option so the next Enter (remote or keyboard) runs it.
        repeat(6) { attempt ->
            delay(if (attempt == 0) 80 else 40)
            val attached = runCatching {
                chips.first().requestFocus()
                true
            }.getOrDefault(false)
            if (attached) return@LaunchedEffect
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .height((if (open) 140 else 74).tvDp()),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .tvFocus(
                    row,
                    {
                        focused = it
                        if (it) onFocus()
                    },
                    {
                        if (open && chips.isNotEmpty()) {
                            // Second OK while the row still holds focus. Move after
                            // this key-up so the chip's gate does not see the same UP.
                            scope.launch {
                                delay(80)
                                runCatching { chips.first().requestFocus() }
                            }
                        } else {
                            onClick()
                        }
                    },
                    toLeft = menu,
                    toDown = chips.firstOrNull(),
                    onKey = onKey,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (focused && !open) PencilTick(scheme, Modifier.height(36.tvDp())) else Spacer(Modifier.width(6.tvDp()))
            Spacer(Modifier.width(16.tvDp()))
            BasicText(
                title,
                style = TextStyle(
                    color = scheme.ink,
                    fontFamily = if (focused) design.faces.focus else design.faces.display,
                    fontWeight = if (focused) FontWeight.Medium else FontWeight.Light,
                    fontSize = 42.tvSp(),
                ),
            )
            if (meta.isNotBlank()) {
                Spacer(Modifier.width(16.tvDp()))
                BasicText(meta, style = TextStyle(color = scheme.muted, fontFamily = design.faces.ui, fontSize = 28.tvSp()))
            }
            Spacer(Modifier.weight(1f))
            if (value.isNotBlank()) {
                BasicText(value, style = TextStyle(color = scheme.muted, fontFamily = design.faces.ui, fontSize = 28.tvSp()))
            }
        }
        if (open) {
            Spacer(Modifier.height(8.tvDp()))
            ActionStrip(strip, chips, row, design, scheme)
        }
    }
}

@Composable
private fun ActionStrip(
    actions: List<Pair<String, () -> Unit>>,
    requesters: List<FocusRequester>,
    up: FocusRequester,
    design: Design,
    scheme: SchemeTokens,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(56.tvDp()), modifier = Modifier.padding(start = 28.tvDp())) {
        actions.forEachIndexed { index, (label, click) ->
            var focused by remember { mutableStateOf(false) }
            Box(
                Modifier.tvFocus(
                    requesters[index],
                    { focused = it },
                    {
                        // Put focus back on the title before the row is rebuilt, so
                        // disposing this chip does not drop the D-pad onto the section menu.
                        runCatching { up.requestFocus() }
                        click()
                    },
                    toLeft = requesters.getOrNull(index - 1),
                    toRight = requesters.getOrNull(index + 1),
                    toUp = up,
                ),
            ) {
                FolioGlyph(label, focused, scheme, design, 30, design.faces.ui, FontWeight.Medium, stroke = true)
            }
        }
    }
}

private fun packageTail(packageName: String?): String {
    if (packageName.isNullOrBlank()) return ""
    val parts = packageName.split('.').filter { it.isNotBlank() }
    return parts.takeLast(2).joinToString(".")
}

@Composable
private fun RenameKeyboard(model: EveningViewModel, state: UiState, design: Design, scheme: SchemeTokens) {
    val seed = when (state.renameKind) {
        "app" -> (state.apps + state.hidden).firstOrNull { it.id == state.renameId }?.label.orEmpty()
        "input" -> state.sources.firstOrNull { it.id == state.renameId }?.label.orEmpty()
        "tickers" -> state.tickers
        "city" -> if (state.locationMode == "manual") state.manualCity else ""
        "custom-en" -> state.customLines.getOrNull(state.renameId?.toIntOrNull() ?: -1)?.english.orEmpty()
        "custom-zh" -> state.customLines.getOrNull(state.renameId?.toIntOrNull() ?: -1)?.chinese.orEmpty()
        else -> ""
    }
    var draft by remember(state.renameKind, state.renameId) { mutableStateOf(seed) }
    var symbols by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Color(FolioPalette.scrim(scheme.id))), contentAlignment = Alignment.TopStart) {
        Column(
            Modifier
                .offset(360.tvDp(), 150.tvDp())
                .width(1200.tvDp())
                .height(800.tvDp())
                .background(Color(0xFF000000L or FolioPalette.card(scheme.id).toLong()), RoundedCornerShape(14.tvDp()))
                .padding(36.tvDp()),
        ) {
            BasicText(
                when (state.renameKind) {
                    "tickers" -> "Tickers"
                    "city" -> "City"
                    "custom-zh" -> "Chinese"
                    else -> "Rename"
                },
                style = TextStyle(color = scheme.ink, fontFamily = design.faces.display, fontSize = 42.tvSp()),
            )
            Spacer(Modifier.height(16.tvDp()))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(96.tvDp())
                    .border(3.tvDp(), scheme.focus.pencil, RoundedCornerShape(8.tvDp()))
                    .padding(horizontal = 24.tvDp()),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicText(
                    draft.ifBlank { " " },
                    maxLines = 1,
                    style = TextStyle(color = scheme.ink, fontFamily = design.faces.display, fontSize = 48.tvSp()),
                )
            }
            Spacer(Modifier.height(20.tvDp()))
            FolioKeys(
                symbols = symbols,
                scheme = scheme,
                design = design,
                onType = { draft += it },
                onBackspace = { if (draft.isNotEmpty()) draft = draft.dropLast(1) },
                onClear = { draft = "" },
                onToggleSymbols = { symbols = !symbols },
                onDone = {
                    if (state.renameKind == "custom-en") {
                        model.commitRename(draft)
                    } else {
                        model.commitRename(draft)
                    }
                },
            )
        }
    }
}

@Composable
private fun FolioKeys(
    symbols: Boolean,
    scheme: SchemeTokens,
    design: Design,
    onType: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onToggleSymbols: () -> Unit,
    onDone: () -> Unit,
) {
    val rows = if (symbols) {
        listOf(
            "1234567890".map { KeySpec(it.toString(), 92) { onType(it.toString()) } },
            listOf("-", "/", ":", ";", "(", ")", "$", "&", "@").map { KeySpec(it, 92) { onType(it) } },
            listOf(".", ",", "?", "!", "'").map { KeySpec(it, 92) { onType(it) } } + KeySpec("⌫", 92, onBackspace),
        )
    } else {
        listOf(
            "qwertyuiop".map { KeySpec(it.toString(), 92) { onType(it.toString()) } },
            "asdfghjkl".map { KeySpec(it.toString(), 92) { onType(it.toString()) } },
            "zxcvbnm".map { KeySpec(it.toString(), 92) { onType(it.toString()) } } + KeySpec("⌫", 92, onBackspace),
        )
    }
    val bottom = listOf(
        KeySpec("中/EN", 160) {},
        KeySpec("123", 140, onToggleSymbols),
        KeySpec("space", 420) { onType(" ") },
        KeySpec("⌫ all", 160, onClear),
        KeySpec("Done", 200, onDone),
    )
    val grid = rows + listOf(bottom)
    val keys = remember(symbols) { grid.map { row -> List(row.size) { FocusRequester() } } }
    LaunchedEffect(symbols) {
        delay(40)
        runCatching { keys.first().first().requestFocus() }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.tvDp())) {
        grid.forEachIndexed { rowIndex, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.tvDp()),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (rowIndex < rows.size) Spacer(Modifier.weight(1f))
                row.forEachIndexed { column, spec ->
                    KeyCap(
                        spec, scheme, design, keys[rowIndex][column],
                        toLeft = keys[rowIndex].getOrNull(column - 1),
                        toRight = keys[rowIndex].getOrNull(column + 1),
                        toUp = keys.getOrNull(rowIndex - 1)?.let { it[column.coerceAtMost(it.lastIndex)] },
                        toDown = keys.getOrNull(rowIndex + 1)?.let { it[column.coerceAtMost(it.lastIndex)] },
                    )
                }
                if (rowIndex < rows.size) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private data class KeySpec(val label: String, val width: Int, val onClick: () -> Unit)

@Composable
private fun KeyCap(
    spec: KeySpec,
    scheme: SchemeTokens,
    design: Design,
    requester: FocusRequester,
    toLeft: FocusRequester?,
    toRight: FocusRequester?,
    toUp: FocusRequester?,
    toDown: FocusRequester?,
) {
    var focused by remember { mutableStateOf(false) }
    val fill = scheme.ink.copy(alpha = FolioPalette.keyFill(scheme.id))
    val shape = RoundedCornerShape(8.tvDp())
    Box(
        Modifier
            .width(spec.width.tvDp())
            .height(80.tvDp())
            .background(fill, shape)
            .then(if (focused) Modifier.border(3.tvDp(), scheme.focus.pencil, shape) else Modifier)
            .tvFocus(requester, { focused = it }, spec.onClick, toLeft = toLeft, toRight = toRight, toUp = toUp, toDown = toDown),
        contentAlignment = Alignment.Center,
    ) {
        if (focused) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(32.tvDp())
                    .background(scheme.focus.swipe.copy(alpha = 0.4f), RoundedCornerShape(bottomStart = 8.tvDp(), bottomEnd = 8.tvDp())),
            )
        }
        BasicText(
            spec.label,
            style = TextStyle(
                color = if (focused) scheme.ink else scheme.ink,
                fontFamily = design.faces.ui,
                fontWeight = FontWeight.Medium,
                fontSize = 34.tvSp(),
            ),
        )
    }
}
