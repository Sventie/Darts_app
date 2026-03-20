package com.dartsapp.ui.screens.stats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsDetailScreen(
    onBack: () -> Unit,
    viewModel: StatsDetailViewModel = hiltViewModel()
) {
    val stats by viewModel.stats.collectAsState()
    val frequencies by viewModel.fieldFrequencies.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stats?.playerName ?: "Statistics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (stats == null) {
            Text("No data yet.", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }

        val s = stats!!
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            item {
                Text("Overview", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                StatRow("Games Played", "${s.gamesPlayed}")
                StatRow("Wins", "${s.wins}")
                StatRow("Avg Score/Dart", String.format(Locale.getDefault(), "%.1f", s.avgScorePerDart))
                StatRow("Avg Score/Round", String.format(Locale.getDefault(), "%.1f", s.avgScorePerRound))
                StatRow("Highest Round", "${s.highestRound}")
                StatRow("Total Darts Thrown", "${s.totalDartsThrown}")
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Field Frequency", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(frequencies) { freq ->
                val label = when (freq.field) {
                    0 -> "Miss"
                    25 -> "Bull"
                    50 -> "Bullseye"
                    else -> "${freq.field}"
                }
                ListItem(
                    headlineContent = { Text(label) },
                    supportingContent = {
                        Text("S:${freq.singleCount}  D:${freq.doubleCount}  T:${freq.tripleCount}  Total:${freq.totalHits}")
                    }
                )
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = { Text(value, style = MaterialTheme.typography.bodyLarge) }
    )
}
