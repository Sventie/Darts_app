package com.dartsapp.ui.screens.game

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.dartsapp.ui.screens.game.components.BustDialog
import com.dartsapp.ui.screens.game.components.PlayerScoreCard
import com.dartsapp.ui.screens.game.components.ScoreInputKeypad

@OptIn(ExperimentalMaterial3Api::class)
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
            TopAppBar(
                title = { Text("Darts") },
                actions = {
                    if (uiState is GameUiState.Playing) {
                        IconButton(onClick = { showAbandonDialog = true }) {
                            Icon(Icons.Default.Close, contentDescription = "Spiel abbrechen")
                        }
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is GameUiState.Loading -> {
                Text("Loading...", modifier = Modifier.padding(padding).padding(16.dp))
            }
            is GameUiState.Playing -> {
                val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

                // Shared composables extracted as lambdas to avoid duplication
                val roundInfo: @Composable () -> Unit = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Round ${state.activeGame.roundNumber}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val dartsText = if (state.currentRoundDarts.isEmpty()) "–  –  –"
                            else state.currentRoundDarts.joinToString("   ") { it.scoreValue.toString() }
                        Text(
                            text = dartsText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Total: ${state.roundTotal}   |   Projected: ${state.projectedScore}",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = if (state.isBust) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                val playerList: @Composable (Modifier) -> Unit = { listModifier ->
                    LazyColumn(modifier = listModifier) {
                        items(state.activeGame.players.indices.toList()) { idx ->
                            val player = state.activeGame.players[idx]
                            PlayerScoreCard(
                                player = player,
                                isCurrentPlayer = idx == state.activeGame.currentPlayerIndex,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                if (isLandscape) {
                    // ── Landscape: input left (70%) | scores right (30%) ──
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(0.7f)
                                .padding(end = 8.dp)
                        ) {
                            roundInfo()
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))
                            ScoreInputKeypad(
                                dartsEntered = state.currentRoundDarts.size,
                                onDartEntered = viewModel::onDartEntered,
                                onUndo = viewModel::onUndoLastDart,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        VerticalDivider(modifier = Modifier.fillMaxHeight().padding(vertical = 8.dp))

                        playerList(
                            Modifier
                                .fillMaxHeight()
                                .weight(0.3f)
                                .padding(start = 8.dp)
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
                        playerList(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        roundInfo()

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        ScoreInputKeypad(
                            dartsEntered = state.currentRoundDarts.size,
                            onDartEntered = viewModel::onDartEntered,
                            onUndo = viewModel::onUndoLastDart,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            is GameUiState.GameOver -> {
                Text(
                    "${state.winnerName} wins!",
                    modifier = Modifier.padding(padding).padding(16.dp),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            is GameUiState.Abandoned -> Unit
            is GameUiState.Error -> {
                Text(
                    "Error: ${state.message}",
                    modifier = Modifier.padding(padding).padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
