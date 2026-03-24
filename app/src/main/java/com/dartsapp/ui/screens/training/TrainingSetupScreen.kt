package com.dartsapp.ui.screens.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dartsapp.data.db.entity.TrainingSessionEntity
import com.dartsapp.domain.model.TrainingDifficulty
import com.dartsapp.domain.model.TrainingMode
import com.dartsapp.ui.screens.setup.PlayerSelectionDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CARD_CORNER = RoundedCornerShape(8.dp)
private val ACTION_BTN_H = 64.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrainingSetupScreen(
    viewModel: TrainingSetupViewModel,
    onBack: () -> Unit,
    onStartTraining: (mode: TrainingMode, difficulty: TrainingDifficulty, playerId: Long) -> Unit,
    onStreuungClick: (playerId: Long) -> Unit = {}
) {
    val players by viewModel.players.collectAsState()
    val selectedPlayerId by viewModel.selectedPlayerId.collectAsState()
    val selectedMode by viewModel.selectedMode.collectAsState()
    val selectedDifficulty by viewModel.selectedDifficulty.collectAsState()
    val recentSessions by viewModel.recentSessions.collectAsState()

    var showPlayerDialog by remember { mutableStateOf(false) }

    if (showPlayerDialog) {
        PlayerSelectionDialog(
            players = players,
            selectedIds = listOfNotNull(selectedPlayerId),
            onToggle = { viewModel.selectPlayer(it) },
            onDismiss = { showPlayerDialog = false }
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
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                    Text("Training", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = { onStreuungClick(selectedPlayerId ?: 0L) },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Streuung", style = MaterialTheme.typography.titleMedium)
                    }
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
            // Top row: Player selection (left) + Recent results (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Left: Spieler
                Column {
                    Text("Spieler", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(4.dp))
                    if (players.isEmpty()) {
                        Text(
                            "Keine Spieler vorhanden",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    } else {
                        val selectedPlayer = players.find { it.id == selectedPlayerId }
                        Card(
                            onClick = { showPlayerDialog = true },
                            modifier = Modifier.size(144.dp),
                            shape = CARD_CORNER,
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedPlayer != null)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = selectedPlayer?.name ?: "?",
                                    style = MaterialTheme.typography.titleLarge,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Right: Letzte Ergebnisse
                if (recentSessions.isNotEmpty()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Letzte Ergebnisse", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(4.dp))
                        val firstRow = recentSessions.take(5)
                        val secondRow = recentSessions.drop(5).take(5)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            firstRow.forEach { session ->
                                RecentResultCard(session = session, modifier = Modifier.weight(1f))
                            }
                            repeat(5 - firstRow.size) { Spacer(Modifier.weight(1f)) }
                        }
                        if (secondRow.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                secondRow.forEach { session ->
                                    RecentResultCard(session = session, modifier = Modifier.weight(1f))
                                }
                                repeat(5 - secondRow.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Mode selection
            Text("Training", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TrainingMode.entries.forEach { mode ->
                    ModeCard(
                        mode = mode,
                        isSelected = mode == selectedMode,
                        onClick = { viewModel.selectMode(mode) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Difficulty selection
            Text("Level", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrainingDifficulty.entries.forEach { difficulty ->
                    DifficultyCard(
                        difficulty = difficulty,
                        isSelected = difficulty == selectedDifficulty,
                        onClick = { viewModel.selectDifficulty(difficulty) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Start button
            val canStart = selectedPlayerId != null
            Button(
                onClick = {
                    val playerId = selectedPlayerId ?: return@Button
                    onStartTraining(selectedMode, selectedDifficulty, playerId)
                },
                enabled = canStart,
                modifier = Modifier.fillMaxWidth().height(ACTION_BTN_H),
                shape = CARD_CORNER,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text("Training starten", fontSize = 28.sp)
            }
        }
    }
}

@Composable
private fun ModeCard(
    mode: TrainingMode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(112.dp),
        shape = CARD_CORNER,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = mode.displayName(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = mode.description(),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun DifficultyCard(
    difficulty: TrainingDifficulty,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(ACTION_BTN_H),
        shape = CARD_CORNER,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = difficulty.displayName(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RecentResultCard(session: TrainingSessionEntity, modifier: Modifier = Modifier) {
    val mode = runCatching { TrainingMode.valueOf(session.mode) }.getOrNull()
    val difficulty = runCatching { TrainingDifficulty.valueOf(session.difficulty) }.getOrNull()
    val dateStr = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
        .format(Date(session.completedAt))
    val resultStr = when (mode) {
        TrainingMode.SCORING_ROUNDS -> "Ø ${session.result / 10.0}"
        else -> "${session.result} Darts"
    }

    // Each row is (playerCardHeight - gap) / 2 = (144 - 6) / 2 = 69dp so two rows + gap = 144dp
    Card(
        modifier = modifier.height(69.dp),
        shape = CARD_CORNER,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = resultStr,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = difficulty?.displayName() ?: session.difficulty,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}
