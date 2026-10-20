package app.cleanlaunch.tv.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import app.cleanlaunch.tv.EveningViewModel
import app.cleanlaunch.tv.data.PhotoDecode
import app.cleanlaunch.tv.data.PhotoEntry
import app.cleanlaunch.tv.data.PhotoLibrary
import app.cleanlaunch.tv.sentence.FrontPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import app.cleanlaunch.tv.UiState
import app.cleanlaunch.tv.theme.Design
import app.cleanlaunch.tv.theme.SchemeTokens
import app.cleanlaunch.tv.theme.tvDp
import app.cleanlaunch.tv.theme.tvSp

@Composable
fun LegacySettingsUnused(model: EveningViewModel, state: UiState, design: Design, scheme: SchemeTokens) {
    val scroll = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(
                horizontal = design.layout.safeHorizontalPx.tvDp(),
                vertical = design.layout.safeVerticalPx.tvDp(),
            ),
    ) {
        BasicText(
            "Settings",
            style = TextStyle(
                color = scheme.ink,
                fontFamily = design.faces.headline,
                fontWeight = FontWeight.Light,
                fontSize = design.type.mastheadPx.tvSp(),
            ),
        )
        Spacer(Modifier.height(8.tvDp()))
        BodyText("Back returns home.", scheme.muted, design, design.type.footerPx)
        Spacer(Modifier.height(design.layout.sectionGapPx.tvDp()))
        SchemeSection(model, state, design, scheme)
        SectionGap(design)
        AppsSection(model, state, design, scheme)
        SectionGap(design)
        FrontPageSection(model, state, design, scheme)
        SectionGap(design)
        BackgroundSection(model, state, design, scheme)
        SectionGap(design)
        PhotoSection(model, state, design, scheme)
        SectionGap(design)
        IdleSection(model, state, design, scheme)
        SectionGap(design)
        DataSection(state, design, scheme)
        SectionGap(design)
        InputsSection(model, state, design, scheme)
        SectionGap(design)
        AboutSection(design, scheme)
        Spacer(Modifier.height(48.tvDp()))
    }
}

@Composable
private fun SectionGap(design: Design) {
    Spacer(Modifier.height(design.layout.sectionGapPx.tvDp()))
}

@Composable
private fun SchemeSection(model: EveningViewModel, state: UiState, design: Design, scheme: SchemeTokens) {
    Kicker("SCHEME", scheme, design)
    Spacer(Modifier.height(12.tvDp()))
    val requesters = remember { design.schemes.associate { it.id to FocusRequester() } }
    LaunchedEffect(Unit) {
        runCatching { requesters[state.schemeId]?.requestFocus() }
    }
    Row(Modifier.fillMaxWidth().height(280.tvDp())) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.tvDp())) {
            design.schemes.forEachIndexed { index, item ->
                val requester = requesters.getValue(item.id)
                val previous = design.schemes.getOrNull(index - 1)?.let { requesters[it.id] }
                val next = design.schemes.getOrNull(index + 1)?.let { requesters[it.id] }
                ChoiceRow(
                    label = item.name,
                    value = if (item.id == state.schemeId) "In use" else item.masthead,
                    scheme = scheme,
                    design = design,
                    requester = requester,
                    toUp = previous,
                    toDown = next,
                    onFocus = { model.previewScheme(item.id) },
                    onClick = { model.commitScheme(item.id) },
                )
            }
        }
        Spacer(Modifier.width(28.tvDp()))
        val previewId = state.previewSchemeId ?: state.schemeId
        SchemePreview(design.scheme(previewId), design, Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
fun SchemePreview(preview: SchemeTokens, design: Design, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(8.tvDp()))
            .background(preview.background)
            .padding(24.tvDp()),
    ) {
        BasicText(
            "${preview.masthead}  ·  Tuesday 20 October",
            style = TextStyle(
                color = preview.ink,
                fontFamily = design.faces.headline,
                fontSize = design.type.footerPx.tvSp(),
            ),
        )
        Spacer(Modifier.height(16.tvDp()))
        BasicText(
            "Newsreader, with Noto Serif beneath.",
            style = TextStyle(
                color = preview.ink,
                fontFamily = design.faces.headline,
                fontSize = design.type.uiPx.tvSp(),
            ),
        )
        Spacer(Modifier.height(18.tvDp()))
        Kicker(design.copy.indexKicker, preview, design)
        Spacer(Modifier.height(8.tvDp()))
        FocusSlot(focused = true, scheme = preview) {
            NameText("Netflix", true, preview, design, design.type.uiPx, serif = true, italic = true)
        }
    }
}

