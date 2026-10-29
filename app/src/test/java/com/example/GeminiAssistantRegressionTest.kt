package com.example

import com.example.data.AssistantChatMessage
import com.example.data.GeminiTradingEngine
import com.example.data.GenuineMarketQuote
import com.example.data.MarketUniverse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class GeminiAssistantRegressionTest {

    private fun createSampleQuote(isEstimated: Boolean = false): GenuineMarketQuote {
        return GenuineMarketQuote(
            asset = MarketUniverse.EUR_USD,
            lastPrice = 1.0865,
            bid = 1.0864,
            ask = 1.0866,
            spreadPipsOrPoints = 0.2,
            change24hPercent = 0.15,
            high24h = 1.0890,
            low24h = 1.0840,
            volume24h = "450K",
            rsi14 = 54.2,
            macdLine = 0.0004,
            macdSignal = 0.0003,
            macdHistogram = 0.0001,
            ema20 = 1.0858,
            ema50 = 1.0850,
            ema200 = 1.0820,
            atr = 0.0018,
            candlePattern = "Bullish Hammer",
            supportLevel = 1.0840,
            resistanceLevel = 1.0890,
            orderFlowBias = "BULLISH_ACCUMULATION",
            recentCloses = listOf(1.0850, 1.0855, 1.0860, 1.0865),
            isBidAskEstimated = isEstimated
        )
    }

    @Test
    fun testChat_missingApiKey_returnsConfigurationPrompt() = runTest {
        val engine = GeminiTradingEngine()
        val quote = createSampleQuote()

        val response = engine.chatWithTradingAssistant(
            quote = quote,
            conversationHistory = emptyList(),
            userMessage = "What is the trend?",
            apiKeyOverride = ""
        )

        assertTrue(
            "Expected configuration prompt when API key is blank",
            response.contains("AI Assistant unavailable: Please set a valid GEMINI_API_KEY in the Secrets panel")
        )
    }

    @Test
    fun testChat_placeholderApiKey_returnsConfigurationPrompt() = runTest {
        val engine = GeminiTradingEngine()
        val quote = createSampleQuote()

        val response = engine.chatWithTradingAssistant(
            quote = quote,
            conversationHistory = emptyList(),
            userMessage = "What is the trend?",
            apiKeyOverride = "MY_GEMINI_API_KEY"
        )

        assertTrue(
            "Expected configuration prompt when placeholder API key is used",
            response.contains("AI Assistant unavailable: Please set a valid GEMINI_API_KEY in the Secrets panel")
        )
    }

    @Test
    fun testChat_duplicateUserMessage_deduplicatesAndIncludesOnce() = runTest {
        var recordedBody = ""
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val buffer = okio.Buffer()
            request.body?.writeTo(buffer)
            recordedBody = buffer.readUtf8()

            val successJson = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          { "text": "The RSI is at 54.2, indicating neutral-to-bullish momentum." }
                        ]
                      }
                    }
                  ]
                }
            """.trimIndent()

            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(successJson.toResponseBody("application/json".toMediaType()))
                .build()
        }.build()

        val engine = GeminiTradingEngine(client = client)
        val quote = createSampleQuote()

        // History already contains the message as last entry (simulating ViewModel adding it)
        val history = listOf(
            AssistantChatMessage(isUser = false, text = "Welcome to CashFlow AI Assistant."),
            AssistantChatMessage(isUser = true, text = "What is the RSI?")
        )

        val answer = engine.chatWithTradingAssistant(
            quote = quote,
            conversationHistory = history,
            userMessage = "What is the RSI?",
            apiKeyOverride = "VALID_TEST_KEY"
        )

        assertEquals("The RSI is at 54.2, indicating neutral-to-bullish momentum.", answer)

        // Parse recorded JSON payload to verify contents array
        val payloadObj = JSONObject(recordedBody)
        val contentsArray = payloadObj.getJSONArray("contents")

        // Prior history had 2 items, last user message should be dropped and re-appended once -> total 2 items: 1 model, 1 user
        assertEquals(2, contentsArray.length())
        assertEquals("model", contentsArray.getJSONObject(0).getString("role"))
        assertEquals("user", contentsArray.getJSONObject(1).getString("role"))
        assertEquals(
            "What is the RSI?",
            contentsArray.getJSONObject(1).getJSONArray("parts").getJSONObject(0).getString("text")
        )
    }

    @Test
    fun testChat_systemInstruction_containsMarketTelemetry() = runTest {
        var recordedBody = ""
        var recordedHeaderKey = ""
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            recordedHeaderKey = request.header("x-goog-api-key").orEmpty()
            val buffer = okio.Buffer()
            request.body?.writeTo(buffer)
            recordedBody = buffer.readUtf8()

            val successJson = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          { "text": "EUR/USD is testing 1.0865 with key support at 1.0840." }
                        ]
                      }
                    }
                  ]
                }
            """.trimIndent()

            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(successJson.toResponseBody("application/json".toMediaType()))
                .build()
        }.build()

        val engine = GeminiTradingEngine(client = client)
        val quote = createSampleQuote(isEstimated = true)

        engine.chatWithTradingAssistant(
            quote = quote,
            conversationHistory = emptyList(),
            userMessage = "Analyze EUR/USD",
            apiKeyOverride = "SECRET_API_KEY_XYZ"
        )

        // API Key passed in header, not in query string
        assertEquals("SECRET_API_KEY_XYZ", recordedHeaderKey)

        val payloadObj = JSONObject(recordedBody)
        assertTrue(payloadObj.has("systemInstruction"))
        val systemInstructionText = payloadObj.getJSONObject("systemInstruction")
            .getJSONArray("parts").getJSONObject(0).getString("text")

        assertTrue(systemInstructionText.contains("EUR/USD"))
        assertTrue(systemInstructionText.contains("1.0865"))
        assertTrue(systemInstructionText.contains("Non-Executable Indicative Spread"))
    }

    @Test
    fun testChat_http400_returnsBadRequestMessage() = runTest {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val errorJson = """
                {
                  "error": {
                    "code": 400,
                    "message": "Invalid argument: contents is empty",
                    "status": "INVALID_ARGUMENT"
                  }
                }
            """.trimIndent()

            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(400)
                .message("Bad Request")
                .body(errorJson.toResponseBody("application/json".toMediaType()))
                .build()
        }.build()

        val engine = GeminiTradingEngine(client = client)
        val response = engine.chatWithTradingAssistant(
            quote = createSampleQuote(),
            conversationHistory = emptyList(),
            userMessage = "Hello",
            apiKeyOverride = "TEST_KEY"
        )

        assertTrue(response.contains("Gemini API Bad Request (HTTP 400)"))
        assertTrue(response.contains("Invalid argument"))
    }

    @Test
    fun testChat_http401_403_returnsAuthFailureMessage() = runTest {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(403)
                .message("Forbidden")
                .body("API_KEY_INVALID".toResponseBody("text/plain".toMediaType()))
                .build()
        }.build()

        val engine = GeminiTradingEngine(client = client)
        val response = engine.chatWithTradingAssistant(
            quote = createSampleQuote(),
            conversationHistory = emptyList(),
            userMessage = "Hello",
            apiKeyOverride = "INVALID_KEY"
        )

        assertTrue(response.contains("Gemini API authentication failed (HTTP 403)"))
        assertTrue(response.contains("Secrets panel"))
    }

    @Test
    fun testChat_http404_returnsModelNotFoundMessage() = runTest {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(404)
                .message("Not Found")
                .body("""{"error": {"message": "models/legacy-model is not found"}}""".toResponseBody("application/json".toMediaType()))
                .build()
        }.build()

        val engine = GeminiTradingEngine(client = client)
        val response = engine.chatWithTradingAssistant(
            quote = createSampleQuote(),
            conversationHistory = emptyList(),
            userMessage = "Hello",
            preferredModel = "gemini-3.5-flash",
            apiKeyOverride = "TEST_KEY"
        )

        assertTrue(response.contains("Gemini model 'gemini-3.5-flash' not found (HTTP 404)"))
    }

    @Test
    fun testChat_http429_proModelFallbacksToFlash() = runTest {
        val callCount = AtomicInteger(0)
        val requestedModels = mutableListOf<String>()

        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val count = callCount.incrementAndGet()
            val url = chain.request().url.toString()
            requestedModels.add(url)

            if (count == 1) {
                // First call to gemini-3.1-pro-preview returns 429 quota exhausted
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(429)
                    .message("Too Many Requests")
                    .body("""{"error": {"message": "Resource has been exhausted (quota)"}}""".toResponseBody("application/json".toMediaType()))
                    .build()
            } else {
                // Second call fallbacks to gemini-3.5-flash which succeeds
                val successJson = """
                    {
                      "candidates": [
                        {
                          "content": {
                            "parts": [
                              { "text": "Fallback answer from gemini-3.5-flash." }
                            ]
                          }
                        }
                      ]
                    }
                """.trimIndent()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(successJson.toResponseBody("application/json".toMediaType()))
                    .build()
            }
        }.build()

        val engine = GeminiTradingEngine(client = client)
        val response = engine.chatWithTradingAssistant(
            quote = createSampleQuote(),
            conversationHistory = emptyList(),
            userMessage = "What is the analysis?",
            preferredModel = "gemini-3.1-pro-preview",
            apiKeyOverride = "TEST_KEY"
        )

        assertEquals("Fallback answer from gemini-3.5-flash.", response)
        assertEquals(2, callCount.get())
        assertTrue(requestedModels[0].contains("gemini-3.1-pro-preview"))
        assertTrue(requestedModels[1].contains("gemini-3.5-flash"))
    }

    @Test
    fun testChat_http5xx_returnsServiceUnavailable() = runTest {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(503)
                .message("Service Unavailable")
                .body("Backend overloaded".toResponseBody("text/plain".toMediaType()))
                .build()
        }.build()

        val engine = GeminiTradingEngine(client = client)
        val response = engine.chatWithTradingAssistant(
            quote = createSampleQuote(),
            conversationHistory = emptyList(),
            userMessage = "Hello",
            apiKeyOverride = "TEST_KEY"
        )

        assertTrue(response.contains("Gemini service temporarily unavailable (HTTP 503)"))
    }

    @Test
    fun testChat_networkFailure_returnsNetworkMessage() = runTest {
        val client = OkHttpClient.Builder().addInterceptor {
            throw IOException("Failed to connect to host (simulated network down)")
        }.build()

        val engine = GeminiTradingEngine(client = client)
        val response = engine.chatWithTradingAssistant(
            quote = createSampleQuote(),
            conversationHistory = emptyList(),
            userMessage = "Hello",
            apiKeyOverride = "TEST_KEY"
        )

        assertTrue(response.contains("Network failure connecting to Gemini"))
        assertTrue(response.contains("simulated network down"))
    }

    @Test
    fun testChat_malformedResponse_returnsMalformedMessage() = runTest {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val emptyCandidatesJson = """
                {
                  "candidates": []
                }
            """.trimIndent()
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(emptyCandidatesJson.toResponseBody("application/json".toMediaType()))
                .build()
        }.build()

        val engine = GeminiTradingEngine(client = client)
        val response = engine.chatWithTradingAssistant(
            quote = createSampleQuote(),
            conversationHistory = emptyList(),
            userMessage = "Hello",
            apiKeyOverride = "TEST_KEY"
        )

        assertTrue(response.contains("Received empty or malformed response from Gemini"))
    }

    @Test
    fun testChat_errorDoesNotExposeApiKey() = runTest {
        val leakCandidateKey = "AIzaSySecretCandidateKey1234567890"
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val errorWithKeyJson = """
                {
                  "error": {
                    "code": 400,
                    "message": "Invalid key=$leakCandidateKey in request"
                  }
                }
            """.trimIndent()
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(400)
                .message("Bad Request")
                .body(errorWithKeyJson.toResponseBody("application/json".toMediaType()))
                .build()
        }.build()

        val engine = GeminiTradingEngine(client = client)
        val response = engine.chatWithTradingAssistant(
            quote = createSampleQuote(),
            conversationHistory = emptyList(),
            userMessage = "Hello",
            apiKeyOverride = leakCandidateKey
        )

        assertFalse("Response must NEVER leak the secret API key", response.contains(leakCandidateKey))
        assertTrue(response.contains("[REDACTED]"))
    }

    @Test
    fun testBuildConfig_secretConfiguration_whenKeyConfigured() {
        assertNotNull("BuildConfig.GEMINI_API_KEY must exist", com.example.BuildConfig.GEMINI_API_KEY)
    }
}
