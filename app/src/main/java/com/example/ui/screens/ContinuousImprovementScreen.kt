package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ImprovementProposalEntity
import com.example.data.ProposalStatus
import com.example.ui.CashFlowViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ContinuousImprovementScreen(
    viewModel: CashFlowViewModel,
    modifier: Modifier = Modifier
) {
    val activeConfig by viewModel.activeRemoteConfig.collectAsState()
    val evaluationReport by viewModel.evaluationReport.collectAsState()
    val proposals by viewModel.allProposals.collectAsState()
    val allOutcomes by viewModel.allPerformanceOutcomes.collectAsState()
    val evalStatusText by viewModel.evaluationStatusText.collectAsState()
    val updateStatusText by viewModel.updateCheckStatusText.collectAsState()

    val greenColor = MaterialTheme.colorScheme.primary
    val cardBg = MaterialTheme.colorScheme.surface
    val cardBorder = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurface
    val subTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    val completedCount = allOutcomes.count { it.outcomeStatus in listOf("WIN", "LOSS") }
    val winCount = allOutcomes.count { it.outcomeStatus == "WIN" }
    val overallWinRate = if (completedCount > 0) (winCount.toDouble() / completedCount * 100.0) else 0.0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("continuous_improvement_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Banner & Active Versions
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, greenColor.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("config_version_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = greenColor.copy(alpha = 0.2f),
                                shape = CircleShape
                            ) {
                                Box(modifier = Modifier.padding(8.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = greenColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "CONTINUOUS LEARNING & AUTO-IMPROVE",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = textColor
                                )
                                Text(
                                    text = "Chronological Out-of-Sample Empirical Evaluation",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = subTextColor
                                )
                            }
                        }

                        Surface(
                            color = greenColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, greenColor)
                        ) {
                            Text(
                                text = "CONFIG v${activeConfig.version}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = greenColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Strategy Version:", style = MaterialTheme.typography.labelSmall, color = subTextColor)
                            Text(
                                activeConfig.strategyVersion,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFF64B5F6)
                            )
                        }
                        Column {
                            Text("Gemini Model ID:", style = MaterialTheme.typography.labelSmall, color = subTextColor)
                            Text(
                                activeConfig.geminiModelId,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = textColor
                            )
                        }
                        Column {
                            Text("Max Risk Clamp:", style = MaterialTheme.typography.labelSmall, color = subTextColor)
                            Text(
                                "${activeConfig.maxOpenRiskPercentage}% (Hard Ceiling <= 2.0%)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = greenColor
                            )
                        }
                    }
                }
            }
        }

        // 2. Status & Error Feedback Banner
        if (evalStatusText != null) {
            item {
                Surface(
                    color = Color(0xFF263238),
                    border = BorderStroke(1.dp, Color(0xFF37474F)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("evaluation_status_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueryStats,
                            contentDescription = null,
                            tint = Color(0xFF80CBC4),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = evalStatusText ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color(0xFFECEFF1)
                        )
                    }
                }
            }
        }

        // 3. Quantitative Out-Of-Sample Metrics Grid
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, cardBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("quantitative_metrics_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "EMPIRICAL OUT-OF-SAMPLE METRICS",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = textColor
                        )

                        Surface(
                            color = Color(0xFF1E88E5).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Zero Data Leakage (70/30)",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color(0xFF90CAF9)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricTile(
                            label = "Signals Logged",
                            value = "${allOutcomes.size}",
                            sublabel = "$completedCount closed"
                        )
                        MetricTile(
                            label = "OOS Win Rate",
                            value = if (evaluationReport != null) "${String.format(Locale.US, "%.1f", evaluationReport!!.winRateOutOfSample)}%" else "${String.format(Locale.US, "%.1f", overallWinRate)}%",
                            sublabel = "Fee/Slippage Adj."
                        )
                        MetricTile(
                            label = "Expectancy",
                            value = if (evaluationReport != null) "$${String.format(Locale.US, "%.2f", evaluationReport!!.expectancyOutOfSample)}" else "$0.00",
                            sublabel = "Net / Trade"
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricTile(
                            label = "Profit Factor",
                            value = if (evaluationReport != null) String.format(Locale.US, "%.2f", evaluationReport!!.profitFactorOutOfSample) else "N/A",
                            sublabel = "Gross P/L Ratio"
                        )
                        MetricTile(
                            label = "Max Drawdown",
                            value = if (evaluationReport != null) "${String.format(Locale.US, "%.1f", evaluationReport!!.maxDrawdownPctOutOfSample)}%" else "0.0%",
                            sublabel = "Limit: <=15.0%"
                        )
                        MetricTile(
                            label = "Market Regime",
                            value = evaluationReport?.detectedRegime ?: "ANALYZING",
                            sublabel = "Volatility Mode"
                        )
                    }
                }
            }
        }

        // 4. Action Controls Bar
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, cardBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("action_controls_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SELF-IMPROVEMENT PIPELINE CONTROLS",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = textColor
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.runQuantitativeEvaluation() },
                            colors = ButtonDefaults.buttonColors(containerColor = greenColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("run_eval_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RUN EVALUATION", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }

                        OutlinedButton(
                            onClick = { viewModel.rollbackToKnownGoodConfig() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("rollback_config_button")
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFFFB300))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("1-TAP ROLLBACK", color = Color(0xFFFFB300), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.checkForAppUpdates() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("check_updates_button")
                        ) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF64B5F6))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CHECK UPDATES", color = Color(0xFF64B5F6), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }

                        OutlinedButton(
                            onClick = { viewModel.purgeOldTelemetry(90) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("purge_telemetry_button")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp), tint = subTextColor)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PURGE >90D", color = subTextColor, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    if (updateStatusText != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = updateStatusText ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color(0xFF90CAF9)
                        )
                    }
                }
            }
        }

        // 5. Proposals List
        item {
            Text(
                text = "ALGORITHMIC IMPROVEMENT PROPOSALS",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = textColor,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (proposals.isEmpty()) {
            item {
                Surface(
                    color = cardBg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = greenColor,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No pending strategy proposals",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = textColor
                        )
                        Text(
                            text = "Accumulate closed trades and tap 'RUN EVALUATION' to test candidates.",
                            style = MaterialTheme.typography.bodySmall,
                            color = subTextColor
                        )
                    }
                }
            }
        } else {
            items(proposals) { proposal ->
                ProposalCard(
                    proposal = proposal,
                    onApprove = { viewModel.approveProposal(proposal) },
                    onReject = { viewModel.rejectProposal(proposal) }
                )
            }
        }
    }
}

