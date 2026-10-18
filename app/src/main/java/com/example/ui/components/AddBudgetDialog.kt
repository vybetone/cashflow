package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.TransactionCategory

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddBudgetDialog(
    onDismiss: () -> Unit,
    onSave: (category: TransactionCategory, monthlyLimit: Double) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(TransactionCategory.FOOD_GROCERIES) }
    var limitText by remember { mutableStateOf("") }

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Set Category Budget",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column {
                Text(
                    text = "Select Expense Category",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    expenseCategories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category.displayName) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("budget_chip_${category.name}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Monthly Target Limit ($)") },
                    placeholder = { Text("e.g. 500.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_limit_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            val limit = limitText.toDoubleOrNull() ?: 0.0
            Button(
                onClick = { onSave(selectedCategory, limit) },
                enabled = limit > 0.0,
                modifier = Modifier.testTag("budget_save_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Budget")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("budget_cancel_btn")
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
