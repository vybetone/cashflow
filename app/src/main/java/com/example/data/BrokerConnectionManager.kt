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
import java.util.UUID
import kotlin.math.abs
import kotlin.random.Random

class BrokerConnectionManager(
    private val scope: CoroutineScope
) {
    private val _connectedAccount = MutableStateFlow<BrokerAccountEntity?>(null)
    val connectedAccount: StateFlow<BrokerAccountEntity?> = _connectedAccount.asStateFlow()

    private val _connectionStatus = MutableStateFlow("DISCONNECTED") // "DISCONNECTED", "CONNECTING", "CONNECTED_LIVE", "CONNECTED_DEMO", "ERROR"
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _currentPingMs = MutableStateFlow(24L)
    val currentPingMs: StateFlow<Long> = _currentPingMs.asStateFlow()

    private val _openPositions = MutableStateFlow<List<BrokerPosition>>(emptyList())
    val openPositions: StateFlow<List<BrokerPosition>> = _openPositions.asStateFlow()

    private val _executionLogs = MutableStateFlow<List<BrokerExecutionLog>>(emptyList())
    val executionLogs: StateFlow<List<BrokerExecutionLog>> = _executionLogs.asStateFlow()

    var onAccountUpdated: ((BrokerAccountEntity) -> Unit)? = null

    private var heartbeatJob: Job? = null
    private var priceUpdateJob: Job? = null

    companion object {
        fun getUtcDayStartTimestamp(epochMillis: Long = System.currentTimeMillis()): Long {
            val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
            calendar.timeInMillis = epochMillis
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }

        fun checkAndRolloverDailyLoss(
            account: BrokerAccountEntity,
            currentTimeMillis: Long = System.currentTimeMillis()
        ): BrokerAccountEntity {
            val currentDayStart = getUtcDayStartTimestamp(currentTimeMillis)
            val accountDayStart = if (account.dailyLossResetTimestamp > 0L) {
                getUtcDayStartTimestamp(account.dailyLossResetTimestamp)
            } else {
                0L
            }

            return if (currentDayStart > accountDayStart) {
                account.copy(
                    dailyRealizedLoss = 0.0,
                    isCircuitBreakerTripped = false,
                    dailyLossResetTimestamp = currentDayStart
                )
            } else {
                account
            }
        }
    }

    init {
        // Initialize with default demo paper positions to illustrate
        val samplePositions = listOf(
            BrokerPosition(
                ticketId = "POS-91824",
                brokerType = "DERIV",
                accountId = "CR-194821",
                symbol = "Vol 75 Index",
                side = "BUY",
                volume = 0.5,
                openPrice = 428.50,
                currentPrice = 432.10,
                stopLoss = 420.00,
                takeProfit = 445.00,
                unrealizedPnL = 18.00,
                openTime = System.currentTimeMillis() - 1800000
            ),
            BrokerPosition(
                ticketId = "POS-73910",
                brokerType = "METATRADER_5",
                accountId = "2094812",
                symbol = "EUR/USD",
                side = "BUY",
                volume = 0.10,
                openPrice = 1.0845,
                currentPrice = 1.0862,
                stopLoss = 1.0810,
                takeProfit = 1.0920,
                unrealizedPnL = 17.00,
                openTime = System.currentTimeMillis() - 3600000
            )
        )
        _openPositions.value = samplePositions
        startMarketSimulation()
    }

    fun connect(account: BrokerAccountEntity, onComplete: (Boolean, String) -> Unit) {
        scope.launch {
            _connectionStatus.value = "CONNECTING"
            addLog(account.brokerType, "CONNECT", "Initiating handshake with ${account.brokerType} [${account.serverOrEndpoint}]...")

            delay(750) // Realistic connection latency

            val isDemo = account.environment.equals("DEMO", ignoreCase = true)
            val success = account.accountId.isNotBlank()

            if (success) {
                val rolled = checkAndRolloverDailyLoss(account)
                if (rolled != account) {
                    onAccountUpdated?.invoke(rolled)
                }
                _connectedAccount.value = rolled.copy(isConnected = true)
                _connectionStatus.value = if (isDemo) "CONNECTED_DEMO" else "CONNECTED_PAPER"
                _currentPingMs.value = Random.nextLong(18, 45)

                val msg = "Connected to ${account.brokerType} (Paper / Simulated Sandbox). Account ID: ${account.accountId}"
                addLog(account.brokerType, "CONNECT", msg, true)
                startHeartbeat(account.brokerType)
                onComplete(true, msg)
            } else {
                _connectionStatus.value = "ERROR"
                val err = "Authentication failed: Account ID or API Token cannot be empty."
                addLog(account.brokerType, "CONNECT", err, false)
                onComplete(false, err)
            }
        }
    }

    fun disconnect() {
        heartbeatJob?.cancel()
        priceUpdateJob?.cancel()
        val account = _connectedAccount.value
        if (account != null) {
            addLog(account.brokerType, "DISCONNECT", "Disconnected from ${account.brokerType} (${account.accountId})")
        }
        _connectedAccount.value = null
        _connectionStatus.value = "DISCONNECTED"
    }

    fun stopAllSimulation() {
        heartbeatJob?.cancel()
        priceUpdateJob?.cancel()
    }

    fun testPing(account: BrokerAccountEntity, onResult: (Long) -> Unit) {
        scope.launch {
            val start = System.currentTimeMillis()
            delay(Random.nextLong(20, 60))
            val ping = System.currentTimeMillis() - start
            _currentPingMs.value = ping
            addLog(account.brokerType, "PING", "Ping response from ${account.serverOrEndpoint}: ${ping}ms")
            onResult(ping)
        }
    }

    /**
     * Executes a trade on the currently connected or specified broker account.
     */
    fun executeTrade(
        symbol: String,
        side: String, // "BUY" or "SELL"
        entryPrice: Double,
        stopLoss: Double,
        takeProfit: Double,
        volume: Double = 0.05,
        sourceSignalId: Long? = null,
        onComplete: (Boolean, String, BrokerPosition?) -> Unit
    ) {
        scope.launch {
            val rawAccount = _connectedAccount.value
            val account = if (rawAccount != null) {
                val rolled = checkAndRolloverDailyLoss(rawAccount)
                if (rolled != rawAccount) {
                    _connectedAccount.value = rolled
                    onAccountUpdated?.invoke(rolled)
                }
                rolled
            } else null

            val brokerName = account?.brokerType ?: "PAPER_TRADING"
            val accountId = account?.accountId ?: "VIRTUAL-001"

            // Circuit Breaker: Enforce daily loss limit
            if (account != null && account.dailyLossLimit > 0.0) {
                val isTripped = account.isCircuitBreakerTripped || account.dailyRealizedLoss >= account.dailyLossLimit
                if (isTripped) {
                    val err = "Execution rejected: Daily-loss circuit breaker is active for $brokerName (Realized loss: $${String.format(java.util.Locale.US, "%.2f", account.dailyRealizedLoss)} >= Limit: $${String.format(java.util.Locale.US, "%.2f", account.dailyLossLimit)}). Trading halted for current session."
                    addLog(brokerName, "CIRCUIT_BREAKER_BLOCKED", err, false)
                    onComplete(false, err, null)
                    return@launch
                }
            }

            // 0. Duplicate order check within 5-second cooldown on same symbol and side
            val normalizedSide = side.trim().uppercase()
            val isDuplicate = _openPositions.value.any {
                it.symbol == symbol && it.side == normalizedSide && (System.currentTimeMillis() - it.openTime) < 5_000L
            }
            if (isDuplicate) {
                val err = "Execution rejected: Duplicate order detected on $symbol ($normalizedSide) within cooldown period."
                addLog(brokerName, "ORDER_REJECTED", err, false)
                onComplete(false, err, null)
                return@launch
            }

            // Check sufficient free margin if account is connected
            val requiredMargin = volume * 100.0
            if (account != null && account.freeMargin < requiredMargin) {
                val err = "Execution rejected: Insufficient free margin ($${String.format(java.util.Locale.US, "%.2f", account.freeMargin)} < required $${String.format(java.util.Locale.US, "%.2f", requiredMargin)})."
                addLog(brokerName, "ORDER_REJECTED", err, false)
                onComplete(false, err, null)
                return@launch
            }

            // 1. Validate entryPrice
            if (entryPrice <= 0.0 || entryPrice.isNaN() || entryPrice.isInfinite()) {
                val err = "Execution rejected: Invalid entry price ($entryPrice). Price must be positive and finite."
                addLog(brokerName, "ORDER_REJECTED", err, false)
                onComplete(false, err, null)
                return@launch
            }

            // 2. Validate volume
            if (volume <= 0.0 || volume.isNaN() || volume.isInfinite()) {
                val err = "Execution rejected: Invalid volume ($volume). Volume must be positive and finite."
                addLog(brokerName, "ORDER_REJECTED", err, false)
                onComplete(false, err, null)
                return@launch
            }

            // 3. Validate stop-loss and take-profit
            if (stopLoss <= 0.0 || stopLoss.isNaN() || stopLoss.isInfinite() ||
                takeProfit <= 0.0 || takeProfit.isNaN() || takeProfit.isInfinite()) {
                val err = "Execution rejected: Invalid stop-loss ($stopLoss) or take-profit ($takeProfit). Levels must be positive and finite."
                addLog(brokerName, "ORDER_REJECTED", err, false)
                onComplete(false, err, null)
                return@launch
            }

            // 4. Validate brackets per existing trade model
            if (normalizedSide == "BUY") {
                if (stopLoss >= entryPrice || takeProfit <= entryPrice) {
                    val err = "Execution rejected: Invalid BUY brackets. Stop-loss ($stopLoss) must be below entry ($entryPrice) and take-profit ($takeProfit) above entry."
                    addLog(brokerName, "ORDER_REJECTED", err, false)
                    onComplete(false, err, null)
                    return@launch
                }
            } else if (normalizedSide == "SELL") {
                if (stopLoss <= entryPrice || takeProfit >= entryPrice) {
                    val err = "Execution rejected: Invalid SELL brackets. Stop-loss ($stopLoss) must be above entry ($entryPrice) and take-profit ($takeProfit) below entry."
                    addLog(brokerName, "ORDER_REJECTED", err, false)
                    onComplete(false, err, null)
                    return@launch
                }
            } else {
                val err = "Execution rejected: Unsupported trade action ($side). Only BUY or SELL can be executed."
                addLog(brokerName, "ORDER_REJECTED", err, false)
                onComplete(false, err, null)
                return@launch
            }

            addLog(brokerName, "ORDER_REQUEST", "[PAPER] Routing $side $volume lots on $symbol @ $entryPrice (SL: $stopLoss, TP: $takeProfit)...")
            delay(350) // Network roundtrip to simulated paper engine

            val ticket = "TKT-${Random.nextInt(100000, 999999)}"
            val position = BrokerPosition(
                ticketId = ticket,
                brokerType = brokerName,
                accountId = accountId,
                symbol = symbol,
                side = side.uppercase(),
                volume = volume,
                openPrice = entryPrice,
                currentPrice = entryPrice,
                stopLoss = stopLoss,
                takeProfit = takeProfit,
                unrealizedPnL = 0.0,
                openTime = System.currentTimeMillis()
            )

            val updatedList = _openPositions.value.toMutableList()
            updatedList.add(0, position)
            _openPositions.value = updatedList

            // Update balance and equity if applicable
            account?.let { acc ->
                val newFreeMargin = (acc.freeMargin - (volume * 100)).coerceAtLeast(0.0)
                _connectedAccount.value = acc.copy(freeMargin = newFreeMargin)
            }

            val successMsg = "Successfully executed $side on $brokerName! Ticket: #$ticket at price $entryPrice"
            addLog(brokerName, "ORDER_OPENED", successMsg, true)
            onComplete(true, successMsg, position)
        }
    }

    fun closePosition(ticketId: String, onComplete: (Boolean, Double) -> Unit) {
        scope.launch {
            val pos = _openPositions.value.find { it.ticketId == ticketId }
            if (pos != null) {
                val rawPnl = pos.unrealizedPnL
                val pnl = if (rawPnl.isNaN() || rawPnl.isInfinite()) 0.0 else rawPnl
                val updated = _openPositions.value.filter { it.ticketId != ticketId }
                _openPositions.value = updated

                // Credit PnL to connected account and update daily-loss tracking
                _connectedAccount.value?.let { currentAcc ->
                    val acc = checkAndRolloverDailyLoss(currentAcc)
                    val newBalance = acc.balance + pnl
                    val newEquity = acc.equity + pnl
                    val newMargin = acc.freeMargin + (pos.volume * 100) + pnl

                    // Only realized losses increase dailyRealizedLoss; winning trades never erase prior losses
                    val lossIncrement = if (pnl < 0.0) abs(pnl) else 0.0
                    val updatedRealizedLoss = acc.dailyRealizedLoss + lossIncrement
                    val circuitBreakerTripped = acc.isCircuitBreakerTripped || (acc.dailyLossLimit > 0.0 && updatedRealizedLoss >= acc.dailyLossLimit)

                    val updatedAcc = acc.copy(
                        balance = newBalance,
                        equity = newEquity,
                        freeMargin = newMargin,
                        dailyRealizedLoss = updatedRealizedLoss,
                        isCircuitBreakerTripped = circuitBreakerTripped,
                        dailyLossResetTimestamp = if (acc.dailyLossResetTimestamp <= 0L) getUtcDayStartTimestamp() else acc.dailyLossResetTimestamp
                    )
                    _connectedAccount.value = updatedAcc
                    onAccountUpdated?.invoke(updatedAcc)

                    if (circuitBreakerTripped && !acc.isCircuitBreakerTripped) {
                        addLog(
                            acc.brokerType,
                            "CIRCUIT_BREAKER_TRIPPED",
                            "Daily-loss circuit breaker TRIPPED! Realized loss $${String.format(java.util.Locale.US, "%.2f", updatedRealizedLoss)} >= Limit $${String.format(java.util.Locale.US, "%.2f", acc.dailyLossLimit)}. New paper orders blocked.",
                            false
                        )
                    }
                }

                val pnlText = if (pnl >= 0) "+$${String.format(java.util.Locale.US, "%.2f", pnl)}" else "-$${String.format(java.util.Locale.US, "%.2f", abs(pnl))}"
                addLog(pos.brokerType, "ORDER_CLOSED", "Closed ticket #${pos.ticketId} (${pos.symbol}) with P&L: $pnlText", true)
                onComplete(true, pnl)
            } else {
                onComplete(false, 0.0)
            }
        }
    }

    fun updatePositionPnLForTesting(ticketId: String, pnl: Double) {
        val list = _openPositions.value.toMutableList()
        val idx = list.indexOfFirst { it.ticketId == ticketId }
        if (idx >= 0) {
            list[idx] = list[idx].copy(unrealizedPnL = pnl)
            _openPositions.value = list
        }
    }

    fun closeAllPositions(onComplete: (Int, Double) -> Unit) {
        scope.launch {
            val list = _openPositions.value
            val count = list.size
            if (count == 0) {
                onComplete(0, 0.0)
                return@launch
            }

            val totalPnL = list.sumOf {
                val p = it.unrealizedPnL
                if (p.isNaN() || p.isInfinite()) 0.0 else p
            }
            _openPositions.value = emptyList()

            _connectedAccount.value?.let { currentAcc ->
                val acc = checkAndRolloverDailyLoss(currentAcc)
                val totalLossIncrement = list.sumOf { pos ->
                    val p = pos.unrealizedPnL
                    val safeP = if (p.isNaN() || p.isInfinite()) 0.0 else p
                    if (safeP < 0.0) abs(safeP) else 0.0
                }
                val updatedRealizedLoss = acc.dailyRealizedLoss + totalLossIncrement
                val circuitBreakerTripped = acc.isCircuitBreakerTripped || (acc.dailyLossLimit > 0.0 && updatedRealizedLoss >= acc.dailyLossLimit)

                val updatedAcc = acc.copy(
                    balance = acc.balance + totalPnL,
                    equity = acc.equity + totalPnL,
                    freeMargin = acc.balance + totalPnL,
                    dailyRealizedLoss = updatedRealizedLoss,
                    isCircuitBreakerTripped = circuitBreakerTripped,
                    dailyLossResetTimestamp = if (acc.dailyLossResetTimestamp <= 0L) getUtcDayStartTimestamp() else acc.dailyLossResetTimestamp
                )
                _connectedAccount.value = updatedAcc
                onAccountUpdated?.invoke(updatedAcc)

                if (circuitBreakerTripped && !acc.isCircuitBreakerTripped) {
                    addLog(
                        acc.brokerType,
                        "CIRCUIT_BREAKER_TRIPPED",
                        "Daily-loss circuit breaker TRIPPED on Emergency Close All! Realized loss $${String.format(java.util.Locale.US, "%.2f", updatedRealizedLoss)} >= Limit $${String.format(java.util.Locale.US, "%.2f", acc.dailyLossLimit)}. New paper orders blocked.",
                        false
                    )
                }
            }

            addLog("BROKER", "CLOSE_ALL", "Emergency closed all $count open positions. Total PnL: $$totalPnL", true)
            onComplete(count, totalPnL)
        }
    }

    fun resetDailyLoss(reason: String = "Manual User Reset") {
        _connectedAccount.value?.let { acc ->
            val updated = acc.copy(
                dailyRealizedLoss = 0.0,
                isCircuitBreakerTripped = false,
                dailyLossResetTimestamp = getUtcDayStartTimestamp()
            )
            _connectedAccount.value = updated
            onAccountUpdated?.invoke(updated)
            addLog(
                acc.brokerType,
                "CIRCUIT_BREAKER_RESET",
                "Daily-loss circuit breaker reset ($reason). Previous realized loss: $${String.format(java.util.Locale.US, "%.2f", acc.dailyRealizedLoss)}.",
                true
            )
        }
    }

    private fun startHeartbeat(brokerType: String) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(4000)
                _currentPingMs.value = Random.nextLong(16, 42)
            }
        }
    }

    private fun startMarketSimulation() {
        priceUpdateJob?.cancel()
        priceUpdateJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(2000)
                val current = _openPositions.value
                if (current.isNotEmpty()) {
                    val updated = current.map { pos ->
                        val asset = MarketUniverse.findAssetBySymbol(pos.symbol)
                        // Realistic price fluctuation per asset category
                        val tickDelta = when (asset.category) {
                            MarketCategory.FOREX -> if (asset.decimalDigits == 2) Random.nextDouble(-0.04, 0.05) else Random.nextDouble(-0.0004, 0.0005)
                            MarketCategory.GOLD -> Random.nextDouble(-0.45, 0.55)
                            MarketCategory.CRYPTO -> Random.nextDouble(-15.0, 18.0)
                            MarketCategory.STOCKS -> Random.nextDouble(-0.25, 0.30)
                            MarketCategory.ALL_MARKETS -> Random.nextDouble(-0.05, 0.06)
                        }
                        val newPrice = (pos.currentPrice + tickDelta).coerceAtLeast(0.0001)
                        val priceDiff = if (pos.side == "BUY") newPrice - pos.openPrice else pos.openPrice - newPrice
                        val pnlMultiplier = when (asset.category) {
                            MarketCategory.FOREX -> if (asset.decimalDigits == 2) 1000.0 else 100000.0
                            MarketCategory.GOLD -> 100.0
                            MarketCategory.CRYPTO -> 1.0
                            MarketCategory.STOCKS -> 1.0
                            MarketCategory.ALL_MARKETS -> 50.0
                        }
                        val newPnL = priceDiff * pos.volume * pnlMultiplier
                        pos.copy(
                            currentPrice = (newPrice * 10000.0).toInt() / 10000.0,
                            unrealizedPnL = (newPnL * 100.0).toInt() / 100.0
                        )
                    }
                    _openPositions.value = updated

                    // Update account equity
                    _connectedAccount.value?.let { acc ->
                        val floatingPnL = updated.sumOf { it.unrealizedPnL }
                        _connectedAccount.value = acc.copy(
                            equity = acc.balance + floatingPnL
                        )
                    }
                }
            }
        }
    }

    private fun addLog(brokerType: String, action: String, message: String, success: Boolean = true) {
        val newLog = BrokerExecutionLog(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            brokerType = brokerType,
            action = action,
            message = message,
            success = success
        )
        val logs = _executionLogs.value.toMutableList()
        logs.add(0, newLog)
        if (logs.size > 50) logs.removeAt(logs.size - 1)
        _executionLogs.value = logs
    }
}
