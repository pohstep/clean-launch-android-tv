package app.cleanlaunch.tv.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 1 when the panel is 960 dp wide (1080p at density 320). Other densities scale with it. */
val LocalPaperScale = compositionLocalOf { 1f }

/** Pixels on a 1920×1080 television, scaled to the real panel. */
@Composable
fun Int.tvDp(): Dp = (this / 2f * LocalPaperScale.current).dp

@Composable
fun Float.tvDp(): Dp = (this / 2f * LocalPaperScale.current).dp

@Composable
fun Int.tvSp(): TextUnit = (this / 2f * LocalPaperScale.current).sp

@Composable
fun Float.tvSp(): TextUnit = (this / 2f * LocalPaperScale.current).sp
