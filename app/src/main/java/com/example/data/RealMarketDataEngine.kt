package com.example.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

data class GenuineMarketQuote(
    val asset: MarketAsset,
    val lastPrice: Double,
    val bid: Double,
    val ask: Double,
    val spreadPipsOrPoints: Double,
    val change24hPercent: Double,
    val high24h: Double,
    val low24h: Double,
    val volume24h: String,
    val rsi14: Double,
    val macdLine: Double,
    val macdSignal: Double,
    val macdHistogram: Double,
    val ema20: Double,
    val ema50: Double,
    val ema200: Double? = null,
    val atr: Double,
    val candlePattern: String,
    val supportLevel: Double,
    val resistanceLevel: Double,
    val orderFlowBias: String, // "BULLISH_ACCUMULATION", "BEARISH_DISTRIBUTION", "BALANCED_RANGE"
    val recentCloses: List<Double>,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis(),
    val isBidAskEstimated: Boolean = false,
    val isStale: Boolean = false
)

class RealMarketDataEngine(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val apiClient: RealMarketDataApiClient = RealMarketDataApiClient()
) {
    private val _marketQuotes = MutableStateFlow<Map<String, GenuineMarketQuote>>(emptyMap())
    val marketQuotes: StateFlow<Map<String, GenuineMarketQuote>> = _marketQuotes.asStateFlow()

    private val _connectionState = MutableStateFlow<MarketConnectionState>(MarketConnectionState.Connecting)
    val connectionState: StateFlow<MarketConnectionState> = _connectionState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _lastErrorMessage = MutableStateFlow<String?>(null)
    val lastErrorMessage: StateFlow<String?> = _lastErrorMessage.asStateFlow()

    private var pollJob: Job? = null

    init {
        startRealMarketDataStream()
    }

    fun refreshNow() {
        scope.launch {
            fetchRealMarketDataOnce()
        }
    }

    private fun startRealMarketDataStream() {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                fetchRealMarketDataOnce()
                // Genuine market data cadence: Poll every 12 seconds to prevent rate limits
                delay(12_000)
            }
        }
    }

    suspend fun fetchRealMarketDataOnce() {
        _isLoading.value = true
        _connectionState.value = MarketConnectionState.Connecting

        val newQuotes = mutableMapOf<String, GenuineMarketQuote>()
        var successCount = 0
        var lastErr: String? = null

        for (asset in MarketUniverse.allAssets) {
            when (val res = apiClient.fetchRealQuote(asset)) {
                is MarketFetchResult.Success -> {
                    newQuotes[asset.symbol] = res.quote.copy(isStale = false)
                    successCount++
                }
                is MarketFetchResult.Failure -> {
                    lastErr = res.errorReason
                    // CRITICAL REQUIREMENT: Never generate fake random-walk prices when the API fails!
                    // Mark cached quote as stale after fetch failure to prevent actionable signals or orders.
                    val existing = _marketQuotes.value[asset.symbol]
                    if (existing != null) {
                        newQuotes[asset.symbol] = existing.copy(isStale = true)
                    }
                }
            }
        }

        _isLoading.value = false

        if (successCount > 0) {
            _marketQuotes.value = newQuotes
            _lastErrorMessage.value = null
            _connectionState.value = MarketConnectionState.Connected(
                provider = "Genuine Live Market Data",
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
        } else {
            // Entire batch failed: update _marketQuotes so any previously cached quotes are marked stale
            if (newQuotes.isNotEmpty()) {
                _marketQuotes.value = newQuotes
            }
            _lastErrorMessage.value = lastErr ?: "Failed to connect to real market data API"
            _connectionState.value = MarketConnectionState.Error(
                provider = "Market Data Service",
                errorMessage = lastErr ?: "Network or API rate limit error",
                canRetry = true
            )
        }
    }

    fun getQuoteForSymbol(symbol: String): GenuineMarketQuote {
        val asset = MarketUniverse.findAssetBySymbol(symbol)
        val quote = _marketQuotes.value[asset.symbol]
        if (quote != null) {
            // Check staleness based on timestamp freshness: quotes older than 5 minutes marked stale
            val isExpired = (System.currentTimeMillis() - quote.lastUpdatedTimestamp) > 300_000L
            return if (isExpired && !quote.isStale) quote.copy(isStale = true) else quote
        }
        // If no real quote is loaded yet, return a safe blank quote explicitly marked stale
        return GenuineMarketQuote(
            asset = asset,
            lastPrice = 0.0,
            bid = 0.0,
            ask = 0.0,
            spreadPipsOrPoints = 0.0,
            change24hPercent = 0.0,
            high24h = 0.0,
            low24h = 0.0,
            volume24h = "Awaiting Live API",
            rsi14 = 50.0,
            macdLine = 0.0,
            macdSignal = 0.0,
            macdHistogram = 0.0,
            ema20 = 0.0,
            ema50 = 0.0,
            ema200 = null,
            atr = 0.0,
            candlePattern = "Awaiting Real Market Data",
            supportLevel = 0.0,
            resistanceLevel = 0.0,
            orderFlowBias = "BALANCED_RANGE",
            recentCloses = emptyList(),
            lastUpdatedTimestamp = 0L,
            isBidAskEstimated = true,
            isStale = true
        )
    }

    fun getQuotesForCategory(category: MarketCategory): List<GenuineMarketQuote> {
        val assets = MarketUniverse.getAssetsForCategory(category)
        return assets.map { getQuoteForSymbol(it.symbol) }
    }

    fun getTelemetrySummaryForAi(quote: GenuineMarketQuote): String {
        val a = quote.asset
        if (quote.lastPrice <= 0.0 || quote.lastUpdatedTimestamp <= 0L || quote.isStale) {
            return """
                --- LIVE MARKET DATA TELEMETRY ---
                Instrument: ${a.symbol} (${a.name}) | Category: ${a.category.title}
                STATUS: Live genuine API data unavailable or stale.
                NEVER execute trades or create fake signals on absent or stale telemetry.
            """.trimIndent()
        }

        val bidAskText = if (quote.isBidAskEstimated) {
            "Current Price: ${quote.lastPrice} | Estimated Indicative Bid: ${quote.bid} | Estimated Indicative Ask: ${quote.ask} (INDICATIVE ONLY - NOT EXECUTABLE BROKER QUOTES)"
        } else {
            "Current Price: ${quote.lastPrice} | Bid: ${quote.bid} | Ask: ${quote.ask}"
        }

        return """
            --- LIVE MARKET DATA TELEMETRY (GENUINE API FEED) ---
            Instrument: ${a.symbol} (${a.name}) | Category: ${a.category.title}
            $bidAskText
            Spread: ${quote.spreadPipsOrPoints} ${a.pipUnit} | 24h Change: ${if (quote.change24hPercent >= 0) "+" else ""}${quote.change24hPercent}%
            24h Range: High: ${quote.high24h} / Low: ${quote.low24h} | Vol: ${quote.volume24h}
            Feed Timestamp: ${quote.lastUpdatedTimestamp}
            Technical Indicators:
            - RSI (14): ${quote.rsi14} (${when {
                quote.rsi14 <= 30.0 -> "OVERSOLD EXTREME"
                quote.rsi14 in 30.1..45.0 -> "BEARISH MOMENTUM / PULLBACK"
                quote.rsi14 in 45.1..55.0 -> "NEUTRAL EQUILIBRIUM"
                quote.rsi14 in 55.1..70.0 -> "BULLISH MOMENTUM"
                else -> "OVERBOUGHT EXTREME"
            }})
            - EMAs: 20 EMA: ${quote.ema20} | 50 EMA: ${quote.ema50} | 200 EMA: ${quote.ema200?.let { "$it" } ?: "UNAVAILABLE (< 200 candles)"}
            - EMA Trend Alignment: ${when {
                quote.ema200 != null && quote.ema20 > quote.ema50 && quote.ema50 > quote.ema200 -> "STRONG BULLISH LADDER (20 > 50 > 200)"
                quote.ema20 > quote.ema50 -> "SHORT-TERM BULLISH CROSS"
                quote.ema200 != null && quote.ema20 < quote.ema50 && quote.ema50 < quote.ema200 -> "STRONG BEARISH CASCADE (20 < 50 < 200)"
                else -> "CHOPPY / CONSOLIDATING"
            }}
            - MACD (12,26,9): Line: ${quote.macdLine} | Signal: ${quote.macdSignal} | Histogram: ${quote.macdHistogram}
            - Volatility (ATR): ${quote.atr}
            - Candlestick Geometry: ${quote.candlePattern}
            - Institutional Order Flow Bias: ${quote.orderFlowBias}
            - Key Support Zone: ${quote.supportLevel} | Key Resistance Zone: ${quote.resistanceLevel}
        """.trimIndent()
    }
}
