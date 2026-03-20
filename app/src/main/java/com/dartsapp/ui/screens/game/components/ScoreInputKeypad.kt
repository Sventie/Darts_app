package com.dartsapp.ui.screens.game.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dartsapp.data.model.ScoreMultiplier
import com.dartsapp.domain.model.DartInput

@Composable
fun ScoreInputKeypad(
    dartsEntered: Int,
    onDartEntered: (DartInput) -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMultiplier by remember { mutableStateOf(ScoreMultiplier.SINGLE) }

    Column(modifier = modifier.padding(8.dp)) {
        // Multiplier row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ScoreMultiplier.entries.forEach { mult ->
                FilterChip(
                    selected = selectedMultiplier == mult,
                    onClick = { selectedMultiplier = mult },
                    label = { Text(mult.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Number grid 1-20
        val numbers = (1..20).toList()
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(numbers) { number ->
                OutlinedButton(
                    onClick = {
                        val effectiveMultiplier = if (number == 25 && selectedMultiplier == ScoreMultiplier.TRIPLE) {
                            ScoreMultiplier.SINGLE
                        } else {
                            selectedMultiplier
                        }
                        onDartEntered(
                            DartInput(
                                field = number,
                                multiplier = effectiveMultiplier,
                                scoreValue = number * effectiveMultiplier.value
                            )
                        )
                    },
                    modifier = Modifier.padding(2.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text("$number", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Special buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Miss
            OutlinedButton(onClick = {
                onDartEntered(DartInput(field = 0, multiplier = ScoreMultiplier.SINGLE, scoreValue = 0))
            }) { Text("Miss") }

            // Bull (25 single/double only)
            OutlinedButton(onClick = {
                val mult = if (selectedMultiplier == ScoreMultiplier.TRIPLE) ScoreMultiplier.DOUBLE else selectedMultiplier
                onDartEntered(DartInput(field = 25, multiplier = mult, scoreValue = 25 * mult.value))
            }) { Text("Bull") }

            // Bullseye (50 = double bull)
            OutlinedButton(onClick = {
                onDartEntered(DartInput(field = 50, multiplier = ScoreMultiplier.SINGLE, scoreValue = 50))
            }) { Text("Bullseye") }

            // Undo
            Button(
                onClick = onUndo,
                enabled = dartsEntered > 0,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Undo") }
        }
    }
}
