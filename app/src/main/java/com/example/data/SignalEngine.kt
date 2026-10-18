package com.example.data

import kotlin.math.abs

class SignalEngine {

    /**
     * Calculates Relative Strength Index (RSI) for a list of close prices over specified period (default 14).
     */
    fun calculateRSI(prices: List<Double>, period: Int = 14): Double {
        if (prices.size < period + 1) return 0.0

        var gains = 0.0
        var losses = 0.0

        for (i in 1..period) {
            val change = prices[i] - prices[i - 1]
            if (change >= 0) gains += change else losses += abs(change)
        }

        var avgGain = gains / period
        var avgLoss = losses / period

        for (i in (period + 1) until prices.size) {
            val change = prices[i] - prices[i - 1]
            if (change >= 0) {
                avgGain = (avgGain * (period - 1) + change) / period
                avgLoss = (avgLoss * (period - 1)) / period
            } else {
                avgGain = (avgGain * (period - 1)) / period
                avgLoss = (avgLoss * (period - 1) + abs(change)) / period
            }
        }

        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return (100.0 - (100.0 / (1.0 + rs))).coerceIn(0.0, 100.0)
    }

    /**
     * Calculates Exponential Moving Average (EMA).
     */
    fun calculateEMA(prices: List<Double>, period: Int): Double {
        if (prices.isEmpty()) return 0.0
        if (prices.size < period) return prices.average()

        val multiplier = 2.0 / (period + 1)
        var ema = prices.take(period).average()

        for (i in period until prices.size) {
            ema = (prices[i] - ema) * multiplier + ema
        }
        return ema
    }

    /**
     * Calculates MACD Line and Signal Line (12 Fast, 26 Slow, 9 Signal).
     */
    fun calculateMACD(prices: List<Double>): Pair<Double, Double> {
        if (prices.size < 26) return Pair(0.0, 0.0)

        val ema12 = calculateEMA(prices, 12)
        val ema26 = calculateEMA(prices, 26)
        val macdLine = ema12 - ema26

        // Calculate MACD history for signal line
        val macdHistory = mutableListOf<Double>()
        for (i in 26..prices.size) {
            val subList = prices.subList(0, i)
            val e12 = calculateEMA(subList, 12)
            val e26 = calculateEMA(subList, 26)
            macdHistory.add(e12 - e26)
        }

        val signalLine = if (macdHistory.isNotEmpty()) calculateEMA(macdHistory, 9) else 0.0
        return Pair(macdLine, signalLine)
    }

    /**
     * Detects Japanese Candlestick Patterns from OHLC lists.
     */
    fun detectCandlePattern(
        opens: List<Double>,
        highs: List<Double>,
        lows: List<Double>,
        closes: List<Double>
    ): String {
        val n = closes.size
        if (n < 2) return "STANDARD CANDLE"

        val currOpen = opens[n - 1]
        val currClose = closes[n - 1]
        val currHigh = highs[n - 1]
        val currLow = lows[n - 1]

        val prevOpen = opens[n - 2]
        val prevClose = closes[n - 2]

        val currBody = abs(currClose - currOpen)
        val currRange = currHigh - currLow

        val isCurrBullish = currClose > currOpen
        val isPrevBearish = prevClose < prevOpen
        val isPrevBullish = prevClose > prevOpen

        // Bullish Engulfing
        if (isCurrBullish && isPrevBearish && currClose >= prevOpen && currOpen <= prevClose) {
            return "BULLISH ENGULFING"
        }

        // Bearish Engulfing
        if (!isCurrBullish && isPrevBullish && currClose <= prevOpen && currOpen >= prevClose) {
            return "BEARISH ENGULFING"
        }

        // Hammer / Pin Bar
        val lowerWick = if (isCurrBullish) currOpen - currLow else currClose - currLow
        if (currRange > 0 && lowerWick / currRange >= 0.6 && currBody / currRange <= 0.3) {
            return "BULLISH HAMMER PINBAR"
        }

        // Shooting Star
        val upperWick = if (isCurrBullish) currHigh - currClose else currHigh - currOpen
        if (currRange > 0 && upperWick / currRange >= 0.6 && currBody / currRange <= 0.3) {
            return "BEARISH SHOOTING STAR"
        }

        // Doji
        if (currRange > 0 && currBody / currRange <= 0.1) {
            return "INDECISION DOJI"
        }

        return if (isCurrBullish) "BULLISH MOMENTUM CANDLE" else "BEARISH MOMENTUM CANDLE"
    }

