package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.TransactionCategory
import com.example.data.TransactionEntity
import com.example.data.TransactionType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionSheet(
    sheetState: SheetState,
    transactionToEdit: TransactionEntity?,
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, type: TransactionType, category: TransactionCategory, note: String, isRecurring: Boolean) -> Unit
) {
    var type by remember(transactionToEdit) { mutableStateOf(transactionToEdit?.type ?: TransactionType.EXPENSE) }
    var title by remember(transactionToEdit) { mutableStateOf(transactionToEdit?.title ?: "") }
    var amountText by remember(transactionToEdit) { mutableStateOf(transactionToEdit?.amount?.toString() ?: "") }
    var selectedCategory by remember(transactionToEdit) {
        mutableStateOf(
            transactionToEdit?.category ?: if (type == TransactionType.INCOME) TransactionCategory.SALARY else TransactionCategory.FOOD_GROCERIES
        )
    }
    var note by remember(transactionToEdit) { mutableStateOf(transactionToEdit?.note ?: "") }
    var isRecurring by remember(transactionToEdit) { mutableStateOf(transactionToEdit?.isRecurring ?: false) }

    val incomeCategories = listOf(
        TransactionCategory.SALARY,
        TransactionCategory.BUSINESS,
        TransactionCategory.INVESTMENT,
        TransactionCategory.FREELANCE,
        TransactionCategory.GIFTS,
        TransactionCategory.OTHER_INCOME
    )

    val expenseCategories = listOf(
        TransactionCategory.HOUSING,
        TransactionCategory.FOOD_GROCERIES,
        TransactionCategory.UTILITIES,
        TransactionCategory.TRANSPORT,
        TransactionCategory.ENTERTAINMENT,
        TransactionCategory.SHOPPING,
        TransactionCategory.HEALTH,
        TransactionCategory.EDUCATION,
        TransactionCategory.OTHER_EXPENSE
    )

    val currentCategories = if (type == TransactionType.INCOME) incomeCategories else expenseCategories

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = if (transactionToEdit == null) "Log Transaction" else "Edit Transaction",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Income / Expense Type Toggle Segment
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        type = TransactionType.INCOME
                        if (!incomeCategories.contains(selectedCategory)) {
                            selectedCategory = TransactionCategory.SALARY
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("type_income_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == TransactionType.INCOME) Color(0xFF059669) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (type == TransactionType.INCOME) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Income (+)", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        type = TransactionType.EXPENSE
                        if (!expenseCategories.contains(selectedCategory)) {
                            selectedCategory = TransactionCategory.FOOD_GROCERIES
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("type_expense_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == TransactionType.EXPENSE) Color(0xFFDC2626) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (type == TransactionType.EXPENSE) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Expense (-)", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount ($)") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_amount_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title / Source") },
                placeholder = { Text("e.g. Salary Paycheck, Client Payment, Grocery Shopping") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_title_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category Chips
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                currentCategories.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category.displayName) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("tx_category_chip_${category.name}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (type == TransactionType.INCOME) Color(0xFFD1FAE5) else Color(0xFFFEE2E2),
                            selectedLabelColor = if (type == TransactionType.INCOME) Color(0xFF047857) else Color(0xFFB91C1C)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Note Input
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note / Memo (Optional)") },
                placeholder = { Text("Add transaction details...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_note_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Recurring Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Recurring Monthly",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Auto-repeat this cash flow every month",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isRecurring,
                    onCheckedChange = { isRecurring = it },
                    modifier = Modifier.testTag("tx_recurring_switch")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("tx_cancel_btn")
                ) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.width(12.dp))

                val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
                Button(
                    onClick = {
                        onSave(title, parsedAmount, type, selectedCategory, note, isRecurring)
                    },
                    enabled = title.isNotBlank() && parsedAmount > 0.0,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("tx_save_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == TransactionType.INCOME) Color(0xFF059669) else Color(0xFFDC2626)
                    )
                ) {
                    Text(if (transactionToEdit == null) "Save Transaction" else "Update Transaction")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