@Composable
private fun AppsSection(model: EveningViewModel, state: UiState, design: Design, scheme: SchemeTokens) {
    Kicker("APPS", scheme, design)
    Spacer(Modifier.height(8.tvDp()))
    BodyText(
        "Italic text, one row per installed app. Names are English. A name typed here is kept on this television. Pinned apps sit at the top with a star, then the ones you opened most recently. TV inputs stay in Sources.",
        scheme.muted,
        design,
        design.type.footerPx,
    )
    Spacer(Modifier.height(12.tvDp()))
    state.apps.forEach { app ->
        Spacer(Modifier.height(8.tvDp()))
        ChoiceRow(
            label = app.label,
            value = if (app.pinned) "Pinned" else "On the page",
            scheme = scheme,
            design = design,
            requester = remember(app.id) { FocusRequester() },
            onClick = { model.longPressApp(app.id) },
            onLong = { model.longPressApp(app.id) },
        )
        Spacer(Modifier.height(6.tvDp()))
        TvField(
            value = app.label,
            onValue = { model.setAppName(app.id, it) },
            scheme = scheme,
            design = design,
            hint = "Name on the page",
        )
    }
    Spacer(Modifier.height(8.tvDp()))
    ChoiceRow(
        label = "Add an installed app",
        value = "",
        scheme = scheme,
        design = design,
        requester = remember { FocusRequester() },
        onClick = { model.openPicker() },
    )
    state.hidden.forEach { app ->
        Spacer(Modifier.height(8.tvDp()))
        ChoiceRow(
            label = "Restore ${app.label}",
            value = "Hidden",
            scheme = scheme,
            design = design,
            requester = remember(app.id) { FocusRequester() },
            onClick = { model.restore(app.id) },
        )
    }
}

@Composable
private fun FrontPageSection(model: EveningViewModel, state: UiState, design: Design, scheme: SchemeTokens) {
    Kicker("FRONT PAGE", scheme, design)
    Spacer(Modifier.height(8.tvDp()))
    BodyText(
        "OK shows or hides a block. Left moves it up and Right moves it down. Custom lines stay even when a live source is quiet. Type with the on-screen keyboard or a paired Mac keyboard.",
        scheme.muted,
        design,
        design.type.footerPx,
    )
    Spacer(Modifier.height(12.tvDp()))
    state.front.forEach { pref ->
        ChoiceRow(
            label = FrontPage.label(pref.id),
            value = if (pref.enabled) "Shown" else "Hidden",
            scheme = scheme,
            design = design,
            requester = remember(pref.id) { FocusRequester() },
            onClick = { model.toggleFront(pref.id) },
            onKeyExtra = { event ->
                if (event.type != KeyEventType.KeyDown) return@ChoiceRow false
                when (event.key) {
                    Key.DirectionLeft -> {
                        model.moveFront(pref.id, -1)
                        true
                    }
                    Key.DirectionRight -> {
                        model.moveFront(pref.id, 1)
                        true
                    }
                    else -> false
                }
            },
        )
        Spacer(Modifier.height(8.tvDp()))
    }
    state.customLines.forEachIndexed { index, copy ->
        Spacer(Modifier.height(8.tvDp()))
        BodyText("Custom line ${index + 1}", scheme.ink, design, design.type.uiPx, serif = true)
        Spacer(Modifier.height(8.tvDp()))
        TvField(
            value = copy.english,
            onValue = { model.setCustomEnglish(index, it) },
            scheme = scheme,
            design = design,
            hint = "English",
        )
        Spacer(Modifier.height(8.tvDp()))
        TvField(
            value = copy.chinese,
            onValue = { model.setCustomChinese(index, it) },
            scheme = scheme,
            design = design,
            hint = "Chinese, optional",
        )
    }
    Spacer(Modifier.height(16.tvDp()))
    BodyText("Tickers", scheme.ink, design, design.type.uiPx, serif = true)
    Spacer(Modifier.height(8.tvDp()))
    BodyText(
        "Price and the day's change only. Commas separate symbols, eight at most.",
        scheme.muted,
        design,
        design.type.footerPx,
    )
    Spacer(Modifier.height(8.tvDp()))
    TvField(
        value = state.tickers,
        onValue = { model.setTickers(it) },
        scheme = scheme,
        design = design,
        hint = "AAPL, MSFT",
    )
}