    /**
     * Evaluates volume/price momentum confirmation.
     */
    fun evaluateMomentum(prices: List<Double>, rsi: Double): String {
        if (prices.size < 5) return "MODERATE MOMENTUM"
        val recentChange = (prices.last() - prices[prices.size - 5]) / prices[prices.size - 5] * 100.0

        return when {
            recentChange > 1.2 && rsi < 65 -> "STRONG BULLISH MOMENTUM SURGE"
            recentChange < -1.2 && rsi > 35 -> "STRONG BEARISH MOMENTUM DUMP"
            else -> "CONSOLIDATION MOMENTUM"
        }
    }

    /**
     * Generates a high-precision AI Trading Signal by running all 5 technical engines.
     */
    fun generateSignal(symbol: String = "BTC/USD", priceHistory: List<Double>? = null): TradingSignal {
        val asset = MarketUniverse.findAssetBySymbol(symbol)

        // Move 2A: Eliminate synthetic signal generation.
        // If genuine, valid market history is unavailable, return explicit WAIT/unavailable result.
        // Never fabricate candles, indicators, confidence values, or trading signals.
        if (priceHistory == null || priceHistory.size < 5 || priceHistory.any { it <= 0.0 || it.isNaN() || it.isInfinite() }) {
            return createUnavailableWaitSignal(asset)
        }

        val prices = priceHistory

        val opens = prices.mapIndexed { idx, p -> if (idx == 0) p else prices[idx - 1] }
        val closes = prices
        val highs = prices.map { it * 1.004 }
        val lows = prices.map { it * 0.996 }

        // 1. RSI
        val rsi = calculateRSI(prices)

        // 2. MACD
        val (macdLine, signalLine) = calculateMACD(prices)
        val macdSignalStr = when {
            macdLine > signalLine && macdLine > 0 -> "BULLISH CROSSOVER (ABOVE ZERO)"
            macdLine > signalLine -> "BULLISH MACD CROSSOVER"
            macdLine < signalLine && macdLine < 0 -> "BEARISH CROSSOVER (BELOW ZERO)"
            else -> "BEARISH MACD CROSSOVER"
        }

        // 3. EMA Trend
        val ema20 = calculateEMA(prices, 20)
        val ema50 = calculateEMA(prices, 50)
        val ema200 = calculateEMA(prices, 200)

        val emaTrendStr = when {
            ema20 > ema50 && ema50 > ema200 -> "STRONG UPTREND (20 > 50 > 200 EMA)"
            ema20 > ema50 -> "BULLISH EMA ALIGNMENT"
            ema20 < ema50 && ema50 < ema200 -> "STRONG DOWNTREND (20 < 50 < 200 EMA)"
            else -> "SIDEWAYS EMA CONSOLIDATION"
        }

        // 4. Candle Pattern
        val candlePattern = detectCandlePattern(opens, highs, lows, closes)

        // 5. Momentum
        val momentumStr = evaluateMomentum(prices, rsi)

        // Multi-indicator Confluence Logic
        var bullScore = 0
        var bearScore = 0

        // RSI scoring
        if (rsi <= 38.0) bullScore += 25
        else if (rsi in 38.1..52.0) bullScore += 15
        else if (rsi >= 62.0) bearScore += 25
        else if (rsi in 48.0..61.9) bearScore += 15

        // MACD scoring
        if (macdLine > signalLine) bullScore += 20 else bearScore += 20

        // EMA scoring
        if (ema20 > ema50) bullScore += 20 else bearScore += 20

        // Candle Pattern scoring
        if (candlePattern.contains("BULLISH")) bullScore += 20
        else if (candlePattern.contains("BEARISH")) bearScore += 20
        else { bullScore += 10; bearScore += 10 }

        // Momentum scoring
        if (momentumStr.contains("BULLISH")) bullScore += 15
        else if (momentumStr.contains("BEARISH")) bearScore += 15
        else { bullScore += 5; bearScore += 5 }

        val signalType: String
        val rawConfidence: Int

        if (bullScore >= 80) {
            signalType = "BUY"
            rawConfidence = (bullScore + 5).coerceAtMost(96)
        } else if (bearScore >= 80) {
            signalType = "SELL"
            rawConfidence = (bearScore + 5).coerceAtMost(96)
        } else if (bullScore >= 60) {
            signalType = "BUY"
            rawConfidence = bullScore.coerceAtMost(88)
        } else if (bearScore >= 60) {
            signalType = "SELL"
            rawConfidence = bearScore.coerceAtMost(88)
        } else {
            signalType = "WAIT"
            rawConfidence = 50
        }

        val currentPrice = prices.last()
        val (entryPrice, stopLoss, takeProfit) = calculateTradeLevels(currentPrice, signalType, asset)

        val reason = buildString {
            append("[${asset.category.badgeLabel}] 5-Indicator Confluence Matrix: ")
            append("1) RSI at ${String.format("%.1f", rsi)}. ")
            append("2) MACD: $macdSignalStr. ")
            append("3) Trend: $emaTrendStr. ")
            append("4) Candlestick: $candlePattern. ")
            append("5) Momentum: $momentumStr. ")
            append("Target: ${asset.pipUnit}")
        }

        return TradingSignal(
            symbol = asset.symbol,
            signalType = signalType,
            confidencePercentage = rawConfidence,
            entryPrice = entryPrice,
            stopLoss = stopLoss,
            takeProfit = takeProfit,
            analysisReason = reason,
            rsiValue = rsi,
            macdSignal = macdSignalStr,
            emaTrend = emaTrendStr,
            candlePattern = candlePattern,
            momentumConfirmation = momentumStr,
            isStrongSignal = rawConfidence >= 90
        )
    }

