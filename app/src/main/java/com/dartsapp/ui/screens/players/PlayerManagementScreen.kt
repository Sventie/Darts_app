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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var playerToEdit by remember { mutableStateOf<PlayerEntity?>(null) }
    var editPlayerName by remember { mutableStateOf("") }

    LaunchedEffect(event) {
        val current = event
        viewModel.clearEvent()
        when (current) {
            is PlayerManagementEvent.PlayerCreated -> {
                newPlayerName = ""
                showAddDialog = false
                snackbarHostState.showSnackbar("Spieler erstellt")
            }
            is PlayerManagementEvent.PlayerRenamed -> {
                playerToEdit = null
                snackbarHostState.showSnackbar("Name gespeichert")
            }
            is PlayerManagementEvent.NameEmpty  -> snackbarHostState.showSnackbar("Name darf nicht leer sein")
            is PlayerManagementEvent.NameTaken  -> snackbarHostState.showSnackbar("Name bereits vergeben")
            null -> {}
        }
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

    // ── Edit-Player Dialog ─────────────────────────────────────────────────
    playerToEdit?.let { player ->
        Dialog(onDismissRequest = { playerToEdit = null }) {
            Surface(
                shape          = MaterialTheme.shapes.large,
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text  = "Spieler bearbeiten",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value         = editPlayerName,
                        onValueChange = { editPlayerName = it },
                        label         = { Text("Spielername") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick  = { viewModel.renamePlayer(player, editPlayerName) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Speichern") }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.deletePlayer(player); playerToEdit = null },
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Löschen") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick  = { playerToEdit = null },
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
                    player  = player,
                    onClick = { playerToEdit = player; editPlayerName = player.name }
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
    player:  PlayerEntity,
    onClick: () -> Unit
) {
    Card(
        onClick  = onClick,
        modifier = Modifier.size(CARD_SIZE),
        shape    = CARD_CORNER,
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AutoSizeText(
                text        = player.name,
                maxFontSize = 26.sp,
                minFontSize = 12.sp,
                fontWeight  = FontWeight.Bold,
                textAlign   = TextAlign.Center,
                modifier    = Modifier.align(Alignment.Center).padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun AutoSizeText(
    text: String,
    maxFontSize: TextUnit,
    minFontSize: TextUnit,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
) {
    var fontSize by remember(text) { mutableStateOf(maxFontSize) }
    var readyToDraw by remember(text) { mutableStateOf(false) }

    Text(
        text      = text,
        fontSize  = fontSize,
        fontWeight = fontWeight,
        textAlign  = textAlign,
        maxLines   = 1,
        softWrap   = false,
        overflow   = TextOverflow.Visible,
        modifier   = modifier.drawWithContent {
            if (readyToDraw) drawContent()
        },
        onTextLayout = { result ->
            if (result.didOverflowWidth) {
                val next = fontSize * 0.85f
                fontSize = if (next >= minFontSize) next else minFontSize
                if (next < minFontSize) readyToDraw = true
            } else {
                readyToDraw = true
            }
        }
    )
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
