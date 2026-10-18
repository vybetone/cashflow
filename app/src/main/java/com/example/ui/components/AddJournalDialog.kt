package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun AddJournalDialog(
    onDismiss: () -> Unit,
    onSave: (
        symbol: String,
        tradeType: String,
        entryPrice: Double,
        exitPrice: Double?,
        stopLoss: Double,
        takeProfit: Double,
        positionSize: Double,
        status: String,
        notes: String
    ) -> Unit
) {
    var symbol by remember { mutableStateOf("") }
    var tradeType by remember { mutableStateOf("BUY") }
    var entryPriceText by remember { mutableStateOf("") }
    var exitPriceText by remember { mutableStateOf("") }
    var stopLossText by remember { mutableStateOf("") }
    var takeProfitText by remember { mutableStateOf("") }
    var positionSizeText by remember { mutableStateOf("1.0") }
    var status by remember { mutableStateOf("OPEN") }
    var notes by remember { mutableStateOf("") }

    val greenColor = Color(0xFF00E676)
    val redColor = Color(0xFFFF5252)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Log New Trade Entry",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        },
        containerColor = Color(0xFF131822),
        confirmButton = {
            Button(
                onClick = {
                    val entry = entryPriceText.toDoubleOrNull() ?: 0.0
                    val exit = exitPriceText.toDoubleOrNull()
                    val sl = stopLossText.toDoubleOrNull() ?: (entry * 0.98)
                    val tp = takeProfitText.toDoubleOrNull() ?: (entry * 1.05)
                    val pos = positionSizeText.toDoubleOrNull() ?: 1.0

                    if (symbol.isNotBlank() && entry > 0.0) {
                        onSave(symbol, tradeType, entry, exit, sl, tp, pos, status, notes)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = greenColor,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("save_journal_entry_btn")
            ) {
                Text("Save Entry", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_journal_entry_btn")
            ) {
                Text("Cancel", color = Color(0xFF90A4AE))
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Symbol Input
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it },
                    label = { Text("Asset Symbol (e.g. BTC/USD, NVDA)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("symbol_input_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = greenColor,
                        unfocusedBorderColor = Color(0xFF263238)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Trade Type Chips
                Row {
                    FilterChip(
                        selected = tradeType == "BUY",
                        onClick = { tradeType = "BUY" },
                        label = { Text("BUY / LONG") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = greenColor.copy(alpha = 0.2f),
                            selectedLabelColor = greenColor
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = tradeType == "SELL",
                        onClick = { tradeType = "SELL" },
                        label = { Text("SELL / SHORT") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = redColor.copy(alpha = 0.2f),
                            selectedLabelColor = redColor
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Prices Row
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = entryPriceText,
                        onValueChange = { entryPriceText = it },
                        label = { Text("Entry Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("entry_price_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = exitPriceText,
                        onValueChange = { exitPriceText = it },
                        label = { Text("Exit Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("exit_price_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Status Chips
                Row {
                    FilterChip(
                        selected = status == "OPEN",
                        onClick = { status = "OPEN" },
                        label = { Text("OPEN") }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    FilterChip(
                        selected = status == "CLOSED_WIN",
                        onClick = { status = "CLOSED_WIN" },
                        label = { Text("CLOSED WIN") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = greenColor.copy(alpha = 0.2f),
                            selectedLabelColor = greenColor
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    FilterChip(
                        selected = status == "CLOSED_LOSS",
                        onClick = { status = "CLOSED_LOSS" },
                        label = { Text("CLOSED LOSS") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = redColor.copy(alpha = 0.2f),
                            selectedLabelColor = redColor
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Strategy Notes & Observations") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trade_notes_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        }
    )
}
