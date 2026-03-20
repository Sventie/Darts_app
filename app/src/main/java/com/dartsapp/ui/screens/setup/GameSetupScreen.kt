package com.dartsapp.ui.screens.setup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dartsapp.domain.model.CloseCondition
import com.dartsapp.domain.model.GameConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSetupScreen(viewModel: GameSetupViewModel) {
    val players by viewModel.players.collectAsState()
    val selectedIds by viewModel.selectedPlayerIds.collectAsState()
    val startingScore by viewModel.startingScore.collectAsState()
    val closeCondition by viewModel.closeCondition.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("New Game") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Select Players", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(players, key = { it.id }) { player ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = player.id in selectedIds,
                            onCheckedChange = { viewModel.togglePlayer(player.id) }
                        )
                        Text(player.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Starting Score", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                GameConfig.validStartingScores.forEach { score ->
                    FilterChip(
                        selected = startingScore == score,
                        onClick = { viewModel.setStartingScore(score) },
                        label = { Text("$score") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Close Condition", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                FilterChip(
                    selected = closeCondition == CloseCondition.DOUBLE_OUT,
                    onClick = { viewModel.setCloseCondition(CloseCondition.DOUBLE_OUT) },
                    label = { Text("Double Out") },
                    modifier = Modifier.padding(end = 8.dp)
                )
                FilterChip(
                    selected = closeCondition == CloseCondition.SINGLE_OUT,
                    onClick = { viewModel.setCloseCondition(CloseCondition.SINGLE_OUT) },
                    label = { Text("Single Out") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.startGame() },
                enabled = selectedIds.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Game")
            }
        }
    }
}
