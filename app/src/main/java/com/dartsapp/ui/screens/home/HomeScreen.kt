package com.dartsapp.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onNavigatePlayers: () -> Unit,
    onNavigateSetup: () -> Unit,
    onNavigateStats: () -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Darts",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onNavigateSetup,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("New Game")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onNavigatePlayers,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Manage Players")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onNavigateStats,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Statistics")
            }
        }
    }
}
