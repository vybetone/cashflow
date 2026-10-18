package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trading_signals")
data class TradingSignalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val symbol: String,
    val timeframe: String,
    val action: String, // "BUY", "SELL", "WAIT"
    val confidenceScore: Int, // 0 to 100
    val reasoning: String,
    val entryZone: String,
    val stopLoss: String,
    val takeProfit: String,
    val riskRewardRatio: String,
    val trendDirection: String, // "BULLISH", "BEARISH", "SIDEWAYS"
    val detectedPatterns: String, // Comma separated list of patterns
    val keyLevels: String, // Support / Resistance
    val source: String, // "SCREEN_ANALYSIS", "CAMERA_ANALYSIS", "MANUAL"
    val imageUri: String? = null,
    val tradeOutcome: String = "PENDING", // "PENDING", "WIN", "LOSS", "CANCELLED"
    val actualProfitLoss: Double? = null
)
