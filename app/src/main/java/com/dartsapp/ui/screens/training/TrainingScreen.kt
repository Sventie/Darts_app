package com.dartsapp.ui.screens.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dartsapp.domain.model.DartInput
import com.dartsapp.domain.model.TrainingMode
import com.dartsapp.ui.screens.game.components.ScoreInputKeypad

private val CARD_CORNER = RoundedCornerShape(12.dp)

@Composable
fun TrainingScreen(
    viewModel: TrainingViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

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
                    Text(
                        viewModel.mode.displayName(),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        viewModel.difficulty.displayName(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is TrainingUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Wird geladen…")
                    }
                }
                is TrainingUiState.Running -> {
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        val isLandscape = maxWidth > maxHeight
                        when (val modeState = state.modeState) {
                            is ModeState.Zielfeld -> ZielfeldContent(
                                state = modeState,
                                isLandscape = isLandscape,
                                onDartEntered = { viewModel.recordZielfeldDart(it) },
                                canUndo = modeState.throwsForCurrentField.isNotEmpty(),
                                onUndo = { viewModel.undoZielfeldThrow() }
                            )
                            is ModeState.AroundTheClock -> AroundTheClockContent(
                                state = modeState,
                                isLandscape = isLandscape,
                                onDartEntered = { viewModel.recordAtcDart(it) }
                            )
                            is ModeState.ScoringRounds -> ScoringRoundsContent(
                                state = modeState,
                                isLandscape = isLandscape,
                                onDartEntered = { viewModel.recordScoringDart(it) },
                                onUndo = { viewModel.undoScoringDart() }
                            )
                        }
                    }
                }
                is TrainingUiState.Finished -> {
                    FinishedDialog(
                        result = state.result,
                        onRestart = { viewModel.restart() },
                        onBack = onBack
                    )
                }
            }
        }
    }
}

// ── Zielfeld ─────────────────────────────────────────────────────────────────

@Composable
private fun ZielfeldContent(
    state: ModeState.Zielfeld,
    isLandscape: Boolean,
    onDartEntered: (DartInput) -> Unit,
    canUndo: Boolean,
    onUndo: () -> Unit
) {
    val progress = "${state.currentFieldIndex + 1} / ${state.targetFields.size}"

    val lastDartAsList = listOfNotNull(state.throwDartsForCurrentField.lastOrNull())

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {
            ScoreInputKeypad(
                currentRoundDarts = lastDartAsList,
                onDartEntered = onDartEntered,
                onUndo = onUndo,
                canUndo = canUndo,
                modifier = Modifier.fillMaxHeight().weight(0.58f)
            )
            VerticalDivider(modifier = Modifier.fillMaxHeight().padding(vertical = 8.dp))
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.42f)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ZielfeldInfoPanel(state = state, progress = progress)
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ZielfeldInfoPanel(state = state, progress = progress)
            }
            ScoreInputKeypad(
                currentRoundDarts = lastDartAsList,
                onDartEntered = onDartEntered,
                onUndo = onUndo,
                canUndo = canUndo,
                modifier = Modifier.fillMaxWidth().weight(0.6f)
            )
        }
    }
}

