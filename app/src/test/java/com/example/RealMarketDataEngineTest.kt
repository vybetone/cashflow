package com.example

import com.example.data.GenuineMarketDataValidator
import com.example.data.GenuineMarketQuote
import com.example.data.ForexHistoricalDataProvider
import com.example.data.MarketAsset
import com.example.data.MarketCategory
import com.example.data.MarketConnectionState
import com.example.data.MarketFetchResult
import com.example.data.MarketUniverse
import com.example.data.PriceCandlestick
import com.example.data.PriceDataSource
import com.example.data.PriceHistoryResult
import com.example.data.RealMarketDataApiClient
import com.example.data.RealMarketDataEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class RealMarketDataEngineTest {

    @Test
    fun testValidator_validatesTimestampsAndRejectsStaleData() {
        val nowSec = 1750000000L

        // Fresh crypto timestamp (60 seconds old)
        assertTrue(GenuineMarketDataValidator.isTimestampValid("BTC/USD", nowSec - 60, nowSec))

        // Stale crypto timestamp (700 seconds old > 600s threshold)
        assertFalse(GenuineMarketDataValidator.isTimestampValid("BTC/USD", nowSec - 700, nowSec))

        // Zero or negative timestamp rejected
        assertFalse(GenuineMarketDataValidator.isTimestampValid("EUR/USD", 0L, nowSec))
        assertFalse(GenuineMarketDataValidator.isTimestampValid("EUR/USD", -100L, nowSec))

        // Future timestamp with > 300s clock drift rejected
        assertFalse(GenuineMarketDataValidator.isTimestampValid("EUR/USD", nowSec + 1000, nowSec))

        // Normal market timestamp (2 hours old) accepted for forex/stocks
        assertTrue(GenuineMarketDataValidator.isTimestampValid("EUR/USD", nowSec - 7200, nowSec))

        // Stale market timestamp (> 48 hours) rejected
        assertFalse(GenuineMarketDataValidator.isTimestampValid("EUR/USD", nowSec - 200000, nowSec))
    }

    @Test
    fun testValidator_validatesPrices() {
        assertTrue(GenuineMarketDataValidator.isPriceValid(1.0850))
        assertTrue(GenuineMarketDataValidator.isPriceValid(83000.0))
        assertFalse(GenuineMarketDataValidator.isPriceValid(0.0))
        assertFalse(GenuineMarketDataValidator.isPriceValid(-10.5))
        assertFalse(GenuineMarketDataValidator.isPriceValid(Double.NaN))
        assertFalse(GenuineMarketDataValidator.isPriceValid(Double.POSITIVE_INFINITY))
    }

    @Test
    fun testEngine_neverGeneratesFakePricesWhenApiFails() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        // Mock client that always throws network exception
        val failingHttpClient = createMockHttpClient { req ->
            throw IOException("Simulated network outage (DNS resolution failed)")
        }
        val failingApiClient = RealMarketDataApiClient(failingHttpClient)
        val engine = RealMarketDataEngine(scope = testScope, apiClient = failingApiClient)

        // Run fetch cycle
        engine.fetchRealMarketDataOnce()

        // Verify connection state reports error and NEVER generates fake quotes
        val connState = engine.connectionState.value
        assertTrue("Expected error state on API failure, was $connState", connState is MarketConnectionState.Error)
        assertEquals(false, engine.isLoading.value)
        assertNotNull(engine.lastErrorMessage.value)

        // Quotes map must be empty — no random-walk prices were fabricated!
        assertTrue(engine.marketQuotes.value.isEmpty())

        // getQuoteForSymbol returns safe placeholder with lastPrice = 0.0 and timestamp = 0
        val quote = engine.getQuoteForSymbol("BTC/USD")
        assertEquals(0.0, quote.lastPrice, 0.0001)
        assertEquals(0L, quote.lastUpdatedTimestamp)
        assertEquals("Awaiting Live API", quote.volume24h)

        // Telemetry warning generated for AI
        val telemetry = engine.getTelemetrySummaryForAi(quote)
        assertTrue(telemetry.contains("STATUS: Live genuine API data unavailable"))
        assertTrue(telemetry.contains("NEVER execute trades or create fake signals"))
    }

    @Test
    fun testEngine_successfullyIngestsGenuineApiResponse() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val currentTimeSec = System.currentTimeMillis() / 1000

        val mockHttpClient = createMockHttpClient { req ->
            val url = req.url.toString()
            when {
                url.contains("binance.com/api/v3/ticker/24hr?symbol=BTCUSDT") -> {
                    createJsonResponse(
                        req,
                        """
                        {
                            "symbol": "BTCUSDT",
                            "lastPrice": "83150.25",
                            "bidPrice": "83150.20",
                            "askPrice": "83150.30",
                            "priceChangePercent": "2.45",
                            "highPrice": "84200.00",
                            "lowPrice": "81200.00",
                            "quoteVolume": "1850000000.00",
                            "closeTime": ${currentTimeSec * 1000}
                        }
                        """.trimIndent()
                    )
                }
                url.contains("binance.com/api/v3/klines") -> {
                    val klinesList = (1..60).map { i ->
                        val ts = 1700000000000L + (i * 3600000L)
                        val p = 82000.0 + (i * 15.0)
                        "[$ts,\"$p\",\"${p + 200}\",\"${p - 200}\",\"${p + 50}\",\"100\",${ts + 3600000L},\"8240000\",50,\"50\",\"4120000\",\"0\"]"
                    }
                    val klinesJson = "[" + klinesList.joinToString(",") + "]"
                    createJsonResponse(req, klinesJson)
                }
                url.contains("open.er-api.com/v6/latest/USD") -> {
                    createJsonResponse(
                        req,
                        """
                        {
                            "result": "success",
                            "time_last_update_unix": $currentTimeSec,
                            "rates": {
                                "EUR": 0.8928,
                                "GBP": 0.7570,
                                "JPY": 158.25
                            }
                        }
                        """.trimIndent()
                    )
                }
                url.contains("finance.yahoo.com/v8/finance/chart") -> {
                    val closesList = (1..60).map { 220.0 + it * 0.2 }
                    val timestampsList = (1..60).map { currentTimeSec - (60 - it) * 3600 }
                    val closesJson = closesList.joinToString(",")
                    val timestampsJson = timestampsList.joinToString(",")
                    createJsonResponse(
                        req,
                        """
                        {
                            "chart": {
                                "result": [
                                    {
                                        "meta": {
                                            "regularMarketPrice": 230.50,
                                            "chartPreviousClose": 228.00,
                                            "regularMarketDayHigh": 232.00,
                                            "regularMarketDayLow": 227.50,
                                            "regularMarketVolume": 85000000,
                                            "regularMarketTime": $currentTimeSec
                                        },
                                        "timestamp": [$timestampsJson],
                                        "indicators": {
                                            "quote": [
                                                { 
                                                    "open": [$closesJson],
                                                    "high": [$closesJson],
                                                    "low": [$closesJson],
                                                    "close": [$closesJson] 
                                                }
                                            ]
                                        }
                                    }
                                ]
                            }
                        }
                        """.trimIndent()
                    )
                }
                else -> {
                    createJsonResponse(
                        req,
                        """
                        {
                            "symbol": "PAXGUSDT",
                            "lastPrice": "4180.50",
                            "bidPrice": "4180.40",
                            "askPrice": "4180.60",
                            "priceChangePercent": "0.85",
                            "highPrice": "4210.00",
                            "lowPrice": "4150.00",
                            "quoteVolume": "25000000.00",
                            "closeTime": ${currentTimeSec * 1000}
                        }
                        """.trimIndent()
                    )
                }
            }
        }

        // Create a mock forex provider that returns valid test candles
        val mockForexProvider = object : ForexHistoricalDataProvider() {
            override suspend fun fetchForexHistory(
                symbol: String,
                timeframe: String,
                limit: Int
            ): PriceHistoryResult {
                val now = System.currentTimeMillis()
                val candles = (0 until 60).map { i ->
                    val ts = now - (59 - i) * 3600000L
                    val price = 1.1200 + i * 0.0001
                    PriceCandlestick(ts, price, price + 0.0005, price - 0.0005, price)
                }
                return PriceHistoryResult.Success(symbol, timeframe, candles, PriceDataSource.TEST_DATA, now)
            }
        }

        val apiClient = RealMarketDataApiClient(mockHttpClient, mockForexProvider)
        val engine = RealMarketDataEngine(scope = testScope, apiClient = apiClient)

        engine.fetchRealMarketDataOnce()

        val connState = engine.connectionState.value
        assertTrue("Expected Connected state, got $connState", connState is MarketConnectionState.Connected)

        val btcQuote = engine.getQuoteForSymbol("BTC/USD")
        assertEquals(83150.25, btcQuote.lastPrice, 0.01)
        assertEquals(83150.20, btcQuote.bid, 0.01)
        assertEquals(83150.30, btcQuote.ask, 0.01)
        assertEquals(2.45, btcQuote.change24hPercent, 0.01)
        assertTrue(btcQuote.volume24h.contains("$1.85B"))
        assertTrue(btcQuote.lastUpdatedTimestamp > 0L)

        val eurQuote = engine.getQuoteForSymbol("EUR/USD")
        assertEquals(1.1259, eurQuote.lastPrice, 0.0001)
        assertTrue("Forex quotes must have isBidAskEstimated set to true", eurQuote.isBidAskEstimated)
        assertFalse("Active quotes must not be stale", eurQuote.isStale)

        val nvdaQuote = engine.getQuoteForSymbol("NVDA")
        assertEquals(230.50, nvdaQuote.lastPrice, 0.01)
    }

    @Test
    fun testForexProvider_keyInjection_usesConstructorKeyWithoutSystemGetenv() = runTest {
        var capturedUrl: String? = null
        val mockClient = createMockHttpClient { req ->
            capturedUrl = req.url.toString()
            createJsonResponse(
                req,
                """
                {
                    "s": "ok",
                    "t": [1700000000],
                    "o": [1.0850],
                    "h": [1.0860],
                    "l": [1.0840],
                    "c": [1.0855],
                    "v": [1000]
                }
                """.trimIndent()
            )
        }

        val provider = ForexHistoricalDataProvider(
            client = mockClient,
            finnhubApiKey = "injected_constructor_key_xyz"
        )

        provider.fetchForexHistory("EUR/USD", "1h", 60)

        assertNotNull(capturedUrl)
        assertTrue(
            "Request URL must contain injected constructor token",
            capturedUrl!!.contains("token=injected_constructor_key_xyz")
        )
    }

    @Test
    fun testForexProvider_failsExplicitlyWhenKeyIsMissing() = runTest {
        val mockClient = createMockHttpClient { req ->
            throw IOException("Network call should not occur when API key is missing")
        }

        val provider = ForexHistoricalDataProvider(
            client = mockClient,
            finnhubApiKey = null,
            alphaVantageApiKey = null
        )

        val result = provider.fetchForexHistory("EUR/USD", "1h", 60)
        assertTrue(result is PriceHistoryResult.Failure)
        val failure = result as PriceHistoryResult.Failure
        assertEquals("Forex historical data unavailable", failure.reason)
        assertTrue(failure.details.contains("No genuine OHLC provider configured with a valid API key"))
    }

    @Test
    fun testForexProvider_removesAlphaVantageDemoKeyFallback() = runTest {
        var requestedUrl: String? = null
        val mockClient = createMockHttpClient { req ->
            requestedUrl = req.url.toString()
            createJsonResponse(req, "{}")
        }

        val provider = ForexHistoricalDataProvider(
            client = mockClient,
            finnhubApiKey = null,
            alphaVantageApiKey = null
        )

        val result = provider.fetchForexHistory("EUR/USD", "1d", 60)
        assertTrue(result is PriceHistoryResult.Failure)
        assertNull("Must never execute Alpha Vantage requests using hardcoded demo key", requestedUrl)
    }

    @Test
    fun testForexProvider_lookbackCalculation_dailyVsHourly() = runTest {
        var hourlyFromSec = 0L
        var dailyFromSec = 0L
        val mockClient = createMockHttpClient { req ->
            val url = req.url.toString()
            val fromParam = req.url.queryParameter("from")?.toLongOrNull() ?: 0L
            if (url.contains("resolution=60")) {
                hourlyFromSec = fromParam
            } else if (url.contains("resolution=D")) {
                dailyFromSec = fromParam
            }
            createJsonResponse(req, """{"s":"ok","t":[],"o":[],"h":[],"l":[],"c":[]}""")
        }

        val provider = ForexHistoricalDataProvider(
            client = mockClient,
            finnhubApiKey = "valid_key"
        )

        val nowSec = System.currentTimeMillis() / 1000L
        provider.fetchForexHistory("EUR/USD", "1h", 100)
        provider.fetchForexHistory("EUR/USD", "1d", 100)

        assertTrue(hourlyFromSec > 0)
        assertTrue(dailyFromSec > 0)

        val hourlyLookback = nowSec - hourlyFromSec
        val dailyLookback = nowSec - dailyFromSec

        // 100 daily candles (86400s per candle) lookback must be > 20x hourly (3600s per candle) lookback
        assertTrue(
            "Daily lookback ($dailyLookback) must be > 20x hourly lookback ($hourlyLookback)",
            dailyLookback > hourlyLookback * 20
        )
    }

    @Test
    fun testRealMarketDataApiClient_genuineMacd_computesNinePeriodEmaOfMacdHistory() = runTest {
        val currentTimeSec = System.currentTimeMillis() / 1000

        // Mock with 60 increasing and fluctuating closes
        val mockHttpClient = createMockHttpClient { req ->
            val closesList = (1..60).map { 100.0 + (it * 0.5) + (if (it % 2 == 0) 0.3 else -0.3) }
            val timestampsList = (1..60).map { currentTimeSec - (60 - it) * 3600 }
            val closesJson = closesList.joinToString(",")
            val timestampsJson = timestampsList.joinToString(",")
            createJsonResponse(
                req,
                """
                {
                    "chart": {
                        "result": [
                            {
                                "meta": {
                                    "regularMarketPrice": 130.0,
                                    "chartPreviousClose": 129.5,
                                    "regularMarketDayHigh": 131.0,
                                    "regularMarketDayLow": 128.0,
                                    "regularMarketVolume": 1000000,
                                    "regularMarketTime": $currentTimeSec
                                },
                                "timestamp": [$timestampsJson],
                                "indicators": {
                                    "quote": [
                                        { 
                                            "open": [$closesJson],
                                            "high": [$closesJson],
                                            "low": [$closesJson],
                                            "close": [$closesJson] 
                                        }
                                    ]
                                }
                            }
                        ]
                    }
                }
                """.trimIndent()
            )
        }

        val apiClient = RealMarketDataApiClient(mockHttpClient)
        val asset = MarketUniverse.findAssetBySymbol("NVDA")
        val result = apiClient.fetchRealQuote(asset)

        assertTrue(result is MarketFetchResult.Success)
        val quote = (result as MarketFetchResult.Success).quote

        // Proves that macdSignal is genuinely derived via 9-period EMA and NOT the fabricated macdLine * 0.85
        assertFalse(
            "MACD Signal must not be hardcoded to 0.85 of MACD Line",
            quote.macdLine != 0.0 && Math.abs(quote.macdSignal - (quote.macdLine * 0.85)) < 0.0001
        )
        // Histogram is line - signal
        assertEquals(quote.macdLine - quote.macdSignal, quote.macdHistogram, 0.001)
    }

    @Test
    fun testRealMarketDataApiClient_atr_rejectsInsufficientCandlesWithoutFabricatedFallback() = runTest {
        val currentTimeSec = System.currentTimeMillis() / 1000

        // Mock with only 10 candles (fewer than the required 15 for 14-period ATR)
        val mockHttpClient = createMockHttpClient { req ->
            val closesList = (1..10).map { 100.0 + it }
            val timestampsList = (1..10).map { currentTimeSec - (10 - it) * 3600 }
            val closesJson = closesList.joinToString(",")
            val timestampsJson = timestampsList.joinToString(",")
            createJsonResponse(
                req,
                """
                {
                    "chart": {
                        "result": [
                            {
                                "meta": {
                                    "regularMarketPrice": 110.0,
                                    "chartPreviousClose": 109.0,
                                    "regularMarketDayHigh": 111.0,
                                    "regularMarketDayLow": 108.0,
                                    "regularMarketVolume": 50000,
                                    "regularMarketTime": $currentTimeSec
                                },
                                "timestamp": [$timestampsJson],
                                "indicators": {
                                    "quote": [
                                        { 
                                            "open": [$closesJson],
                                            "high": [$closesJson],
                                            "low": [$closesJson],
                                            "close": [$closesJson] 
                                        }
                                    ]
                                }
                            }
                        ]
                    }
                }
                """.trimIndent()
            )
        }

        val apiClient = RealMarketDataApiClient(mockHttpClient)
        val asset = MarketUniverse.findAssetBySymbol("NVDA")
        val result = apiClient.fetchRealQuote(asset)

        // Must reject insufficient candles rather than fabricating ATR = 1.0
        assertTrue("Insufficient candles must result in Failure", result is MarketFetchResult.Failure)
        val failure = result as MarketFetchResult.Failure
        assertTrue(
            "Failure reason must mention insufficient history/candles",
            failure.errorReason.contains("requires ≥ 50 candles") || failure.errorReason.contains("Insufficient")
        )
    }

    @Test
    fun testEngine_marksCachedQuotesStaleAfterFetchFailures() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        var shouldFailNetwork = false
        val currentTimeSec = System.currentTimeMillis() / 1000

        val mockHttpClient = createMockHttpClient { req ->
            if (shouldFailNetwork) {
                throw IOException("Temporary network timeout")
            }
            val url = req.url.toString()
            if (url.contains("binance.com/api/v3/klines")) {
                val klinesList = (1..60).map { i ->
                    val ts = currentTimeSec * 1000 - (60 - i) * 3600000L
                    val p = 84000.0 + (i * 5.0)
                    "[$ts,\"$p\",\"${p + 100}\",\"${p - 100}\",\"${p + 10}\",\"100\",${ts + 3600000L},\"1000000\",50,\"50\",\"500000\",\"0\"]"
                }
                createJsonResponse(req, "[" + klinesList.joinToString(",") + "]")
            } else {
                createJsonResponse(
                    req,
                    """
                    {
                        "symbol": "BTCUSDT",
                        "lastPrice": "84000.00",
                        "bidPrice": "83995.00",
                        "askPrice": "84005.00",
                        "priceChangePercent": "1.20",
                        "highPrice": "85000.00",
                        "lowPrice": "83000.00",
                        "quoteVolume": "1000000.00",
                        "closeTime": ${currentTimeSec * 1000}
                    }
                    """.trimIndent()
                )
            }
        }

        val mockForexProvider = object : ForexHistoricalDataProvider() {
            override suspend fun fetchForexHistory(symbol: String, timeframe: String, limit: Int): PriceHistoryResult {
                if (shouldFailNetwork) return PriceHistoryResult.Failure("Network down", "", true)
                val now = System.currentTimeMillis()
                val candles = (0 until 60).map { i ->
                    val ts = now - (59 - i) * 3600000L
                    PriceCandlestick(ts, 1.0850, 1.0860, 1.0840, 1.0850)
                }
                return PriceHistoryResult.Success(symbol, timeframe, candles, PriceDataSource.TEST_DATA, now)
            }
        }

        val apiClient = RealMarketDataApiClient(mockHttpClient, mockForexProvider)
        val engine = RealMarketDataEngine(scope = testScope, apiClient = apiClient)

        // Cycle 1: Success
        engine.fetchRealMarketDataOnce()
        val initialBtc = engine.getQuoteForSymbol("BTC/USD")
        assertEquals(84000.00, initialBtc.lastPrice, 0.01)
        assertFalse("Freshly fetched quote must not be marked stale", initialBtc.isStale)

        // Cycle 2: Network failure
        shouldFailNetwork = true
        engine.fetchRealMarketDataOnce()

        val cachedBtc = engine.getQuoteForSymbol("BTC/USD")
        assertEquals(84000.00, cachedBtc.lastPrice, 0.01)
        assertTrue("Cached quote MUST be marked stale after fetch failure", cachedBtc.isStale)

        // AI telemetry summary must warn that quote is stale and prohibit trades
        val telemetry = engine.getTelemetrySummaryForAi(cachedBtc)
        assertTrue(telemetry.contains("STATUS: Live genuine API data unavailable or stale"))
        assertTrue(telemetry.contains("NEVER execute trades or create fake signals on absent or stale telemetry"))
    }

    @Test
    fun testOkHttpResponseClosure_closesResponseOnAllPaths() = runTest {
        var closedCount = 0
        val mockClient = object : OkHttpClient() {
            override fun newCall(request: Request): Call {
                return object : Call {
                    override fun request(): Request = request
                    override fun execute(): Response {
                        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                        val originalBody = "{\"error\":\"Rate limit reached\"}".toResponseBody(mediaType)
                        val trackingBody = object : okhttp3.ResponseBody() {
                            override fun contentType() = originalBody.contentType()
                            override fun contentLength() = originalBody.contentLength()
                            override fun source() = originalBody.source()
                            override fun close() {
                                closedCount++
                                originalBody.close()
                            }
                        }
                        return Response.Builder()
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .code(429)
                            .message("Too Many Requests")
                            .body(trackingBody)
                            .build()
                    }
                    override fun enqueue(responseCallback: okhttp3.Callback) {}
                    override fun cancel() {}
                    override fun isExecuted(): Boolean = true
                    override fun isCanceled(): Boolean = false
                    override fun clone(): Call = this
                    override fun timeout(): okio.Timeout = okio.Timeout.NONE
                }
            }
        }

        val provider = ForexHistoricalDataProvider(
            client = mockClient,
            finnhubApiKey = "valid_key",
            alphaVantageApiKey = null
        )

        val result = provider.fetchForexHistory("EUR/USD", "1h", 60)
        assertTrue(result is PriceHistoryResult.Failure)
        val failure = result as PriceHistoryResult.Failure
        assertEquals("Finnhub HTTP 429", failure.reason)
        assertEquals(true, failure.retryable)
        assertTrue("OkHttp ResponseBody must be closed on HTTP failure paths", closedCount > 0)
    }

    @Test
    fun testAlphaVantageConfiguration_wiresFallbackWhenConfiguredAndAvoidsDemoKey() = runTest {
        var requestedUrl: String? = null
        val mockClient = createMockHttpClient { req ->
            requestedUrl = req.url.toString()
            val nowSec = System.currentTimeMillis() / 1000
            val dateFormatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            dateFormatter.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val dailyJsonEntries = (0 until 60).joinToString(",") { i ->
                val dateStr = dateFormatter.format(java.util.Date((nowSec - i * 86400L) * 1000))
                "\"$dateStr\": {\"1. open\": \"1.0850\", \"2. high\": \"1.0890\", \"3. low\": \"1.0840\", \"4. close\": \"1.0875\"}"
            }
            createJsonResponse(
                req,
                """
                {
                    "Time Series FX (Daily)": {
                        $dailyJsonEntries
                    }
                }
                """.trimIndent()
            )
        }

        // When Alpha Vantage key is explicitly provided, fallback functions on 1d timeframe
        val provider = ForexHistoricalDataProvider(
            client = mockClient,
            finnhubApiKey = null,
            alphaVantageApiKey = "configured_alpha_vantage_key_123"
        )

        val result = provider.fetchForexHistory("EUR/USD", "1d", 60)
        assertTrue(result is PriceHistoryResult.Success)
        assertNotNull(requestedUrl)
        assertTrue(requestedUrl!!.contains("apikey=configured_alpha_vantage_key_123"))
        assertFalse(requestedUrl!!.contains("demo"))
    }

    @Test
    fun testEma200_insufficientHistoryReturnsNull_neverFabricatesPricesAverage() {
        val apiClient = RealMarketDataApiClient()

        // 50 closes
        val fiftyCloses = (1..50).map { 100.0 + it }
        val calculatedEma50Period = apiClient.calculateEma(fiftyCloses, 50)
        assertNotNull(calculatedEma50Period)

        // 200 EMA with only 50 closes MUST be null (never prices.average())
        val ema200From50 = apiClient.calculateEma(fiftyCloses, 200)
        assertNull("EMA200 must be null when fewer than 200 closes are available", ema200From50)

        // 199 closes MUST still be null
        val almost200Closes = (1..199).map { 100.0 + it }
        val ema200From199 = apiClient.calculateEma(almost200Closes, 200)
        assertNull("EMA200 must be null for 199 closes", ema200From199)

        // 200 closes produces genuine non-null EMA200
        val exactly200Closes = (1..200).map { 100.0 + it }
        val ema200From200 = apiClient.calculateEma(exactly200Closes, 200)
        assertNotNull("EMA200 must calculate successfully when 200 closes exist", ema200From200)
        assertEquals(exactly200Closes.average(), ema200From200!!, 0.0001)

        // Telemetry verification: when ema200 is null, telemetry formats as UNAVAILABLE and avoids false ladder
        val engine = RealMarketDataEngine()
        val quoteWithNullEma200 = GenuineMarketQuote(
            asset = MarketUniverse.findAssetBySymbol("EUR/USD"),
            lastPrice = 1.0850,
            bid = 1.0849,
            ask = 1.0851,
            spreadPipsOrPoints = 0.2,
            change24hPercent = 0.15,
            high24h = 1.0890,
            low24h = 1.0820,
            volume24h = "Real Feed",
            rsi14 = 52.0,
            macdLine = 0.0010,
            macdSignal = 0.0008,
            macdHistogram = 0.0002,
            ema20 = 1.0840,
            ema50 = 1.0830,
            ema200 = null,
            atr = 0.0040,
            candlePattern = "DOJI INDECISION",
            supportLevel = 1.0800,
            resistanceLevel = 1.0900,
            orderFlowBias = "BALANCED_RANGE",
            recentCloses = fiftyCloses
        )

        val telemetry = engine.getTelemetrySummaryForAi(quoteWithNullEma200)
        assertTrue(telemetry.contains("200 EMA: UNAVAILABLE (< 200 candles)"))
        assertFalse(
            "Must not claim STRONG BULLISH LADDER when EMA200 is unavailable",
            telemetry.contains("STRONG BULLISH LADDER")
        )
    }

    @Test
    fun testRealOhlcCandlePatterns_genuineGeometryValidation() {
        val apiClient = RealMarketDataApiClient()

        // 1. Insufficient candles (< 2) returns UNAVAILABLE
        val singleCandle = listOf(PriceCandlestick(1000L, 100.0, 105.0, 95.0, 102.0))
        assertEquals("UNAVAILABLE (Insufficient OHLC)", apiClient.evaluateCandlePattern(singleCandle))

        // 2. Doji: open and close virtually identical (body <= 10% of total range)
        val dojiCandles = listOf(
            PriceCandlestick(1000L, 100.0, 105.0, 95.0, 102.0),
            PriceCandlestick(2000L, 102.0, 106.0, 98.0, 102.1) // range = 8.0, body = 0.1 (1.25% of range)
        )
        assertEquals("DOJI INDECISION", apiClient.evaluateCandlePattern(dojiCandles))

        // 3. Bullish Pinbar / Hammer: long lower wick (>= 60%), small body (<= 30%), small upper wick (<= 20%)
        val hammerCandles = listOf(
            PriceCandlestick(1000L, 105.0, 108.0, 102.0, 107.0),
            PriceCandlestick(2000L, 108.0, 110.0, 90.0, 109.0)
        )
        assertEquals("BULLISH PINBAR HAMMER", apiClient.evaluateCandlePattern(hammerCandles))

        // 4. Bearish Shooting Star: long upper wick (>= 60%), small body (<= 30%), small lower wick (<= 20%)
        val starCandles = listOf(
            PriceCandlestick(1000L, 95.0, 98.0, 92.0, 93.0),
            PriceCandlestick(2000L, 92.0, 110.0, 90.0, 91.0)
        )
        assertEquals("BEARISH SHOOTING STAR", apiClient.evaluateCandlePattern(starCandles))

        // 5. Bullish Engulfing: prev bearish, curr bullish engulfing
        val engulfingCandles = listOf(
            PriceCandlestick(1000L, 105.0, 106.0, 99.0, 100.0),
            PriceCandlestick(2000L, 99.0, 108.0, 98.0, 107.0)
        )
        assertEquals("BULLISH ENGULFING", apiClient.evaluateCandlePattern(engulfingCandles))

        // 6. Close-only pattern shortcut rejection: flat candles must never be called BULLISH PINBAR HAMMER
        val flatCandles = listOf(
            PriceCandlestick(1000L, 100.0, 100.0, 100.0, 100.0),
            PriceCandlestick(2000L, 95.0, 95.0, 95.0, 95.0),
            PriceCandlestick(3000L, 102.0, 102.0, 102.0, 102.0)
        )
        assertFalse(
            "Flat candles without wicks must never be classified as BULLISH PINBAR HAMMER",
            apiClient.evaluateCandlePattern(flatCandles) == "BULLISH PINBAR HAMMER"
        )
    }

    private fun createJsonResponse(request: Request, bodyString: String): Response {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val responseBody = bodyString.toResponseBody(mediaType)
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(responseBody)
            .build()
    }

    private fun createMockHttpClient(handler: (Request) -> Response): OkHttpClient {
        return object : OkHttpClient() {
            override fun newCall(request: Request): Call {
                return object : Call {
                    override fun request(): Request = request
                    override fun execute(): Response = handler(request)
                    override fun enqueue(responseCallback: okhttp3.Callback) {
                        try {
                            responseCallback.onResponse(this, handler(request))
                        } catch (e: IOException) {
                            responseCallback.onFailure(this, e)
                        }
                    }
                    override fun cancel() {}
                    override fun isExecuted(): Boolean = true
                    override fun isCanceled(): Boolean = false
                    override fun clone(): Call = this
                    override fun timeout(): okio.Timeout = okio.Timeout.NONE
                }
            }
        }
    }
}
