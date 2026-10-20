package app.cleanlaunch.tv.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.text.BasicText
import app.cleanlaunch.tv.EveningViewModel
import app.cleanlaunch.tv.UiState
import app.cleanlaunch.tv.sentence.ClauseBit
import app.cleanlaunch.tv.sentence.Clauses
import app.cleanlaunch.tv.sentence.SentenceLayout
import app.cleanlaunch.tv.theme.Design
import app.cleanlaunch.tv.theme.SchemeTokens
import app.cleanlaunch.tv.theme.tvDp
import app.cleanlaunch.tv.theme.tvSp

@Composable
fun EditorScreen(model: EveningViewModel, state: UiState, design: Design, scheme: SchemeTokens) {
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
            "Sentence",
            style = TextStyle(
                color = scheme.ink,
                fontFamily = design.faces.headline,
                fontWeight = FontWeight.Light,
                fontSize = design.type.mastheadPx.tvSp(),
            ),
        )
        Spacer(Modifier.height(16.tvDp()))
        val clausesFocus = remember { FocusRequester() }
        androidx.compose.runtime.LaunchedEffect(Unit) {
            runCatching { clausesFocus.requestFocus() }
        }
        Row {
            ModeChip("Clauses", !state.templateMode, model, design, scheme, clausesFocus) { model.setTemplateMode(false) }
            Spacer(Modifier.width(16.tvDp()))
            ModeChip("Template", state.templateMode, model, design, scheme, remember { FocusRequester() }) { model.setTemplateMode(true) }
        }
        Spacer(Modifier.height(design.layout.sectionGapPx.tvDp()))
        Kicker("PREVIEW", scheme, design)
        Spacer(Modifier.height(8.tvDp()))
        Preview(state, design, scheme)
        Spacer(Modifier.height(design.layout.sectionGapPx.tvDp()))
        if (state.templateMode) {
            TemplateEditor(model, state, design, scheme)
        } else {
            ClauseEditor(model, state, design, scheme)
        }
    }
}

@Composable
private fun ModeChip(
    label: String,
    selected: Boolean,
    model: EveningViewModel,
    design: Design,
    scheme: SchemeTokens,
    requester: FocusRequester,
    onClick: () -> Unit,
) {
    ChoiceRow(
        label = label,
        value = if (selected) "In use" else "",
        scheme = scheme,
        design = design,
        requester = requester,
        onClick = onClick,
    )
}

@Composable
private fun Preview(state: UiState, design: Design, scheme: SchemeTokens) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val style = TextStyle(
        fontFamily = design.faces.headline,
        fontWeight = FontWeight.Light,
        fontSize = design.type.sentencePx.tvSp(),
    )
    val screen = with(density) { 1920.tvDp().toPx() }
    val width = (screen - design.layout.safeHorizontalPx * 2 - design.layout.columnGapPx) *
        design.layout.sentenceWeight / (design.layout.sentenceWeight + design.layout.indexWeight)
    val measure = { text: String ->
        if (text.isEmpty()) 0f else measurer.measure(text, style = style, maxLines = 1).size.width.toFloat()
    }
    val fitted = if (state.templateMode) {
        SentenceLayout.fitTemplate(state.template, modelTokens(state), width, measure, design.layout.maxSentenceLines)
    } else {
        val enabled = state.clauses.filter { it.enabled }
        SentenceLayout.fitClauses(
            enabled.map { ClauseBit(it.id, Clauses.meta(it.id).label, Clauses.meta(it.id).priority) },
            state.sentence,
            width,
            measure,
            design.layout.maxSentenceLines,
        )
    }
    if (fitted.lines.isEmpty()) {
        BodyText("Nothing is turned on.", scheme.muted, design, design.type.uiPx, serif = true)
    } else {
        fitted.lines.forEach { line ->
            BasicText(
                line,
                style = TextStyle(
                    color = scheme.ink,
                    fontFamily = design.faces.headline,
                    fontSize = design.type.uiPx.tvSp(),
                    lineHeight = (design.type.uiPx * 1.35f).tvSp(),
                ),
            )
        }
    }
    if (fitted.dropped.isNotEmpty()) {
        Spacer(Modifier.height(8.tvDp()))
        BodyText("Dropped to fit: ${fitted.dropped.joinToString(", ")}", scheme.muted, design, design.type.footerPx)
    }
}

private fun modelTokens(state: UiState) = app.cleanlaunch.tv.sentence.SentenceComposer.tokens(state.sentence)

@Composable
private fun ClauseEditor(model: EveningViewModel, state: UiState, design: Design, scheme: SchemeTokens) {
    BodyText(
        "OK turns a line on or off. Hold OK, then Up or Down, to move it.",
        scheme.muted,
        design,
        design.type.footerPx,
    )
    Spacer(Modifier.height(12.tvDp()))
    state.clauses.forEach { clause ->
        val meta = Clauses.meta(clause.id)
        val moving = state.movingClauseId == clause.id
        ChoiceRow(
            label = meta.label,
            value = when {
                moving -> "Moving"
                clause.enabled -> "On"
                else -> "Off"
            },
            scheme = scheme,
            design = design,
            requester = remember(clause.id) { FocusRequester() },
            onClick = {
                if (moving) model.dropClause() else model.toggleClause(clause.id)
            },
            onLong = { model.liftClause(clause.id) },
            onKeyExtra = if (moving) {
                { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                        model.moveClause(-1)
                        true
                    } else if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                        model.moveClause(1)
                        true
                    } else {
                        false
                    }
                }
            } else {
                null
            },
        )
        Spacer(Modifier.height(8.tvDp()))
    }
    Spacer(Modifier.height(8.tvDp()))
    Row(verticalAlignment = Alignment.CenterVertically) {
        KeyboardIcon(scheme.accent, Modifier.size(design.type.uiPx.tvDp()))
        Spacer(Modifier.width(12.tvDp()))
        BodyText("Custom line", scheme.ink, design, design.type.uiPx)
    }
    Spacer(Modifier.height(8.tvDp()))
    TvField(
        value = state.customText,
        onValue = { model.setCustom(it) },
        scheme = scheme,
        design = design,
        hint = "A line of your own",
    )
    Spacer(Modifier.height(8.tvDp()))
    BodyText(
        "Type with the on-screen keyboard, or adb shell input text.",
        scheme.muted,
        design,
        design.type.footerPx,
    )
}

@Composable
private fun TemplateEditor(model: EveningViewModel, state: UiState, design: Design, scheme: SchemeTokens) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        KeyboardIcon(scheme.accent, Modifier.size(design.type.uiPx.tvDp()))
        Spacer(Modifier.width(12.tvDp()))
        BodyText("Template", scheme.ink, design, design.type.uiPx)
    }
    Spacer(Modifier.height(8.tvDp()))
    TvField(
        value = state.template,
        onValue = { model.setTemplate(it) },
        scheme = scheme,
        design = design,
        hint = "It's {time}. {weather}.",
    )
    Spacer(Modifier.height(8.tvDp()))
    BodyText(
        "Tokens: {time} {date} {weather} {event} {watchlist} {now_playing} {custom}",
        scheme.muted,
        design,
        design.type.footerPx,
    )
    BodyText(
        "Type with the on-screen keyboard, or adb shell input text. A space is %s.",
        scheme.muted,
        design,
        design.type.footerPx,
    )
}