@Composable
private fun BackgroundSection(model: EveningViewModel, state: UiState, design: Design, scheme: SchemeTokens) {
    Kicker("BACKGROUND", scheme, design)
    Spacer(Modifier.height(8.tvDp()))
    BodyText(
        "A slow wash behind the home page. It pauses when the page dims, when another app is in front, and while Settings is open. A photograph replaces it while Photos is not Off.",
        scheme.muted,
        design,
        design.type.footerPx,
    )
    Spacer(Modifier.height(12.tvDp()))
    ChoiceRow(
        label = "Gradient wave",
        value = if (state.wave) "On" else "Off",
        scheme = scheme,
        design = design,
        requester = remember { FocusRequester() },
        onClick = { model.setWave(!state.wave) },
    )
}

@Composable
private fun PhotoSection(model: EveningViewModel, state: UiState, design: Design, scheme: SchemeTokens) {
    Kicker("PHOTOS", scheme, design)
    Spacer(Modifier.height(8.tvDp()))
    BodyText(
        "Off keeps the paper. Fixed holds one photograph. Rotation crossfades after 5, 15, 30, or 60 minutes. Carousel slides every 10, 20, or 30 seconds and pans slowly. The show pauses when the page dims. Hold OK, then Up or Down, to reorder. OK sets it down. Left removes it. In Fixed, OK on a resting row chooses that photograph.",
        scheme.muted,
        design,
        design.type.footerPx,
    )
    Spacer(Modifier.height(8.tvDp()))
    BodyText(
        "Add from the television or a USB drive, or copy from a computer into the app photos folder. The path is shown on Front page.",
        scheme.muted,
        design,
        design.type.footerPx,
    )
    Spacer(Modifier.height(12.tvDp()))
    ChoiceRow(
        label = "Mode",
        value = PhotoLibrary.modeLabel(state.photoMode),
        scheme = scheme,
        design = design,
        requester = remember { FocusRequester() },
        onClick = { model.cyclePhotoMode() },
    )
    Spacer(Modifier.height(8.tvDp()))
    if (state.photoMode == "rotation") {
        ChoiceRow(
            label = "Every",
            value = "${state.photoMinutes} minutes",
            scheme = scheme,
            design = design,
            requester = remember { FocusRequester() },
            onClick = { model.cyclePhotoMinutes() },
        )
        Spacer(Modifier.height(8.tvDp()))
    }
    if (state.photoMode == "carousel") {
        ChoiceRow(
            label = "Every",
            value = "${state.photoSeconds} seconds",
            scheme = scheme,
            design = design,
            requester = remember { FocusRequester() },
            onClick = { model.cyclePhotoSeconds() },
        )
        Spacer(Modifier.height(8.tvDp()))
    }
    ChoiceRow(
        label = "Add from storage",
        value = "",
        scheme = scheme,
        design = design,
        requester = remember { FocusRequester() },
        onClick = { model.openPhotoPicker() },
    )
    Spacer(Modifier.height(8.tvDp()))
    ChoiceRow(
        label = "Folder",
        value = if (state.photoReadable) "Watching" else "Needs access",
        scheme = scheme,
        design = design,
        requester = remember { FocusRequester() },
        onClick = { model.requestPhotoRead() },
    )
    Spacer(Modifier.height(12.tvDp()))
    if (state.photos.isEmpty()) {
        BodyText("No photographs on the page yet.", scheme.muted, design, design.type.footerPx)
        return
    }
    state.photos.forEach { entry ->
        PhotoRow(entry, model, state, design, scheme)
        Spacer(Modifier.height(8.tvDp()))
    }
}

