package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TradingSignal

@Composable
fun AiSignal90Card(
    signal: TradingSignal,
    onRefreshSignal: () -> Unit,
    onLogToJournal: ((TradingSignal) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val buyColor = Color(0xFF00E676)
    val sellColor = Color(0xFFFF5252)
    val waitColor = Color(0xFFFFB300)
    val goldColor = Color(0xFFFFD700)

    val actionColor = when (signal.signalType.uppercase()) {
        "BUY" -> buyColor
        "SELL" -> sellColor
        else -> waitColor
    }

    val isStrong = signal.confidencePercentage >= 90 || signal.isStrongSignal

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("ai_signal_90_card"),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F172A)
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = if (isStrong) 2.dp else 1.dp,
            color = if (isStrong) goldColor else actionColor.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: AI SIGNAL Title & High Confidence Gold Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isStrong) goldColor.copy(alpha = 0.2f) else actionColor.copy(alpha = 0.2f))
                            .border(1.dp, if (isStrong) goldColor else actionColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (isStrong) goldColor else actionColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "AI SIGNAL",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = signal.symbol,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                if (isStrong) {
                    Surface(
                        color = goldColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, goldColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = goldColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "STRONG AI SIGNAL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = goldColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Signal Banner: e.g. BUY 92%
            Surface(
                color = actionColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, actionColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (signal.signalType == "SELL") Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = signal.signalType,
                            tint = actionColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${signal.signalType} ${signal.confidencePercentage}%",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = actionColor
                        )
                    }

                    IconButton(
                        onClick = onRefreshSignal,
                        modifier = Modifier.testTag("refresh_ai_signal_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recalculate AI Signal",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI Confidence Score",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.width(8.dp))
                LinearProgressIndicator(
                    progress = { signal.confidencePercentage / 100f },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isStrong) goldColor else actionColor,
                    trackColor = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${signal.confidencePercentage}%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = if (isStrong) goldColor else actionColor
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Display Entry, Stop Loss, Take Profit
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF020617), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                LevelRow(label = "Entry:", value = "$${String.format("%.2f", signal.entryPrice)}", valueColor = Color.White)
                Spacer(modifier = Modifier.height(6.dp))
                LevelRow(label = "Stop Loss:", value = "$${String.format("%.2f", signal.stopLoss)}", valueColor = sellColor)
                Spacer(modifier = Modifier.height(6.dp))
                LevelRow(label = "Take Profit:", value = "$${String.format("%.2f", signal.takeProfit)}", valueColor = buyColor)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Display Reason
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "Reason:",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = signal.analysisReason,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFE2E8F0),
                        lineHeight = 18.sp
                    )
                )
            }

            // Expandable Technical Indicator Breakdown
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(Color(0xFF020617), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "TECHNICAL INDICATOR BREAKDOWN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFF59E0B)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    IndicatorBadge(
                        name = "RSI (14)",
                        detail = if (signal.rsiValue <= 0.0) "INSUFFICIENT DATA" else "${String.format("%.1f", signal.rsiValue)} - ${if (signal.rsiValue <= 35) "Oversold" else if (signal.rsiValue >= 65) "Overbought" else "Neutral"}"
                    )
                    IndicatorBadge(name = "MACD", detail = signal.macdSignal)
                    IndicatorBadge(name = "EMA Trend", detail = signal.emaTrend)
                    IndicatorBadge(name = "Candle Pattern", detail = signal.candlePattern)
                    IndicatorBadge(name = "Momentum", detail = signal.momentumConfirmation)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Card Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.testTag("toggle_indicators_btn")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Indicators",
                            tint = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (expanded) "Hide Indicators" else "View 5 Indicators",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                if (onLogToJournal != null) {
                    FilledTonalButton(
                        onClick = { onLogToJournal(signal) },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF1E293B),
                            contentColor = buyColor
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("log_ai_90_signal_btn")
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
private fun LevelRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF94A3B8)
        )
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

@Composable
private fun IndicatorBadge(name: String, detail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF94A3B8)
            )
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )
        )
    }
}
