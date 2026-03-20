package com.dartsapp.ui.screens.game.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.dartsapp.ui.screens.game.BustInfo

@Composable
fun BustDialog(bustInfo: BustInfo, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bust!") },
        text = {
            Text(
                "${bustInfo.playerName} busted! " +
                "Attempted: ${bustInfo.attemptedScore}, " +
                "Score stays at ${bustInfo.remainingBefore}."
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}
