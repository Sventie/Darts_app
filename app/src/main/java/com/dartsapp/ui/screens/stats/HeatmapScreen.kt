package com.dartsapp.ui.screens.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.dartsapp.data.db.entity.PlayerEntity
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeatmapScreen(
    onBack: () -> Unit,
    viewModel: HeatmapViewModel = hiltViewModel()
) {
    val playerName          by viewModel.playerName.collectAsState()
    val allPlayers          by viewModel.allPlayers.collectAsState()
    val selectedId          by viewModel.selectedPlayerId.collectAsState()
    val hitPositions        by viewModel.hitPositions.collectAsState()
    val gameCount           by viewModel.gameCount.collectAsState()
    val fromGame            by viewModel.fromGame.collectAsState()
    val toGame              by viewModel.toGame.collectAsState()
    val dispersion          by viewModel.dispersion.collectAsState()
    val trainingThrowCount  by viewModel.trainingThrowCount.collectAsState()

    var playerDialogOpen by remember { mutableStateOf(false) }
    var showHeatmap      by remember { mutableStateOf(true) }

    // Clamp displayed "to" value against the actual game count
    val effectiveTo = if (toGame == Int.MAX_VALUE) gameCount else toGame.coerceAtMost(gameCount)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Heatmap – $playerName") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Left: filter panel ────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.32f)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Spieler",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick  = { playerDialogOpen = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text     = playerName.ifEmpty { "Spieler wählen" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                HorizontalDivider()

                Text(
                    "Ansicht",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = showHeatmap,
                        onClick  = { showHeatmap = true },
                        label    = { Text("Heatmap") }
                    )
                    FilterChip(
                        selected = !showHeatmap,
                        onClick  = { showHeatmap = false },
                        label    = { Text("Streuung") }
                    )
                }

                if (!showHeatmap) {
                    Text(
                        text  = "Ø Abweichung: ${"%.2f".format(dispersion)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text  = "Basis: $trainingThrowCount Trainingswürfe",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (showHeatmap) {
                    HorizontalDivider()

                    Text(
                        "Spielbereich",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    when {
                        gameCount == 0 -> Text(
                            "Noch keine Spiele.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        gameCount == 1 -> Text(
                            "Spiel 1 von 1",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        else -> {
                            Text(
                                text  = "Spiel $fromGame – $effectiveTo von $gameCount",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Text(
                                "Von",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value         = fromGame.toFloat(),
                                onValueChange = { v ->
                                    val newFrom = v.roundToInt().coerceIn(1, effectiveTo)
                                    viewModel.setGameRange(newFrom, toGame)
                                },
                                valueRange    = 1f..gameCount.toFloat(),
                                steps         = (gameCount - 2).coerceAtLeast(0),
                                modifier      = Modifier.fillMaxWidth()
                            )

                            Text(
                                "Bis",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value         = effectiveTo.toFloat(),
                                onValueChange = { v ->
                                    val newTo = v.roundToInt().coerceIn(fromGame, gameCount)
                                    viewModel.setGameRange(fromGame, newTo)
                                },
                                valueRange    = 1f..gameCount.toFloat(),
                                steps         = (gameCount - 2).coerceAtLeast(0),
                                modifier      = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            VerticalDivider(modifier = Modifier.fillMaxHeight().padding(vertical = 8.dp))

            // ── Right: heatmap / dispersion ──────────────────────────────
            Box(
                modifier         = Modifier.fillMaxHeight().weight(0.68f),
                contentAlignment = Alignment.Center
            ) {
                val boardModifier = Modifier
                    .fillMaxHeight(0.92f)
                    .aspectRatio(1f)
                if (showHeatmap) {
                    DartBoardHeatmap(
                        hitPositions = hitPositions,
                        modifier     = boardModifier
                    )
                } else {
                    DartBoardDispersion(
                        dispersion = dispersion,
                        modifier   = boardModifier
                    )
                }
            }
        }

        // ── Player selection dialog ───────────────────────────────────────
        if (playerDialogOpen) {
            SelectPlayerDialog(
                allPlayers = allPlayers,
                selectedId = selectedId,
                onDismiss  = { playerDialogOpen = false },
                onSelect   = { playerId ->
                    viewModel.selectPlayer(playerId)
                    playerDialogOpen = false
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectPlayerDialog(
    allPlayers: List<PlayerEntity>,
    selectedId: Long,
    onDismiss:  () -> Unit,
    onSelect:   (Long) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape          = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Spieler auswählen", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp)
                ) {
                    allPlayers.forEach { player ->
                        val isSelected = player.id == selectedId
                        Card(
                            onClick  = { onSelect(player.id) },
                            modifier = Modifier.size(72.dp),
                            colors   = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(
                                modifier         = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text      = player.name,
                                    style     = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center,
                                    maxLines  = 2,
                                    overflow  = TextOverflow.Ellipsis,
                                    modifier  = Modifier.padding(6.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick  = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) { Text("Abbrechen") }
            }
        }
    }
}
