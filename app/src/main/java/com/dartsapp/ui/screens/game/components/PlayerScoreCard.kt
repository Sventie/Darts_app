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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dartsapp.domain.model.ActivePlayer

@Composable
fun PlayerScoreCard(
    player: ActivePlayer,
    isCurrentPlayer: Boolean,
    modifier: Modifier = Modifier,
    displayScore: Int = player.remainingScore
) {
    val isFinished = player.placement != null
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isFinished     -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                isCurrentPlayer -> MaterialTheme.colorScheme.primaryContainer
                else           -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.playerName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    color = if (isFinished) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            else Color.Unspecified
                )
                when {
                    isFinished -> Text(
                        text = placementLabel(player.placement!!),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    isCurrentPlayer -> Text(
                        text = "Du bist dran",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (isFinished) {
                Text(
                    text = placementBadge(player.placement!!),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            } else {
                Text(
                    text = "$displayScore",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
    }
}

private fun placementLabel(placement: Int) = when (placement) {
    1 -> "Gewinner"
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