@Composable
private fun PhotoRow(
    entry: PhotoEntry,
    model: EveningViewModel,
    state: UiState,
    design: Design,
    scheme: SchemeTokens,
) {
    val lifting = state.liftingPhotoId == entry.id
    val showing = state.photoMode == "fixed" && entry.id == state.photoFixedId
    val value = when {
        lifting -> "Moving"
        showing -> "Showing"
        entry.source == "folder" -> "Folder"
        else -> "Stored"
    }
    var focused by remember { mutableStateOf(false) }
    FocusSlot(
        focused = focused,
        scheme = scheme,
        fill = true,
        modifier = Modifier
            .fillMaxWidth()
            .tvFocus(
                requester = remember(entry.id) { FocusRequester() },
                onFocusChange = { focused = it },
                onClick = {
                    if (lifting) model.dropPhoto() else model.choosePhoto(entry.id)
                },
                onLong = { model.liftPhoto(entry.id) },
                onKey = { event ->
                    if (event.type != KeyEventType.KeyDown) return@tvFocus false
                    when (event.key) {
                        Key.DirectionUp -> if (lifting) {
                            model.movePhoto(entry.id, -1)
                            true
                        } else {
                            false
                        }
                        Key.DirectionDown -> if (lifting) {
                            model.movePhoto(entry.id, 1)
                            true
                        } else {
                            false
                        }
                        Key.DirectionLeft -> {
                            model.removePhoto(entry.id)
                            true
                        }
                        else -> false
                    }
                },
            )
            .padding(vertical = 4.tvDp()),
    ) {
        PhotoThumb(entry.uri, scheme)
        Spacer(Modifier.width(16.tvDp()))
        NameText(entry.name, focused, scheme, design, design.type.uiPx, serif = false, italic = false, modifier = Modifier.weight(1f, fill = false))
        Spacer(Modifier.width(12.tvDp()))
        BodyText(
            value,
            if (focused && scheme.focus.style == "highlighter") scheme.focus.text else scheme.muted,
            design,
            design.type.footerPx,
        )
    }
}

