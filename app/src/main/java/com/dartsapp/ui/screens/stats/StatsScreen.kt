package com.dartsapp.ui.screens.stats

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

private val STAT_CATEGORIES = listOf(
    StatCategory("Gespielte Spiele") { "${it.gamesPlayed}" },
    StatCategory("Siege")            { "${it.wins}" },
    StatCategory("Ø Punkte/Dart")    { String.format(Locale.getDefault(), "%.1f", it.avgScorePerDart) },
    StatCategory("Ø Punkte/Runde")   { String.format(Locale.getDefault(), "%.1f", it.avgScorePerRound) },
    StatCategory("Höchste Runde")    { "${it.highestRound}" },
    StatCategory("Darts gesamt")     { "${it.totalDartsThrown}" }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsOverviewViewModel = hiltViewModel()
) {
    val allStats by viewModel.allStats.collectAsState()

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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            STAT_CATEGORIES.forEach { category ->
                item(key = category.title) {
                    StatCategorySection(
                        title    = category.title,
                        players  = allStats,
                        getValue = category.getValue
                    )
                }
            }
        }
    }
}

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
        Row(
            modifier              = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
