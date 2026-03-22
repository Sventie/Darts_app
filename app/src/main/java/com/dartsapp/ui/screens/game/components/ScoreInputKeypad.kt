package com.dartsapp.ui.screens.game.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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

/** Corner radius matching the player score cards. */
private val BTN_CORNER = RoundedCornerShape(8.dp)

/** Height for mode/multiplier/special buttons. */
private val BTN_H: Dp = 48.dp

/** Larger gap between logical button groups (mode→multiplier, multiplier→grid). */
private val GROUP_GAP: Dp = 20.dp

@Composable
fun ScoreInputKeypad(
    dartsEntered: Int,
    onDartEntered: (DartInput) -> Unit,
    onUndo: () -> Unit,
    canUndo: Boolean = dartsEntered > 0,
    modifier: Modifier = Modifier
) {
    var inputMode by remember { mutableStateOf(InputMode.BOARD) }
    var selectedMultiplier by remember { mutableStateOf(ScoreMultiplier.SINGLE) }

    Column(modifier = modifier.fillMaxHeight()) {

        // ── Input mode toggle (always padded) ─────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToggleButton(
                selected = inputMode == InputMode.BOARD,
                onClick  = { inputMode = InputMode.BOARD },
                label    = "Scheibe",
                modifier = Modifier.weight(1f).height(BTN_H)
            )
            ToggleButton(
                selected = inputMode == InputMode.KEYPAD,
                onClick  = { inputMode = InputMode.KEYPAD },
                label    = "Tastatur",
                modifier = Modifier.weight(1f).height(BTN_H)
            )
        }

        when (inputMode) {

            // ── Board mode: fills all remaining space edge-to-edge ────────
            InputMode.BOARD -> {
                BoxWithConstraints(
                    modifier         = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val boardSize = minOf(maxWidth, maxHeight)
                    DartBoardInput(
                        onDartEntered = onDartEntered,
                        dartsEntered  = dartsEntered,
                        modifier      = Modifier.size(boardSize)
                    )
                    Button(
                        onClick  = onUndo,
                        enabled  = canUndo,
                        shape    = BTN_CORNER,
                        colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .height(BTN_H)
                            .padding(bottom = 4.dp, end = 8.dp)
                    ) { Text("Rückgängig") }
                }
            }

            // ── Keypad mode ───────────────────────────────────────────────
            InputMode.KEYPAD -> {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 8.dp)
                        .padding(bottom = 8.dp)
                ) {
                    Spacer(modifier = Modifier.height(GROUP_GAP))

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

                    Spacer(modifier = Modifier.height(GROUP_GAP))

                    // Number grid 1-20: 4 rows × 5 cols, scales to fill all remaining height
                    Column(
                        modifier            = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        (1..20).chunked(5).forEach { row ->
                            Row(
                                modifier              = Modifier.fillMaxWidth().weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                row.forEach { number ->
                                    OutlinedButton(
                                        onClick = {
                                            val eff = if (number == 25 && selectedMultiplier == ScoreMultiplier.TRIPLE)
                                                ScoreMultiplier.SINGLE else selectedMultiplier
                                            onDartEntered(DartInput(number, eff, number * eff.value))
                                            selectedMultiplier = ScoreMultiplier.SINGLE
                                        },
                                        shape          = BTN_CORNER,
                                        modifier       = Modifier.weight(1f).fillMaxHeight(),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                    ) {
                                        Text("$number", style = MaterialTheme.typography.titleLarge)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(GROUP_GAP))

                    // Special buttons
                    val isSingle = selectedMultiplier == ScoreMultiplier.SINGLE
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick  = {
                                onDartEntered(DartInput(0, ScoreMultiplier.SINGLE, 0))
                                selectedMultiplier = ScoreMultiplier.SINGLE
                            },
                            enabled  = isSingle,
                            shape    = BTN_CORNER,
                            modifier = Modifier.weight(1f).height(BTN_H)
                        ) { Text("Daneben") }

                        OutlinedButton(
                            onClick  = {
                                onDartEntered(DartInput(25, ScoreMultiplier.SINGLE, 25))
                                selectedMultiplier = ScoreMultiplier.SINGLE
                            },
                            enabled  = isSingle,
                            shape    = BTN_CORNER,
                            modifier = Modifier.weight(1f).height(BTN_H)
                        ) { Text("Bull") }

                        OutlinedButton(
                            onClick  = {
                                onDartEntered(DartInput(50, ScoreMultiplier.SINGLE, 50))
                                selectedMultiplier = ScoreMultiplier.SINGLE
                            },
                            enabled  = isSingle,
                            shape    = BTN_CORNER,
                            modifier = Modifier.weight(1f).height(BTN_H)
                        ) { Text("Bullseye") }
                    }

                    Spacer(modifier = Modifier.height(GROUP_GAP))

                    // Rückgängig at the very bottom-end
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick  = onUndo,
                            enabled  = canUndo,
                            shape    = BTN_CORNER,
                            colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.height(BTN_H)
                        ) { Text("Rückgängig") }
                    }
                }
            }
        }
    }
}

/**
 * A toggle button with card-like rounded corners.
 * Renders filled when selected, outlined otherwise.
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
            shape    = BTN_CORNER,
            modifier = modifier
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        OutlinedButton(
            onClick  = onClick,
            shape    = BTN_CORNER,
            modifier = modifier
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
