package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trade_journal")
data class TradeJournalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val signalId: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val assetSymbol: String,
    val tradeType: String, // "BUY", "SELL"
    val entryPrice: Double,
    val exitPrice: Double? = null,
    val stopLoss: Double,
    val takeProfit: Double,
    val positionSize: Double = 1.0,
    val status: String = "OPEN", // "OPEN", "CLOSED_WIN", "CLOSED_LOSS", "CANCELLED"
    val pnl: Double = 0.0,
    val notes: String = "",
    val screenshotPath: String? = null,
    val chartPattern: String = ""
)
