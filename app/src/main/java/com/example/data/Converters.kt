package com.example.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType?): String? {
        return type?.name
    }

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? {
        return value?.let {
            try {
                TransactionType.valueOf(it)
            } catch (e: Exception) {
                TransactionType.EXPENSE
            }
        }
    }

    @TypeConverter
    fun fromTransactionCategory(category: TransactionCategory?): String? {
        return category?.name
    }

    @TypeConverter
    fun toTransactionCategory(value: String?): TransactionCategory? {
        return value?.let {
            try {
                TransactionCategory.valueOf(it)
            } catch (e: Exception) {
                TransactionCategory.OTHER_EXPENSE
            }
        }
    }
}
