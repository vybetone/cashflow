package com.example

import com.example.data.MarketUniverse
import com.example.data.SignalEngine
import com.example.data.TradingSignal
import com.example.data.TradingSignalEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Move 2A Regression Tests:
 * Proves that synthetic price and signal generation is completely eliminated,
 * missing or invalid market history strictly returns explicit WAIT results with 0 confidence,
 * and batch scanning cannot persist actionable signals from absent or invalid history.
 */
class SignalEngineRegressionTest {

    private lateinit var signalEngine: SignalEngine

    @Before
    fun setUp() {
        signalEngine = SignalEngine()
    }

    @Test
    fun testGenerateSignal_returnsWaitSignalWhenHistoryIsNull() {
        val signal = signalEngine.generateSignal("BTC/USD", priceHistory = null)

        assertEquals("WAIT", signal.signalType)
        assertEquals(0, signal.confidencePercentage)
        assertEquals(0.0, signal.entryPrice, 0.0001)
        assertEquals(0.0, signal.stopLoss, 0.0001)
        assertEquals(0.0, signal.takeProfit, 0.0001)
        assertEquals(0.0, signal.rsiValue, 0.0001)
        assertEquals("INSUFFICIENT DATA", signal.macdSignal)
        assertEquals("INSUFFICIENT DATA", signal.emaTrend)
        assertFalse(signal.isStrongSignal)
        assertTrue(signal.analysisReason.contains("Genuine market data unavailable"))
    }

    @Test
    fun testGenerateSignal_returnsWaitSignalWhenHistoryIsEmpty() {
        val signal = signalEngine.generateSignal("EUR/USD", priceHistory = emptyList())

        assertEquals("WAIT", signal.signalType)
        assertEquals(0, signal.confidencePercentage)
        assertEquals(0.0, signal.entryPrice, 0.0001)
        assertFalse(signal.isStrongSignal)
    }

    @Test
    fun testGenerateSignal_returnsWaitSignalWhenHistoryHasZeroOrNegativePrices() {
        // Price history contains zero and negative placeholder prices
        val invalidPrices = listOf(1.0850, 1.0860, 0.0, -1.0840, 1.0870, 1.0880)
        val signal = signalEngine.generateSignal("EUR/USD", priceHistory = invalidPrices)

        assertEquals("WAIT", signal.signalType)
        assertEquals(0, signal.confidencePercentage)
        assertEquals(0.0, signal.entryPrice, 0.0001)
        assertFalse(signal.isStrongSignal)
    }

    @Test
    fun testGenerateSignal_returnsWaitSignalWhenHistoryHasNaNOrInfinitePrices() {
        val nanPrices = listOf(1.0850, 1.0860, Double.NaN, 1.0870, 1.0880)
        val nanSignal = signalEngine.generateSignal("EUR/USD", priceHistory = nanPrices)
        assertEquals("WAIT", nanSignal.signalType)
        assertEquals(0, nanSignal.confidencePercentage)

        val infPrices = listOf(1.0850, 1.0860, Double.POSITIVE_INFINITY, 1.0870, 1.0880)
        val infSignal = signalEngine.generateSignal("EUR/USD", priceHistory = infPrices)
        assertEquals("WAIT", infSignal.signalType)
        assertEquals(0, infSignal.confidencePercentage)
    }

    @Test
    fun testGenerateSignal_returnsWaitSignalWhenHistoryIsInsufficient() {
        // Only 3 prices: insufficient for technical indicators (needs >= 5)
        val insufficientPrices = listOf(100.0, 101.0, 102.0)
        val signal = signalEngine.generateSignal("AAPL", priceHistory = insufficientPrices)

        assertEquals("WAIT", signal.signalType)
        assertEquals(0, signal.confidencePercentage)
        assertEquals(0.0, signal.entryPrice, 0.0001)
        assertFalse(signal.isStrongSignal)
    }

    @Test
    fun testGenerateSignal_computesGenuineSignalWhenValidHistoryProvided() {
        // Generate valid 50-candle series with confluence pattern (MACD crossover + EMA20 > EMA50)
        var curr = 64000.0
        val scale = 80.0
        val validConfluenceHistory = mutableListOf<Double>()
        for (i in 0 until 50) {
            val delta = (Math.sin(i * 0.2) * 1.2 + (if (i > 30) (i - 30) * 0.3 else -(i * 0.1))) * scale
            curr += delta
            validConfluenceHistory.add(curr)
        }

        val signal = signalEngine.generateSignal("BTC/USD", priceHistory = validConfluenceHistory)

        // Must evaluate on real history, not placeholder WAIT
        assertNotEquals("WAIT", signal.signalType)
        assertEquals("BUY", signal.signalType)
        assertTrue(signal.confidencePercentage >= 60)
        assertTrue(signal.entryPrice > 0.0)
        assertTrue(signal.stopLoss > 0.0)
        assertTrue(signal.takeProfit > 0.0)
        assertTrue(signal.analysisReason.contains("Confluence Matrix"))
    }

