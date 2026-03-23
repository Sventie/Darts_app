package com.dartsapp.ui.screens.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.dartsapp.domain.model.PlayerStats
import java.util.Locale

private val STAT_CARD_SIZE = 120.dp
private val STAT_CARD_CORNER = RoundedCornerShape(12.dp)

private data class StatCategory(
    val title: String,
    val description: String,
    val getValue: (PlayerStats) -> String
)

private fun pct(hits: Int, total: Int): String =
    if (total > 0) String.format(Locale.getDefault(), "%.1f%%", hits * 100.0 / total) else "-"

private val STAT_CATEGORIES = listOf(
    StatCategory("Gespielte Spiele",  "Anzahl abgeschlossener Spiele")                          { "${it.gamesPlayed}" },
    StatCategory("Siege",             "1. Platz-Finishes")                                       { "${it.wins}" },
    StatCategory("2. Platz",          "nur bei 3 oder mehr Spielern gewertet")                   { "${it.secondPlace}" },
    StatCategory("3. Platz",          "nur bei 4 oder mehr Spielern gewertet")                   { "${it.thirdPlace}" },
    StatCategory("Darts gesamt",      "alle geworfenen Darts über alle Spiele")                  { "${it.totalDartsThrown}" },
    StatCategory("Ø Punkte/Dart",     "ohne Bust- und Checkout-Runden")                         { String.format(Locale.getDefault(), "%.1f", it.avgScorePerDart) },
    StatCategory("Ø Punkte/Runde",    "ohne Bust- und Checkout-Runden")                         { String.format(Locale.getDefault(), "%.1f", it.avgScorePerRound) },
    StatCategory("First 9 Ø",        "durchschnittlicher Gesamtwert der ersten 3 Runden")       { String.format(Locale.getDefault(), "%.1f", it.first9Average) },
    StatCategory("Höchstes Checkout", "höchster Reststand beim Sieg")                           { "${it.highestCheckout}" },
    StatCategory("Höchste Runde",     "bestes Ergebnis in einer einzelnen Runde")               { "${it.highestRound}" },
    StatCategory("Double-Quote",      "Anteil der Darts auf Doubles")                           { pct(it.doubleHits, it.totalDartsThrown) },
    StatCategory("Triple-Quote",      "Anteil der Darts auf Triples")                           { pct(it.tripleHits, it.totalDartsThrown) },
    StatCategory("Out of Bounce",     "Anteil der Darts außerhalb der Scheibe")                 { pct(it.outOfBounceCount, it.totalDartsThrown) },
    StatCategory("Runden < 10",       "Anteil der Runden mit weniger als 10 Punkten")           { pct(it.roundsUnder10, it.totalRounds) },
    StatCategory("Bust-Quote",        "fehlgeschlagene Checkout-Versuche")                      { pct(it.bustCount, it.checkoutAttempts) },
    StatCategory("Best Buddy",        "häufigster Mitspieler")                                  { it.bestBuddyName ?: "-" },
    StatCategory("Erzfeind",          "Gegner, gegen den am häufigsten verloren wurde")         { it.rivalName ?: "-" },
    StatCategory("Easy Win",          "Gegner, gegen den am häufigsten gewonnen wurde")         { it.easyWinName ?: "-" }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack:         () -> Unit,
    onHeatmapClick: (Long) -> Unit,
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
                filterIds        = filterIds,
                hasEnoughPlayers = allStats.size >= 2,
                hasPlayers       = allStats.isNotEmpty(),
                onCompareClick   = { compareDialogOpen = true },
                onHeatmapClick   = { allStats.firstOrNull()?.let { s -> onHeatmapClick(s.playerId) } },
                onClearFilter    = { filterIds = null }
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
                            title       = category.title,
                            description = category.description,
                            players     = displayedStats,
                            getValue    = category.getValue
                        )
                    }
                }
            }
        }

        if (compareDialogOpen) {
            ComparePlayersDialog(
                allPlayers       = allStats,
                initialSelection = filterIds ?: emptySet(),
                onDismiss        = { compareDialogOpen = false },
                onConfirm        = { ids ->
                    filterIds = ids.ifEmpty { null }
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
    filterIds:        Set<Long>?,
    hasEnoughPlayers: Boolean,
    hasPlayers:       Boolean,
    onCompareClick:   () -> Unit,
    onHeatmapClick:   () -> Unit,
    onClearFilter:    () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onCompareClick,
            enabled = hasEnoughPlayers
        ) {
            Text("Vergleichen")
        }

        Button(
            onClick = onHeatmapClick,
            enabled = hasPlayers
        ) {
            Text("Heatmap")
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
// Compare dialog – modelled after PlayerSelectionDialog
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ComparePlayersDialog(
    allPlayers: List<PlayerStats>,
    initialSelection: Set<Long>,
    onDismiss: () -> Unit,
    onConfirm: (Set<Long>) -> Unit
) {
    var selectedIds by remember { mutableStateOf(initialSelection) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Spieler vergleichen", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allPlayers.forEach { player ->
                        val isSelected = player.playerId in selectedIds
                        Card(
                            onClick = {
                                selectedIds = if (isSelected) selectedIds - player.playerId
                                              else selectedIds + player.playerId
                            },
                            modifier = Modifier.size(72.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                 else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = player.playerName,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) { Text("Abbrechen") }
                    Button(
                        onClick = { onConfirm(selectedIds) },
                        enabled = selectedIds.size >= 2
                    ) {
                        Text("Vergleichen")
                    }
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
    title:       String,
    description: String,
    players:     List<PlayerStats>,
    getValue:    (PlayerStats) -> String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text       = title,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text  = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
                color      = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Single-player selection dialog for Heatmap
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectPlayerForHeatmapDialog(
    allPlayers: List<PlayerStats>,
    onDismiss:  () -> Unit,
    onSelect:   (Long) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape        = RoundedCornerShape(16.dp),
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
                        Card(
                            onClick  = { onSelect(player.playerId) },
                            modifier = Modifier.size(72.dp),
                            colors   = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(
                                modifier            = Modifier.fillMaxSize(),
                                contentAlignment    = Alignment.Center
                            ) {
                                Text(
                                    text      = player.playerName,
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
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Abbrechen")
                }
            }
        }
    }
}