@Composable
private fun ZielfeldInfoPanel(state: ModeState.Zielfeld, progress: String) {
    Text(
        progress,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    )
    Spacer(Modifier.height(8.dp))
    Card(
        modifier = Modifier.size(160.dp),
        shape = CARD_CORNER,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Zielfeld",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = state.currentField,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
    if (state.throwsForCurrentField.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        Text("Bisherige Würfe:", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            text = state.throwsForCurrentField.joinToString("  "),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
    }
    if (state.completedFields.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        Text(
            "Gesamt: ${state.totalDartsSoFar} Darts",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

// ── Around the Clock ─────────────────────────────────────────────────────────

@Composable
private fun AroundTheClockContent(
    state: ModeState.AroundTheClock,
    isLandscape: Boolean,
    onDartEntered: (DartInput) -> Unit
) {
    val lastDartAsList = listOfNotNull(state.lastDart)

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {
            ScoreInputKeypad(
                currentRoundDarts = lastDartAsList,
                onDartEntered = onDartEntered,
                onUndo = {},
                canUndo = false,
                modifier = Modifier.fillMaxHeight().weight(0.58f)
            )
            VerticalDivider(modifier = Modifier.fillMaxHeight().padding(vertical = 8.dp))
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.42f)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AtcTargetPanel(state = state)
                Spacer(Modifier.height(16.dp))
                AtcProgressGrid(state = state)
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AtcTargetPanel(state = state)
                Spacer(Modifier.height(12.dp))
                AtcProgressGrid(state = state)
            }
            ScoreInputKeypad(
                currentRoundDarts = lastDartAsList,
                onDartEntered = onDartEntered,
                onUndo = {},
                canUndo = false,
                modifier = Modifier.fillMaxWidth().weight(0.6f)
            )
        }
    }
}

@Composable
private fun AtcTargetPanel(state: ModeState.AroundTheClock) {
    Text(
        "${state.completedNumbers.size} / 20",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    )
    Spacer(Modifier.height(8.dp))
    Card(
        modifier = Modifier.size(160.dp),
        shape = CARD_CORNER,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val label = if (state.requiresDoubleForCurrent) "Treffe D${state.currentNumber}" else "Treffe ${state.currentNumber}"
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${state.currentNumber}",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )
                if (state.requiresDoubleForCurrent) {
                    Text(
                        "Double!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        "Gesamt: ${state.totalDarts} Darts",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AtcProgressGrid(state: ModeState.AroundTheClock) {
    Text("Fortschritt", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        (1..20).forEach { n ->
            val isDone = n in state.completedNumbers
            val isCurrent = n == state.currentNumber
            Card(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isDone -> MaterialTheme.colorScheme.primaryContainer
                        isCurrent -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "$n",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ── Scoring Rounds ────────────────────────────────────────────────────────────

@Composable
private fun ScoringRoundsContent(
    state: ModeState.ScoringRounds,
    isLandscape: Boolean,
    onDartEntered: (DartInput) -> Unit,
    onUndo: () -> Unit
) {
    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {
            ScoreInputKeypad(
                currentRoundDarts = state.pendingDarts,
                onDartEntered = onDartEntered,
                onUndo = onUndo,
                canUndo = state.pendingDarts.isNotEmpty(),
                modifier = Modifier.fillMaxHeight().weight(0.58f)
            )
            VerticalDivider(modifier = Modifier.fillMaxHeight().padding(vertical = 8.dp))
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.42f)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ScoringInfoPanel(state = state)
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ScoringInfoPanel(state = state)
            }
            ScoreInputKeypad(
                currentRoundDarts = state.pendingDarts,
                onDartEntered = onDartEntered,
                onUndo = onUndo,
                canUndo = state.pendingDarts.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().weight(0.6f)
            )
        }
    }
}

@Composable
private fun ScoringInfoPanel(state: ModeState.ScoringRounds) {
    Text(
        "Runde ${state.currentRound} / ${state.totalRounds}",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    )
    Spacer(Modifier.height(8.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CARD_CORNER,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Dein Ø",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Text(
                text = if (state.roundScores.isEmpty()) "–"
                       else String.format("%.1f", state.runningAverage),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Ziel: ${state.targetAverage}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
            )
        }
    }
    if (state.pendingDarts.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CARD_CORNER,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${state.pendingDarts.size} / 3 Darts",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    "${state.pendingScore} Punkte",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
    if (state.roundScores.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        Text("Letzte Runden:", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            text = state.roundScores.takeLast(5).joinToString("  •  "),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
    }
}

// ── Finished Dialog ───────────────────────────────────────────────────────────

@Composable
private fun FinishedDialog(
    result: TrainingResult,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Training beendet!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    result.playerName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = when (result.mode) {
                        TrainingMode.SCORING_ROUNDS ->
                            "Ø ${result.primaryResult / 10.0} Punkte / Runde"
                        else ->
                            "${result.primaryResult} Darts für ${result.fieldsCompleted} Felder"
                    },
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        shape = CARD_CORNER
                    ) {
                        Text("Zurück")
                    }
                    Button(
                        onClick = onRestart,
                        modifier = Modifier.weight(1f),
                        shape = CARD_CORNER
                    ) {
                        Text("Nochmal")
                    }
                }
            }
        }
    }
}
