package com.dartsapp.ui.screens.game.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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

/** Smaller gap within a group or between grid and special row. */
private val INNER_GAP: Dp = 6.dp

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

    // fillMaxHeight so the inner weight(1f) on the grid can resolve against a bounded height.
    // The parent always provides a bounded height: in landscape via fillMaxHeight().weight(0.58f),
    // in portrait via fillMaxWidth().weight(1f).
    Column(modifier = modifier.fillMaxHeight().padding(horizontal = 8.dp, vertical = 8.dp)) {

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

        when (inputMode) {

            // ── Keypad mode ───────────────────────────────────────────
            InputMode.KEYPAD -> {
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

                // Number grid 1-20: 4 rows × 5 cols, each button square via aspectRatio(1f)
                Column(
                    modifier            = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    (1..20).chunked(5).forEach { row ->
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
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
                                    modifier       = Modifier.weight(1f).aspectRatio(1f),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                ) {
                                    Text("$number", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(GROUP_GAP))

                // Special buttons – directly below grid, same gap as multiplier row above
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

                // Remaining space goes here, pushing Rückgängig to the bottom
                Spacer(modifier = Modifier.weight(1f))

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

            // ── Board mode ────────────────────────────────────────────
            InputMode.BOARD -> {
                Spacer(modifier = Modifier.height(INNER_GAP))

                // Board fills all remaining height; Rückgängig overlaid at bottom-end
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    DartBoardInput(
                        onDartEntered = onDartEntered,
                        dartsEntered  = dartsEntered,
                        modifier      = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f, matchHeightConstraintsFirst = true)
                            .align(Alignment.Center)
                    )
                    Button(
                        onClick  = onUndo,
                        enabled  = canUndo,
                        shape    = BTN_CORNER,
                        colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .height(BTN_H)
                            .padding(bottom = 4.dp)
                    ) { Text("Rückgängig") }
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
