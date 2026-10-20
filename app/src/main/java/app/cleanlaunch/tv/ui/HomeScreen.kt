package app.cleanlaunch.tv.ui

import androidx.compose.runtime.Composable
import app.cleanlaunch.tv.EveningViewModel
import app.cleanlaunch.tv.UiState
import app.cleanlaunch.tv.theme.Design
import app.cleanlaunch.tv.theme.SchemeTokens

@Composable
fun HomeScreen(
    model: EveningViewModel,
    state: UiState,
    design: Design,
    scheme: SchemeTokens,
    dim: Float = 0f,
) {
    FolioFront(model, state, design, scheme)
}
