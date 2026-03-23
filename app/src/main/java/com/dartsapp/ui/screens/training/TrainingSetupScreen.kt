package com.dartsapp.ui.screens.training

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dartsapp.data.db.entity.PlayerEntity
import com.dartsapp.data.db.entity.TrainingSessionEntity
import com.dartsapp.domain.model.TrainingDifficulty
import com.dartsapp.domain.model.TrainingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CARD_CORNER = RoundedCornerShape(12.dp)
private val ACTION_BTN_H = 56.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrainingSetupScreen(
    viewModel: TrainingSetupViewModel,
    onBack: () -> Unit,
    onStartTraining: (mode: TrainingMode, difficulty: TrainingDifficulty, playerId: Long) -> Unit
) {
    val players by viewModel.players.collectAsState()
    val selectedPlayerId by viewModel.selectedPlayerId.collectAsState()
    val selectedMode by viewModel.selectedMode.collectAsState()
    val selectedDifficulty by viewModel.selectedDifficulty.collectAsState()
    val recentSessions by viewModel.recentSessions.collectAsState()

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
            // Player selection
            Text("Spieler", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (players.isEmpty()) {
                Text(
                    "Keine Spieler vorhanden",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    players.forEach { player ->
                        PlayerSelectCard(
                            player = player,
                            isSelected = player.id == selectedPlayerId,
                            onClick = { viewModel.selectPlayer(player.id) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Mode selection
            Text("Training", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
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

            Spacer(Modifier.height(24.dp))

            // Difficulty selection
            Text("Einstellungen", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
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

            // Recent results
            if (recentSessions.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text("Letzte Ergebnisse", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                recentSessions.forEach { session ->
                    RecentResultCard(session = session)
                    Spacer(Modifier.height(6.dp))
                }
            }

            Spacer(Modifier.height(24.dp))

            // Start button
            val canStart = selectedPlayerId != null && selectedMode != null
            Button(
                onClick = {
                    val mode = selectedMode ?: return@Button
                    val playerId = selectedPlayerId ?: return@Button
                    onStartTraining(mode, selectedDifficulty, playerId)
                },
                enabled = canStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ACTION_BTN_H),
                shape = CARD_CORNER
            ) {
                Text("Training starten", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun PlayerSelectCard(
    player: PlayerEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.size(72.dp),
        shape = CARD_CORNER,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = player.name,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
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
        modifier = modifier.height(96.dp),
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
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = mode.description(),
                    style = MaterialTheme.typography.labelSmall,
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
        modifier = modifier.height(56.dp),
        shape = CARD_CORNER,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = difficulty.displayName(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RecentResultCard(session: TrainingSessionEntity) {
    val mode = runCatching { TrainingMode.valueOf(session.mode) }.getOrNull()
    val difficulty = runCatching { TrainingDifficulty.valueOf(session.difficulty) }.getOrNull()
    val dateStr = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
        .format(Date(session.completedAt))
    val resultStr = when (mode) {
        TrainingMode.SCORING_ROUNDS -> "Ø ${session.result / 10.0} Punkte"
        else -> "${session.result} Darts"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CARD_CORNER,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = mode?.displayName() ?: session.mode,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = difficulty?.displayName() ?: session.difficulty,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = resultStr,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
