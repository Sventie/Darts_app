package com.dartsapp.ui.screens.game.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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

private enum class InputMode { KEYPAD, BOARD }

@Composable
fun ScoreInputKeypad(
    dartsEntered: Int,
    onDartEntered: (DartInput) -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputMode by remember { mutableStateOf(InputMode.KEYPAD) }
    var selectedMultiplier by remember { mutableStateOf(ScoreMultiplier.SINGLE) }

    Column(modifier = modifier.padding(horizontal = 8.dp)) {

        // ── Input mode toggle ──────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            FilterChip(
                selected = inputMode == InputMode.KEYPAD,
                onClick  = { inputMode = InputMode.KEYPAD },
                label    = { Text("Tastatur") },
                modifier = Modifier.padding(end = 8.dp)
            )
            FilterChip(
                selected = inputMode == InputMode.BOARD,
                onClick  = { inputMode = InputMode.BOARD },
                label    = { Text("Scheibe") }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        when (inputMode) {

            // ── Keypad mode ───────────────────────────────────────────
            InputMode.KEYPAD -> {
                // Multiplier row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ScoreMultiplier.entries.forEach { mult ->
                        FilterChip(
                            selected = selectedMultiplier == mult,
                            onClick  = { selectedMultiplier = mult },
                            label    = { Text(mult.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Number grid 1-20  (4 rows × 52dp + 3 gaps × 4dp = 220dp)
                val numbers = (1..20).toList()
                LazyVerticalGrid(
                    columns             = GridCells.Fixed(5),
                    modifier            = Modifier.fillMaxWidth().height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(numbers) { number ->
                        OutlinedButton(
                            onClick = {
                                val eff = if (number == 25 && selectedMultiplier == ScoreMultiplier.TRIPLE)
                                    ScoreMultiplier.SINGLE else selectedMultiplier
                                onDartEntered(DartInput(number, eff, number * eff.value))
                                selectedMultiplier = ScoreMultiplier.SINGLE
                            },
                            modifier       = Modifier.height(52.dp).padding(2.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Text("$number", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Special buttons row
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = {
                        onDartEntered(DartInput(0, ScoreMultiplier.SINGLE, 0))
                        selectedMultiplier = ScoreMultiplier.SINGLE
                    }) { Text("Daneben") }

                    OutlinedButton(onClick = {
                        val mult = if (selectedMultiplier == ScoreMultiplier.TRIPLE) ScoreMultiplier.DOUBLE else selectedMultiplier
                        onDartEntered(DartInput(25, mult, 25 * mult.value))
                        selectedMultiplier = ScoreMultiplier.SINGLE
                    }) { Text("Bull") }

                    OutlinedButton(onClick = {
                        onDartEntered(DartInput(50, ScoreMultiplier.SINGLE, 50))
                        selectedMultiplier = ScoreMultiplier.SINGLE
                    }) { Text("Bullseye") }

                    Button(
                        onClick = onUndo,
                        enabled = dartsEntered > 0,
                        colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Rückgängig") }
                }
            }

            // ── Board mode ────────────────────────────────────────────
            InputMode.BOARD -> {
                DartBoardInput(
                    onDartEntered = onDartEntered,
                    dartsEntered  = dartsEntered,
                    modifier      = Modifier.fillMaxWidth(0.85f).aspectRatio(1f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onUndo,
                        enabled = dartsEntered > 0,
                        colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Rückgängig") }
                }
            }
        }
    }
}
