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

/**
 * Distinguishes observation fidelity to ensure data provenance and prevent false assumptions.
 */
enum class ObservationSource {
    REAL_MARKET,        // Genuine live price feed telemetry
    SIMULATED,          // Simulated paper engine backtest / shadow execution
    CONFIRMED_BROKER    // Confirmed fills from broker API
}

/**
 * Status of an automated or human-approved strategy improvement proposal.
 */
enum class ProposalStatus {
    PROPOSED,
    TESTING_SHADOW,
    APPROVED,
    REJECTED,
    ROLLED_BACK
}

/**
 * Records individual signal performance, telemetry snapshot, and subsequent realized outcome.
 */
@Entity(tableName = "signal_performance_outcomes")
data class SignalPerformanceOutcomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val symbol: String,
    val timeframe: String = "M15",
    val marketCategory: String, // FOREX, GOLD, CRYPTO, STOCKS
    val signalAction: String, // BUY, SELL, WAIT
    val confidenceScore: Int,
    val entryPrice: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val riskRewardRatio: Double,
    // Market conditions snapshot at signal time
    val rsiValue: Double = 0.0,
    val macdSignal: String = "",
    val trendDirection: String = "",
    val candlePattern: String = "",
    val volatilityRegime: String = "NORMAL", // LOW, NORMAL, HIGH
    // Realized outcome
    val outcomeStatus: String = "PENDING", // PENDING, WIN, LOSS, CANCELLED, TIMEOUT
    val exitPrice: Double? = null,
    val exitTimestamp: Long? = null,
    val rawPnL: Double = 0.0,
    val simulatedSlippagePips: Double = 1.0,
    val simulatedCommissionUsd: Double = 3.0,
    val netPnLAfterFees: Double = 0.0,
    val accuracyScore: Double = 0.0, // 1.0 for WIN, 0.0 for LOSS
    val predictionErrorReason: String? = null,
    // Provenance & Versioning
    val observationSource: String = ObservationSource.REAL_MARKET.name,
    val modelVersion: String = "gemini-1.5-flash",
    val strategyVersion: String = "v1.0.0-baseline",
    val configVersion: Int = 1
)

/**
 * Tracks proposed algorithmic adjustments generated through empirical evaluation.
 */
@Entity(tableName = "improvement_proposals")
data class ImprovementProposalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val proposalTitle: String,
    val hypothesis: String,
    val candidateStrategyVersion: String,
    val targetMarket: String = "ALL_MARKETS",
    // Parameter adjustments proposed
    val minConfidenceThreshold: Int = 85,
    val rsiOversoldThreshold: Double = 35.0,
    val rsiOverboughtThreshold: Double = 65.0,
    val riskRewardMultiplier: Double = 2.8,
    // Backtest & Out-Of-Sample Results
    val outOfSampleSampleSize: Int = 0,
    val outOfSampleWinRate: Double = 0.0,
    val outOfSampleExpectancy: Double = 0.0,
    val outOfSampleProfitFactor: Double = 0.0,
    val outOfSampleMaxDrawdownPct: Double = 0.0,
    val baselineOutperformanceDelta: Double = 0.0,
    // Approval lifecycle
    val status: String = ProposalStatus.PROPOSED.name,
    val approvalNotes: String = "",
    val appliedTimestamp: Long? = null,
    val isAutoApproved: Boolean = false
)

/**
 * Secure, versioned remote configuration payload.
 */
data class RemoteConfigPayload(
    val version: Int,
    val timestamp: Long,
    val expiryTimestamp: Long,
    val geminiModelId: String = "gemini-1.5-flash",
    val scanIntervalSeconds: Int = 15,
    val minConfidenceThreshold: Int = 80,
    val maxOpenRiskPercentage: Double = 1.0,
    val promptTemplateVersion: String = "prompt-v2-confluence",
    val enableShadowTesting: Boolean = true,
    val strategyVersion: String = "v1.0.0-baseline",
    val payloadChecksumSha256: String = "",
    val signatureHex: String = ""
)

@Dao
interface ContinuousLearningDao {
    @Query("SELECT * FROM signal_performance_outcomes ORDER BY timestamp DESC")
    fun getAllOutcomes(): Flow<List<SignalPerformanceOutcomeEntity>>

    @Query("SELECT * FROM signal_performance_outcomes WHERE outcomeStatus != 'PENDING' ORDER BY timestamp ASC")
    suspend fun getChronologicalCompletedOutcomes(): List<SignalPerformanceOutcomeEntity>

    @Query("SELECT * FROM signal_performance_outcomes WHERE observationSource = :source ORDER BY timestamp DESC")
    fun getOutcomesBySource(source: String): Flow<List<SignalPerformanceOutcomeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutcome(outcome: SignalPerformanceOutcomeEntity): Long

    @Update
    suspend fun updateOutcome(outcome: SignalPerformanceOutcomeEntity)

    @Query("DELETE FROM signal_performance_outcomes WHERE timestamp < :cutoffTimestamp")
    suspend fun purgeOutcomesOlderThan(cutoffTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM signal_performance_outcomes")
    fun getTotalSignalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM signal_performance_outcomes WHERE outcomeStatus = 'WIN'")
    fun getWinCount(): Flow<Int>

    // Proposals
    @Query("SELECT * FROM improvement_proposals ORDER BY createdAt DESC")
    fun getAllProposals(): Flow<List<ImprovementProposalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProposal(proposal: ImprovementProposalEntity): Long

    @Update
    suspend fun updateProposal(proposal: ImprovementProposalEntity)

    @Query("SELECT * FROM improvement_proposals WHERE status = 'APPROVED' ORDER BY appliedTimestamp DESC LIMIT 1")
    suspend fun getLatestActiveProposal(): ImprovementProposalEntity?
}
