package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransactionCategory
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import java.text.NumberFormat
import java.util.Locale

@Composable
fun FinancialAnalyticsScreen(
    transactions: List<TransactionEntity>,
    totalIncome: Double,
    totalExpense: Double,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
    val netCash = totalIncome - totalExpense
    val savingsRate = if (totalIncome > 0) ((netCash / totalIncome) * 100).coerceIn(-100.0, 100.0) else 0.0

    // Expense breakdown by category
    val expenseTransactions = transactions.filter { it.type == TransactionType.EXPENSE }
    val totalExpenseSum = if (totalExpense > 0) totalExpense else 1.0

    val dayLabels = listOf("Wk 1", "Wk 2", "Wk 3", "Wk 4")
    val weeklyInflow = listOf(1200f, 1500f, 850f, 1920f)
    val weeklyOutflow = listOf(650f, 800f, 450f, 920f)
    val maxVal = 2000f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_screen_column"),
        contentPadding = PaddingValues(bottom = 100.dp, top = 16.dp, start = 20.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Savings Rate & Health Score Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("analytics_health_banner"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF047857),
                                    Color(0xFF059669),
                                    Color(0xFF10B981)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Speed,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "SAVINGS RATE",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${savingsRate.toInt()}%",
                                color = Color.White,
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (savingsRate >= 20.0) "Excellent financial savings buffer!" else "Aim to save at least 20% of income.",
                                color = Color.White.copy(alpha = 0.95f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.AutoGraph,
                                    contentDescription = "Analytics",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Cash Flow Trend Dual-Bar Chart
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cashflow_trend_chart"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Monthly Inflow vs Outflow",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Filled.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Legend
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(0xFF059669), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Inflow", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(0xFFDC2626), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Outflow", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Compose Canvas Dual-Bar
                    val greenColor = Color(0xFF059669)
                    val redColor = Color(0xFFDC2626)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val spacing = size.width / dayLabels.size
                            val barWidth = spacing / 3.5f

                            dayLabels.forEachIndexed { index, _ ->
                                val xInflow = index * spacing + spacing / 6
                                val xOutflow = xInflow + barWidth + 6f

                                val hInflow = (weeklyInflow[index] / maxVal) * (size.height - 20.dp.toPx())
                                val hOutflow = (weeklyOutflow[index] / maxVal) * (size.height - 20.dp.toPx())

                                // Draw Inflow Bar
                                drawRoundRect(
                                    color = greenColor,
                                    topLeft = Offset(xInflow, size.height - 20.dp.toPx() - hInflow),
                                    size = Size(barWidth, hInflow),
                                    cornerRadius = CornerRadius(8f, 8f)
                                )

                                // Draw Outflow Bar
                                drawRoundRect(
                                    color = redColor,
                                    topLeft = Offset(xOutflow, size.height - 20.dp.toPx() - hOutflow),
                                    size = Size(barWidth, hOutflow),
                                    cornerRadius = CornerRadius(8f, 8f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        dayLabels.forEach { label ->
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Expense Category Breakdown Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_category_breakdown"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Expense Distribution",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val categories = TransactionCategory.entries.filter { cat ->
                        expenseTransactions.any { it.category == cat }
                    }

                    if (categories.isEmpty()) {
                        Text(
                            text = "No expense data recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        categories.forEach { category ->
                            val spent = expenseTransactions.filter { it.category == category }.sumOf { it.amount }
                            val ratio = (spent / totalExpenseSum).toFloat()

                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = category.displayName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${currencyFormatter.format(spent)} (${(ratio * 100).toInt()}%)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = Color(0xFFDC2626),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
