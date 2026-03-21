package com.dartsapp.ui.screens.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dartsapp.data.db.entity.PlayerEntity
import com.dartsapp.domain.model.CloseCondition
import com.dartsapp.domain.model.GameConfig
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private val CARD_CORNER = RoundedCornerShape(12.dp)
private val ACTION_BTN_H: Dp = 56.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GameSetupScreen(viewModel: GameSetupViewModel, onBack: () -> Unit) {
    val players by viewModel.players.collectAsState()
    val selectedIds by viewModel.selectedPlayerIds.collectAsState()
    val startingScore by viewModel.startingScore.collectAsState()
    val closeCondition by viewModel.closeCondition.collectAsState()
    var showPlayerDialog by remember { mutableStateOf(false) }

    val selectedPlayers = remember(players, selectedIds) {
        selectedIds.mapNotNull { id -> players.find { it.id == id } }
    }

    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        viewModel.reorderPlayers(from.index, to.index)
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
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                    Text("Neues Spiel", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Spieler", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            LazyRow(
                state = lazyListState,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedPlayers, key = { it.id }) { player ->
                    ReorderableItem(reorderState, key = player.id) { _ ->
                        PlayerCard(player = player, modifier = Modifier.draggableHandle())
                    }
                }
                item(key = "add_button") {
                    AddPlayerCard(onClick = { showPlayerDialog = true })
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { viewModel.randomizePlayerOrder() },
                enabled = selectedIds.size > 1,
                modifier = Modifier.fillMaxWidth().height(ACTION_BTN_H),
                shape = CARD_CORNER
            ) {
                Text("Zufällige Reihenfolge", style = MaterialTheme.typography.bodyLarge)
            }

            Spacer(Modifier.height(24.dp))
            Text("Startpunkte", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GameConfig.validStartingScores.forEach { score ->
                    ScoreCard(
                        score = score,
                        isSelected = startingScore == score,
                        onClick = { viewModel.setStartingScore(score) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Abschluss", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CloseConditionCard(
                    label = "Single Out",
                    isSelected = closeCondition == CloseCondition.SINGLE_OUT,
                    onClick = { viewModel.setCloseCondition(CloseCondition.SINGLE_OUT) },
                    modifier = Modifier.weight(1f)
                )
                CloseConditionCard(
                    label = "Double Out",
                    isSelected = closeCondition == CloseCondition.DOUBLE_OUT,
                    onClick = { viewModel.setCloseCondition(CloseCondition.DOUBLE_OUT) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { viewModel.startGame() },
                enabled = selectedIds.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(ACTION_BTN_H),
                shape = CARD_CORNER
            ) {
                Text("Spiel starten", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    if (showPlayerDialog) {
        PlayerSelectionDialog(
            players = players,
            selectedIds = selectedIds,
            onToggle = { viewModel.togglePlayer(it) },
            onDismiss = { showPlayerDialog = false }
        )
    }
}

@Composable
private fun PlayerCard(player: PlayerEntity, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.size(144.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = player.name.take(2).uppercase(),
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun AddPlayerCard(onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.size(144.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Add, contentDescription = "Spieler hinzufügen")
        }
    }
}

@Composable
private fun ScoreCard(score: Int, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.size(width = 160.dp, height = 112.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("$score", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun CloseConditionCard(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(ACTION_BTN_H),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
