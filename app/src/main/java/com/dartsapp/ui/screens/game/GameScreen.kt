package com.dartsapp.ui.screens.game

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dartsapp.domain.model.DartInput
import com.dartsapp.ui.screens.game.components.BustDialog
import com.dartsapp.ui.screens.game.components.GameOverDialog
import com.dartsapp.ui.screens.game.components.PlayerScoreCard
import com.dartsapp.ui.screens.game.components.ScoreInputKeypad

@Composable
fun GameScreen(
    onGameOver: () -> Unit,
    onAbandonGame: () -> Unit,
    viewModel: GameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var bustInfo by remember { mutableStateOf<BustInfo?>(null) }
    var showAbandonDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.bustEvent.collect { info ->
            bustInfo = info
        }
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is GameUiState.GameOver  -> onGameOver()
            is GameUiState.Abandoned -> onAbandonGame()
            else -> Unit
        }
    }

    // Placement / game-over dialog
    if (uiState is GameUiState.Playing) {
        val playing = uiState as GameUiState.Playing
        playing.playerJustFinished?.let { finished ->
            GameOverDialog(
                playerJustFinished = finished,
                allPlayers         = playing.activeGame.players,
                canContinue        = !playing.allPlayersFinished,
                onContinue         = viewModel::continueAfterPlacement,
                onEndGame          = viewModel::endGame
            )
        }
    }

    bustInfo?.let { info ->
        BustDialog(bustInfo = info, onDismiss = { bustInfo = null })
    }

    if (showAbandonDialog) {
        Dialog(onDismissRequest = { showAbandonDialog = false }) {
            Surface(
                shape = MaterialTheme.shapes.large,
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Spiel abbrechen?",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Das laufende Spiel wird beendet und nicht gewertet.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            showAbandonDialog = false
                            viewModel.abandonGame()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Spiel abbrechen")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showAbandonDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Weiterspielen")
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Darts",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    if (uiState is GameUiState.Playing) {
                        IconButton(onClick = { showAbandonDialog = true }) {
                            Icon(Icons.Default.Close, contentDescription = "Spiel abbrechen")
                        }
                    }
                }
            }
        }
    ) { padding ->
        when (val state = uiState) {
            is GameUiState.Loading -> {
                Text("Laden...", modifier = Modifier.padding(padding).padding(16.dp))
            }
            is GameUiState.Playing -> {
                val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

                if (isLandscape) {
                    // ── Landscape: input left (~60%) | info right (~40%) ──
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        // Left: score input (dartboard or keypad)
                        ScoreInputKeypad(
                            dartsEntered = state.currentRoundDarts.size,
                            onDartEntered = viewModel::onDartEntered,
                            onUndo = viewModel::onUndoLastDart,
                            canUndo = state.currentRoundDarts.isNotEmpty() || state.lastCommittedRound != null,
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(0.58f)
                        )

                        VerticalDivider(modifier = Modifier.fillMaxHeight().padding(vertical = 8.dp))

                        // Right: player cards + round info + checkout
                        GameSidePanel(
                            state = state,
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(0.42f)
                                .padding(start = 4.dp, end = 8.dp)
                        )
                    }
                } else {
                    // ── Portrait: scores top, input bottom ────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp)
                    ) {
                        // Player cards
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(state.activeGame.players.indices.toList()) { idx ->
                                val player = state.activeGame.players[idx]
                                val isCurrent = idx == state.activeGame.currentPlayerIndex
                                PlayerScoreCard(
                                    player = player,
                                    isCurrentPlayer = isCurrent,
                                    displayScore = if (isCurrent) state.projectedScore else player.remainingScore
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        // Round info (portrait only)
                        RoundSummaryRow(state = state, modifier = Modifier.fillMaxWidth())

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        ScoreInputKeypad(
                            dartsEntered = state.currentRoundDarts.size,
                            onDartEntered = viewModel::onDartEntered,
                            onUndo = viewModel::onUndoLastDart,
                            canUndo = state.currentRoundDarts.isNotEmpty() || state.lastCommittedRound != null,
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        )
                    }
                }
            }
            is GameUiState.GameOver -> Unit // navigation handled via LaunchedEffect
            is GameUiState.Abandoned -> Unit
            is GameUiState.Error -> {
                Text(
                    "Fehler: ${state.message}",
                    modifier = Modifier.padding(padding).padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ── Right-side panel (landscape) ──────────────────────────────────────────────

@Composable
private fun GameSidePanel(
    state: GameUiState.Playing,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight().padding(vertical = 8.dp)
    ) {
        // Player cards – natural (rectangular) height, 2 per row, generous spacing
        val players = state.activeGame.players
        val rows = players.chunked(2)
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rows.forEach { rowPlayers ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowPlayers.forEachIndexed { colIdx, player ->
                        val globalIdx = players.indexOf(player)
                        val isCurrent = globalIdx == state.activeGame.currentPlayerIndex
                        PlayerScoreCard(
                            player = player,
                            isCurrentPlayer = isCurrent,
                            displayScore = if (isCurrent) state.projectedScore else player.remainingScore,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill empty slot if row has only one player
                    if (rowPlayers.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.weight(1f))

        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        // Dart throws + checkout suggestion – always visible
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            DartsInfoCard(
                darts = state.currentRoundDarts,
                total = state.roundTotal,
                isBust = state.isBust,
                modifier = Modifier.weight(1f)
            )

            CheckoutSuggestionCard(
                suggestion = state.checkoutSuggestion,
                modifier   = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DartsInfoCard(
    darts: List<DartInput>,
    total: Int,
    isBust: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isBust) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: label + dart values
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Würfe",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val dartLabels = if (darts.isEmpty()) "–"
                    else darts.joinToString("  –  ") { it.scoreValue.toString() }
                Text(
                    text = dartLabels,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            // Right: total score (hidden when no darts thrown yet)
            if (darts.isNotEmpty()) {
                Text(
                    text = "$total",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isBust) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun CheckoutSuggestionCard(
    suggestion: List<String>?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Checkout",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = if (suggestion != null) suggestion.joinToString("  –  ") else "–",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

// ── Portrait round summary ────────────────────────────────────────────────────

@Composable
private fun RoundSummaryRow(
    state: GameUiState.Playing,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Runde ${state.activeGame.roundNumber}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val dartsText = if (state.currentRoundDarts.isEmpty()) "–  –  –"
                else state.currentRoundDarts.joinToString("   ") { it.scoreValue.toString() }
            Text(
                text = dartsText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Gesamt: ${state.roundTotal}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.isBust) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface
            )
        }

        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(
                text = "Checkout",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (state.checkoutSuggestion != null) {
                state.checkoutSuggestion.forEach { dart ->
                    Text(
                        text = dart,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    )
                }
            } else {
                Text(
                    text = "–",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}
