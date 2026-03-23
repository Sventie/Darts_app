package com.dartsapp.ui.screens.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dartsapp.domain.model.TrainingDifficulty
import com.dartsapp.domain.model.TrainingMode

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
                                difficulty = viewModel.difficulty,
                                isLandscape = isLandscape,
                                onThrow = { viewModel.recordZielfeldThrow(it) }
                            )
                            is ModeState.AroundTheClock -> AroundTheClockContent(
                                state = modeState,
                                isLandscape = isLandscape,
                                onHit = { viewModel.recordAtcThrow(true) },
                                onMiss = { viewModel.recordAtcThrow(false) }
                            )
                            is ModeState.ScoringRounds -> ScoringRoundsContent(
                                state = modeState,
                                isLandscape = isLandscape,
                                onAppendDigit = { viewModel.scoringAppendDigit(it) },
                                onDelete = { viewModel.scoringDeleteDigit() },
                                onConfirm = { viewModel.scoringConfirmRound() }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ZielfeldContent(
    state: ModeState.Zielfeld,
    difficulty: TrainingDifficulty,
    isLandscape: Boolean,
    onThrow: (String) -> Unit
) {
    val fieldButtons = buildFieldButtons(difficulty)
    val progress = "${state.currentFieldIndex + 1} / ${state.targetFields.size}"

    if (isLandscape) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left: target + throws history
            Column(
                modifier = Modifier.weight(0.55f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ZielfeldTargetPanel(state = state, progress = progress)
            }
            // Right: field buttons
            Column(
                modifier = Modifier.weight(0.45f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Getroffenes Feld eingeben", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                FieldButtonGrid(fieldButtons = fieldButtons, onThrow = onThrow)
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ZielfeldTargetPanel(state = state, progress = progress)
            Spacer(Modifier.height(16.dp))
            Text("Getroffenes Feld eingeben", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            FieldButtonGrid(fieldButtons = fieldButtons, onThrow = onThrow)
        }
    }
}

@Composable
private fun ZielfeldTargetPanel(state: ModeState.Zielfeld, progress: String) {
    Text(progress, style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    Spacer(Modifier.height(8.dp))
    Card(
        modifier = Modifier.size(160.dp),
        shape = CARD_CORNER,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Zielfeld", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
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
        Text("Bisherige Würfe:", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            text = state.throwsForCurrentField.joinToString("  "),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
    if (state.completedFields.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        Text("Gesamt: ${state.totalDartsSoFar} Darts",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FieldButtonGrid(fieldButtons: List<String>, onThrow: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        fieldButtons.forEach { field ->
            FilledTonalButton(
                onClick = { onThrow(field) },
                shape = CARD_CORNER,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(field, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

private fun buildFieldButtons(difficulty: TrainingDifficulty): List<String> = buildList {
    for (i in 1..20) add("S$i")
    add("Bull")
    if (difficulty != TrainingDifficulty.BEGINNER) {
        for (i in 1..20) add("D$i")
    }
    if (difficulty == TrainingDifficulty.PRO) {
        for (i in 1..20) add("T$i")
        add("Bullseye")
    }
    add("Miss")
}

// ── Around the Clock ─────────────────────────────────────────────────────────

@Composable
private fun AroundTheClockContent(
    state: ModeState.AroundTheClock,
    isLandscape: Boolean,
    onHit: () -> Unit,
    onMiss: () -> Unit
) {
    if (isLandscape) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(0.55f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AtcTargetPanel(state = state)
                Spacer(Modifier.height(24.dp))
                AtcButtons(
                    requiresDouble = state.requiresDoubleForCurrent,
                    onHit = onHit,
                    onMiss = onMiss
                )
            }
            Column(
                modifier = Modifier.weight(0.45f).fillMaxHeight()
            ) {
                AtcProgressGrid(state = state)
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AtcTargetPanel(state = state)
            Spacer(Modifier.height(24.dp))
            AtcButtons(
                requiresDouble = state.requiresDoubleForCurrent,
                onHit = onHit,
                onMiss = onMiss
            )
            Spacer(Modifier.height(24.dp))
            AtcProgressGrid(state = state)
        }
    }
}

@Composable
private fun AtcTargetPanel(state: ModeState.AroundTheClock) {
    Text(
        "${state.completedNumbers.size} / 20",
        style = MaterialTheme.typography.labelLarge,
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
                Text(label, style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${state.currentNumber}",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )
                if (state.requiresDoubleForCurrent) {
                    Text(
                        "Double!",
                        style = MaterialTheme.typography.labelSmall,
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
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    )
}

@Composable
private fun AtcButtons(requiresDouble: Boolean, onHit: () -> Unit, onMiss: () -> Unit) {
    val hitLabel = if (requiresDouble) "Double getroffen" else "Getroffen"
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(
            onClick = onHit,
            modifier = Modifier.weight(1f).height(56.dp),
            shape = CARD_CORNER
        ) {
            Text(hitLabel, style = MaterialTheme.typography.titleMedium)
        }
        OutlinedButton(
            onClick = onMiss,
            modifier = Modifier.weight(1f).height(56.dp),
            shape = CARD_CORNER
        ) {
            Text("Verfehlt", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AtcProgressGrid(state: ModeState.AroundTheClock) {
    Text("Fortschritt", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(8.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        (1..20).forEach { n ->
            val isDone = n in state.completedNumbers
            val isCurrent = n == state.currentNumber
            Card(
                modifier = Modifier.size(40.dp),
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
                        style = MaterialTheme.typography.labelMedium,
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
    onAppendDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onConfirm: () -> Unit
) {
    if (isLandscape) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(0.5f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ScoringInfoPanel(state = state)
            }
            Column(
                modifier = Modifier.weight(0.5f).fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ScoringKeypad(
                    state = state,
                    onAppendDigit = onAppendDigit,
                    onDelete = onDelete,
                    onConfirm = onConfirm
                )
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScoringInfoPanel(state = state)
            Spacer(Modifier.height(24.dp))
            ScoringKeypad(
                state = state,
                onAppendDigit = onAppendDigit,
                onDelete = onDelete,
                onConfirm = onConfirm
            )
        }
    }
}

@Composable
private fun ScoringInfoPanel(state: ModeState.ScoringRounds) {
    Text(
        "Runde ${state.currentRound} / ${state.totalRounds}",
        style = MaterialTheme.typography.titleMedium,
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
            Text("Dein Ø", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
            Text(
                text = if (state.roundScores.isEmpty()) "–"
                       else String.format("%.1f", state.runningAverage),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Ziel: ${state.targetAverage}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
            )
        }
    }
    if (state.roundScores.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        Text("Letzte Runden:", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            text = state.roundScores.takeLast(5).joinToString("  •  "),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ScoringKeypad(
    state: ModeState.ScoringRounds,
    onAppendDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onConfirm: () -> Unit
) {
    Card(
        shape = CARD_CORNER,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (state.pendingInput.isEmpty()) "0" else state.pendingInput,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("⌫", "0", "✓")
            )
            rows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    row.forEach { key ->
                        when (key) {
                            "⌫" -> FilledTonalButton(
                                onClick = onDelete,
                                modifier = Modifier.size(64.dp),
                                shape = CARD_CORNER,
                                contentPadding = PaddingValues(0.dp)
                            ) { Text(key, style = MaterialTheme.typography.titleLarge) }
                            "✓" -> Button(
                                onClick = onConfirm,
                                enabled = state.pendingInput.isNotEmpty(),
                                modifier = Modifier.size(64.dp),
                                shape = CARD_CORNER,
                                contentPadding = PaddingValues(0.dp)
                            ) { Text(key, style = MaterialTheme.typography.titleLarge) }
                            else -> OutlinedButton(
                                onClick = { onAppendDigit(key) },
                                modifier = Modifier.size(64.dp),
                                shape = CARD_CORNER,
                                contentPadding = PaddingValues(0.dp)
                            ) { Text(key, style = MaterialTheme.typography.titleLarge) }
                        }
                    }
                }
            }
        }
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
                Text("Training beendet!", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(result.playerName, style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
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
