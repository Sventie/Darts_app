package com.dartsapp.ui.screens.game.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dartsapp.data.model.ScoreMultiplier
import com.dartsapp.domain.model.DartInput

private enum class InputMode { KEYPAD, BOARD }

/** Height for toggle/chip-style buttons (mode select, multiplier, special). */
private val BTN_H: Dp = 72.dp

/** Height for number grid cells. */
private val NUM_H: Dp = 96.dp

@Composable
fun ScoreInputKeypad(
    dartsEntered: Int,
    onDartEntered: (DartInput) -> Unit,
    onUndo: () -> Unit,
    canUndo: Boolean = dartsEntered > 0,
    modifier: Modifier = Modifier
) {
    var inputMode by remember { mutableStateOf(InputMode.KEYPAD) }
    var selectedMultiplier by remember { mutableStateOf(ScoreMultiplier.SINGLE) }

    Column(modifier = modifier.padding(horizontal = 8.dp)) {

        // ── Input mode toggle ──────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToggleButton(
                selected = inputMode == InputMode.KEYPAD,
                onClick  = { inputMode = InputMode.KEYPAD },
                label    = "Tastatur",
                modifier = Modifier.weight(1f).height(BTN_H)
            )
            ToggleButton(
                selected = inputMode == InputMode.BOARD,
                onClick  = { inputMode = InputMode.BOARD },
                label    = "Scheibe",
                modifier = Modifier.weight(1f).height(BTN_H)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        when (inputMode) {

            // ── Keypad mode ───────────────────────────────────────────
            InputMode.KEYPAD -> {
                // Multiplier row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScoreMultiplier.entries.forEach { mult ->
                        ToggleButton(
                            selected = selectedMultiplier == mult,
                            onClick  = { selectedMultiplier = mult },
                            label    = mult.name.lowercase().replaceFirstChar { it.uppercase() },
                            modifier = Modifier.weight(1f).height(BTN_H)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Number grid 1-20
                val numbers = (1..20).toList()
                LazyVerticalGrid(
                    columns               = GridCells.Fixed(5),
                    modifier              = Modifier.fillMaxWidth().height(NUM_H * 4 + 4.dp * 3),
                    verticalArrangement   = Arrangement.spacedBy(4.dp),
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
                            modifier       = Modifier.height(NUM_H),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Text("$number", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Special buttons row
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick  = {
                            onDartEntered(DartInput(0, ScoreMultiplier.SINGLE, 0))
                            selectedMultiplier = ScoreMultiplier.SINGLE
                        },
                        modifier = Modifier.weight(1f).height(BTN_H)
                    ) { Text("Daneben") }

                    OutlinedButton(
                        onClick  = {
                            val mult = if (selectedMultiplier == ScoreMultiplier.TRIPLE) ScoreMultiplier.DOUBLE else selectedMultiplier
                            onDartEntered(DartInput(25, mult, 25 * mult.value))
                            selectedMultiplier = ScoreMultiplier.SINGLE
                        },
                        modifier = Modifier.weight(1f).height(BTN_H)
                    ) { Text("Bull") }

                    OutlinedButton(
                        onClick  = {
                            onDartEntered(DartInput(50, ScoreMultiplier.SINGLE, 50))
                            selectedMultiplier = ScoreMultiplier.SINGLE
                        },
                        modifier = Modifier.weight(1f).height(BTN_H)
                    ) { Text("Bullseye") }

                    Button(
                        onClick  = onUndo,
                        enabled  = canUndo,
                        colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f).height(BTN_H)
                    ) { Text("Rückgängig") }
                }
            }

            // ── Board mode ────────────────────────────────────────────
            InputMode.BOARD -> {
                Box(
                    modifier          = Modifier.fillMaxWidth(),
                    contentAlignment  = Alignment.Center
                ) {
                    DartBoardInput(
                        onDartEntered = onDartEntered,
                        dartsEntered  = dartsEntered,
                        modifier      = Modifier.fillMaxWidth(0.85f).aspectRatio(1f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onUndo,
                        enabled = canUndo,
                        colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.height(BTN_H)
                    ) { Text("Rückgängig") }
                }
            }
        }
    }
}

/**
 * A toggle button that renders filled (selected) or outlined (unselected).
 */
@Composable
private fun ToggleButton(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(
            onClick  = onClick,
            modifier = modifier
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        OutlinedButton(
            onClick  = onClick,
            modifier = modifier
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
