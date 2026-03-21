package com.dartsapp.ui.screens.game.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dartsapp.domain.model.ActivePlayer

@Composable
fun PlayerScoreCard(
    player: ActivePlayer,
    isCurrentPlayer: Boolean,
    modifier: Modifier = Modifier,
    displayScore: Int = player.remainingScore
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentPlayer)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.playerName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
                if (isCurrentPlayer) {
                    Text(
                        text = "Du bist dran",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$displayScore",
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}
