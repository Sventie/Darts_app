package com.dartsapp.ui.screens.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import com.dartsapp.data.db.entity.PlayerEntity
import com.dartsapp.domain.model.CloseCondition
import com.dartsapp.domain.model.GameConfig
import com.dartsapp.domain.model.PlayerStats
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private val CARD_CORNER = RoundedCornerShape(12.dp)
private val ACTION_BTN_H: Dp = 64.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GameSetupScreen(viewModel: GameSetupViewModel, onBack: () -> Unit, onTraining: () -> Unit = {}) {
    val players by viewModel.players.collectAsState()
    val selectedIds by viewModel.selectedPlayerIds.collectAsState()
    val startingScore by viewModel.startingScore.collectAsState()
    val closeCondition by viewModel.closeCondition.collectAsState()
    val playerStats by viewModel.playerStats.collectAsState()
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
            Text("Spieler", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val trainingBtnW = 160f
                val rowGap       = 8f   // gap between LazyRow and Training button
                val cardSpacing  = 8f   // gap between cards inside LazyRow
                val normalSize   = 144f
                val itemCount    = selectedPlayers.size + 1  // player cards + add-button

                val availForCards = maxWidth.value - trainingBtnW - rowGap
                val neededWidth   = itemCount * normalSize + (itemCount - 1) * cardSpacing
                val cardSizeDp    = if (neededWidth <= availForCards) normalSize.dp
                                    else ((availForCards - (itemCount - 1) * cardSpacing) / itemCount)
                                        .coerceAtLeast(48f).dp

                Row(
                    horizontalArrangement = Arrangement.spacedBy(rowGap.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    LazyRow(
                        state = lazyListState,
                        horizontalArrangement = Arrangement.spacedBy(cardSpacing.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(selectedPlayers, key = { it.id }) { player ->
                            ReorderableItem(reorderState, key = player.id) { _ ->
                                PlayerCard(
                                    player = player,
                                    stats = playerStats[player.id],
                                    size = cardSizeDp,
                                    modifier = Modifier.draggableHandle()
                                )
                            }
                        }
                        item(key = "add_button") {
                            AddPlayerCard(onClick = { showPlayerDialog = true }, size = cardSizeDp)
                        }
                    }
                    Button(
                        onClick = onTraining,
                        modifier = Modifier.size(width = trainingBtnW.dp, height = cardSizeDp),
                        shape = CARD_CORNER
                    ) {
                        Text("Training", fontSize = 28.sp)
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick        = { viewModel.randomizePlayerOrder() },
                enabled        = selectedIds.size > 1,
                modifier       = Modifier.fillMaxWidth().defaultMinSize(minHeight = ACTION_BTN_H),
                shape          = CARD_CORNER,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text("Zufällige Reihenfolge", fontSize = 28.sp)
            }

            Spacer(Modifier.height(12.dp))
            Text("Startpunkte", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
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

            Spacer(Modifier.height(12.dp))
            Text("Abschluss", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
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

            Spacer(Modifier.height(12.dp))
            Button(
                onClick        = { viewModel.startGame() },
                enabled        = selectedIds.isNotEmpty(),
                modifier       = Modifier.fillMaxWidth().defaultMinSize(minHeight = ACTION_BTN_H),
                shape          = CARD_CORNER,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text("Spiel starten", fontSize = 28.sp)
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

@Composable
private fun PlayerCard(
    player: PlayerEntity,
    stats: PlayerStats?,
    size: Dp = 144.dp,
    modifier: Modifier = Modifier
) {
    // Pick one random stat label per player; only re-pick when stats first becomes available,
    // not on every stats update (avoids flickering when game start triggers a DB write).
    val hasStats = stats != null && stats.gamesPlayed > 0
    val statLabel = remember(player.id, hasStats) { if (hasStats) randomStatLabel(stats) else null }

    Card(
        modifier = modifier.size(size),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AutoSizeText(
                    text        = player.name,
                    maxFontSize = 26.sp,
                    minFontSize = 12.sp,
                    fontWeight  = FontWeight.Bold,
                    textAlign   = TextAlign.Center,
                    modifier    = Modifier.fillMaxWidth()
                )
                if (statLabel != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = statLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun randomStatLabel(stats: PlayerStats?): String? {
    if (stats == null || stats.gamesPlayed == 0) return null
    val options = buildList {
        add("Ø Punkte/Runde: ${String.format("%.1f", stats.avgScorePerRound)}")
        add("Höchste Runde: ${stats.highestRound}")
        add("Siege: ${stats.wins}")
        add("Gespielte Spiele: ${stats.gamesPlayed}")
        if (stats.totalDartsThrown > 0) add("Darts gesamt: ${stats.totalDartsThrown}")
    }
    return options.random()
}

@Composable
private fun AddPlayerCard(onClick: () -> Unit, size: Dp = 144.dp) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.size(size)
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
            Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 20.sp))
        }
    }
}
