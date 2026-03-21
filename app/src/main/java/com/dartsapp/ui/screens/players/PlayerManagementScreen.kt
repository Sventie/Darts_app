package com.dartsapp.ui.screens.players

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dartsapp.data.db.entity.PlayerEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerManagementScreen(
    onBack: () -> Unit,
    viewModel: PlayerManagementViewModel = hiltViewModel()
) {
    val players by viewModel.players.collectAsState()
    val event by viewModel.event.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var newPlayerName by remember { mutableStateOf("") }
    var playerToDelete by remember { mutableStateOf<PlayerEntity?>(null) }

    LaunchedEffect(event) {
        when (event) {
            is PlayerManagementEvent.PlayerCreated -> {
                newPlayerName = ""
                snackbarHostState.showSnackbar("Spieler erstellt")
            }
            is PlayerManagementEvent.NameEmpty -> snackbarHostState.showSnackbar("Name darf nicht leer sein")
            is PlayerManagementEvent.NameTaken -> snackbarHostState.showSnackbar("Name bereits vergeben")
            null -> {}
        }
        viewModel.clearEvent()
    }

    playerToDelete?.let { player ->
        AlertDialog(
            onDismissRequest = { playerToDelete = null },
            title = { Text("Spieler löschen") },
            text = { Text("\"${player.name}\" löschen? Dies kann nicht rückgängig gemacht werden.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlayer(player)
                    playerToDelete = null
                }) { Text("Löschen") }
            },
            dismissButton = {
                TextButton(onClick = { playerToDelete = null }) { Text("Abbrechen") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spieler verwalten") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = newPlayerName,
                    onValueChange = { newPlayerName = it },
                    label = { Text("Spielername") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.createPlayer(newPlayerName) },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Hinzufügen")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (players.isEmpty()) {
                Text(
                    text = "Noch keine Spieler. Füge oben einen hinzu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                LazyColumn {
                    items(players, key = { it.id }) { player ->
                        ListItem(
                            headlineContent = { Text(player.name) },
                            trailingContent = {
                                IconButton(onClick = { playerToDelete = player }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Löschen: ${player.name}")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
