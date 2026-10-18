package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TradingSignalDao {
    @Query("SELECT * FROM trading_signals ORDER BY timestamp DESC")
    fun getAllSignals(): Flow<List<TradingSignalEntity>>

    @Query("SELECT * FROM trading_signals WHERE id = :id")
    suspend fun getSignalById(id: Long): TradingSignalEntity?

    @Query("SELECT * FROM trading_signals WHERE symbol LIKE '%' || :query || '%' OR action LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchSignals(query: String): Flow<List<TradingSignalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignal(signal: TradingSignalEntity): Long

    @Update
    suspend fun updateSignal(signal: TradingSignalEntity)

    @Query("DELETE FROM trading_signals WHERE id = :id")
    suspend fun deleteSignalById(id: Long)

    @Query("SELECT COUNT(*) FROM trading_signals")
    fun getSignalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM trading_signals")
    suspend fun getSignalCountSync(): Int
}
