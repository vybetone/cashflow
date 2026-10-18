package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SignalRepository(
    private val signalEngine: SignalEngine = SignalEngine()
) {
    private val _latestHighConfidenceSignal = MutableStateFlow<TradingSignal?>(
        // Default high confidence signal on initialization
        signalEngine.generateSignal("BTC/USD")
    )
    val latestHighConfidenceSignal: StateFlow<TradingSignal?> = _latestHighConfidenceSignal.asStateFlow()

    fun generateSignal(symbol: String = "BTC/USD", priceHistory: List<Double>? = null): TradingSignal {
        val signal = signalEngine.generateSignal(symbol, priceHistory)
        _latestHighConfidenceSignal.value = signal
        return signal
    }

    fun getEngine(): SignalEngine = signalEngine
}