@Composable
private fun PhotoThumb(uri: String, scheme: SchemeTokens) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(uri) {
        val decoded = withContext(Dispatchers.IO) { PhotoDecode.thumb(context, uri) }
        bitmap = decoded?.asImageBitmap()
    }
    val image = bitmap
    Box(
        Modifier
            .size(96.tvDp(), 54.tvDp())
            .clip(RoundedCornerShape(4.tvDp()))
            .background(scheme.rule),
    ) {
        if (image != null) {
            Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun IdleSection(model: EveningViewModel, state: UiState, design: Design, scheme: SchemeTokens) {
    Kicker("IDLE", scheme, design)
    Spacer(Modifier.height(8.tvDp()))
    BodyText("The page also dims on a second Back, once you are already home.", scheme.muted, design, design.type.footerPx)
    Spacer(Modifier.height(12.tvDp()))
    listOf(0 to "Off", 1 to "1 minute", 2 to "2 minutes", 5 to "5 minutes", 10 to "10 minutes").forEach { (minutes, label) ->
        ChoiceRow(
            label = label,
            value = if (state.idleMinutes == minutes) "In use" else "",
            scheme = scheme,
            design = design,
            requester = remember(label) { FocusRequester() },
            onClick = { model.setIdle(minutes) },
        )
        Spacer(Modifier.height(8.tvDp()))
    }
}

@Composable
private fun DataSection(state: UiState, design: Design, scheme: SchemeTokens) {
    Kicker("DATA SOURCES", scheme, design)
    Spacer(Modifier.height(8.tvDp()))
    BodyText(
        "Weather is Open-Meteo for a city that matches this television, then wttr.in. Set the city under Front page, or leave it automatic. Quotes are Tencent, then Sina: last price and the day's change. The ticker row stays off until symbols are added. Headlines are public RSS, BBC first, with a fallback, and a Traditional Chinese line only when translation succeeds. Calendar, the watchlist, and now playing stay off this page.",
        scheme.muted,
        design,
        design.type.footerPx,
    )
    Spacer(Modifier.height(12.tvDp()))
    state.providerLines.forEach { line ->
        BodyText("${line.name}  ·  ${line.source}  ·  ${line.slot}", scheme.ink, design, design.type.uiPx)
        Spacer(Modifier.height(8.tvDp()))
    }
}

@Composable
private fun InputsSection(model: EveningViewModel, state: UiState, design: Design, scheme: SchemeTokens) {
    Kicker("INPUTS", scheme, design)
    Spacer(Modifier.height(8.tvDp()))
    BodyText(
        "HDMI 1 to HDMI 4 are always on the home page. AV and TV tuner stay hidden until you show them. OK opens the port when the set allows it. The emulator usually reports no ports.",
        scheme.muted,
        design,
        design.type.footerPx,
    )
    Spacer(Modifier.height(12.tvDp()))
    ChoiceRow(
        label = "AV",
        value = if (state.showAv) "Shown" else "Hidden",
        scheme = scheme,
        design = design,
        requester = remember { FocusRequester() },
        onClick = { model.setShowAv(!state.showAv) },
    )
    Spacer(Modifier.height(8.tvDp()))
    ChoiceRow(
        label = "TV tuner",
        value = if (state.showTuner) "Shown" else "Hidden",
        scheme = scheme,
        design = design,
        requester = remember { FocusRequester() },
        onClick = { model.setShowTuner(!state.showTuner) },
    )
    Spacer(Modifier.height(8.tvDp()))
    state.sources.forEach { slot ->
        ChoiceRow(
            label = slot.label,
            value = slot.detail,
            scheme = scheme,
            design = design,
            requester = remember(slot.id) { FocusRequester() },
            onClick = { model.switchSource(slot) },
        )
        Spacer(Modifier.height(8.tvDp()))
    }
}

@Composable
private fun AboutSection(design: Design, scheme: SchemeTokens) {
    Kicker("ABOUT", scheme, design)
    Spacer(Modifier.height(8.tvDp()))
    BodyText("Clean Launch", scheme.ink, design, design.type.uiPx, serif = true)
    BodyText("A personal home screen for Android TV.", scheme.ink, design, design.type.uiPx)
    Spacer(Modifier.height(8.tvDp()))
    BodyText("Version 1.0", scheme.muted, design, design.type.footerPx)
    BodyText(
        "For Android TV, including sets without Google Play services.",
        scheme.muted,
        design,
        design.type.footerPx,
    )
    val face = if (design.faces.headlinesAreBoska) "Boska" else "Newsreader"
    BodyText(
        "Headlines are $face. Chinese is Noto Serif. Interface text is Familjen Grotesk. Boska can be dropped in later.",
        scheme.muted,
        design,
        design.type.footerPx,
    )
}

@Composable
fun ChoiceRow(
    label: String,
    value: String,
    scheme: SchemeTokens,
    design: Design,
    requester: FocusRequester,
    toUp: FocusRequester? = null,
    toDown: FocusRequester? = null,
    onFocus: () -> Unit = {},
    onClick: () -> Unit,
    onLong: (() -> Unit)? = null,
    onKeyExtra: ((androidx.compose.ui.input.key.KeyEvent) -> Boolean)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    FocusSlot(
        focused = focused,
        scheme = scheme,
        fill = true,
        modifier = Modifier
            .fillMaxWidth()
            .tvFocus(
                requester = requester,
                onFocusChange = {
                    focused = it
                    if (it) onFocus()
                },
                onClick = onClick,
                onLong = onLong,
                toUp = toUp,
                toDown = toDown,
                onKey = onKeyExtra,
            )
            .padding(vertical = 4.tvDp()),
    ) {
        NameText(label, focused, scheme, design, design.type.uiPx, serif = false, italic = false)
        Spacer(Modifier.weight(1f))
        if (value.isNotEmpty()) {
            BodyText(
                value,
                if (focused && scheme.focus.style == "highlighter") scheme.focus.text else scheme.muted,
                design,
                design.type.footerPx,
            )
        }
    }
}

@Composable
fun PickerScreen(model: EveningViewModel, state: UiState, design: Design, scheme: SchemeTokens) {
    val onHome = state.apps.map { it.id }.toSet() + state.hidden.map { it.id }
    val available = state.installed.filter { app.cleanlaunch.tv.data.AppCatalog.idFor(it.packageName, design.marks) !in onHome }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = design.layout.safeHorizontalPx.tvDp(),
                vertical = design.layout.safeVerticalPx.tvDp(),
            ),
    ) {
        BasicText(
            "Add an app",
            style = TextStyle(
                color = scheme.ink,
                fontFamily = design.faces.headline,
                fontSize = design.type.mastheadPx.tvSp(),
            ),
        )
        Spacer(Modifier.height(16.tvDp()))
        val done = remember { FocusRequester() }
        LaunchedEffect(Unit) { runCatching { done.requestFocus() } }
        ChoiceRow(
            label = "Done",
            value = "",
            scheme = scheme,
            design = design,
            requester = done,
            onClick = { model.closePicker() },
        )
        Spacer(Modifier.height(12.tvDp()))
        if (available.isEmpty()) {
            BodyText("Every launchable app is already on the page.", scheme.muted, design, design.type.uiPx)
        }
        available.forEach { app ->
            ChoiceRow(
                label = app.label,
                value = "Add",
                scheme = scheme,
                design = design,
                requester = remember(app.packageName) { FocusRequester() },
                onClick = { model.addInstalled(app.packageName) },
            )
            Spacer(Modifier.height(8.tvDp()))
        }
    }
}

