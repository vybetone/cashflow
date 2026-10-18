package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey
    val category: TransactionCategory,
    val monthlyLimit: Double,
    val currentSpent: Double = 0.0
)
