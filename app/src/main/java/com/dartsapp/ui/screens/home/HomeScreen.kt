package com.dartsapp.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onNavigatePlayers: () -> Unit,
    onNavigateSetup: () -> Unit,
    onNavigateStats: () -> Unit,
    onNavigateSpider: () -> Unit = {}
) {
    Scaffold { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (maxWidth > maxHeight) {
                LandscapeLayout(
                    onNavigateSetup = onNavigateSetup,
                    onNavigateSpider = onNavigateSpider,
                    onNavigateStats = onNavigateStats
                )
            } else {
                PortraitLayout(
                    onNavigateSetup = onNavigateSetup,
                    onNavigateSpider = onNavigateSpider,
                    onNavigateStats = onNavigateStats
                )
            }
        }
    }
}

@Composable
private fun LandscapeLayout(
    onNavigateSetup: () -> Unit,
    onNavigateSpider: () -> Unit,
    onNavigateStats: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Title – obere Hälfte
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Cloudflight Darts",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        // Drei Karten – untere Hälfte
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            MenuCard(
                label = "Neues Spiel",
                onClick = onNavigateSetup,
                modifier = Modifier.weight(1f)
            )
            MenuCard(
                label = "Spieler",
                onClick = onNavigateSpider,
                modifier = Modifier.weight(1f)
            )
            MenuCard(
                label = "Statistik",
                onClick = onNavigateStats,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PortraitLayout(
    onNavigateSetup: () -> Unit,
    onNavigateSpider: () -> Unit,
    onNavigateStats: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Cloudflight Darts",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        MenuCard(
            label = "Neues Spiel",
            onClick = onNavigateSetup,
            modifier = Modifier.fillMaxWidth()
        )
        MenuCard(
            label = "Spieler",
            onClick = onNavigateSpider,
            modifier = Modifier.fillMaxWidth()
        )
        MenuCard(
            label = "Statistik",
            onClick = onNavigateStats,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MenuCard(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}
