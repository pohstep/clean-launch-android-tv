package app.cleanlaunch.tv.ui

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import app.cleanlaunch.tv.EveningViewModel
import app.cleanlaunch.tv.UiState
import app.cleanlaunch.tv.data.NowPlayingInfo
import app.cleanlaunch.tv.data.NowPlayingPick
import app.cleanlaunch.tv.theme.Design
import app.cleanlaunch.tv.theme.FolioPalette
import app.cleanlaunch.tv.theme.SchemeTokens
import app.cleanlaunch.tv.theme.tvDp
import app.cleanlaunch.tv.theme.tvSp
import kotlinx.coroutines.delay

/** The three D-pad stops of the strip, left to right. */
class NowPlayingFocus {
    val previous = FocusRequester()
    val toggle = FocusRequester()
    val next = FocusRequester()
}

/**
 * A slim strip in the page header: small cover, title, artist, and previous, play or
 * pause, next. Newspaper-plain: hairlines, the scheme's ink, and the same focus marker
 * as the HDMI tiles. It is drawn only while a media session is playing or paused.
 */
@Composable
fun NowPlayingStrip(
    info: NowPlayingInfo,
    model: EveningViewModel,
    state: UiState,
    design: Design,
    scheme: SchemeTokens,
    focus: NowPlayingFocus,
    resting: Boolean,
    toRight: FocusRequester,
    toDown: FocusRequester?,
    modifier: Modifier = Modifier,
) {
    val line = scheme.ink.copy(alpha = FolioPalette.hairlineAlpha(scheme.id))
    Row(modifier.height(84.tvDp()), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(64.tvDp())
                .clip(RoundedCornerShape(4.tvDp()))
                .background(line)
                .border(2.tvDp(), line, RoundedCornerShape(4.tvDp())),
        ) {
            info.art?.let {
                Image(
                    remember(it) { it.asImageBitmap() },
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Spacer(Modifier.width(16.tvDp()))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            BasicText(
                info.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(color = scheme.ink, fontFamily = design.faces.ui, fontWeight = FontWeight.Medium, fontSize = 26.tvSp()),
            )
            BasicText(
                info.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(color = scheme.muted, fontFamily = design.faces.ui, fontSize = 22.tvSp()),
            )
            Spacer(Modifier.height(6.tvDp()))
            Progress(info, scheme, line)
        }
        Spacer(Modifier.width(12.tvDp()))
        Row(horizontalArrangement = Arrangement.spacedBy(8.tvDp())) {
            Control(Glyph.Previous, scheme, focus.previous, resting, model, { model.nowPlayingPrevious() },
                toLeft = null, toRight = focus.toggle, toDown = toDown)
            Control(if (info.playing) Glyph.Pause else Glyph.Play, scheme, focus.toggle, resting, model, { model.nowPlayingToggle() },
                toLeft = focus.previous, toRight = focus.next, toDown = toDown)
            Control(Glyph.Next, scheme, focus.next, resting, model, { model.nowPlayingNext() },
                toLeft = focus.toggle, toRight = toRight, toDown = toDown)
        }
    }
}

@Composable
private fun Progress(info: NowPlayingInfo, scheme: SchemeTokens, line: Color) {
    if (info.durationMs <= 0L) {
        Box(Modifier.fillMaxWidth().height(2.tvDp()).background(line))
        return
    }
    var now by remember { mutableStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(info.playing, info.positionAt) {
        now = SystemClock.elapsedRealtime()
        while (info.playing) {
            delay(1000)
            now = SystemClock.elapsedRealtime()
        }
    }
    val position = NowPlayingPick.position(info.positionMs, info.positionAt, now, info.speed, info.playing, info.durationMs)
    val share = (position.toFloat() / info.durationMs).coerceIn(0f, 1f)
    Box(Modifier.fillMaxWidth().height(2.tvDp()).background(line)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(share).background(scheme.ink.copy(alpha = 0.75f)))
    }
}

private enum class Glyph { Previous, Play, Pause, Next }

@Composable
private fun Control(
    glyph: Glyph,
    scheme: SchemeTokens,
    requester: FocusRequester,
    resting: Boolean,
    model: EveningViewModel,
    onClick: () -> Unit,
    toLeft: FocusRequester?,
    toRight: FocusRequester?,
    toDown: FocusRequester?,
) {
    var focused by remember { mutableStateOf(false) }
    val show = focused && !resting
    val line = scheme.ink.copy(alpha = FolioPalette.hairlineAlpha(scheme.id))
    val ink = if (show && scheme.id != "cream" && scheme.focus.text != Color.Unspecified) scheme.focus.text else scheme.ink
    Box(
        Modifier
            .size(56.tvDp())
            .clip(RoundedCornerShape(6.tvDp()))
            .border(2.tvDp(), if (show) scheme.focus.pencil else line, RoundedCornerShape(6.tvDp()))
            .tvFocus(requester, {
                focused = it
                if (it) model.reportFocus("np", null)
            }, onClick, toLeft = toLeft, toRight = toRight, toDown = toDown),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(24.tvDp())) {
            val w = size.width
            val h = size.height
            when (glyph) {
                Glyph.Play -> drawPath(
                    Path().apply {
                        moveTo(w * 0.2f, h * 0.08f)
                        lineTo(w * 0.92f, h * 0.5f)
                        lineTo(w * 0.2f, h * 0.92f)
                        close()
                    },
                    ink,
                )
                Glyph.Pause -> {
                    drawRect(ink, Offset(w * 0.14f, h * 0.1f), Size(w * 0.24f, h * 0.8f))
                    drawRect(ink, Offset(w * 0.62f, h * 0.1f), Size(w * 0.24f, h * 0.8f))
                }
                Glyph.Previous -> {
                    drawRect(ink, Offset(w * 0.08f, h * 0.1f), Size(w * 0.14f, h * 0.8f))
                    drawPath(
                        Path().apply {
                            moveTo(w * 0.94f, h * 0.1f)
                            lineTo(w * 0.3f, h * 0.5f)
                            lineTo(w * 0.94f, h * 0.9f)
                            close()
                        },
                        ink,
                    )
                }
                Glyph.Next -> {
                    drawRect(ink, Offset(w * 0.78f, h * 0.1f), Size(w * 0.14f, h * 0.8f))
                    drawPath(
                        Path().apply {
                            moveTo(w * 0.06f, h * 0.1f)
                            lineTo(w * 0.7f, h * 0.5f)
                            lineTo(w * 0.06f, h * 0.9f)
                            close()
                        },
                        ink,
                    )
                }
            }
        }
    }
}
