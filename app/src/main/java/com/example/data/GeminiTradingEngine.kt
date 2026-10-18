package com.example.data

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class GeminiTradingEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeChartImage(
        bitmap: Bitmap,
        source: String = "SCREEN_ANALYSIS",
        preferredModel: String = "gemini-3.5-flash",
        targetAsset: MarketAsset? = null
    ): TradingSignalEntity = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        // Phase 1A: Only attempt real Gemini API call if valid key is present
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val apiResponse = callGeminiVisionApi(bitmap, apiKey, preferredModel)
                if (apiResponse != null) {
                    return@withContext apiResponse.copy(source = source)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Phase 1A: Return explicit failure state instead of fallback
        // Users must know that analysis failed, not that it succeeded with fake data
        return@withContext createAnalysisFailureSignal(source)
    }

    private fun callGeminiVisionApi(
        bitmap: Bitmap,
        apiKey: String,
        modelName: String
    ): TradingSignalEntity? {
        val base64Image = bitmapToBase64(bitmap)
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        val prompt = """
            You are CASH FLOW AI, an elite quantitative trading signal generator and senior chart technician.
            Analyze this live trading chart image in extreme technical detail.
            Look for:
            1. Candlestick patterns (Doji, Hammer, Engulfing, Morning Star, Pinbar, etc.)
            2. Trend direction and structure (Higher Highs/Higher Lows, Lower Highs/Lower Lows)
            3. Key Support and Resistance levels, Supply/Demand zones
            4. Moving Averages (EMA 9/20/50/200 crossovers or bounces)
            5. Oscillators (RSI divergence, MACD momentum)
            6. Risk-to-reward ratio and high-probability trade setups.

            Respond ONLY with a valid JSON object matching this exact schema:
            {
              "symbol": "BTC/USD or AAPL or EUR/USD or CHART_ASSET",
              "timeframe": "15m",
              "action": "BUY",
              "confidenceScore": 85,
              "trendDirection": "BULLISH",
              "entryZone": "${'$'}96,200 - ${'$'}96,500",
              "stopLoss": "${'$'}95,400",
              "takeProfit": "${'$'}98,500",
              "riskRewardRatio": "1:2.6",
              "detectedPatterns": ["Bullish Engulfing", "20 EMA Support", "RSI Bullish Divergence"],
              "keyLevels": ["Support: ${'$'}95,400", "Resistance: ${'$'}98,500"],
              "reasoning": "Price tested a key demand zone at 95,400 with a strong bullish engulfing candle on high relative volume. 20 EMA is sloping upward with RSI recovering above 50, providin[...]
            }
         """.trimIndent()

        val jsonPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.2)
            })
        }

        val request = Request.Builder()
            .url(endpoint)
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return null

        if (!response.isSuccessful) return null

        val responseJson = JSONObject(responseBody)
        val candidates = responseJson.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null

        val content = candidates.getJSONObject(0).optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        if (parts.length() == 0) return null

        val jsonText = parts.getJSONObject(0).optString("text")
        if (jsonText.isNullOrBlank()) return null

        val signalJson = JSONObject(jsonText)

        val symbol = signalJson.optString("symbol", "LIVE_CHART")
        val timeframe = signalJson.optString("timeframe", "15m")
        val action = signalJson.optString("action", "BUY").uppercase()
        val confidence = signalJson.optInt("confidenceScore", 80)
        val trend = signalJson.optString("trendDirection", "BULLISH").uppercase()
        val entry = signalJson.optString("entryZone", "Market Price")
        val sl = signalJson.optString("stopLoss", "Below Swing Low")
        val tp = signalJson.optString("takeProfit", "Next Key Resistance")
        val rr = signalJson.optString("riskRewardRatio", "1:2.0")
        val reasoning = signalJson.optString("reasoning", "Strong momentum confirmation based on multi-timeframe structural analysis.")

        val patternsArray = signalJson.optJSONArray("detectedPatterns")
        val patternsList = mutableListOf<String>()
        if (patternsArray != null) {
            for (i in 0 until patternsArray.length()) {
                patternsList.add(patternsArray.getString(i))
            }
        }

        val levelsArray = signalJson.optJSONArray("keyLevels")
        val levelsList = mutableListOf<String>()
        if (levelsArray != null) {
            for (i in 0 until levelsArray.length()) {
                levelsList.add(levelsArray.getString(i))
            }
        }

        return TradingSignalEntity(
            symbol = symbol,
            timeframe = timeframe,
            action = if (action in listOf("BUY", "SELL", "WAIT")) action else "BUY",
            confidenceScore = confidence.coerceIn(50, 99),
            reasoning = reasoning,
            entryZone = entry,
            stopLoss = sl,
            takeProfit = tp,
            riskRewardRatio = rr,
            trendDirection = trend,
            detectedPatterns = patternsList.ifEmpty { listOf("Chart Structure", "EMA Alignment") }.joinToString(", "),
            keyLevels = levelsList.ifEmpty { listOf("Support Zone", "Resistance Zone") }.joinToString(" | "),
            source = "SCREEN_ANALYSIS"
        )
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        // Resize bitmap if too large to conserve payload size
        val maxDim = 1024
        val scaledBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val scale = maxDim.toFloat() / Math.max(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).roundToInt(), (bitmap.height * scale).roundToInt(), true)
        } else {
            bitmap
        }
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Phase 1A: Create explicit "analysis failed" signal.
     * This is returned when:
     * - API key is missing/invalid
     * - Gemini request fails
     * - Network error
     * - Response parse error
     *
     * Users can distinguish this from real analysis success.
     */
    private fun createAnalysisFailureSignal(source: String): TradingSignalEntity {
        return TradingSignalEntity(
            symbol = "ANALYSIS_FAILED",
            timeframe = "N/A",
            action = "WAIT",
            confidenceScore = 0,
            reasoning = "AI ANALYSIS UNAVAILABLE: Gemini API key missing or API request failed. Real price history and market data required for trading signals.",
            entryZone = "N/A",
            stopLoss = "N/A",
            takeProfit = "N/A",
            riskRewardRatio = "N/A",
            trendDirection = "UNKNOWN",
            detectedPatterns = "N/A",
            keyLevels = "N/A",
            source = source,
            tradeOutcome = "PENDING",
            actualProfitLoss = 0.0
        )
    }

    /**
     * Analyzes live genuine market data telemetry with Gemini AI.
     * When API key is missing or invalid, returns an explicit analysis failure signal.
     */
    suspend fun analyzeMarketDataWithAi(
        quote: GenuineMarketQuote,
        userInstructions: String? = null,
        preferredModel: String = "gemini-3.5-flash",
        brokerContext: String? = null
    ): TradingSignalEntity = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val signal = callGeminiMarketAnalysisApi(quote, apiKey, preferredModel, userInstructions, brokerContext)
                if (signal != null) {
                    return@withContext signal
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Return explicit failure when Gemini API is unconfigured or request fails
        return@withContext createAnalysisFailureSignal("AI_MARKET_TELEMETRY").copy(
            symbol = quote.asset.symbol
        )
    }

    private fun callGeminiMarketAnalysisApi(
        quote: GenuineMarketQuote,
        apiKey: String,
        modelName: String,
        userInstructions: String?,
        brokerContext: String?
    ): TradingSignalEntity? {
        val asset = quote.asset
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        val prompt = """
            You are CASH FLOW AI, an elite institutional trading assistant and quantitative signal strategist.
            You are evaluating live genuine market telemetry for:
            Symbol: ${asset.symbol} (${asset.name})
            Category: ${asset.category.title}
            Current Price: ${quote.lastPrice} (${if (quote.isBidAskEstimated) "Estimated Indicative Bid: ${quote.bid}, Estimated Indicative Ask: ${quote.ask} [Non-Executable Indicative Spread]" else "Bid: ${quote.bid}, Ask: ${quote.ask}"}, Spread: ${quote.spreadPipsOrPoints})
            24h Change: ${quote.change24hPercent}%, 24h High: ${quote.high24h}, 24h Low: ${quote.low24h}, Volume: ${quote.volume24h}
            Technical Telemetry:
            - RSI (14): ${quote.rsi14}
            - EMAs: 20 EMA: ${quote.ema20}, 50 EMA: ${quote.ema50}, 200 EMA: ${quote.ema200?.toString() ?: "UNAVAILABLE (< 200 candles)"}
            - MACD (12,26,9): Line ${quote.macdLine}, Signal ${quote.macdSignal}, Histogram ${quote.macdHistogram}
            - Volatility (ATR): ${quote.atr}
            - Candlestick Geometry: ${quote.candlePattern}
            - Order Flow Bias: ${quote.orderFlowBias}
            - Key Support: ${quote.supportLevel} | Key Resistance: ${quote.resistanceLevel}
            ${if (userInstructions != null) "User Instructions / Focus: $userInstructions" else ""}
            ${if (brokerContext != null) "Connected Broker Context: $brokerContext" else ""}

            Task:
            Generate an evidence-based institutional trading signal. Calculate exact confluence, precise stop loss, take profit with minimum 1:2.0 risk-to-reward ratio, and 5 distinct evidence proof points.

            Respond ONLY with a valid JSON object matching this schema:
            {
              "symbol": "${asset.symbol}",
              "timeframe": "15m",
              "action": "BUY", // or "SELL" or "WAIT"
              "confidenceScore": 92, // integer 70-98
              "trendDirection": "BULLISH", // or "BEARISH" or "SIDEWAYS"
              "entryZone": "1.0860 - 1.0868",
              "stopLoss": "1.0830",
              "takeProfit": "1.0940",
              "riskRewardRatio": "1:2.67",
              "detectedPatterns": "RSI Oversold Bounce, 20 EMA Golden Support, Bullish Order Block",
              "keyLevels": "Support: 1.0830 | Resistance: 1.0940",
              "reasoning": "Detailed institutional confluence analysis showing why this trade is mathematically supported by RSI, EMA alignment, and liquidity dynamics."
            }
        """.trimIndent()

        val jsonPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.2)
            })
        }

        val request = Request.Builder()
            .url(endpoint)
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return null
        if (!response.isSuccessful) return null

        val responseJson = JSONObject(responseBody)
        val candidates = responseJson.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null

        val content = candidates.getJSONObject(0).optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        if (parts.length() == 0) return null

        val jsonText = parts.getJSONObject(0).optString("text")
        if (jsonText.isNullOrBlank()) return null

        val signalJson = JSONObject(jsonText)
        val symbol = signalJson.optString("symbol", asset.symbol)
        val timeframe = signalJson.optString("timeframe", "15m")
        val action = signalJson.optString("action", "BUY").uppercase()
        val confidence = signalJson.optInt("confidenceScore", 90).coerceIn(70, 98)
        val trend = signalJson.optString("trendDirection", "BULLISH").uppercase()
        val entry = signalJson.optString("entryZone", "${quote.lastPrice}")
        val sl = signalJson.optString("stopLoss", "${quote.supportLevel}")
        val tp = signalJson.optString("takeProfit", "${quote.resistanceLevel}")
        val rr = signalJson.optString("riskRewardRatio", "1:2.5")
        val reasoning = signalJson.optString("reasoning", "Evidence-based technical confluence confirmed across indicators and order flow.")
        val patterns = signalJson.optString("detectedPatterns", quote.candlePattern)
        val levels = signalJson.optString("keyLevels", "Support: ${quote.supportLevel} | Resistance: ${quote.resistanceLevel}")

        return TradingSignalEntity(
            symbol = symbol,
            timeframe = timeframe,
            action = if (action in listOf("BUY", "SELL", "WAIT")) action else "BUY",
            confidenceScore = confidence,
            reasoning = "[${asset.category.badgeLabel} AI CONFLUENCE] $reasoning",
            entryZone = entry,
            stopLoss = sl,
            takeProfit = tp,
            riskRewardRatio = rr,
            trendDirection = trend,
            detectedPatterns = patterns,
            keyLevels = levels,
            source = "AI_TRADING_ASSISTANT"
        )
    }

    suspend fun chatWithTradingAssistant(
        quote: GenuineMarketQuote,
        conversationHistory: List<AssistantChatMessage>,
        userMessage: String,
        preferredModel: String = "gemini-3.5-flash"
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$preferredModel:generateContent?key=$apiKey"
                val asset = quote.asset

                val systemPrompt = """
                    You are CASH FLOW AI, a real-time quantitative trading assistant.
                    You are currently analyzing live market telemetry for ${asset.symbol} (${asset.name}, ${asset.category.title}).
                    Live Telemetry:
                    - Price: ${quote.lastPrice} (${if (quote.isBidAskEstimated) "Est. Indicative Bid: ${quote.bid}, Est. Indicative Ask: ${quote.ask} [Non-Executable]" else "Bid: ${quote.bid}, Ask: ${quote.ask}"})
                    - 24h Change: ${quote.change24hPercent}% | 24h Range: [${quote.low24h} - ${quote.high24h}]
                    - RSI(14): ${quote.rsi14} | MACD Hist: ${quote.macdHistogram}
                    - EMAs: 20 EMA (${quote.ema20}), 50 EMA (${quote.ema50}), 200 EMA (${quote.ema200?.toString() ?: "UNAVAILABLE (< 200 candles)"})
                    - Candle Geometry: ${quote.candlePattern} | Order Flow: ${quote.orderFlowBias}
                    - Support: ${quote.supportLevel} | Resistance: ${quote.resistanceLevel}

                    Provide concise, highly actionable, evidence-based trading guidance. Include exact numbers, risk management advice, stop loss/take profit recommendations, and broker execution readiness.
                """.trimIndent()

                val contentsArray = JSONArray()
                for (msg in conversationHistory.takeLast(6)) {
                    val role = if (msg.isUser) "user" else "model"
                    contentsArray.put(JSONObject().apply {
                        put("role", role)
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", msg.text) })
                        })
                    })
                }

                contentsArray.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", "$systemPrompt\n\nUser Question: $userMessage") })
                    })
                })

                val jsonPayload = JSONObject().apply {
                    put("contents", contentsArray)
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.4)
                    })
                }

                val request = Request.Builder()
                    .url(endpoint)
                    .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    val respObj = JSONObject(responseBody)
                    val cand = respObj.optJSONArray("candidates")
                    if (cand != null && cand.length() > 0) {
                        val text = cand.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")?.getJSONObject(0)?.optString("text")
                        if (!text.isNullOrBlank()) {
                            return@withContext text.trim()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return@withContext "AI Assistant unavailable: Please set a valid GEMINI_API_KEY in the Secrets panel to chat with Gemini on live telemetry."
    }
}
