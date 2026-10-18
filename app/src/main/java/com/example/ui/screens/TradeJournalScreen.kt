package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TradeJournalEntity
import com.example.ui.CashFlowViewModel
import com.example.ui.components.AddJournalDialog
import com.example.ui.components.TradeJournalCard
import java.util.Locale

@Composable
fun TradeJournalScreen(
    viewModel: CashFlowViewModel,
    modifier: Modifier = Modifier
) {
    val trades by viewModel.allJournalTrades.collectAsState()
    val totalPnL by viewModel.totalJournalPnL.collectAsState()
    val winCount by viewModel.winCount.collectAsState()
    val totalClosedCount by viewModel.totalClosedTradesCount.collectAsState()
    val showAddDialog by viewModel.showAddJournalDialog.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") }

    val greenColor = Color(0xFF00E676)
    val redColor = Color(0xFFFF5252)

    val calculatedWinRate = if (totalClosedCount > 0) {
        (winCount.toFloat() / totalClosedCount * 100).toInt()
    } else 0

    val currentPnL = totalPnL ?: 0.0

    val filteredTrades = remember(trades, searchQuery, statusFilter) {
        trades.filter { tr: TradeJournalEntity ->
            val matchesSearch = searchQuery.isBlank() || tr.assetSymbol.contains(searchQuery, ignoreCase = true) || tr.notes.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (statusFilter) {
                "OPEN" -> tr.status == "OPEN"
                "WIN" -> tr.status == "CLOSED_WIN"
                "LOSS" -> tr.status == "CLOSED_LOSS"
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0E14))
            .testTag("trade_journal_screen")
    ) {
        val isWideScreen = maxWidth >= 600.dp
        val isDesktopExpanded = maxWidth >= 960.dp

        if (isWideScreen) {
            // MULTI-PANE GRID DESKTOP / PC LAYOUT
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // LEFT PANE: Sticky PnL Summary & Filter Controls
                Column(
                    modifier = Modifier
                        .weight(if (isDesktopExpanded) 0.38f else 0.44f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TradePnLSummaryCard(
                        currentPnL = currentPnL,
                        calculatedWinRate = calculatedWinRate,
                        winCount = winCount,
                        totalClosedCount = totalClosedCount,
                        totalTradesCount = trades.size,
                        greenColor = greenColor,
                        redColor = redColor,
                        onLogTrade = { viewModel.openAddJournalDialog() }
                    )

                    // Filter & Search Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131822)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF263238))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Filter Journal Positions",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )

                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search by Symbol or Note...") },
                                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF78909C)) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("journal_search_bar"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = greenColor,
                                    unfocusedBorderColor = Color(0xFF263238)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Row(modifier = Modifier.fillMaxWidth()) {
                                FilterChip(
                                    selected = statusFilter == "ALL",
                                    onClick = { statusFilter = "ALL" },
                                    label = { Text("ALL") }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                FilterChip(
                                    selected = statusFilter == "OPEN",
                                    onClick = { statusFilter = "OPEN" },
                                    label = { Text("OPEN") }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                FilterChip(
                                    selected = statusFilter == "WIN",
                                    onClick = { statusFilter = "WIN" },
                                    label = { Text("WINS") },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = greenColor.copy(alpha = 0.2f), selectedLabelColor = greenColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                FilterChip(
                                    selected = statusFilter == "LOSS",
                                    onClick = { statusFilter = "LOSS" },
                                    label = { Text("LOSSES") },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = redColor.copy(alpha = 0.2f), selectedLabelColor = redColor)
                                )
                            }
                        }
                    }

                    // Desktop Grid Pill
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF00E676).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = null,
                                tint = greenColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "DESKTOP JOURNAL GRID",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = greenColor
                                )
                                Text(
                                    text = "2-pane responsive grid view for analyzing execution history on PC.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF90A4AE)
                                )
                            }
                        }
                    }
                }

                // RIGHT PANE: Multi-Pane Grid for Journal Entries
                Column(
                    modifier = Modifier
                        .weight(if (isDesktopExpanded) 0.62f else 0.56f)
                        .fillMaxHeight()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Executed Trade Ledger",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${filteredTrades.size} trades",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color(0xFF82B1FF)
                            )
                        }
                    }

                    if (filteredTrades.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No trades in journal matching filters.\nTap 'LOG TRADE' to record a position.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF78909C)
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(if (isDesktopExpanded) 2 else 1),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("journal_trades_list"),
                            contentPadding = PaddingValues(bottom = 30.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredTrades) { trade ->
                                TradeJournalCard(
                                    trade = trade,
                                    onDelete = { viewModel.deleteJournalTrade(it) }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // VERTICAL MOBILE LIST VIEW (Default Compact Screen)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                TradePnLSummaryCard(
                    currentPnL = currentPnL,
                    calculatedWinRate = calculatedWinRate,
                    winCount = winCount,
                    totalClosedCount = totalClosedCount,
                    totalTradesCount = trades.size,
                    greenColor = greenColor,
                    redColor = redColor,
                    onLogTrade = { viewModel.openAddJournalDialog() }
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by Symbol or Note...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF78909C)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("journal_search_bar"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = greenColor,
                        unfocusedBorderColor = Color(0xFF263238)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = statusFilter == "ALL",
                        onClick = { statusFilter = "ALL" },
                        label = { Text("ALL") }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    FilterChip(
                        selected = statusFilter == "OPEN",
                        onClick = { statusFilter = "OPEN" },
                        label = { Text("OPEN") }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    FilterChip(
                        selected = statusFilter == "WIN",
                        onClick = { statusFilter = "WIN" },
                        label = { Text("WINS") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = greenColor.copy(alpha = 0.2f), selectedLabelColor = greenColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    FilterChip(
                        selected = statusFilter == "LOSS",
                        onClick = { statusFilter = "LOSS" },
                        label = { Text("LOSSES") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = redColor.copy(alpha = 0.2f), selectedLabelColor = redColor)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (filteredTrades.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No trades in journal matching filters.\nTap 'LOG TRADE' to record a position.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF78909C)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("journal_trades_list")
                    ) {
                        items(filteredTrades) { trade ->
                            TradeJournalCard(
                                trade = trade,
                                onDelete = { viewModel.deleteJournalTrade(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddJournalDialog(
            onDismiss = { viewModel.closeAddJournalDialog() },
            onSave = { symbol, type, entry, exit, sl, tp, pos, status, notes ->
                viewModel.saveJournalTrade(symbol, type, entry, exit, sl, tp, pos, status, notes)
            }
        )
    }
}

@Composable
private fun TradePnLSummaryCard(
    currentPnL: Double,
    calculatedWinRate: Int,
    winCount: Int,
    totalClosedCount: Int,
    totalTradesCount: Int,
    greenColor: Color,
    redColor: Color,
    onLogTrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131822)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF263238))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "REALIZED TRADING JOURNAL P&L",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = Color(0xFF90A4AE)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (currentPnL >= 0) "+$${String.format(Locale.US, "%.2f", currentPnL)}" else "-$${String.format(Locale.US, "%.2f", Math.abs(currentPnL))}",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = if (currentPnL >= 0) greenColor else redColor
                )

                Button(
                    onClick = onLogTrade,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = greenColor,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("open_add_journal_dialog_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("LOG TRADE", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0B0E14), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Win Rate", style = MaterialTheme.typography.labelSmall, color = Color(0xFF78909C))
                    Text("$calculatedWinRate%", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace), color = greenColor)
                }
                Column {
                    Text("Wins / Closed", style = MaterialTheme.typography.labelSmall, color = Color(0xFF78909C))
                    Text("$winCount / $totalClosedCount", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace), color = Color.White)
                }
                Column {
                    Text("Total Journaled", style = MaterialTheme.typography.labelSmall, color = Color(0xFF78909C))
                    Text("$totalTradesCount", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace), color = Color(0xFF82B1FF))
                }
            }
        }
    }
}

