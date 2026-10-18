package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

enum class BrokerType(val displayName: String, val website: String, val defaultServer: String) {
    DERIV("Deriv", "https://deriv.com", "wss://ws.derivws.com/websockets/v3"),
    METATRADER_5("MetaTrader 5 (MT5)", "https://www.metatrader5.com", "MetaQuotes-Demo"),
    INTERACTIVE_BROKERS("Interactive Brokers", "https://www.interactivebrokers.com", "https://127.0.0.1:5000/v1/api"),
    OANDA("OANDA (v20)", "https://www.oanda.com", "https://api-fxpractice.oanda.com/v3"),
    PAPER_TRADING("CashFlow Paper Broker", "Built-in Virtual Engine", "Virtual In-Memory Sandbox")
}

@Entity(tableName = "broker_accounts")
data class BrokerAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val brokerType: String, // DERIV, METATRADER_5, INTERACTIVE_BROKERS, OANDA, PAPER_TRADING
    val accountName: String,
    val accountId: String,
    val apiTokenOrPassword: String,
    val serverOrEndpoint: String,
    val environment: String = "DEMO", // "LIVE" or "DEMO"
    val currency: String = "USD",
    val balance: Double = 10000.0,
    val equity: Double = 10000.0,
    val freeMargin: Double = 10000.0,
    val isConnected: Boolean = false,
    val lastPingMs: Long = 28L,
    val autoTradeEnabled: Boolean = false,
    val minConfidenceThreshold: Int = 80,
    val defaultLotSize: Double = 0.05,
    val maxRiskPercentage: Double = 1.0,
    val autoStopLossTakeProfit: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class BrokerPosition(
    val ticketId: String,
    val brokerType: String,
    val accountId: String,
    val symbol: String,
    val side: String, // "BUY" or "SELL"
    val volume: Double,
    val openPrice: Double,
    val currentPrice: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val unrealizedPnL: Double,
    val openTime: Long = System.currentTimeMillis()
)

data class BrokerExecutionLog(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val brokerType: String,
    val action: String, // "ORDER_OPENED", "ORDER_CLOSED", "PING", "CONNECT", "DISCONNECT"
    val message: String,
    val success: Boolean = true
)

@Dao
interface BrokerAccountDao {
    @Query("SELECT * FROM broker_accounts ORDER BY id ASC")
    fun getAllAccounts(): Flow<List<BrokerAccountEntity>>

    @Query("SELECT * FROM broker_accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Long): BrokerAccountEntity?

    @Query("SELECT * FROM broker_accounts WHERE isConnected = 1 LIMIT 1")
    fun getActiveConnectedAccount(): Flow<BrokerAccountEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: BrokerAccountEntity): Long

    @Update
    suspend fun updateAccount(account: BrokerAccountEntity)

    @Delete
    suspend fun deleteAccount(account: BrokerAccountEntity)

    @Query("UPDATE broker_accounts SET isConnected = 0")
    suspend fun disconnectAll()

    @Query("UPDATE broker_accounts SET isConnected = 1 WHERE id = :id")
    suspend fun setActiveConnected(id: Long)
}
