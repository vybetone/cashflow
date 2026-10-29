package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        TradingSignalEntity::class,
        TradeJournalEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        BrokerAccountEntity::class,
        SignalPerformanceOutcomeEntity::class,
        ImprovementProposalEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tradingSignalDao(): TradingSignalDao
    abstract fun tradeJournalDao(): TradeJournalDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun brokerAccountDao(): BrokerAccountDao
    abstract fun continuousLearningDao(): ContinuousLearningDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE broker_accounts ADD COLUMN dailyLossLimit REAL NOT NULL DEFAULT 500.0")
                db.execSQL("ALTER TABLE broker_accounts ADD COLUMN dailyRealizedLoss REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE broker_accounts ADD COLUMN dailyLossResetTimestamp INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE broker_accounts ADD COLUMN isCircuitBreakerTripped INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS signal_performance_outcomes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        symbol TEXT NOT NULL,
                        timeframe TEXT NOT NULL,
                        marketCategory TEXT NOT NULL,
                        signalAction TEXT NOT NULL,
                        confidenceScore INTEGER NOT NULL,
                        entryPrice REAL NOT NULL,
                        stopLoss REAL NOT NULL,
                        takeProfit REAL NOT NULL,
                        riskRewardRatio REAL NOT NULL,
                        rsiValue REAL NOT NULL,
                        macdSignal TEXT NOT NULL,
                        trendDirection TEXT NOT NULL,
                        candlePattern TEXT NOT NULL,
                        volatilityRegime TEXT NOT NULL,
                        outcomeStatus TEXT NOT NULL,
                        exitPrice REAL,
                        exitTimestamp INTEGER,
                        rawPnL REAL NOT NULL,
                        simulatedSlippagePips REAL NOT NULL,
                        simulatedCommissionUsd REAL NOT NULL,
                        netPnLAfterFees REAL NOT NULL,
                        accuracyScore REAL NOT NULL,
                        predictionErrorReason TEXT,
                        observationSource TEXT NOT NULL,
                        modelVersion TEXT NOT NULL,
                        strategyVersion TEXT NOT NULL,
                        configVersion INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS improvement_proposals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        proposalTitle TEXT NOT NULL,
                        hypothesis TEXT NOT NULL,
                        candidateStrategyVersion TEXT NOT NULL,
                        targetMarket TEXT NOT NULL,
                        minConfidenceThreshold INTEGER NOT NULL,
                        rsiOversoldThreshold REAL NOT NULL,
                        rsiOverboughtThreshold REAL NOT NULL,
                        riskRewardMultiplier REAL NOT NULL,
                        outOfSampleSampleSize INTEGER NOT NULL,
                        outOfSampleWinRate REAL NOT NULL,
                        outOfSampleExpectancy REAL NOT NULL,
                        outOfSampleProfitFactor REAL NOT NULL,
                        outOfSampleMaxDrawdownPct REAL NOT NULL,
                        baselineOutperformanceDelta REAL NOT NULL,
                        status TEXT NOT NULL,
                        approvalNotes TEXT NOT NULL,
                        appliedTimestamp INTEGER,
                        isAutoApproved INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cashflow_database"
                )
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                .fallbackToDestructiveMigrationFrom(true, 1, 2, 3)
                .fallbackToDestructiveMigrationOnDowngrade(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
