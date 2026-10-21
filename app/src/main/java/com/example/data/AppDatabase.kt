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
        BrokerAccountEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tradingSignalDao(): TradingSignalDao
    abstract fun tradeJournalDao(): TradeJournalDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun brokerAccountDao(): BrokerAccountDao

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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cashflow_database"
                )
                .addMigrations(MIGRATION_4_5)
                .fallbackToDestructiveMigrationFrom(true, 1, 2, 3)
                .fallbackToDestructiveMigrationOnDowngrade(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
