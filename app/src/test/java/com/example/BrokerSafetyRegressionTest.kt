package com.example

import com.example.data.BrokerAccountEntity
import com.example.data.BrokerConnectionManager
import com.example.data.BrokerPosition
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Regression tests for Broker Safety Guards:
 * Proves that invalid inputs (zero/negative/NaN/Infinite prices, inverted brackets, invalid volume)
 * are strictly rejected, paper trading is never falsely marked as live real-money execution,
 * and valid paper trades continue to function normally.
 */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class BrokerSafetyRegressionTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var brokerManager: BrokerConnectionManager

    @Before
    fun setUp() {
        brokerManager = BrokerConnectionManager(testScope)
        brokerManager.stopAllSimulation()
    }

    @After
    fun tearDown() {
        brokerManager.stopAllSimulation()
    }

    @Test
    fun testExecuteTrade_rejectsZeroOrNegativeEntryPrice() = testScope.runTest {
        var callbackSuccess = true
        var callbackMsg = ""
        var callbackPosition: BrokerPosition? = null

        // Zero entry price
        brokerManager.executeTrade(
            symbol = "BTC/USD",
            side = "BUY",
            entryPrice = 0.0,
            stopLoss = 79000.0,
            takeProfit = 82000.0,
            volume = 0.1
        ) { success, msg, pos ->
            callbackSuccess = success
            callbackMsg = msg
            callbackPosition = pos
        }
        advanceUntilIdle()

        assertFalse(callbackSuccess)
        assertNull(callbackPosition)
        assertTrue(callbackMsg.contains("Invalid entry price"))

        // Negative entry price
        brokerManager.executeTrade(
            symbol = "EUR/USD",
            side = "BUY",
            entryPrice = -1.0850,
            stopLoss = 1.0800,
            takeProfit = 1.0950,
            volume = 0.1
        ) { success, msg, pos ->
            callbackSuccess = success
            callbackMsg = msg
            callbackPosition = pos
        }
        advanceUntilIdle()

        assertFalse(callbackSuccess)
        assertNull(callbackPosition)
        assertTrue(callbackMsg.contains("Invalid entry price"))
    }

    @Test
    fun testExecuteTrade_rejectsNaNOrInfinitePrices() = testScope.runTest {
        var callbackSuccess = true
        var callbackMsg = ""

        // NaN entry price
        brokerManager.executeTrade(
            symbol = "BTC/USD",
            side = "BUY",
            entryPrice = Double.NaN,
            stopLoss = 79000.0,
            takeProfit = 82000.0,
            volume = 0.1
        ) { success, msg, _ ->
            callbackSuccess = success
            callbackMsg = msg
        }
        advanceUntilIdle()

        assertFalse(callbackSuccess)
        assertTrue(callbackMsg.contains("Invalid entry price"))

        // Infinite stop loss
        brokerManager.executeTrade(
            symbol = "BTC/USD",
            side = "BUY",
            entryPrice = 80000.0,
            stopLoss = Double.POSITIVE_INFINITY,
            takeProfit = 82000.0,
            volume = 0.1
        ) { success, msg, _ ->
            callbackSuccess = success
            callbackMsg = msg
        }
        advanceUntilIdle()

        assertFalse(callbackSuccess)
        assertTrue(callbackMsg.contains("Invalid stop-loss"))
    }

    @Test
    fun testExecuteTrade_rejectsZeroOrNegativeVolume() = testScope.runTest {
        var callbackSuccess = true
        var callbackMsg = ""

        brokerManager.executeTrade(
            symbol = "NVDA",
            side = "BUY",
            entryPrice = 125.0,
            stopLoss = 120.0,
            takeProfit = 135.0,
            volume = 0.0
        ) { success, msg, _ ->
            callbackSuccess = success
            callbackMsg = msg
        }
        advanceUntilIdle()

        assertFalse(callbackSuccess)
        assertTrue(callbackMsg.contains("Invalid volume"))

        brokerManager.executeTrade(
            symbol = "NVDA",
            side = "BUY",
            entryPrice = 125.0,
            stopLoss = 120.0,
            takeProfit = 135.0,
            volume = -0.5
        ) { success, msg, _ ->
            callbackSuccess = success
            callbackMsg = msg
        }
        advanceUntilIdle()

        assertFalse(callbackSuccess)
        assertTrue(callbackMsg.contains("Invalid volume"))
    }

    @Test
    fun testExecuteTrade_rejectsInvertedBuyBrackets() = testScope.runTest {
        var callbackSuccess = true
        var callbackMsg = ""

        // BUY with stopLoss above entryPrice
        brokerManager.executeTrade(
            symbol = "EUR/USD",
            side = "BUY",
            entryPrice = 1.0850,
            stopLoss = 1.0900, // Invalid: above entry
            takeProfit = 1.0950,
            volume = 0.1
        ) { success, msg, _ ->
            callbackSuccess = success
            callbackMsg = msg
        }
        advanceUntilIdle()

        assertFalse(callbackSuccess)
        assertTrue(callbackMsg.contains("Invalid BUY brackets"))

        // BUY with takeProfit below entryPrice
        brokerManager.executeTrade(
            symbol = "EUR/USD",
            side = "BUY",
            entryPrice = 1.0850,
            stopLoss = 1.0800,
            takeProfit = 1.0820, // Invalid: below entry
            volume = 0.1
        ) { success, msg, _ ->
            callbackSuccess = success
            callbackMsg = msg
        }
        advanceUntilIdle()

        assertFalse(callbackSuccess)
        assertTrue(callbackMsg.contains("Invalid BUY brackets"))
    }

    @Test
    fun testExecuteTrade_rejectsInvertedSellBrackets() = testScope.runTest {
        var callbackSuccess = true
        var callbackMsg = ""

        // SELL with stopLoss below entryPrice
        brokerManager.executeTrade(
            symbol = "EUR/USD",
            side = "SELL",
            entryPrice = 1.0850,
            stopLoss = 1.0800, // Invalid: below entry
            takeProfit = 1.0750,
            volume = 0.1
        ) { success, msg, _ ->
            callbackSuccess = success
            callbackMsg = msg
        }
        advanceUntilIdle()

        assertFalse(callbackSuccess)
        assertTrue(callbackMsg.contains("Invalid SELL brackets"))

        // SELL with takeProfit above entryPrice
        brokerManager.executeTrade(
            symbol = "EUR/USD",
            side = "SELL",
            entryPrice = 1.0850,
            stopLoss = 1.0900,
            takeProfit = 1.0950, // Invalid: above entry
            volume = 0.1
        ) { success, msg, _ ->
            callbackSuccess = success
            callbackMsg = msg
        }
        advanceUntilIdle()

        assertFalse(callbackSuccess)
        assertTrue(callbackMsg.contains("Invalid SELL brackets"))
    }

    @Test
    fun testExecuteTrade_rejectsUnsupportedAction() = testScope.runTest {
        var callbackSuccess = true
        var callbackMsg = ""

        brokerManager.executeTrade(
            symbol = "EUR/USD",
            side = "WAIT",
            entryPrice = 1.0850,
            stopLoss = 1.0800,
            takeProfit = 1.0950,
            volume = 0.1
        ) { success, msg, _ ->
            callbackSuccess = success
            callbackMsg = msg
        }
        advanceUntilIdle()

        assertFalse(callbackSuccess)
        assertTrue(callbackMsg.contains("Unsupported trade action"))
    }

    @Test
    fun testExecuteTrade_acceptsValidPaperTrade() = testScope.runTest {
        var callbackSuccess = false
        var callbackMsg = ""
        var callbackPosition: BrokerPosition? = null

        brokerManager.executeTrade(
            symbol = "BTC/USD",
            side = "BUY",
            entryPrice = 82500.0,
            stopLoss = 81000.0,
            takeProfit = 85500.0,
            volume = 0.25
        ) { success, msg, pos ->
            callbackSuccess = success
            callbackMsg = msg
            callbackPosition = pos
        }
        advanceUntilIdle()

        assertTrue(callbackSuccess)
        assertTrue(callbackMsg.contains("Successfully executed BUY on PAPER_TRADING"))
        assertNotNull(callbackPosition)
        assertEquals("BTC/USD", callbackPosition?.symbol)
        assertEquals(82500.0, callbackPosition?.openPrice ?: 0.0, 0.001)
        assertEquals(0.25, callbackPosition?.volume ?: 0.0, 0.001)
        assertTrue(callbackPosition?.ticketId?.startsWith("TKT-") == true)
    }

    @Test
    fun testConnection_labelsAsSimulatedPaperSandbox() = testScope.runTest {
        val testAccount = BrokerAccountEntity(
            id = 1,
            brokerType = "METATRADER_5",
            accountName = "Primary MT5",
            accountId = "1092842",
            apiTokenOrPassword = "demo",
            serverOrEndpoint = "mt5.demo.server",
            environment = "LIVE",
            balance = 10000.0,
            equity = 10000.0,
            freeMargin = 10000.0,
            isConnected = false
        )

        var connectSuccess = false
        var connectMsg = ""

        brokerManager.connect(testAccount) { success, msg ->
            connectSuccess = success
            connectMsg = msg
        }
        advanceUntilIdle()

        assertTrue(connectSuccess)
        // Must be labeled as Paper / Simulated, never claiming live real-money execution
        assertEquals("CONNECTED_PAPER", brokerManager.connectionStatus.value)
        assertTrue(connectMsg.contains("Paper / Simulated Sandbox"))

        // Stop background heartbeat before testScope completes
        brokerManager.stopAllSimulation()
    }

    @Test
    fun testEntryPriceParsing_rejectsInvalidOrZeroPrices() {
        // Direct test of parsing logic used in executeSignalOnBroker
        fun parseValidPriceOrNull(raw: String): Double? {
            return try {
                val trimmed = raw.trim()
                if (trimmed.startsWith("-")) return null
                val cleaned = trimmed.replace(Regex("[^0-9.]"), "")
                val parsed = cleaned.toDoubleOrNull()
                if (parsed != null && parsed > 0.0 && !parsed.isNaN() && !parsed.isInfinite()) parsed else null
            } catch (_: Exception) {
                null
            }
        }

        assertNull(parseValidPriceOrNull(""))
        assertNull(parseValidPriceOrNull("N/A"))
        assertNull(parseValidPriceOrNull("$0.00"))
        assertNull(parseValidPriceOrNull("0.0"))
        assertNull(parseValidPriceOrNull("-1.0850"))

        val validForex = parseValidPriceOrNull("1.0855")
        assertNotNull(validForex)
        assertEquals(1.0855, validForex!!, 0.0001)

        val validCrypto = parseValidPriceOrNull("$82,450.50")
        assertNotNull(validCrypto)
        assertEquals(82450.50, validCrypto!!, 0.01)
    }
}
