package com.dartsapp.ui.screens.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dartsapp.data.db.entity.PlayerEntity

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerSelectionDialog(
    players:     List<PlayerEntity>,
    selectedIds: List<Long>,
    onToggle:    (Long) -> Unit,
    onDismiss:   () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape          = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            modifier       = Modifier.fillMaxSize(0.8f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Spieler auswählen", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(16.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp)
                ) {
                    players.forEach { player ->
                        val isSelected = player.id in selectedIds
                        Card(
                            onClick  = { onToggle(player.id) },
                            modifier = Modifier.size(144.dp),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                AutoSizeText(
                                    text        = player.name,
                                    maxFontSize = 26.sp,
                                    minFontSize = 12.sp,
                                    fontWeight  = FontWeight.Bold,
                                    textAlign   = TextAlign.Center,
                                    modifier    = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick        = onDismiss,
                    modifier       = Modifier.fillMaxWidth().defaultMinSize(minHeight = 64.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text("Fertig", fontSize = 28.sp)
                }
            }
        }
    }
}

@Composable
private fun AutoSizeText(
    text:        String,
    maxFontSize: TextUnit,
    minFontSize: TextUnit,
    modifier:    Modifier = Modifier,
    fontWeight:  FontWeight? = null,
    textAlign:   TextAlign? = null,
    color:       Color = Color.Unspecified,
) {
    var fontSize    by remember(text) { mutableStateOf(maxFontSize) }
    var readyToDraw by remember(text) { mutableStateOf(false) }

    Text(
        text       = text,
        fontSize   = fontSize,
        fontWeight = fontWeight,
        textAlign  = textAlign,
        color      = color,
        maxLines   = 1,
        softWrap   = false,
        overflow   = TextOverflow.Visible,
        modifier   = modifier.drawWithContent { if (readyToDraw) drawContent() },
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
