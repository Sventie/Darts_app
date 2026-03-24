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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.dartsapp.data.db.entity.PlayerEntity
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeatmapScreen(
    onBack: () -> Unit,
    initialShowDispersion: Boolean = false,
    viewModel: HeatmapViewModel = hiltViewModel()
) {
    val playerName              by viewModel.playerName.collectAsState()
    val allPlayers              by viewModel.allPlayers.collectAsState()
    val selectedId              by viewModel.selectedPlayerId.collectAsState()
    val hitPositions            by viewModel.hitPositions.collectAsState()
    val gameCount               by viewModel.gameCount.collectAsState()
    val fromGame                by viewModel.fromGame.collectAsState()
    val toGame                  by viewModel.toGame.collectAsState()
    val dispersion              by viewModel.dispersion.collectAsState()
    val trainingThrowCount      by viewModel.trainingThrowCount.collectAsState()
    val trainingSessionCount    by viewModel.trainingSessionCount.collectAsState()
    val fromTraining            by viewModel.fromTraining.collectAsState()
    val toTraining              by viewModel.toTraining.collectAsState()

    var playerDialogOpen by remember { mutableStateOf(false) }
    var showHeatmap      by remember { mutableStateOf(!initialShowDispersion) }

    // Clamp displayed "to" values against the actual counts
    val effectiveTo         = if (toGame == Int.MAX_VALUE) gameCount else toGame.coerceAtMost(gameCount)
    val effectiveTrainingTo = if (toTraining == Int.MAX_VALUE) trainingSessionCount else toTraining.coerceAtMost(trainingSessionCount)

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
                    .weight(0.44f)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Spieler",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick        = { playerDialogOpen = true },
                    modifier       = Modifier.fillMaxWidth().defaultMinSize(minHeight = 64.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        text     = playerName.ifEmpty { "Spieler wählen" },
                        fontSize = 28.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                HorizontalDivider()

                Text(
                    "Ansicht",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(true to "Heatmap", false to "Streuung").forEach { (isHeatmap, label) ->
                        Card(
                            onClick  = { showHeatmap = isHeatmap },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = CardDefaults.cardColors(
                                containerColor = if (showHeatmap == isHeatmap)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text       = label,
                                    style      = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (showHeatmap == isHeatmap) FontWeight.Bold else FontWeight.Normal,
                                    textAlign  = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                if (!showHeatmap) {
                    Text(
                        text  = "Ø Abweichung: ${"%.2f".format(dispersion)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 20.sp)
                    )
                    Text(
                        text  = "Basis: $trainingThrowCount Trainingswürfe",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 16.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider()

                    Text(
                        "Trainingsbereich",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    when {
                        trainingSessionCount == 0 -> Text(
                            "Noch keine Trainings.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 16.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        trainingSessionCount == 1 -> Text(
                            "Training 1 von 1",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 20.sp)
                        )
                        else -> {
                            Text(
                                text  = "Training $fromTraining – $effectiveTrainingTo von $trainingSessionCount",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 20.sp)
                            )

                            Text(
                                "Von",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 16.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value         = fromTraining.toFloat(),
                                onValueChange = { v ->
                                    val newFrom = v.roundToInt().coerceIn(1, effectiveTrainingTo)
                                    viewModel.setTrainingRange(newFrom, toTraining)
                                },
                                valueRange    = 1f..trainingSessionCount.toFloat(),
                                steps         = (trainingSessionCount - 2).coerceAtLeast(0),
                                modifier      = Modifier.fillMaxWidth()
                            )

                            Text(
                                "Bis",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 16.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value         = effectiveTrainingTo.toFloat(),
                                onValueChange = { v ->
                                    val newTo = v.roundToInt().coerceIn(fromTraining, trainingSessionCount)
                                    viewModel.setTrainingRange(fromTraining, newTo)
                                },
                                valueRange    = 1f..trainingSessionCount.toFloat(),
                                steps         = (trainingSessionCount - 2).coerceAtLeast(0),
                                modifier      = Modifier.fillMaxWidth()
                            )
                        }
                    }
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
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 16.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        gameCount == 1 -> Text(
                            "Spiel 1 von 1",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 20.sp)
                        )
                        else -> {
                            Text(
                                text  = "Spiel $fromGame – $effectiveTo von $gameCount",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 20.sp)
                            )

                            Text(
                                "Von",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 16.sp),
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
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 16.sp),
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
                modifier         = Modifier.fillMaxHeight().weight(0.56f),
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

@Composable
private fun AutoSizeText(
    text:        String,
    maxFontSize: TextUnit,
    minFontSize: TextUnit,
    modifier:    Modifier = Modifier,
    fontWeight:  FontWeight? = null,
    textAlign:   TextAlign? = null,
    color:       Color = Color.Unspecified,
) {
    var fontSize    by remember(text) { mutableStateOf(maxFontSize) }
    var readyToDraw by remember(text) { mutableStateOf(false) }

    Text(
        text       = text,
        fontSize   = fontSize,
        fontWeight = fontWeight,
        textAlign  = textAlign,
        color      = color,
        maxLines   = 1,
        softWrap   = false,
        overflow   = TextOverflow.Visible,
        modifier   = modifier.drawWithContent { if (readyToDraw) drawContent() },
        onTextLayout = { result ->
            if (result.didOverflowWidth) {
                val next = fontSize * 0.85f
                fontSize = if (next >= minFontSize) next else minFontSize
                if (next < minFontSize) readyToDraw = true
            } else {
                readyToDraw = true
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectPlayerDialog(
    allPlayers: List<PlayerEntity>,
    selectedId: Long,
    onDismiss:  () -> Unit,
    onSelect:   (Long) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape          = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            modifier       = Modifier.fillMaxSize(0.8f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Spieler auswählen", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(16.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp)
                ) {
                    allPlayers.forEach { player ->
                        val isSelected = player.id == selectedId
                        Card(
                            onClick  = { onSelect(player.id) },
                            modifier = Modifier.size(144.dp),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                AutoSizeText(
                                    text        = player.name,
                                    maxFontSize = 26.sp,
                                    minFontSize = 12.sp,
                                    fontWeight  = FontWeight.Bold,
                                    textAlign   = TextAlign.Center,
                                    modifier    = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                androidx.compose.foundation.layout.Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(
                        onClick        = onDismiss,
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                        modifier       = Modifier.defaultMinSize(minHeight = 64.dp)
                    ) { Text("Abbrechen", fontSize = 28.sp) }
                }
            }
        }
    }
}
