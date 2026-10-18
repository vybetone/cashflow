package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeJournalDao {
    @Query("SELECT * FROM trade_journal ORDER BY timestamp DESC")
    fun getAllTrades(): Flow<List<TradeJournalEntity>>

    @Query("SELECT * FROM trade_journal WHERE id = :id")
    suspend fun getTradeById(id: Long): TradeJournalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: TradeJournalEntity): Long

    @Update
    suspend fun updateTrade(trade: TradeJournalEntity)

    @Query("DELETE FROM trade_journal WHERE id = :id")
    suspend fun deleteTradeById(id: Long)

    @Query("SELECT SUM(pnl) FROM trade_journal WHERE status LIKE 'CLOSED%'")
    fun getTotalPnL(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM trade_journal WHERE status = 'CLOSED_WIN'")
    fun getWinCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM trade_journal WHERE status LIKE 'CLOSED%'")
    fun getTotalClosedTradesCount(): Flow<Int>
}
