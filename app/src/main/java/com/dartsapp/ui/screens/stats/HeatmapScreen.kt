package com.dartsapp.ui.screens.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HeatmapScreen(
    onBack: () -> Unit,
    viewModel: HeatmapViewModel = hiltViewModel()
) {
    val playerName   by viewModel.playerName.collectAsState()
    val allPlayers   by viewModel.allPlayers.collectAsState()
    val selectedId   by viewModel.selectedPlayerId.collectAsState()
    val hitPositions by viewModel.hitPositions.collectAsState()
    val gameCount    by viewModel.gameCount.collectAsState()
    val fromGame     by viewModel.fromGame.collectAsState()
    val toGame       by viewModel.toGame.collectAsState()

    // Clamp the displayed "to" value against the actual game count
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
        Row(
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
                Text("Spieler", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(4.dp)
                ) {
                    allPlayers.forEach { player ->
                        FilterChip(
                            selected = player.id == selectedId,
                            onClick  = { viewModel.selectPlayer(player.id) },
                            label    = { Text(player.name) }
                        )
                    }
                }

                HorizontalDivider()

                Text("Spielbereich", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

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
                        RangeSlider(
                            value           = fromGame.toFloat()..effectiveTo.toFloat(),
                            onValueChange   = { range ->
                                val newFrom = range.start.roundToInt().coerceIn(1, gameCount)
                                val newTo   = range.endInclusive.roundToInt().coerceIn(newFrom, gameCount)
                                viewModel.setGameRange(newFrom, newTo)
                            },
                            valueRange      = 1f..gameCount.toFloat(),
                            steps           = (gameCount - 2).coerceAtLeast(0),
                            modifier        = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            VerticalDivider(modifier = Modifier.fillMaxHeight().padding(vertical = 8.dp))

            // ── Right: heatmap ────────────────────────────────────────────
            Box(
                modifier         = Modifier.fillMaxHeight().weight(0.68f),
                contentAlignment = Alignment.Center
            ) {
                DartBoardHeatmap(
                    hitPositions = hitPositions,
                    modifier     = Modifier
                        .fillMaxHeight(0.92f)
                        .aspectRatio(1f)
                )
            }
        }
    }
}
