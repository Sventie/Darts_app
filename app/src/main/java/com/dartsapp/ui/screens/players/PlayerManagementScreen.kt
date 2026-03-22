package com.dartsapp.ui.screens.players

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.dartsapp.data.db.entity.PlayerEntity

private val CARD_SIZE = 144.dp
private val CARD_CORNER = RoundedCornerShape(12.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerManagementScreen(
    onBack: () -> Unit,
    viewModel: PlayerManagementViewModel = hiltViewModel()
) {
    val players by viewModel.players.collectAsState()
    val event by viewModel.event.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddDialog by remember { mutableStateOf(false) }
    var newPlayerName by remember { mutableStateOf("") }
    var playerToDelete by remember { mutableStateOf<PlayerEntity?>(null) }

    LaunchedEffect(event) {
        when (event) {
            is PlayerManagementEvent.PlayerCreated -> {
                newPlayerName = ""
                showAddDialog = false
                snackbarHostState.showSnackbar("Spieler erstellt")
            }
            is PlayerManagementEvent.NameEmpty  -> snackbarHostState.showSnackbar("Name darf nicht leer sein")
            is PlayerManagementEvent.NameTaken  -> snackbarHostState.showSnackbar("Name bereits vergeben")
            null -> {}
        }
        viewModel.clearEvent()
    }

    // ── Add-Player Dialog ──────────────────────────────────────────────────
    if (showAddDialog) {
        Dialog(onDismissRequest = { showAddDialog = false; newPlayerName = "" }) {
            Surface(
                shape         = MaterialTheme.shapes.large,
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text  = "Neuer Spieler",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value         = newPlayerName,
                        onValueChange = { newPlayerName = it },
                        label         = { Text("Spielername") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick  = { viewModel.createPlayer(newPlayerName) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Hinzufügen") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick  = { showAddDialog = false; newPlayerName = "" },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Abbrechen") }
                }
            }
        }
    }

    // ── Delete-Player Dialog ───────────────────────────────────────────────
    playerToDelete?.let { player ->
        Dialog(onDismissRequest = { playerToDelete = null }) {
            Surface(
                shape          = MaterialTheme.shapes.large,
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text  = "Spieler löschen",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text  = "\"${player.name}\" löschen? Dies kann nicht rückgängig gemacht werden.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.deletePlayer(player); playerToDelete = null },
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Löschen") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick  = { playerToDelete = null },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Abbrechen") }
                }
            }
        }
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
        LazyVerticalGrid(
            columns             = GridCells.Adaptive(CARD_SIZE),
            modifier            = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement   = Arrangement.spacedBy(8.dp)
        ) {
            items(players, key = { it.id }) { player ->
                PlayerManagementCard(
                    player    = player,
                    onDelete  = { playerToDelete = player }
                )
            }
            item {
                AddPlayerCard(onClick = { showAddDialog = true })
            }
        }
    }
}

@Composable
private fun PlayerManagementCard(
    player:   PlayerEntity,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.size(CARD_SIZE),
        shape    = CARD_CORNER,
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text      = player.name,
                style     = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
                textAlign  = TextAlign.Center,
                modifier   = Modifier.align(Alignment.Center).padding(horizontal = 8.dp)
            )
            IconButton(
                onClick  = onDelete,
                modifier = Modifier.align(Alignment.TopEnd).size(36.dp)
            ) {
                Icon(
                    imageVector        = Icons.Default.Delete,
                    contentDescription = "Löschen: ${player.name}",
                    tint               = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                    modifier           = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AddPlayerCard(onClick: () -> Unit) {
    OutlinedCard(
        onClick  = onClick,
        modifier = Modifier.size(CARD_SIZE),
        shape    = CARD_CORNER
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector        = Icons.Default.Add,
                contentDescription = "Spieler hinzufügen"
            )
        }
    }
}
