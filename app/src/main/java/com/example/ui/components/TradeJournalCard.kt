package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.TradeJournalEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TradeJournalCard(
    trade: TradeJournalEntity,
    onDelete: (TradeJournalEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val greenColor = Color(0xFF00E676)
    val redColor = Color(0xFFFF5252)

    val isBuy = trade.tradeType.uppercase() == "BUY"
    val isWin = trade.status == "CLOSED_WIN" || trade.pnl > 0

    val pnlColor = if (isWin) greenColor else if (trade.status == "CLOSED_LOSS" || trade.pnl < 0) redColor else Color(0xFFFFB300)

    val formattedDate = remember(trade.timestamp) {
        SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault()).format(Date(trade.timestamp))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("journal_card_${trade.id}"),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF131822)
        ),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, pnlColor.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (isBuy) greenColor.copy(alpha = 0.15f) else redColor.copy(alpha = 0.15f),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                if (isBuy) greenColor.copy(alpha = 0.5f) else redColor.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBuy) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = trade.tradeType,
                            tint = if (isBuy) greenColor else redColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = trade.assetSymbol,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color.White
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF78909C)
                        )
                    }
                }

                // P&L Badge
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (trade.pnl >= 0) "+$${String.format(Locale.US, "%.2f", trade.pnl)}" else "-$${String.format(Locale.US, "%.2f", Math.abs(trade.pnl))}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = pnlColor
                    )
                    Surface(
                        color = pnlColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = trade.status.replace("_", " "),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = pnlColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Entry & Exit Price Details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0B0E14), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Entry: $${trade.entryPrice}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color(0xFFB0BEC5)
                )
                Text(
                    text = "Exit: ${trade.exitPrice?.let { "$$it" } ?: "Active"}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color(0xFFB0BEC5)
                )
                Text(
                    text = "SL: $${trade.stopLoss}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = redColor
                )
                Text(
                    text = "TP: $${trade.takeProfit}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = greenColor
                )
            }

            if (trade.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = trade.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF90A4AE),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onDelete(trade) },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("delete_journal_entry_btn_${trade.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFF607D8B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
