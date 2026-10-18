package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TradingSignalEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LiveSignalCard(
    signal: TradingSignalEntity,
    onConvertToJournal: (TradingSignalEntity) -> Unit,
    onDelete: (TradingSignalEntity) -> Unit,
    onExecuteOnBroker: ((TradingSignalEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val buyColor = Color(0xFF00E676)
    val sellColor = Color(0xFFFF5252)
    val waitColor = Color(0xFFFFB300)

    val actionColor = when (signal.action.uppercase()) {
        "BUY" -> buyColor
        "SELL" -> sellColor
        else -> waitColor
    }

    val actionIcon = when (signal.action.uppercase()) {
        "BUY" -> Icons.AutoMirrored.Filled.TrendingUp
        "SELL" -> Icons.AutoMirrored.Filled.TrendingDown
        else -> Icons.Default.Analytics
    }

    val formattedTime = remember(signal.timestamp) {
        SimpleDateFormat("HH:mm:ss - MMM dd", Locale.getDefault()).format(Date(signal.timestamp))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("signal_card_${signal.id}"),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF131822)
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, actionColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row: Symbol, Timeframe, Action Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(actionColor.copy(alpha = 0.15f))
                            .border(1.dp, actionColor.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = actionIcon,
                            contentDescription = signal.action,
                            tint = actionColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = signal.symbol,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFF1F2838),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = signal.timeframe,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color(0xFF90A4AE)
                                )
                            }
                        }
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF78909C)
                        )
                    }
                }

                // Action Badge
                Surface(
                    color = actionColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, actionColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = signal.action,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = actionColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${signal.confidenceScore}%",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Confidence Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI Confidence",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF90A4AE)
                )
                Spacer(modifier = Modifier.width(8.dp))
                LinearProgressIndicator(
                    progress = { signal.confidenceScore / 100f },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = actionColor,
                    trackColor = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${signal.confidenceScore}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = actionColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Key Trade Metrics Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0B0E14), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn(title = "Entry Zone", value = signal.entryZone, valueColor = Color.White)
                MetricColumn(title = "Stop Loss", value = signal.stopLoss, valueColor = sellColor)
                MetricColumn(title = "Take Profit", value = signal.takeProfit, valueColor = buyColor)
                MetricColumn(title = "R : R", value = signal.riskRewardRatio, valueColor = Color(0xFFFFD700))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Detected Patterns Tag Cloud
            if (signal.detectedPatterns.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = Color(0xFF82B1FF),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = signal.detectedPatterns,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color(0xFFB0BEC5),
                        maxLines = 1
                    )
                }
            }

            // Expandable Detailed AI Reasoning Section
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(Color(0xFF0F141D), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Institutional AI Reasoning",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF82B1FF)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = signal.reasoning,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCFD8DC),
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Key Levels & Zones",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFFFD54F)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = signal.keyLevels,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color(0xFFB0BEC5)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Card Footer Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.testTag("toggle_details_btn_${signal.id}")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand",
                            tint = Color(0xFF90A4AE)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (expanded) "Hide Details" else "AI Reasoning",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF90A4AE)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onExecuteOnBroker != null && signal.action != "WAIT") {
                        Button(
                            onClick = { onExecuteOnBroker(signal) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00E676),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("execute_broker_btn_${signal.id}")
                        ) {
                            Text(
                                text = "⚡ Execute",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    IconButton(
                        onClick = { onDelete(signal) },
                        modifier = Modifier.testTag("delete_signal_btn_${signal.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Signal",
                            tint = Color(0xFF78909C),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    FilledTonalButton(
                        onClick = { onConvertToJournal(signal) },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF1E293B),
                            contentColor = Color(0xFF00E676)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("log_journal_btn_${signal.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Log Trade",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricColumn(
    title: String,
    value: String,
    valueColor: Color
) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF78909C)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            ),
            color = valueColor
        )
    }
}
