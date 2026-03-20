package com.dartsapp.ui.screens.game

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dartsapp.ui.screens.game.components.BustDialog
import com.dartsapp.ui.screens.game.components.PlayerScoreCard
import com.dartsapp.ui.screens.game.components.ScoreInputKeypad

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    onGameOver: (Long) -> Unit,
    viewModel: GameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var bustInfo by remember { mutableStateOf<BustInfo?>(null) }

    LaunchedEffect(Unit) {
        viewModel.bustEvent.collect { info ->
            bustInfo = info
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is GameUiState.GameOver) {
            onGameOver((uiState as GameUiState.GameOver).gameId)
        }
    }

    bustInfo?.let { info ->
        BustDialog(bustInfo = info, onDismiss = { bustInfo = null })
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Darts") }) }
    ) { padding ->
        when (val state = uiState) {
            is GameUiState.Loading -> {
                Text("Loading...", modifier = Modifier.padding(padding).padding(16.dp))
            }
            is GameUiState.Playing -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                ) {
                    // Player score cards
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(state.activeGame.players.indices.toList()) { idx ->
                            val player = state.activeGame.players[idx]
                            PlayerScoreCard(
                                player = player,
                                isCurrentPlayer = idx == state.activeGame.currentPlayerIndex,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Round info
                    Text(
                        text = "Round ${state.activeGame.roundNumber}  |  " +
                            "Darts: ${state.currentRoundDarts.joinToString("  ") { it.scoreValue.toString() }}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Total this round: ${state.roundTotal}  →  Projected: ${state.projectedScore}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.isBust) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ScoreInputKeypad(
                        dartsEntered = state.currentRoundDarts.size,
                        onDartEntered = viewModel::onDartEntered,
                        onUndo = viewModel::onUndoLastDart,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            is GameUiState.GameOver -> {
                Text(
                    "${state.winnerName} wins!",
                    modifier = Modifier.padding(padding).padding(16.dp),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
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
