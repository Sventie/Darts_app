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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
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
import com.dartsapp.ui.screens.game.components.PlayerScoreCard
import com.dartsapp.ui.screens.game.components.ScoreInputKeypad

@Composable
fun GameScreen(
    onGameOver: (Long) -> Unit,
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
            is GameUiState.GameOver -> onGameOver((uiState as GameUiState.GameOver).gameId)
            is GameUiState.Abandoned -> onAbandonGame()
            else -> Unit
        }
    }

    bustInfo?.let { info ->
        BustDialog(bustInfo = info, onDismiss = { bustInfo = null })
    }

    if (showAbandonDialog) {
        AlertDialog(
            onDismissRequest = { showAbandonDialog = false },
            title = { Text("Spiel abbrechen?") },
            text = { Text("Das laufende Spiel wird beendet und nicht gewertet.") },
            confirmButton = {
                TextButton(onClick = {
                    showAbandonDialog = false
                    viewModel.abandonGame()
                }) { Text("Abbrechen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showAbandonDialog = false }) { Text("Weiterspielen") }
            }
        )
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
                                .padding(start = 8.dp, end = 4.dp)
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
                                PlayerScoreCard(
                                    player = player,
                                    isCurrentPlayer = idx == state.activeGame.currentPlayerIndex
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
            is GameUiState.GameOver -> {
                Text(
                    "${state.winnerName} gewinnt!",
                    modifier = Modifier.padding(padding).padding(16.dp),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
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
        modifier = modifier.fillMaxHeight().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Player cards in a 2-column grid (weight(1f) so it fills available space)
        val players = state.activeGame.players
        val columns = if (players.size > 2) 2 else players.size.coerceAtLeast(1)
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(players.indices.toList()) { idx ->
                PlayerScoreCard(
                    player = players[idx],
                    isCurrentPlayer = idx == state.activeGame.currentPlayerIndex
                )
            }
        }

        HorizontalDivider()

        // Dart throws + checkout suggestion
        if (state.currentRoundDarts.isNotEmpty() || state.checkoutSuggestion != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Left: thrown darts + total (only when darts have been entered)
                if (state.currentRoundDarts.isNotEmpty()) {
                    DartsInfoCard(
                        darts = state.currentRoundDarts,
                        total = state.roundTotal,
                        isBust = state.isBust,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Right: checkout suggestion
                state.checkoutSuggestion?.let { suggestion ->
                    CheckoutSuggestionCard(
                        suggestion = suggestion,
                        modifier = if (state.currentRoundDarts.isEmpty()) Modifier.fillMaxWidth()
                        else Modifier.weight(1f)
                    )
                }
            }
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
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Würfe",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Individual dart scores
            val dartLabels = darts.joinToString("  –  ") { it.scoreValue.toString() }
            Text(
                text = dartLabels,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Gesamt",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$total",
                    style = MaterialTheme.typography.headlineSmall,
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
    suggestion: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Checkout",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
            suggestion.forEach { dart ->
                Text(
                    text = dart,
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

        state.checkoutSuggestion?.let { suggestion ->
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(
                    text = "Checkout",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                suggestion.forEach { dart ->
                    Text(
                        text = dart,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}
