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
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.data.TransactionEntity
import com.example.ui.CashFlowViewModel
import com.example.ui.components.AddTransactionSheet
import com.example.ui.components.TransactionCard
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioCashFlowScreen(
    viewModel: CashFlowViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.filteredTransactions.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val showAddSheet by viewModel.showAddTransactionSheet.collectAsState()
    val txToEdit by viewModel.transactionToEdit.collectAsState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val netCashFlow = totalIncome - totalExpense

    val greenColor = Color(0xFF00E676)
    val redColor = Color(0xFFFF5252)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0E14))
            .testTag("portfolio_cash_flow_screen")
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
                // LEFT PANE: Portfolio Cash Flow Summary & Action Hub
                Column(
                    modifier = Modifier
                        .weight(if (isDesktopExpanded) 0.38f else 0.44f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PortfolioTotalCard(
                        netCashFlow = netCashFlow,
                        totalIncome = totalIncome,
                        totalExpense = totalExpense,
                        greenColor = greenColor,
                        redColor = redColor
                    )

                    // Quick Actions Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131822)),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF263238))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "PORTFOLIO CONTROLS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color(0xFF90A4AE)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { viewModel.openAddTransactionSheet() },
                                colors = ButtonDefaults.buttonColors(containerColor = greenColor, contentColor = Color.Black),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("add_cash_flow_entry_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Record Cash Inflow / Outflow", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Desktop Grid Mode Badge
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
                                    text = "MULTI-PANE DESKTOP GRID",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = greenColor
                                )
                                Text(
                                    text = "Split view active: Live analytics on the left, ledger grid on the right.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF90A4AE)
                                )
                            }
                        }
                    }
                }

                // RIGHT PANE: Multi-Pane Grid / Ledger
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
                            text = "Account Cash Flow History",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${transactions.size} records",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color(0xFF82B1FF)
                            )
                        }
                    }

                    if (transactions.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No cash flow entries logged.", color = Color(0xFF78909C))
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(if (isDesktopExpanded) 2 else 1),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("portfolio_transactions_list"),
                            contentPadding = PaddingValues(bottom = 30.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(transactions) { tx ->
                                TransactionCard(
                                    transaction = tx,
                                    onEdit = { viewModel.openAddTransactionSheet(tx) },
                                    onDelete = { viewModel.deleteTransaction(tx) }
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
                PortfolioTotalCard(
                    netCashFlow = netCashFlow,
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    greenColor = greenColor,
                    redColor = redColor
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Account Cash Flow History",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    Button(
                        onClick = { viewModel.openAddTransactionSheet() },
                        colors = ButtonDefaults.buttonColors(containerColor = greenColor, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("add_cash_flow_entry_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Entry", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (transactions.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No cash flow entries logged.", color = Color(0xFF78909C))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f).testTag("portfolio_transactions_list")
                    ) {
                        items(transactions) { tx ->
                            TransactionCard(
                                transaction = tx,
                                onEdit = { viewModel.openAddTransactionSheet(tx) },
                                onDelete = { viewModel.deleteTransaction(tx) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddTransactionSheet(
            sheetState = sheetState,
            transactionToEdit = txToEdit,
            onDismiss = { viewModel.closeAddTransactionSheet() },
            onSave = { title, amount, type, category, note, isRecurring ->
                viewModel.saveTransaction(title, amount, type, category, note, isRecurring)
            }
        )
    }
}

@Composable
private fun PortfolioTotalCard(
    netCashFlow: Double,
    totalIncome: Double,
    totalExpense: Double,
    greenColor: Color,
    redColor: Color,
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
                text = "TOTAL TRADING ACCOUNT CASH FLOW",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = Color(0xFF90A4AE)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$${String.format(Locale.US, "%.2f", netCashFlow)}",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                ),
                color = if (netCashFlow >= 0) greenColor else redColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0B0E14), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Trading Profits / Inflows", style = MaterialTheme.typography.labelSmall, color = Color(0xFF78909C))
                    Text("+$${String.format(Locale.US, "%.2f", totalIncome)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace), color = greenColor)
                }
                Column {
                    Text("Outflows & Fees", style = MaterialTheme.typography.labelSmall, color = Color(0xFF78909C))
                    Text("-$${String.format(Locale.US, "%.2f", totalExpense)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace), color = redColor)
                }
            }
        }
    }
}

