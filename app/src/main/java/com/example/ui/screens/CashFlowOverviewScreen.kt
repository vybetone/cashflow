package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransactionCategory
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.components.TransactionCard
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CashFlowOverviewScreen(
    transactions: List<TransactionEntity>,
    totalIncome: Double,
    totalExpense: Double,
    searchQuery: String,
    selectedTypeFilter: TransactionType?,
    selectedCategoryFilter: TransactionCategory?,
    onSearchChange: (String) -> Unit,
    onTypeFilterSelect: (TransactionType?) -> Unit,
    onCategoryFilterSelect: (TransactionCategory?) -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val netCashFlow = totalIncome - totalExpense
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp
        val isDesktopExpanded = maxWidth >= 960.dp

        if (isWideScreen) {
            // MULTI-PANE GRID / SPLIT DESKTOP LAYOUT (PC Screen / Tablet)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("cashflow_wide_multipane_layout")
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // LEFT PANE: Sticky Financial Analytics & Filters Summary
                Column(
                    modifier = Modifier
                        .weight(if (isDesktopExpanded) 0.38f else 0.44f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Net Cash Flow Master Banner
                    CashFlowHeroBanner(
                        netCashFlow = netCashFlow,
                        totalIncome = totalIncome,
                        totalExpense = totalExpense,
                        currencyFormatter = currencyFormatter
                    )

                    // Search and Filter controls card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Search & Filter Cash Flow",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = onSearchChange,
                                placeholder = { Text("Search memos or payees...") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = "Search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { onSearchChange("") }) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Clear",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("cashflow_search_bar"),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.Transparent
                                )
                            )

                            // Filter Chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedTypeFilter == null,
                                    onClick = { onTypeFilterSelect(null) },
                                    label = { Text("All Flow") },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("filter_chip_all_flow")
                                )

                                FilterChip(
                                    selected = selectedTypeFilter == TransactionType.INCOME,
                                    onClick = { onTypeFilterSelect(TransactionType.INCOME) },
                                    label = { Text("Income") },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("filter_chip_income")
                                )

                                FilterChip(
                                    selected = selectedTypeFilter == TransactionType.EXPENSE,
                                    onClick = { onTypeFilterSelect(TransactionType.EXPENSE) },
                                    label = { Text("Expense") },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("filter_chip_expense")
                                )
                            }
                        }
                    }

                    // PC Mode Info Pill
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF1E88E5).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E88E5).copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = null,
                                tint = Color(0xFF64B5F6),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "RESPONSIVE DESKTOP GRID",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFF64B5F6)
                                )
                                Text(
                                    text = "Multi-pane grid enabled for PC display with side-by-side ledger.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // RIGHT PANE: Multi-Pane Grid / Cards List for PC
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Transaction Activity Grid",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "${transactions.size} entries",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (transactions.isEmpty()) {
                        EmptyCashFlowState(searchQuery = searchQuery)
                    } else {
                        // Responsive 2-column grid on desktop screens
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(if (isDesktopExpanded) 2 else 1),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("cashflow_overview_grid"),
                            contentPadding = PaddingValues(bottom = 40.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(transactions, key = { it.id }) { transaction ->
                                TransactionCard(
                                    transaction = transaction,
                                    onEdit = { onEditTransaction(transaction) },
                                    onDelete = { onDeleteTransaction(transaction) }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // VERTICAL MOBILE LIST VIEW (Default Compact Screen)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("cashflow_overview_column"),
                contentPadding = PaddingValues(bottom = 100.dp, top = 16.dp, start = 20.dp, end = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Main Hero Banner: Net Cash Flow
                item {
                    CashFlowHeroBanner(
                        netCashFlow = netCashFlow,
                        totalIncome = totalIncome,
                        totalExpense = totalExpense,
                        currencyFormatter = currencyFormatter
                    )
                }

                // Search Bar
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = { Text("Search transactions or memos...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchChange("") }) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cashflow_search_bar"),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                }

                // Filter Chips Row (Type + Category)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedTypeFilter == null,
                            onClick = { onTypeFilterSelect(null) },
                            label = { Text("All Flow") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("filter_chip_all_flow")
                        )

                        FilterChip(
                            selected = selectedTypeFilter == TransactionType.INCOME,
                            onClick = { onTypeFilterSelect(TransactionType.INCOME) },
                            label = { Text("Income Only") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("filter_chip_income")
                        )

                        FilterChip(
                            selected = selectedTypeFilter == TransactionType.EXPENSE,
                            onClick = { onTypeFilterSelect(TransactionType.EXPENSE) },
                            label = { Text("Expense Only") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("filter_chip_expense")
                        )
                    }
                }

                // Transactions Section Title & Count
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Transaction Activity",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ) {
                            Text(
                                text = "${transactions.size}",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Empty state or list items
                if (transactions.isEmpty()) {
                    item {
                        EmptyCashFlowState(searchQuery = searchQuery)
                    }
                } else {
                    items(transactions, key = { it.id }) { transaction ->
                        TransactionCard(
                            transaction = transaction,
                            onEdit = { onEditTransaction(transaction) },
                            onDelete = { onDeleteTransaction(transaction) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CashFlowHeroBanner(
    netCashFlow: Double,
    totalIncome: Double,
    totalExpense: Double,
    currencyFormatter: NumberFormat,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("net_cashflow_banner"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF1E1B4B),
                            Color(0xFF312E81),
                            Color(0xFF4338CA)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "NET CASH FLOW",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Surface(
                        color = if (netCashFlow >= 0) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (netCashFlow >= 0) "Surplus" else "Deficit",
                            color = if (netCashFlow >= 0) Color(0xFF34D399) else Color(0xFFFCA5A5),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = currencyFormatter.format(netCashFlow),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Inflow vs Outflow Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total Inflow Card
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF10B981),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDownward,
                                        contentDescription = "Income",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Inflow",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = currencyFormatter.format(totalIncome),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Total Outflow Card
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEF4444),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowUpward,
                                        contentDescription = "Expense",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Outflow",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = currencyFormatter.format(totalExpense),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCashFlowState(
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
            .testTag("cashflow_empty_state"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Receipt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (searchQuery.isNotBlank()) "No matching transactions" else "No Cash Flow recorded yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (searchQuery.isNotBlank()) "Try resetting your search query." else "Tap '+' below to log income or expense.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

