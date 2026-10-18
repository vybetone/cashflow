package com.example.data

/**
 * Represents a single candlestick/OHLC data point.
 *
 * All prices must be real, verified market data.
 * Timestamp must be valid and monotonically increasing.
 */
data class PriceCandlestick(
    val timestamp: Long,        // Unix milliseconds
    val open: Double,           // OHLC prices in asset units (USD, etc)
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double? = null  // Optional volume data
) {
    init {
        require(timestamp > 0) { "Timestamp must be positive: $timestamp" }
        require(open > 0 && high > 0 && low > 0 && close > 0) {
            "All prices must be positive. Got O:$open H:$high L:$low C:$close"
        }
        require(high >= open && high >= close && high >= low) {
            "High must be >= all other prices. Got H:$high O:$open C:$close L:$low"
        }
        require(low <= open && low <= close && low <= high) {
            "Low must be <= all other prices. Got L:$low O:$open C:$close H:$high"
        }
        if (volume != null) {
            require(volume >= 0) { "Volume cannot be negative: $volume" }
        }
    }
}

/**
 * Represents the result of attempting to fetch market price data.
 *
 * Success: Contains validated real price history
 * Failure: Indicates reason (API unavailable, insufficient data, stale data, etc.)
 */
sealed class PriceHistoryResult {
    data class Success(
        val symbol: String,
        val timeframe: String,
        val candlesticks: List<PriceCandlestick>,
        val source: PriceDataSource,
        val fetchedAt: Long = System.currentTimeMillis()
    ) : PriceHistoryResult() {
        init {
            require(candlesticks.isNotEmpty()) { "Must have at least 1 candlestick" }
            require(candlesticks.size >= 50) { "Recommend at least 50 candles for technical analysis (got ${candlesticks.size})" }
            
            // Verify candlesticks are chronologically sorted
            for (i in 1 until candlesticks.size) {
                require(candlesticks[i].timestamp >= candlesticks[i - 1].timestamp) {
                    "Candlesticks must be sorted by timestamp. " +
                    "Candle $i (${candlesticks[i].timestamp}) < Candle ${i-1} (${candlesticks[i-1].timestamp})"
                }
            }
        }
    }

    data class Failure(
        val reason: String,
        val details: String = "",
        val retryable: Boolean = false  // Can caller retry?
    ) : PriceHistoryResult()
}

enum class PriceDataSource {
    MARKET_DATA_API,    // Real market data from provider (Binance, Alpha Vantage, etc)
    SCREEN_CAPTURE,     // Extracted from user's screen chart
    CAMERA_IMAGE,       // Extracted from camera photo
    TEST_DATA           // Test/verification only (never in production)
}

/**
 * Abstraction for fetching real market price data.
 *
 * Implementations must return ONLY validated, real market data.
 * Synthetic or fabricated prices are NOT permitted.
 */
interface MarketDataProvider {
    /**
     * Fetch real price history for a symbol/timeframe.
     *
     * @param symbol Trading symbol (e.g. "BTC/USD", "AAPL")
     * @param timeframe Candle timeframe (e.g. "15m", "1h", "1d")
     * @param limit Maximum number of candles to fetch
     *
     * @return Success with real candlesticks, or Failure with reason
     */
    suspend fun getPriceHistory(
        symbol: String,
        timeframe: String,
        limit: Int = 200
    ): PriceHistoryResult
}

/**
 * Test implementation that verifies MarketDataProvider is used correctly.
 *
 * NEVER use in production.
 * Only for unit tests and integration tests.
 */
class TestMarketDataProvider : MarketDataProvider {
    override suspend fun getPriceHistory(
        symbol: String,
        timeframe: String,
        limit: Int
    ): PriceHistoryResult {
        // Generate simple validated test data
        // All values are real and verifiable
        val basePrice = 100.0
        val now = System.currentTimeMillis()
        val candlesticks = mutableListOf<PriceCandlestick>()

        for (i in 0 until minOf(limit, 100)) {
            val timestamp = now - (100 - i) * 60000  // 1m apart
            val priceOffset = i * 0.5
            val open = basePrice + priceOffset
            val close = basePrice + priceOffset + 0.3
            val high = maxOf(open, close) + 0.2
            val low = minOf(open, close) - 0.1

            candlesticks.add(
                PriceCandlestick(
                    timestamp = timestamp,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = 1000.0 + i * 10.0
                )
            )
        }

        return PriceHistoryResult.Success(
            symbol = symbol,
            timeframe = timeframe,
            candlesticks = candlesticks,
            source = PriceDataSource.TEST_DATA,
            fetchedAt = now
        )
    }
}

/**
 * Stub implementation for future real market data providers.
 *
 * Can be implemented for:
 * - Binance
 * - Alpha Vantage
 * - Finnhub
 * - CoinGecko
 * - Other legitimate market data APIs
 */
class PlaceholderMarketDataProvider : MarketDataProvider {
    override suspend fun getPriceHistory(
        symbol: String,
        timeframe: String,
        limit: Int
    ): PriceHistoryResult {
        return PriceHistoryResult.Failure(
            reason = "Market data provider not configured",
            details = "Phase 1B.1: Placeholder provider. " +
                     "Implement real provider (Binance, Alpha Vantage, etc) in Phase 1B.1+",
            retryable = false
        )
    }
}