@Composable
fun MetricTile(label: String, value: String, sublabel: String) {
    Column(
        modifier = Modifier.width(100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black
            ),
            color = MaterialTheme.colorScheme.primary
        )
        Text(text = sublabel, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
    }
}

@Composable
fun ProposalCard(
    proposal: ImprovementProposalEntity,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val cardBg = MaterialTheme.colorScheme.surface
    val cardBorder = MaterialTheme.colorScheme.surfaceVariant
    val greenColor = MaterialTheme.colorScheme.primary
    val isPending = proposal.status == ProposalStatus.PROPOSED.name

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, if (isPending) Color(0xFF64B5F6) else cardBorder),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("proposal_item_${proposal.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = proposal.proposalTitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    color = when (proposal.status) {
                        ProposalStatus.APPROVED.name -> greenColor.copy(alpha = 0.2f)
                        ProposalStatus.REJECTED.name -> Color(0xFFEF5350).copy(alpha = 0.2f)
                        else -> Color(0xFF64B5F6).copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = proposal.status,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = when (proposal.status) {
                            ProposalStatus.APPROVED.name -> greenColor
                            ProposalStatus.REJECTED.name -> Color(0xFFEF5350)
                            else -> Color(0xFF90CAF9)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = proposal.hypothesis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Version: ${proposal.candidateStrategyVersion}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF90CAF9))
                Text("Min Confidence: ${proposal.minConfidenceThreshold}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                Text("RSI Oversold: ${proposal.rsiOversoldThreshold}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
            }

            if (isPending) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("reject_proposal_${proposal.id}")
                    ) {
                        Text("REJECT", color = Color(0xFFEF5350), style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = greenColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("approve_proposal_${proposal.id}")
                    ) {
                        Text("APPROVE & APPLY", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}
