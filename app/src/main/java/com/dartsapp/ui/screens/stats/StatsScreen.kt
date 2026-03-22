package com.dartsapp.ui.screens.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.dartsapp.domain.model.PlayerStats
import java.util.Locale

private val STAT_CARD_SIZE = 120.dp
private val STAT_CARD_CORNER = RoundedCornerShape(12.dp)

private data class StatCategory(
    val title: String,
    val getValue: (PlayerStats) -> String
)

private fun pct(hits: Int, total: Int): String =
    if (total > 0) String.format(Locale.getDefault(), "%.1f%%", hits * 100.0 / total) else "-"

private val STAT_CATEGORIES = listOf(
    StatCategory("Gespielte Spiele")  { "${it.gamesPlayed}" },
    StatCategory("Siege")             { "${it.wins}" },
    StatCategory("2. Platz")          { "${it.secondPlace}" },
    StatCategory("3. Platz")          { "${it.thirdPlace}" },
    StatCategory("Darts gesamt")      { "${it.totalDartsThrown}" },
    StatCategory("Ø Punkte/Dart")     { String.format(Locale.getDefault(), "%.1f", it.avgScorePerDart) },
    StatCategory("Ø Punkte/Runde")    { String.format(Locale.getDefault(), "%.1f", it.avgScorePerRound) },
    StatCategory("First 9 Ø")        { String.format(Locale.getDefault(), "%.1f", it.first9Average) },
    StatCategory("Höchstes Checkout") { "${it.highestCheckout}" },
    StatCategory("Höchste Runde")     { "${it.highestRound}" },
    StatCategory("Double-Quote")      { pct(it.doubleHits, it.totalDartsThrown) },
    StatCategory("Triple-Quote")      { pct(it.tripleHits, it.totalDartsThrown) },
    StatCategory("Out of Bounce")     { pct(it.outOfBounceCount, it.totalDartsThrown) },
    StatCategory("Runden < 10")       { pct(it.roundsUnder10, it.totalRounds) },
    StatCategory("Bust-Quote")        { pct(it.bustCount, it.checkoutAttempts) }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsOverviewViewModel = hiltViewModel()
) {
    val allStats by viewModel.allStats.collectAsState()

    var compareDialogOpen by remember { mutableStateOf(false) }
    var filterIds by remember { mutableStateOf<Set<Long>?>(null) }

    val displayedStats = remember(allStats, filterIds) {
        val ids = filterIds
        if (ids != null) allStats.filter { it.playerId in ids } else allStats
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistik") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        if (allStats.isEmpty()) {
            Text(
                "Noch keine Spieler.",
                modifier = Modifier.padding(padding).padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            StatsActionBar(
                filterIds = filterIds,
                hasEnoughPlayers = allStats.size >= 2,
                onCompareClick = { compareDialogOpen = true },
                onClearFilter = { filterIds = null }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                STAT_CATEGORIES.forEach { category ->
                    item(key = category.title) {
                        StatCategorySection(
                            title    = category.title,
                            players  = displayedStats,
                            getValue = category.getValue
                        )
                    }
                }
            }
        }

        if (compareDialogOpen) {
            ComparePlayersDialog(
                allPlayers      = allStats,
                initialSelection = filterIds ?: emptySet(),
                onDismiss       = { compareDialogOpen = false },
                onConfirm       = { ids ->
                    filterIds = ids
                    compareDialogOpen = false
                }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Action toolbar
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatsActionBar(
    filterIds: Set<Long>?,
    hasEnoughPlayers: Boolean,
    onCompareClick: () -> Unit,
    onClearFilter: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onCompareClick,
            enabled = hasEnoughPlayers
        ) {
            Text("Vergleichen")
        }

        if (filterIds != null) {
            FilterChip(
                selected = true,
                onClick = onClearFilter,
                label = { Text("${filterIds.size} Spieler") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Filter zurücksetzen",
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Compare dialog
// ---------------------------------------------------------------------------

@Composable
private fun ComparePlayersDialog(
    allPlayers: List<PlayerStats>,
    initialSelection: Set<Long>,
    onDismiss: () -> Unit,
    onConfirm: (Set<Long>) -> Unit
) {
    var slots by remember {
        val initial = initialSelection.toList().take(4)
        mutableStateOf(buildList<Long?> {
            addAll(initial)
            repeat(4 - initial.size) { add(null) }
        })
    }
    var pickingForSlot by remember { mutableStateOf<Int?>(null) }

    val selectedIds = slots.filterNotNull().toSet()
    val availablePlayers = allPlayers.filter { it.playerId !in selectedIds }

    AlertDialog(
        onDismissRequest = {
            if (pickingForSlot != null) pickingForSlot = null else onDismiss()
        },
        title = {
            Text(if (pickingForSlot != null) "Spieler auswählen" else "Spieler vergleichen")
        },
        text = {
            if (pickingForSlot != null) {
                // Player picker view
                Column {
                    if (availablePlayers.isEmpty()) {
                        Text(
                            "Keine weiteren Spieler verfügbar.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        availablePlayers.forEach { player ->
                            TextButton(
                                onClick = {
                                    val idx = pickingForSlot!!
                                    slots = slots.toMutableList().also { it[idx] = player.playerId }
                                    pickingForSlot = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = player.playerName,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }
                }
            } else {
                // Slot grid view
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Wähle 2 bis 4 Spieler für den Vergleich.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    for (row in 0..1) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (col in 0..1) {
                                val slotIdx = row * 2 + col
                                val playerId = slots[slotIdx]
                                val playerName = allPlayers.find { it.playerId == playerId }?.playerName
                                PlayerSlotCard(
                                    modifier = Modifier.weight(1f),
                                    playerName = playerName,
                                    canAdd = availablePlayers.isNotEmpty() || playerId != null,
                                    onClick = {
                                        if (playerId != null) {
                                            slots = slots.toMutableList().also { it[slotIdx] = null }
                                        } else if (availablePlayers.isNotEmpty()) {
                                            pickingForSlot = slotIdx
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (pickingForSlot == null) {
                Button(
                    onClick = { onConfirm(selectedIds) },
                    enabled = selectedIds.size >= 2
                ) {
                    Text("Vergleichen")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (pickingForSlot != null) pickingForSlot = null else onDismiss()
                }
            ) {
                Text(if (pickingForSlot != null) "Zurück" else "Abbrechen")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerSlotCard(
    modifier: Modifier,
    playerName: String?,
    canAdd: Boolean,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        enabled = canAdd
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (playerName != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Entfernen",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = playerName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Spieler hinzufügen",
                        tint = if (canAdd) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Text(
                        text = "Hinzufügen",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (canAdd) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Existing stat composables
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatCategorySection(
    title:    String,
    players:  List<PlayerStats>,
    getValue: (PlayerStats) -> String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text       = title,
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            players.forEach { stats ->
                StatCard(name = stats.playerName, value = getValue(stats))
            }
        }
    }
}

@Composable
private fun StatCard(name: String, value: String) {
    Card(
        modifier = Modifier.size(STAT_CARD_SIZE),
        shape    = STAT_CARD_CORNER,
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier            = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text      = name,
                style     = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines  = 2,
                overflow  = TextOverflow.Ellipsis,
                color     = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text       = value,
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
                color      = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