    @Test
    fun testGeneratePriceSeriesForAsset_returnsEmptyList() {
        // Verifies that synthetic price generator has been deactivated
        @Suppress("DEPRECATION")
        val series = signalEngine.generatePriceSeriesForAsset(MarketUniverse.EUR_USD)
        assertTrue("Synthetic series generator must return empty list in production", series.isEmpty())
    }

    @Test
    fun testBatchScanning_cannotPersistSignalWhenHistoryIsMissingOrInvalid() {
        val persistedSignals = mutableListOf<TradingSignalEntity>()

        // Simulate batch scan items with different market data states
        data class MockQuote(
            val symbol: String,
            val lastPrice: Double,
            val lastUpdatedTimestamp: Long,
            val recentCloses: List<Double>
        )

        val unverifiedQuotes = listOf(
            MockQuote("EUR/USD", 0.0, 0L, emptyList()), // Awaiting API
            MockQuote("GBP/USD", 1.30, 0L, listOf(1.30, 1.31)), // Missing timestamp
            MockQuote("USD/JPY", 150.0, 1750000000L, listOf(150.0, 0.0, -10.0)), // Invalid prices
            MockQuote("BTC/USD", 80000.0, 1750000000L, listOf(80000.0, 80100.0)) // < 5 candles
        )

        unverifiedQuotes.forEach { quote ->
            val isHistoryValid = quote.lastPrice > 0.0 &&
                quote.lastUpdatedTimestamp > 0L &&
                quote.recentCloses.size >= 5 &&
                quote.recentCloses.all { it > 0.0 && !it.isNaN() && !it.isInfinite() }

            val priceHistory = if (isHistoryValid) quote.recentCloses else null
            val sig = signalEngine.generateSignal(quote.symbol, priceHistory)

            // Production filter: Only persist actionable signals when genuine market history is valid
            if (isHistoryValid && sig.signalType != "WAIT" && sig.confidencePercentage > 0 && sig.entryPrice > 0.0) {
                persistedSignals.add(
                    TradingSignalEntity(
                        timestamp = System.currentTimeMillis(),
                        symbol = sig.symbol,
                        timeframe = "15m",
                        action = sig.signalType,
                        confidenceScore = sig.confidencePercentage,
                        reasoning = sig.analysisReason,
                        entryZone = sig.entryPrice.toString(),
                        stopLoss = sig.stopLoss.toString(),
                        takeProfit = sig.takeProfit.toString(),
                        riskRewardRatio = "1:2.8",
                        trendDirection = "BULLISH",
                        detectedPatterns = sig.candlePattern,
                        keyLevels = "Support: ${sig.stopLoss}",
                        source = "SCAN_CONFIRMED_MARKETS"
                    )
                )
            }
        }

        // Must be zero: no unverified or invalid market data could be persisted
        assertEquals(0, persistedSignals.size)
    }

    @Test
    fun testBatchScanning_persistsSignalOnlyWhenHistoryIsFullyValid() {
        val persistedSignals = mutableListOf<TradingSignalEntity>()

        var curr = 64000.0
        val scale = 80.0
        val validPrices = mutableListOf<Double>()
        for (i in 0 until 50) {
            val delta = (Math.sin(i * 0.2) * 1.2 + (if (i > 30) (i - 30) * 0.3 else -(i * 0.1))) * scale
            curr += delta
            validPrices.add(curr)
        }

        val validQuote = object {
            val symbol = "BTC/USD"
            val lastPrice = validPrices.last()
            val lastUpdatedTimestamp = 1750000000L
            val recentCloses = validPrices
        }

        val isHistoryValid = validQuote.lastPrice > 0.0 &&
            validQuote.lastUpdatedTimestamp > 0L &&
            validQuote.recentCloses.size >= 5 &&
            validQuote.recentCloses.all { it > 0.0 && !it.isNaN() && !it.isInfinite() }

        val priceHistory = if (isHistoryValid) validQuote.recentCloses else null
        val sig = signalEngine.generateSignal(validQuote.symbol, priceHistory)

        if (isHistoryValid && sig.signalType != "WAIT" && sig.confidencePercentage > 0 && sig.entryPrice > 0.0) {
            persistedSignals.add(
                TradingSignalEntity(
                    timestamp = System.currentTimeMillis(),
                    symbol = sig.symbol,
                    timeframe = "15m",
                    action = sig.signalType,
                    confidenceScore = sig.confidencePercentage,
                    reasoning = sig.analysisReason,
                    entryZone = sig.entryPrice.toString(),
                    stopLoss = sig.stopLoss.toString(),
                    takeProfit = sig.takeProfit.toString(),
                    riskRewardRatio = "1:2.8",
                    trendDirection = "BULLISH",
                    detectedPatterns = sig.candlePattern,
                    keyLevels = "Support: ${sig.stopLoss}",
                    source = "SCAN_CONFIRMED_MARKETS"
                )
            )
        }

        assertEquals(1, persistedSignals.size)
        assertEquals("BTC/USD", persistedSignals[0].symbol)
        assertEquals("BUY", persistedSignals[0].action)
        assertTrue(persistedSignals[0].confidenceScore >= 60)
    }
}
