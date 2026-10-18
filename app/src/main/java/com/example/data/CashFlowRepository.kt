package com.example.data

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.FileOutputStream

class CashFlowRepository(
    private val tradingSignalDao: TradingSignalDao,
    private val tradeJournalDao: TradeJournalDao,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val brokerAccountDao: BrokerAccountDao
) {
    val allSignals: Flow<List<TradingSignalEntity>> = tradingSignalDao.getAllSignals()
    val allJournalTrades: Flow<List<TradeJournalEntity>> = tradeJournalDao.getAllTrades()
    val totalJournalPnL: Flow<Double?> = tradeJournalDao.getTotalPnL()
    val winCount: Flow<Int> = tradeJournalDao.getWinCount()
    val totalClosedTradesCount: Flow<Int> = tradeJournalDao.getTotalClosedTradesCount()

    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allBudgets: Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()
    val allBrokerAccounts: Flow<List<BrokerAccountEntity>> = brokerAccountDao.getAllAccounts()
    val activeBrokerAccount: Flow<BrokerAccountEntity?> = brokerAccountDao.getActiveConnectedAccount()

    private val geminiEngine = GeminiTradingEngine()

    suspend fun analyzeAndSaveSignal(
        bitmap: Bitmap,
        context: Context,
        source: String = "SCREEN_ANALYSIS",
        preferredModel: String = "gemini-3.5-flash",
        targetAsset: MarketAsset? = null
    ): TradingSignalEntity {
        val signal = geminiEngine.analyzeChartImage(bitmap, source, preferredModel, targetAsset)
        
        // Save image to internal storage
        val filename = "chart_${System.currentTimeMillis()}.jpg"
        val file = File(context.cacheDir, filename)
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val savedSignal = signal.copy(imageUri = file.absolutePath)
        val insertedId = tradingSignalDao.insertSignal(savedSignal)
        return savedSignal.copy(id = insertedId)
    }

    suspend fun analyzeMarketQuoteAndSaveSignal(
        quote: GenuineMarketQuote,
        userInstructions: String? = null,
        preferredModel: String = "gemini-3.5-flash",
        brokerContext: String? = null
    ): TradingSignalEntity {
        val signal = geminiEngine.analyzeMarketDataWithAi(quote, userInstructions, preferredModel, brokerContext)
        val insertedId = tradingSignalDao.insertSignal(signal)
        return signal.copy(id = insertedId)
    }

    suspend fun chatWithAssistant(
        quote: GenuineMarketQuote,
        history: List<AssistantChatMessage>,
        userMessage: String,
        preferredModel: String = "gemini-3.5-flash"
    ): String {
        return geminiEngine.chatWithTradingAssistant(quote, history, userMessage, preferredModel)
    }

    suspend fun insertSignal(signal: TradingSignalEntity): Long = tradingSignalDao.insertSignal(signal)
    suspend fun updateSignal(signal: TradingSignalEntity) = tradingSignalDao.updateSignal(signal)
    suspend fun deleteSignal(id: Long) = tradingSignalDao.deleteSignalById(id)

    suspend fun insertJournalTrade(trade: TradeJournalEntity): Long = tradeJournalDao.insertTrade(trade)
    suspend fun updateJournalTrade(trade: TradeJournalEntity) = tradeJournalDao.updateTrade(trade)
    suspend fun deleteJournalTrade(id: Long) = tradeJournalDao.deleteTradeById(id)

    suspend fun insertTransaction(transaction: TransactionEntity) = transactionDao.insertTransaction(transaction)
    suspend fun updateTransaction(transaction: TransactionEntity) = transactionDao.updateTransaction(transaction)
    suspend fun deleteTransaction(transaction: TransactionEntity) = transactionDao.deleteTransaction(transaction)

    suspend fun saveBudget(budget: BudgetEntity) = budgetDao.insertOrUpdateBudget(budget)

    suspend fun insertBrokerAccount(account: BrokerAccountEntity): Long = brokerAccountDao.insertAccount(account)
    suspend fun updateBrokerAccount(account: BrokerAccountEntity) = brokerAccountDao.updateAccount(account)
    suspend fun deleteBrokerAccount(account: BrokerAccountEntity) = brokerAccountDao.deleteAccount(account)
    suspend fun disconnectAllBrokers() = brokerAccountDao.disconnectAll()
    suspend fun setActiveBroker(id: Long) {
        brokerAccountDao.disconnectAll()
        brokerAccountDao.setActiveConnected(id)
    }

    suspend fun seedInitialDataIfNeeded() {
        seedBrokerAccountsIfEmpty()

        if (tradingSignalDao.getSignalCountSync() == 0) {
            seedTradingSignalsAndJournal()
        }

        if (transactionDao.getTransactionCount() == 0) {
            val now = System.currentTimeMillis()
            val day = 86400000L

            val defaultTransactions = listOf(
                TransactionEntity(
                    title = "Trading Account Deposit",
                    amount = 10000.00,
                    type = TransactionType.INCOME,
                    category = TransactionCategory.INVESTMENT,
                    dateMillis = now - (day * 1),
                    note = "Initial capital allocation for AI algorithmic execution",
                    isRecurring = false
                ),
                TransactionEntity(
                    title = "Bloomberg Terminal Subscription",
                    amount = 250.00,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.UTILITIES,
                    dateMillis = now - (day * 2),
                    note = "Market data & news feed"
                )
            )

            defaultTransactions.forEach { transactionDao.insertTransaction(it) }
        }

        if (budgetDao.getBudgetCount() == 0) {
            val defaultBudgets = listOf(
                BudgetEntity(category = TransactionCategory.INVESTMENT, monthlyLimit = 15000.0),
                BudgetEntity(category = TransactionCategory.UTILITIES, monthlyLimit = 500.0)
            )
            defaultBudgets.forEach { budgetDao.insertOrUpdateBudget(it) }
        }

        // Seed initial trading signals if empty
        seedTradingSignalsAndJournal()
    }

    private suspend fun seedTradingSignalsAndJournal() {
        val now = System.currentTimeMillis()
        val hour = 3600000L

        val initialSignals = listOf(
            // 1. FOREX
            TradingSignalEntity(
                timestamp = now - (hour * 2),
                symbol = "EUR/USD",
                timeframe = "15m",
                action = "BUY",
                confidenceScore = 91,
                reasoning = "[1. FOREX] Bullish order block defense at 1.0850 followed by 20/50 EMA golden cross. Clean liquidity grab beneath Asian session lows with strong European session expansion.",
                entryZone = "1.0862 - 1.0870",
                stopLoss = "1.0835 (-27 pips)",
                takeProfit = "1.0945 (+83 pips)",
                riskRewardRatio = "1:3.1",
                trendDirection = "BULLISH",
                detectedPatterns = "Bullish Order Block, Asian Low Sweep, 20 EMA Bounce",
                keyLevels = "Demand: 1.0848 | Resistance: 1.0940 | Daily Pivot: 1.0860",
                source = "SCREEN_ANALYSIS",
                tradeOutcome = "WIN",
                actualProfitLoss = 415.00
            ),
            TradingSignalEntity(
                timestamp = now - (hour * 5),
                symbol = "GBP/USD",
                timeframe = "30m",
                action = "BUY",
                confidenceScore = 89,
                reasoning = "[1. FOREX] Clean bullish flag breakout above 1.3030 resistance. RSI holding above 58 with expanding volume on Cable impulse candle.",
                entryZone = "1.3038 - 1.3045",
                stopLoss = "1.3005 (-33 pips)",
                takeProfit = "1.3125 (+87 pips)",
                riskRewardRatio = "1:2.6",
                trendDirection = "BULLISH",
                detectedPatterns = "Bull Flag Breakout, Trend Continuation, RSI Momentum",
                keyLevels = "Support: 1.3000 | Target 1: 1.3100 | Target 2: 1.3130",
                source = "SCREEN_ANALYSIS",
                tradeOutcome = "WIN",
                actualProfitLoss = 520.00
            ),
            TradingSignalEntity(
                timestamp = now - (hour * 7),
                symbol = "USD/JPY",
                timeframe = "1h",
                action = "SELL",
                confidenceScore = 87,
                reasoning = "[1. FOREX] Rejection at key psychological resistance 153.20. Bearish MACD divergence on hourly chart with institutional supply holding.",
                entryZone = "152.90 - 153.10",
                stopLoss = "153.45 (+45 pips)",
                takeProfit = "151.80 (-110 pips)",
                riskRewardRatio = "1:2.4",
                trendDirection = "BEARISH",
                detectedPatterns = "MACD Bearish Divergence, Supply Rejection, Pinbar",
                keyLevels = "Supply: 153.25 | Demand: 151.75 | 200 EMA: 152.10",
                source = "SCREEN_ANALYSIS",
                tradeOutcome = "PENDING",
                actualProfitLoss = 0.0
            ),
            // 2. GOLD
            TradingSignalEntity(
                timestamp = now - (hour * 1),
                symbol = "XAU/USD",
                timeframe = "15m",
                action = "BUY",
                confidenceScore = 95,
                reasoning = "[2. GOLD] Major safe haven accumulation bounce off $2,648 demand zone. 200 EMA support held on high volume with institutional absorption.",
                entryZone = "$2,654 - $2,658",
                stopLoss = "$2,638 (-$16)",
                takeProfit = "$2,705 (+$47)",
                riskRewardRatio = "1:2.9",
                trendDirection = "BULLISH",
                detectedPatterns = "Double Bottom, Institutional Absorption, 200 EMA Hold",
                keyLevels = "Support: $2,638 | Resistance: $2,700 | All-Time Zone: $2,720",
                source = "SCREEN_ANALYSIS",
                tradeOutcome = "WIN",
                actualProfitLoss = 1850.00
            ),
            // 3. CRYPTO
            TradingSignalEntity(
                timestamp = now - (hour * 3),
                symbol = "BTC/USD",
                timeframe = "15m",
                action = "BUY",
                confidenceScore = 93,
                reasoning = "[3. CRYPTO] High probability liquidity sweep below $64,800 followed by strong bullish engulfing expansion candle on spot volume surge.",
                entryZone = "$65,200 - $65,500",
                stopLoss = "$64,300 (-1.4%)",
                takeProfit = "$67,800 (+3.7%)",
                riskRewardRatio = "1:2.6",
                trendDirection = "BULLISH",
                detectedPatterns = "Liquidity Sweep, Bullish Engulfing, Volume Surge",
                keyLevels = "Support: $64,300 | Resistance: $68,000 | Key Pivot: $65,000",
                source = "SCREEN_ANALYSIS",
                tradeOutcome = "WIN",
                actualProfitLoss = 1240.00
            ),
            TradingSignalEntity(
                timestamp = now - (hour * 6),
                symbol = "ETH/USD",
                timeframe = "1h",
                action = "BUY",
                confidenceScore = 88,
                reasoning = "[3. CRYPTO] Ethereum holding multi-day ascending trendline with RSI reset above 50. Bullish continuation wedge formation.",
                entryZone = "$2,635 - $2,650",
                stopLoss = "$2,580 (-2.2%)",
                takeProfit = "$2,790 (+5.5%)",
                riskRewardRatio = "1:2.5",
                trendDirection = "BULLISH",
                detectedPatterns = "Ascending Trendline, Falling Wedge Breakout, Volume Hold",
                keyLevels = "Support: $2,580 | Resistance: $2,800 | 50 EMA: $2,620",
                source = "SCREEN_ANALYSIS",
                tradeOutcome = "PENDING",
                actualProfitLoss = 0.0
            ),
            // 4. STOCKS
            TradingSignalEntity(
                timestamp = now - (hour * 4),
                symbol = "NVDA",
                timeframe = "1h",
                action = "BUY",
                confidenceScore = 92,
                reasoning = "[4. STOCKS] Classic 1h Double Bottom formation resting on key 200 EMA support. Bullish MACD crossover with expanding green histogram bars.",
                entryZone = "$133.50 - $134.80",
                stopLoss = "$131.00 (-2.2%)",
                takeProfit = "$142.50 (+6.0%)",
                riskRewardRatio = "1:2.7",
                trendDirection = "BULLISH",
                detectedPatterns = "Double Bottom, 200 EMA Support, MACD Crossover",
                keyLevels = "Support: $131.00 | Resistance: $143.00 | Pivot: $134.00",
                source = "CAMERA_ANALYSIS",
                tradeOutcome = "WIN",
                actualProfitLoss = 1420.50
            ),
            TradingSignalEntity(
                timestamp = now - (hour * 9),
                symbol = "TSLA",
                timeframe = "30m",
                action = "BUY",
                confidenceScore = 90,
                reasoning = "[4. STOCKS] High-momentum breakout above $245 resistance shelf. Massive retail & institutional call option flow confirming upward surge.",
                entryZone = "$247.00 - $249.00",
                stopLoss = "$241.50 (-2.6%)",
                takeProfit = "$264.00 (+6.5%)",
                riskRewardRatio = "1:2.5",
                trendDirection = "BULLISH",
                detectedPatterns = "Resistance Breakout, High Beta Momentum, Volume Expansion",
                keyLevels = "Support: $241.50 | Target 1: $260.00 | Target 2: $265.00",
                source = "SCREEN_ANALYSIS",
                tradeOutcome = "PENDING",
                actualProfitLoss = 0.0
            ),
            TradingSignalEntity(
                timestamp = now - (hour * 11),
                symbol = "AAPL",
                timeframe = "1h",
                action = "BUY",
                confidenceScore = 86,
                reasoning = "[4. STOCKS] Symmetrical triangle breakout towards $234 level with 50 EMA golden cross over 100 EMA.",
                entryZone = "$230.50 - $231.80",
                stopLoss = "$227.00 (-1.7%)",
                takeProfit = "$239.00 (+3.5%)",
                riskRewardRatio = "1:2.1",
                trendDirection = "BULLISH",
                detectedPatterns = "Triangle Breakout, 50/100 EMA Golden Cross",
                keyLevels = "Support: $227.00 | Resistance: $240.00 | Volume Shelf: $231.00",
                source = "SCREEN_ANALYSIS",
                tradeOutcome = "WIN",
                actualProfitLoss = 780.00
            )
        )

        initialSignals.forEach { tradingSignalDao.insertSignal(it) }
    }

    private suspend fun seedBrokerAccountsIfEmpty() {
        // Only seed default broker templates if none exist
        val initialAccounts = listOf(
            BrokerAccountEntity(
                brokerType = "DERIV",
                accountName = "Deriv Official (deriv.com)",
                accountId = "CR-194821",
                apiTokenOrPassword = "••••••••••••••••",
                serverOrEndpoint = "wss://ws.derivws.com/websockets/v3",
                environment = "DEMO",
                currency = "USD",
                balance = 10000.0,
                equity = 10018.0,
                freeMargin = 9950.0,
                isConnected = false,
                lastPingMs = 24L,
                autoTradeEnabled = false,
                minConfidenceThreshold = 80,
                defaultLotSize = 0.50
            ),
            BrokerAccountEntity(
                brokerType = "METATRADER_5",
                accountName = "MetaTrader 5 (MT5 Live/Demo)",
                accountId = "2094812",
                apiTokenOrPassword = "••••••••••••••••",
                serverOrEndpoint = "MetaQuotes-Demo",
                environment = "DEMO",
                currency = "USD",
                balance = 25000.0,
                equity = 25017.0,
                freeMargin = 24800.0,
                isConnected = false,
                lastPingMs = 32L,
                autoTradeEnabled = false,
                minConfidenceThreshold = 85,
                defaultLotSize = 0.10
            ),
            BrokerAccountEntity(
                brokerType = "INTERACTIVE_BROKERS",
                accountName = "Interactive Brokers (IBKR)",
                accountId = "U1849204",
                apiTokenOrPassword = "••••••••••••••••",
                serverOrEndpoint = "https://127.0.0.1:5000/v1/api",
                environment = "DEMO",
                currency = "USD",
                balance = 50000.0,
                equity = 50000.0,
                freeMargin = 50000.0,
                isConnected = false,
                lastPingMs = 18L,
                autoTradeEnabled = false,
                minConfidenceThreshold = 80,
                defaultLotSize = 1.00
            ),
            BrokerAccountEntity(
                brokerType = "OANDA",
                accountName = "OANDA (v20 REST API)",
                accountId = "001-004-9843-001",
                apiTokenOrPassword = "••••••••••••••••",
                serverOrEndpoint = "https://api-fxpractice.oanda.com/v3",
                environment = "DEMO",
                currency = "USD",
                balance = 10000.0,
                equity = 10000.0,
                freeMargin = 10000.0,
                isConnected = false,
                lastPingMs = 22L,
                autoTradeEnabled = false,
                minConfidenceThreshold = 80,
                defaultLotSize = 0.05
            ),
            BrokerAccountEntity(
                brokerType = "PAPER_TRADING",
                accountName = "CashFlow Paper Engine",
                accountId = "VIRTUAL-QUANT-01",
                apiTokenOrPassword = "INTERNAL_SANDBOX",
                serverOrEndpoint = "Local High-Speed Virtual Engine",
                environment = "DEMO",
                currency = "USD",
                balance = 100000.0,
                equity = 100035.0,
                freeMargin = 99750.0,
                isConnected = true,
                lastPingMs = 4L,
                autoTradeEnabled = true,
                minConfidenceThreshold = 80,
                defaultLotSize = 0.10
            )
        )

        initialAccounts.forEach { brokerAccountDao.insertAccount(it) }
    }
}

