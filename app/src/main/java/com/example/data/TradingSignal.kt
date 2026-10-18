package com.example.data

data class TradingSignal(
    val symbol: String = "BTC/USD",
    val signalType: String = "BUY", // "BUY", "SELL", "WAIT"
    val confidencePercentage: Int = 92, // 0 to 100
    val entryPrice: Double = 64250.0,
    val stopLoss: Double = 63100.0,
    val takeProfit: Double = 66800.0,
    val analysisReason: String = "Multi-timeframe confluence: RSI oversold recovery at 32.4 combined with MACD Golden Cross above 200 EMA and Bullish Engulfing candlestick formation.",
    val timestamp: Long = System.currentTimeMillis(),
    val rsiValue: Double = 32.4,
    val macdSignal: String = "BULLISH CROSSOVER",
    val emaTrend: String = "STRONG UPTREND (20 > 50 > 200 EMA)",
    val candlePattern: String = "BULLISH ENGULFING",
    val momentumConfirmation: String = "HIGH BUYING VOLUME SURGE",
    val isStrongSignal: Boolean = confidencePercentage >= 90
)
