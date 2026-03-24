package com.dartsapp.ui.screens.game.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
        modifier = modifier.fillMaxWidth().aspectRatio(2f),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isFinished      -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                isCurrentPlayer -> MaterialTheme.colorScheme.primaryContainer
                else            -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = player.playerName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = if (isFinished)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else Color.Unspecified
                )

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
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    if (isCurrentPlayer) {
                        Text(
                            text = "Du bist dran",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

private fun placementBadge(placement: Int) = when (placement) {
    1 -> "🥇"
    2 -> "🥈"
    3 -> "🥉"
    else -> "$placement."
}
