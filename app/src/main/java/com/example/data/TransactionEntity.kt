package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    INCOME, EXPENSE
}

enum class TransactionCategory(val displayName: String) {
    // Income
    SALARY("Salary & Paycheck"),
    BUSINESS("Business Revenue"),
    INVESTMENT("Investment Returns"),
    FREELANCE("Freelance & Side Gig"),
    GIFTS("Gifts & Grants"),
    OTHER_INCOME("Other Income"),

    // Expense
    HOUSING("Rent & Mortgage"),
    FOOD_GROCERIES("Food & Groceries"),
    UTILITIES("Bills & Utilities"),
    TRANSPORT("Transport & Fuel"),
    ENTERTAINMENT("Entertainment & Dining"),
    SHOPPING("Shopping & Personal"),
    HEALTH("Health & Medical"),
    EDUCATION("Education & Self Improvement"),
    OTHER_EXPENSE("Other Expense")
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: TransactionCategory,
    val dateMillis: Long = System.currentTimeMillis(),
    val note: String = "",
    val isRecurring: Boolean = false
)
