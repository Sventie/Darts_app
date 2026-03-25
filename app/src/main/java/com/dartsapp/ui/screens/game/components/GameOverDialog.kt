package com.dartsapp.ui.screens.game.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dartsapp.domain.model.ActivePlayer

@Composable
fun GameOverDialog(
    playerJustFinished: ActivePlayer,
    allPlayers: List<ActivePlayer>,
    canContinue: Boolean,
    onContinue: () -> Unit,
    onEndGame: () -> Unit
) {
    // Finished players sorted by placement, then active players by remaining score
    val sortedPlayers = remember(allPlayers) {
        val finished = allPlayers.filter { it.placement != null }.sortedBy { it.placement }
        val active   = allPlayers.filter { it.placement == null }.sortedBy { it.remainingScore }
        finished + active
    }

    Dialog(
        onDismissRequest = { /* not dismissible by back/outside tap */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                val badge = when (playerJustFinished.placement) {
                    1 -> "🏆"
                    2 -> "🥈"
                    3 -> "🥉"
                    else -> "🎯"
                }
                Text(
                    text = badge,
                    style = MaterialTheme.typography.displaySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = playerJustFinished.playerName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = placementTitle(playerJustFinished.placement!!),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Player standings
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(sortedPlayers) { player ->
                        PlayerStandingRow(player = player)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons
                if (canContinue) {
                    Button(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Weitere Plätze ausspielen")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                OutlinedButton(
                    onClick = onEndGame,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Spiel beenden")
                }
            }
        }
    }
}

@Composable
private fun PlayerStandingRow(player: ActivePlayer) {
    val isFinished = player.placement != null
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isFinished)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (isFinished) {
                    Text(
                        text = placementBadge(player.placement!!),
                        style = MaterialTheme.typography.titleLarge
                    )
                } else {
                    Text(
                        text = "–",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
                Text(
                    text = player.playerName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isFinished) FontWeight.Normal else FontWeight.SemiBold,
                    color = if (isFinished)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
            if (!isFinished) {
                Text(
                    text = "${player.remainingScore}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun placementTitle(placement: Int) = when (placement) {
    1 -> "Gewinner!"
    2 -> "2. Platz"
    3 -> "3. Platz"
    else -> "$placement. Platz"
}

private fun placementBadge(placement: Int) = when (placement) {
    1 -> "🥇"
    2 -> "🥈"
    3 -> "🥉"
    else -> "$placement."
}