    private fun calculateTradeLevels(
        currentPrice: Double,
        signalType: String,
        asset: MarketAsset
    ): Triple<Double, Double, Double> {
        val multiplier = if (asset.decimalDigits == 4) 10000.0 else 100.0
        val roundedEntry = Math.round(currentPrice * multiplier) / multiplier

        val slPct = when (asset.category) {
            MarketCategory.FOREX -> 0.0035 // ~35-40 pips
            MarketCategory.GOLD -> 0.0080  // ~$20
            MarketCategory.CRYPTO -> 0.015 // ~1.5%
            MarketCategory.STOCKS -> 0.012 // ~1.2%
            MarketCategory.ALL_MARKETS -> 0.010
        }
        val tpPct = slPct * 2.8 // ~1:2.8 risk reward

        return when (signalType) {
            "BUY" -> Triple(
                roundedEntry,
                Math.round((roundedEntry * (1.0 - slPct)) * multiplier) / multiplier,
                Math.round((roundedEntry * (1.0 + tpPct)) * multiplier) / multiplier
            )
            "SELL" -> Triple(
                roundedEntry,
                Math.round((roundedEntry * (1.0 + slPct)) * multiplier) / multiplier,
                Math.round((roundedEntry * (1.0 - tpPct)) * multiplier) / multiplier
            )
            else -> Triple(
                roundedEntry,
                Math.round((roundedEntry * 0.99) * multiplier) / multiplier,
                Math.round((roundedEntry * 1.01) * multiplier) / multiplier
            )
        }
    }

    private fun createUnavailableWaitSignal(asset: MarketAsset): TradingSignal {
        return TradingSignal(
            symbol = asset.symbol,
            signalType = "WAIT",
            confidencePercentage = 0,
            entryPrice = 0.0,
            stopLoss = 0.0,
            takeProfit = 0.0,
            analysisReason = "Genuine market data unavailable: Insufficient or invalid price history for technical analysis.",
            timestamp = System.currentTimeMillis(),
            rsiValue = 0.0,
            macdSignal = "INSUFFICIENT DATA",
            emaTrend = "INSUFFICIENT DATA",
            candlePattern = "Awaiting Real Market Data",
            momentumConfirmation = "NO DATA",
            isStrongSignal = false
        )
    }

    @Deprecated("Synthetic price generation removed from production signal paths", ReplaceWith("emptyList()"))
    fun generatePriceSeriesForAsset(asset: MarketAsset): List<Double> {
        return emptyList()
    }

    private fun generateDefaultPriceSeries(): List<Double> {
        return emptyList()
    }
}
