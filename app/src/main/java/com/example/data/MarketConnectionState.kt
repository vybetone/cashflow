package com.example.data

sealed class MarketConnectionState {
    data object Connecting : MarketConnectionState()
    data class Connected(
        val provider: String,
        val lastUpdatedTimestamp: Long,
        val isStale: Boolean = false
    ) : MarketConnectionState()
    data class Error(
        val provider: String,
        val errorMessage: String,
        val canRetry: Boolean = true
    ) : MarketConnectionState()
}