@Composable
fun MoveMenu(model: EveningViewModel, state: UiState, design: Design, scheme: SchemeTokens) {
    val app = state.apps.firstOrNull { it.id == state.menuAppId }
        ?: state.hidden.firstOrNull { it.id == state.menuAppId }
    val title = app?.label ?: "App"
    val first = remember { FocusRequester() }
    LaunchedEffect(state.menuAppId) {
        if (state.menuAppId != null) runCatching { first.requestFocus() }
    }
    Box(Modifier.fillMaxSize().background(design.scrim), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .width(560.tvDp())
                .clip(RoundedCornerShape(8.tvDp()))
                .background(scheme.background)
                .padding(28.tvDp()),
        ) {
            BodyText(title, scheme.ink, design, design.type.uiPx, serif = true, italic = true)
            Spacer(Modifier.height(16.tvDp()))
            ChoiceRow(if (app?.pinned == true) "Unpin" else "Pin", "", scheme, design, first, onClick = {
                state.menuAppId?.let { model.togglePin(it) }
            })
            Spacer(Modifier.height(8.tvDp()))
            ChoiceRow("Move up", "", scheme, design, remember { FocusRequester() }, onClick = {
                state.menuAppId?.let { model.moveApp(it, -1) }
            })
            Spacer(Modifier.height(8.tvDp()))
            ChoiceRow("Move down", "", scheme, design, remember { FocusRequester() }, onClick = {
                state.menuAppId?.let { model.moveApp(it, 1) }
            })
            Spacer(Modifier.height(8.tvDp()))
            ChoiceRow("Hide", "", scheme, design, remember { FocusRequester() }, onClick = {
                state.menuAppId?.let { model.hideApp(it) }
            })
            Spacer(Modifier.height(8.tvDp()))
            ChoiceRow("Cancel", "", scheme, design, remember { FocusRequester() }, onClick = { model.closeMenu() })
        }
    }
}
