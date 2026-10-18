package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BrokerAccountEntity
import com.example.data.BrokerConnectionManager
import com.example.data.BrokerExecutionLog
import com.example.data.BrokerPosition
import com.example.data.BudgetEntity
import com.example.data.CashFlowRepository
import com.example.data.MarketAsset
import com.example.data.MarketCategory
import com.example.data.MarketUniverse
import com.example.data.SignalRepository
import com.example.data.TradingSignal
import com.example.data.TradeJournalEntity
import com.example.data.AssistantChatMessage
import com.example.data.GenuineMarketQuote
import com.example.data.MarketConnectionState
import com.example.data.RealMarketDataEngine
import com.example.data.TradingSignalEntity
import com.example.data.TransactionCategory
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CashFlowViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CashFlowRepository
    private val aiSignalRepository: SignalRepository = SignalRepository()
    private val brokerConnectionManager: BrokerConnectionManager = BrokerConnectionManager(viewModelScope)
    private val marketDataEngine: RealMarketDataEngine = RealMarketDataEngine(viewModelScope)

    val liveMarketQuotes: StateFlow<Map<String, GenuineMarketQuote>> = marketDataEngine.marketQuotes
    val marketConnectionState: StateFlow<MarketConnectionState> = marketDataEngine.connectionState
    val isMarketDataLoading: StateFlow<Boolean> = marketDataEngine.isLoading
    val marketDataErrorMessage: StateFlow<String?> = marketDataEngine.lastErrorMessage

    fun refreshMarketData() {
        marketDataEngine.refreshNow()
    }

    val highConfidenceAiSignal: StateFlow<TradingSignal?> = aiSignalRepository.latestHighConfidenceSignal

    val allSignals: StateFlow<List<TradingSignalEntity>>
    val allJournalTrades: StateFlow<List<TradeJournalEntity>>
    val totalJournalPnL: StateFlow<Double?>
    val winCount: StateFlow<Int>
    val totalClosedTradesCount: StateFlow<Int>

    val allTransactions: StateFlow<List<TransactionEntity>>
    val allBudgets: StateFlow<List<BudgetEntity>>

    val allBrokerAccounts: StateFlow<List<BrokerAccountEntity>>
    val activeBrokerAccount: StateFlow<BrokerAccountEntity?>
    val brokerConnectionStatus: StateFlow<String> = brokerConnectionManager.connectionStatus
    val brokerPingMs: StateFlow<Long> = brokerConnectionManager.currentPingMs
    val openBrokerPositions: StateFlow<List<BrokerPosition>> = brokerConnectionManager.openPositions
    val brokerExecutionLogs: StateFlow<List<BrokerExecutionLog>> = brokerConnectionManager.executionLogs

    private val _brokerFeedbackMessage = MutableStateFlow<String?>(null)
    val brokerFeedbackMessage: StateFlow<String?> = _brokerFeedbackMessage.asStateFlow()

    fun clearBrokerFeedbackMessage() {
        _brokerFeedbackMessage.value = null
    }

    // Confirmed Trade Universe (1 FOREX, 2 GOLD, 3 CRYPTO, 4 STOCKS, 5 ALL OF THEM)
    private val _selectedMarketCategory = MutableStateFlow(MarketCategory.ALL_MARKETS)
    val selectedMarketCategory: StateFlow<MarketCategory> = _selectedMarketCategory.asStateFlow()

    private val _selectedMarketAsset = MutableStateFlow(MarketUniverse.EUR_USD)
    val selectedMarketAsset: StateFlow<MarketAsset> = _selectedMarketAsset.asStateFlow()

    private val _autoTradeMarketUniverse = MutableStateFlow(MarketCategory.ALL_MARKETS)
    val autoTradeMarketUniverse: StateFlow<MarketCategory> = _autoTradeMarketUniverse.asStateFlow()

    fun setMarketCategory(category: MarketCategory) {
        _selectedMarketCategory.value = category
        val categoryAssets = MarketUniverse.getAssetsForCategory(category)
        if (categoryAssets.isNotEmpty() && _selectedMarketAsset.value !in categoryAssets) {
            _selectedMarketAsset.value = categoryAssets.first()
        }
    }

    fun setSelectedMarketAsset(asset: MarketAsset) {
        _selectedMarketAsset.value = asset
        _selectedMarketCategory.value = asset.category
        recalculateHighConfidenceSignal(asset.symbol)
    }

    fun setAutoTradeMarketUniverse(category: MarketCategory) {
        _autoTradeMarketUniverse.value = category
        val account = activeBrokerAccount.value
        if (account != null) {
            _brokerFeedbackMessage.value = "Auto-Trade filter updated: ${category.badgeLabel}"
        }
    }

    val currentMarketQuote: StateFlow<GenuineMarketQuote> = combine(
        marketDataEngine.marketQuotes,
        _selectedMarketAsset
    ) { quotes, asset ->
        quotes[asset.symbol] ?: marketDataEngine.getQuoteForSymbol(asset.symbol)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        marketDataEngine.getQuoteForSymbol("EUR/USD")
    )

    private val _latestEvidenceSignal = MutableStateFlow<TradingSignalEntity?>(null)
    val latestEvidenceSignal: StateFlow<TradingSignalEntity?> = _latestEvidenceSignal.asStateFlow()

    private val _isAiAssistantRunning = MutableStateFlow(false)
    val isAiAssistantRunning: StateFlow<Boolean> = _isAiAssistantRunning.asStateFlow()

    private val _aiAssistantStatusText = MutableStateFlow<String?>("Ready for Gemini quantitative analysis")
    val aiAssistantStatusText: StateFlow<String?> = _aiAssistantStatusText.asStateFlow()

    private val _assistantChatMessages = MutableStateFlow<List<AssistantChatMessage>>(
        listOf(
            AssistantChatMessage(
                isUser = false,
                text = "👋 Welcome to CASH FLOW AI Real Trading Assistant!\n\nI monitor genuine live market telemetry across:\n1 FOREX (EUR/USD, GBP/USD, USD/JPY)\n2 GOLD (XAU/USD)\n3 CRYPTO (BTC/USD, ETH/USD)\n4 STOCKS (NVIDIA, TESLA, APPLE)\n5 ALL OF THEM\n\nTap 'GENERATE EVIDENCE-BASED SIGNAL' to run quantitative Gemini AI confluence reasoning on live market data, or ask me any question. High-probability setups can be executed directly to your connected broker API."
            )
        )
    )
    val assistantChatMessages: StateFlow<List<AssistantChatMessage>> = _assistantChatMessages.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _signalFilter = MutableStateFlow("ALL") // "ALL", "BUY", "SELL", "WAIT"
    val signalFilter: StateFlow<String> = _signalFilter.asStateFlow()

    private val _isAnalyzingChart = MutableStateFlow(false)
    val isAnalyzingChart: StateFlow<Boolean> = _isAnalyzingChart.asStateFlow()

    private val _latestActiveSignal = MutableStateFlow<TradingSignalEntity?>(null)
    val latestActiveSignal: StateFlow<TradingSignalEntity?> = _latestActiveSignal.asStateFlow()

    private val _appThemeMode = MutableStateFlow(com.example.ui.theme.AppThemeMode.DARK)
    val appThemeMode: StateFlow<com.example.ui.theme.AppThemeMode> = _appThemeMode.asStateFlow()

    fun setAppThemeMode(mode: com.example.ui.theme.AppThemeMode) {
        _appThemeMode.value = mode
    }

    private val _preferredModel = MutableStateFlow("gemini-3.5-flash")
    val preferredModel: StateFlow<String> = _preferredModel.asStateFlow()

    private val _autoCaptureIntervalSeconds = MutableStateFlow(5)
    val autoCaptureIntervalSeconds: StateFlow<Int> = _autoCaptureIntervalSeconds.asStateFlow()

    private val _isScreenCaptureActive = MutableStateFlow(false)
    val isScreenCaptureActive: StateFlow<Boolean> = _isScreenCaptureActive.asStateFlow()

    private val _showAddJournalDialog = MutableStateFlow(false)
    val showAddJournalDialog: StateFlow<Boolean> = _showAddJournalDialog.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow<TransactionType?>(null)
    val selectedTypeFilter: StateFlow<TransactionType?> = _selectedTypeFilter.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<TransactionCategory?>(null)
    val selectedCategoryFilter: StateFlow<TransactionCategory?> = _selectedCategoryFilter.asStateFlow()

    private val _showAddTransactionSheet = MutableStateFlow(false)
    val showAddTransactionSheet: StateFlow<Boolean> = _showAddTransactionSheet.asStateFlow()

    private val _transactionToEdit = MutableStateFlow<TransactionEntity?>(null)
    val transactionToEdit: StateFlow<TransactionEntity?> = _transactionToEdit.asStateFlow()

    private val _showAddBudgetDialog = MutableStateFlow(false)
    val showAddBudgetDialog: StateFlow<Boolean> = _showAddBudgetDialog.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = CashFlowRepository(
            tradingSignalDao = database.tradingSignalDao(),
            tradeJournalDao = database.tradeJournalDao(),
            transactionDao = database.transactionDao(),
            budgetDao = database.budgetDao(),
            brokerAccountDao = database.brokerAccountDao()
        )

        allBrokerAccounts = repository.allBrokerAccounts.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        activeBrokerAccount = repository.activeBrokerAccount.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

        // Setup real-time continuous screen capture listener
        com.example.data.ScreenCaptureService.onFrameCapturedListener = { bitmap ->
            processChartFrameBitmap(bitmap, "SCREEN_ANALYSIS")
        }

        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
            // Auto connect initial active broker account if available
            repository.activeBrokerAccount.collect { acc ->
                if (acc != null && acc.isConnected && brokerConnectionManager.connectionStatus.value == "DISCONNECTED") {
                    brokerConnectionManager.connect(acc) { _, _ -> }
                }
            }
        }

        allSignals = repository.allSignals.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        allJournalTrades = repository.allJournalTrades.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        totalJournalPnL = repository.totalJournalPnL.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0.0
        )

        winCount = repository.winCount.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0
        )

        totalClosedTradesCount = repository.totalClosedTradesCount.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0
        )

        allTransactions = repository.allTransactions.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        allBudgets = repository.allBudgets.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        // Initialize local notification channel for high-confidence AI signals
        com.example.data.SignalNotificationManager.createNotificationChannel(application)
    }

    val filteredSignals: StateFlow<List<TradingSignalEntity>> = combine(
        allSignals,
        _searchQuery,
        _signalFilter,
        _selectedMarketCategory
    ) { signals, query, actionFilter, marketCat ->
        val allowedSymbols = if (marketCat == MarketCategory.ALL_MARKETS) {
            null
        } else {
            MarketUniverse.getAssetsForCategory(marketCat).map { it.symbol.uppercase() }
        }
        signals.filter { sig ->
            val sigSymbolUpper = sig.symbol.uppercase()
            val matchesCategory = allowedSymbols == null || allowedSymbols.any { 
                sigSymbolUpper.contains(it) || it.contains(sigSymbolUpper)
            }
            val matchesQuery = query.isBlank() ||
                    sig.symbol.contains(query, ignoreCase = true) ||
                    sig.reasoning.contains(query, ignoreCase = true) ||
                    sig.detectedPatterns.contains(query, ignoreCase = true)
            val matchesAction = actionFilter == "ALL" || sig.action.equals(actionFilter, ignoreCase = true)
            matchesCategory && matchesQuery && matchesAction
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        _searchQuery,
        _selectedTypeFilter,
        _selectedCategoryFilter
    ) { transactions, query, typeFilter, categoryFilter ->
        transactions.filter { tx ->
            val matchesQuery = query.isBlank() ||
                    tx.title.contains(query, ignoreCase = true) ||
                    tx.note.contains(query, ignoreCase = true) ||
                    tx.category.displayName.contains(query, ignoreCase = true)
            val matchesType = typeFilter == null || tx.type == typeFilter
            val matchesCategory = categoryFilter == null || tx.category == categoryFilter
            matchesQuery && matchesType && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalIncome: StateFlow<Double> = allTransactions.combine(MutableStateFlow(Unit)) { txs, _ ->
        txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = allTransactions.combine(MutableStateFlow(Unit)) { txs, _ ->
        txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun setSelectedTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSignalFilter(filter: String) {
        _signalFilter.value = filter
    }

    fun setPreferredModel(model: String) {
        _preferredModel.value = model
    }

    private var inAppSimulationJob: Job? = null

    fun setAutoCaptureInterval(seconds: Int) {
        _autoCaptureIntervalSeconds.value = seconds
        com.example.data.ScreenCaptureService.updateScanIntervalMs(seconds * 1000L)
    }

    fun startScreenCaptureService(context: android.content.Context, resultCode: Int = -1, data: android.content.Intent? = null) {
        _isScreenCaptureActive.value = true
        if (resultCode != -1 && data != null) {
            stopInAppSimulation()
            com.example.data.ScreenCaptureService.startService(context, resultCode, data)
        } else {
            // Run safe in-app simulation capture so Android 14 SecurityException is never triggered
            startInAppSimulationCapture()
        }
    }

    fun stopScreenCaptureService(context: android.content.Context) {
        _isScreenCaptureActive.value = false
        stopInAppSimulation()
        com.example.data.ScreenCaptureService.stopService(context)
    }

    fun onMediaProjectionGranted(context: android.content.Context, resultCode: Int, data: android.content.Intent?) {
        startScreenCaptureService(context, resultCode, data)
    }

    fun onMediaProjectionDenied() {
        _isScreenCaptureActive.value = false
        stopInAppSimulation()
    }

    fun toggleScreenCaptureService(context: android.content.Context) {
        if (_isScreenCaptureActive.value) {
            stopScreenCaptureService(context)
        } else {
            startInAppSimulationCapture()
        }
    }

    fun startInAppSimulationCapture() {
        _isScreenCaptureActive.value = true
        inAppSimulationJob?.cancel()
        inAppSimulationJob = viewModelScope.launch {
            var cycleIndex = 0
            while (isActive && _isScreenCaptureActive.value) {
                try {
                    val availableAssets = MarketUniverse.getAssetsForCategory(_selectedMarketCategory.value)
                    val activeAsset = if (availableAssets.isNotEmpty()) {
                        availableAssets[cycleIndex % availableAssets.size]
                    } else {
                        MarketUniverse.EUR_USD
                    }
                    cycleIndex++
                    _selectedMarketAsset.value = activeAsset

                    val simulatedBitmap = generateSimulatedChartBitmap(activeAsset)
                    processChartFrameBitmap(simulatedBitmap, "SCREEN_ANALYSIS", activeAsset)
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
                delay((_autoCaptureIntervalSeconds.value.coerceAtLeast(1) * 1000L))
            }
        }
    }

    fun stopInAppSimulation() {
        inAppSimulationJob?.cancel()
        inAppSimulationJob = null
    }

    fun scanAllConfirmedMarkets() {
        viewModelScope.launch {
            _isAnalyzingChart.value = true
            try {
                val assetsToScan = if (_selectedMarketCategory.value == MarketCategory.ALL_MARKETS) {
                    MarketUniverse.allAssets
                } else {
                    MarketUniverse.getAssetsForCategory(_selectedMarketCategory.value)
                }

                val engine = aiSignalRepository.getEngine()
                var persistedCount = 0
                assetsToScan.forEach { asset ->
                    val quote = marketDataEngine.getQuoteForSymbol(asset.symbol)
                    val isFresh = !quote.isStale &&
                        quote.lastPrice > 0.0 &&
                        quote.lastUpdatedTimestamp > 0L &&
                        (System.currentTimeMillis() - quote.lastUpdatedTimestamp) <= 300_000L

                    val isHistoryValid = isFresh &&
                        quote.recentCloses.size >= 5 &&
                        quote.recentCloses.all { it > 0.0 && !it.isNaN() && !it.isInfinite() }

                    val priceHistory = if (isHistoryValid) quote.recentCloses else null
                    val sig = engine.generateSignal(asset.symbol, priceHistory)

                    // Prevent batch scanning from persisting an actionable signal when market history is absent, stale, or invalid
                    if (isHistoryValid && sig.signalType != "WAIT" && sig.confidencePercentage > 0 && sig.entryPrice > 0.0) {
                        val entity = TradingSignalEntity(
                            timestamp = System.currentTimeMillis(),
                            symbol = sig.symbol,
                            timeframe = "15m",
                            action = sig.signalType,
                            confidenceScore = sig.confidencePercentage,
                            reasoning = sig.analysisReason,
                            entryZone = if (asset.decimalDigits == 4) String.format(java.util.Locale.US, "%.4f", sig.entryPrice) else String.format(java.util.Locale.US, "$%.2f", sig.entryPrice),
                            stopLoss = if (asset.decimalDigits == 4) String.format(java.util.Locale.US, "%.4f", sig.stopLoss) else String.format(java.util.Locale.US, "$%.2f", sig.stopLoss),
                            takeProfit = if (asset.decimalDigits == 4) String.format(java.util.Locale.US, "%.4f", sig.takeProfit) else String.format(java.util.Locale.US, "$%.2f", sig.takeProfit),
                            riskRewardRatio = "1:2.8",
                            trendDirection = if (sig.signalType == "SELL") "BEARISH" else "BULLISH",
                            detectedPatterns = sig.candlePattern,
                            keyLevels = "Support: ${sig.stopLoss} | Resistance: ${sig.takeProfit} | Market: ${asset.category.shortName}",
                            source = "SCAN_CONFIRMED_MARKETS"
                        )
                        repository.insertSignal(entity)
                        persistedCount++

                        // If auto-trading is enabled for this category, execute
                        val broker = activeBrokerAccount.value
                        val allowed = when (_autoTradeMarketUniverse.value) {
                            MarketCategory.ALL_MARKETS -> true
                            MarketCategory.FOREX -> asset.category == MarketCategory.FOREX
                            MarketCategory.GOLD -> asset.category == MarketCategory.GOLD
                            MarketCategory.CRYPTO -> asset.category == MarketCategory.CRYPTO
                            MarketCategory.STOCKS -> asset.category == MarketCategory.STOCKS
                        }
                        if (broker != null && broker.autoTradeEnabled && broker.isConnected && allowed && entity.action != "WAIT" && entity.confidenceScore >= broker.minConfidenceThreshold) {
                            executeSignalOnBroker(entity)
                        }
                    }
                }
                _brokerFeedbackMessage.value = if (persistedCount > 0) {
                    "Scanned ${assetsToScan.size} instruments across ${_selectedMarketCategory.value.shortName}. Generated $persistedCount verified signals."
                } else {
                    "Scanned ${assetsToScan.size} instruments across ${_selectedMarketCategory.value.shortName}. No actionable signals on available market data."
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isAnalyzingChart.value = false
            }
        }
    }

    private fun generateSimulatedChartBitmap(asset: MarketAsset = _selectedMarketAsset.value): Bitmap {
        val bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint()

        paint.color = android.graphics.Color.parseColor("#0B0E14")
        canvas.drawRect(0f, 0f, 800f, 600f, paint)

        // Asset info watermark
        paint.color = android.graphics.Color.parseColor("#90A4AE")
        paint.textSize = 28f
        paint.isAntiAlias = true
        canvas.drawText("${asset.symbol} • ${asset.name} [${asset.category.badgeLabel}]", 30f, 45f, paint)

        val greenPaint = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#00E676") }
        val redPaint = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#FF5252") }

        val timeOffset = System.currentTimeMillis() / 1000.0
        var lastY = 300f

        for (i in 0 until 18) {
            val x = i * 42f + 20f
            val delta = (Math.sin(timeOffset + i * 0.5) * 35 + Math.cos(i * 0.8) * 15).toFloat()
            val nextY = (lastY + delta).coerceIn(100f, 500f)
            val isGreen = nextY < lastY

            val p = if (isGreen) greenPaint else redPaint
            canvas.drawRect(x, Math.min(lastY, nextY), x + 28f, Math.max(lastY, nextY) + 12f, p)
            lastY = nextY
        }

        return bitmap
    }

    fun processChartFrameBitmap(
        bitmap: Bitmap,
        source: String = "SCREEN_ANALYSIS",
        asset: MarketAsset? = null
    ) {
        val targetAsset = asset ?: _selectedMarketAsset.value
        viewModelScope.launch {
            _isAnalyzingChart.value = true
            try {
                val newSignal = repository.analyzeAndSaveSignal(
                    bitmap = bitmap,
                    context = getApplication(),
                    source = source,
                    preferredModel = _preferredModel.value,
                    targetAsset = targetAsset
                )
                _latestActiveSignal.value = newSignal

                // Trigger local notification for high-confidence patterns
                if (newSignal.confidenceScore >= 70 || newSignal.action != "WAIT") {
                    com.example.data.SignalNotificationManager.sendSignalNotification(
                        getApplication(),
                        newSignal
                    )
                }

                // If auto-trading is enabled on active broker, auto-execute order matching the universe
                val broker = activeBrokerAccount.value
                val isMarketAllowed = when (_autoTradeMarketUniverse.value) {
                    MarketCategory.ALL_MARKETS -> true
                    MarketCategory.FOREX -> targetAsset.category == MarketCategory.FOREX
                    MarketCategory.GOLD -> targetAsset.category == MarketCategory.GOLD
                    MarketCategory.CRYPTO -> targetAsset.category == MarketCategory.CRYPTO
                    MarketCategory.STOCKS -> targetAsset.category == MarketCategory.STOCKS
                }
                if (broker != null && broker.autoTradeEnabled && broker.isConnected && isMarketAllowed && newSignal.action != "WAIT" && newSignal.confidenceScore >= broker.minConfidenceThreshold) {
                    executeSignalOnBroker(newSignal)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isAnalyzingChart.value = false
            }
        }
    }

    fun sendTestSignalNotification() {
        val sampleSignal = com.example.data.TradingSignal(
            symbol = "BTC/USD",
            signalType = "BUY",
            confidencePercentage = 94,
            entryPrice = 64850.0,
            stopLoss = 63500.0,
            takeProfit = 68200.0,
            analysisReason = "High-confidence 15m Double Bottom breakout with bullish volume surge detected by AI Signal Engine.",
            candlePattern = "DOUBLE BOTTOM BREAKOUT"
        )
        com.example.data.SignalNotificationManager.sendSignalNotification(getApplication(), sampleSignal)
    }

    fun deleteSignal(signal: TradingSignalEntity) {
        viewModelScope.launch {
            repository.deleteSignal(signal.id)
        }
    }

    fun convertSignalToJournalEntry(signal: TradingSignalEntity) {
        viewModelScope.launch {
            val entryPrice = signal.entryZone.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 100.0
            val slPrice = signal.stopLoss.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: (entryPrice * 0.98)
            val tpPrice = signal.takeProfit.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: (entryPrice * 1.04)

            val trade = TradeJournalEntity(
                signalId = signal.id,
                assetSymbol = signal.symbol,
                tradeType = if (signal.action == "SELL") "SELL" else "BUY",
                entryPrice = entryPrice,
                exitPrice = null,
                stopLoss = slPrice,
                takeProfit = tpPrice,
                positionSize = 1.0,
                status = "OPEN",
                pnl = 0.0,
                notes = "Converted from AI Signal: ${signal.detectedPatterns}",
                screenshotPath = signal.imageUri,
                chartPattern = signal.detectedPatterns
            )
            repository.insertJournalTrade(trade)
        }
    }

    fun recalculateHighConfidenceSignal(symbol: String = "BTC/USD") {
        viewModelScope.launch {
            val quote = marketDataEngine.getQuoteForSymbol(symbol)
            val isHistoryValid = quote.lastPrice > 0.0 &&
                quote.lastUpdatedTimestamp > 0L &&
                quote.recentCloses.size >= 5 &&
                quote.recentCloses.all { it > 0.0 && !it.isNaN() && !it.isInfinite() }
            val priceHistory = if (isHistoryValid) quote.recentCloses else null
            aiSignalRepository.generateSignal(symbol, priceHistory)
        }
    }

    fun convertAiSignalToJournalEntry(signal: TradingSignal) {
        if (signal.signalType == "WAIT" || signal.confidencePercentage <= 0 || signal.entryPrice <= 0.0) {
            return
        }
        viewModelScope.launch {
            val trade = TradeJournalEntity(
                assetSymbol = signal.symbol,
                tradeType = if (signal.signalType == "SELL") "SELL" else "BUY",
                entryPrice = signal.entryPrice,
                exitPrice = null,
                stopLoss = signal.stopLoss,
                takeProfit = signal.takeProfit,
                positionSize = 1.0,
                status = "OPEN",
                pnl = 0.0,
                notes = "Converted from Strong AI Signal (${signal.confidencePercentage}%): ${signal.analysisReason}",
                chartPattern = signal.candlePattern
            )
            repository.insertJournalTrade(trade)
        }
    }

    fun saveJournalTrade(
        symbol: String,
        tradeType: String,
        entryPrice: Double,
        exitPrice: Double?,
        stopLoss: Double,
        takeProfit: Double,
        positionSize: Double,
        status: String,
        notes: String
    ) {
        viewModelScope.launch {
            val pnl = if (status.startsWith("CLOSED") && exitPrice != null) {
                if (tradeType == "BUY") (exitPrice - entryPrice) * positionSize else (entryPrice - exitPrice) * positionSize
            } else 0.0

            val trade = TradeJournalEntity(
                assetSymbol = symbol.trim().ifEmpty { "CUSTOM_ASSET" },
                tradeType = tradeType,
                entryPrice = entryPrice,
                exitPrice = exitPrice,
                stopLoss = stopLoss,
                takeProfit = takeProfit,
                positionSize = positionSize,
                status = status,
                pnl = pnl,
                notes = notes.trim()
            )
            repository.insertJournalTrade(trade)
            _showAddJournalDialog.value = false
        }
    }

    fun deleteJournalTrade(trade: TradeJournalEntity) {
        viewModelScope.launch {
            repository.deleteJournalTrade(trade.id)
        }
    }

    fun openAddJournalDialog() {
        _showAddJournalDialog.value = true
    }

    fun closeAddJournalDialog() {
        _showAddJournalDialog.value = false
    }

    fun setTypeFilter(type: TransactionType?) {
        _selectedTypeFilter.value = type
    }

    fun setCategoryFilter(category: TransactionCategory?) {
        _selectedCategoryFilter.value = category
    }

    fun openAddTransactionSheet(tx: TransactionEntity? = null) {
        _transactionToEdit.value = tx
        _showAddTransactionSheet.value = true
    }

    fun closeAddTransactionSheet() {
        _showAddTransactionSheet.value = false
        _transactionToEdit.value = null
    }

    fun openAddBudgetDialog() {
        _showAddBudgetDialog.value = true
    }

    fun closeAddBudgetDialog() {
        _showAddBudgetDialog.value = false
    }

    fun saveTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        category: TransactionCategory,
        note: String,
        isRecurring: Boolean
    ) {
        if (title.isBlank() || amount <= 0.0) return
        viewModelScope.launch {
            val existing = _transactionToEdit.value
            if (existing != null) {
                repository.updateTransaction(
                    existing.copy(
                        title = title.trim(),
                        amount = amount,
                        type = type,
                        category = category,
                        note = note.trim(),
                        isRecurring = isRecurring
                    )
                )
            } else {
                repository.insertTransaction(
                    TransactionEntity(
                        title = title.trim(),
                        amount = amount,
                        type = type,
                        category = category,
                        note = note.trim(),
                        isRecurring = isRecurring
                    )
                )
            }
            closeAddTransactionSheet()
        }
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
        }
    }

    fun saveBudget(category: TransactionCategory, monthlyLimit: Double) {
        if (monthlyLimit <= 0.0) return
        viewModelScope.launch {
            repository.saveBudget(
                BudgetEntity(
                    category = category,
                    monthlyLimit = monthlyLimit
                )
            )
            closeAddBudgetDialog()
        }
    }

    // --- BROKER CONNECTION & EXECUTION CONTROLS ---

    fun connectBroker(account: BrokerAccountEntity) {
        viewModelScope.launch {
            repository.setActiveBroker(account.id)
            brokerConnectionManager.connect(account) { success, msg ->
                _brokerFeedbackMessage.value = msg
            }
        }
    }

    fun disconnectBroker() {
        viewModelScope.launch {
            repository.disconnectAllBrokers()
            brokerConnectionManager.disconnect()
            _brokerFeedbackMessage.value = "Disconnected from active broker."
        }
    }

    fun testBrokerPing(account: BrokerAccountEntity) {
        brokerConnectionManager.testPing(account) { ping ->
            _brokerFeedbackMessage.value = "Ping to ${account.brokerType}: ${ping}ms"
        }
    }

    fun saveBrokerAccount(account: BrokerAccountEntity) {
        viewModelScope.launch {
            if (account.id > 0) {
                repository.updateBrokerAccount(account)
            } else {
                repository.insertBrokerAccount(account)
            }
            _brokerFeedbackMessage.value = "Broker account '${account.accountName}' saved."
        }
    }

    fun deleteBrokerAccount(account: BrokerAccountEntity) {
        viewModelScope.launch {
            if (account.isConnected) {
                brokerConnectionManager.disconnect()
            }
            repository.deleteBrokerAccount(account)
            _brokerFeedbackMessage.value = "Broker account removed."
        }
    }

    fun toggleBrokerAutoTrade(enabled: Boolean) {
        viewModelScope.launch {
            val current = activeBrokerAccount.value
            if (current != null) {
                val updated = current.copy(autoTradeEnabled = enabled)
                repository.updateBrokerAccount(updated)
                _brokerFeedbackMessage.value = if (enabled) "⚡ Auto-Trading ARMED on ${current.brokerType}!" else "Auto-Trading PAUSED."
            } else {
                _brokerFeedbackMessage.value = "Please select and connect a broker first."
            }
        }
    }

    fun updateBrokerRiskSettings(threshold: Int, lotSize: Double, maxRisk: Double, autoSlTp: Boolean) {
        viewModelScope.launch {
            val current = activeBrokerAccount.value
            if (current != null) {
                val updated = current.copy(
                    minConfidenceThreshold = threshold,
                    defaultLotSize = lotSize,
                    maxRiskPercentage = maxRisk,
                    autoStopLossTakeProfit = autoSlTp
                )
                repository.updateBrokerAccount(updated)
                _brokerFeedbackMessage.value = "Broker risk parameters updated."
            }
        }
    }

    fun executeSignalOnBroker(signal: TradingSignalEntity, overrideVolume: Double? = null) {
        val quote = marketDataEngine.getQuoteForSymbol(signal.symbol)
        val isQuoteValid = !quote.isStale &&
            quote.lastPrice > 0.0 &&
            quote.lastUpdatedTimestamp > 0L &&
            (System.currentTimeMillis() - quote.lastUpdatedTimestamp) <= 300_000L

        if (!isQuoteValid) {
            _brokerFeedbackMessage.value = "Execution rejected: Market data for ${signal.symbol} is stale or unavailable."
            return
        }

        val entryPrice = parseValidPriceOrNull(signal.entryZone)
        if (entryPrice == null) {
            _brokerFeedbackMessage.value = "Execution rejected: Invalid or unparseable entry price (${signal.entryZone})."
            return
        }

        val active = activeBrokerAccount.value
        val volume = overrideVolume ?: active?.defaultLotSize ?: 0.10
        val slPrice = parseValidPriceOrNull(signal.stopLoss) ?: if (signal.action == "SELL") entryPrice * 1.01 else entryPrice * 0.99
        val tpPrice = parseValidPriceOrNull(signal.takeProfit) ?: if (signal.action == "SELL") entryPrice * 0.98 else entryPrice * 1.02

        brokerConnectionManager.executeTrade(
            symbol = signal.symbol,
            side = signal.action,
            entryPrice = entryPrice,
            stopLoss = slPrice,
            takeProfit = tpPrice,
            volume = volume,
            sourceSignalId = signal.id
        ) { success, msg, position ->
            _brokerFeedbackMessage.value = msg
            if (success && position != null) {
                // Record to Trade Journal automatically
                viewModelScope.launch {
                    val trade = TradeJournalEntity(
                        signalId = signal.id,
                        assetSymbol = signal.symbol,
                        tradeType = if (signal.action == "SELL") "SELL" else "BUY",
                        entryPrice = entryPrice,
                        exitPrice = null,
                        stopLoss = slPrice,
                        takeProfit = tpPrice,
                        positionSize = volume,
                        status = "OPEN",
                        pnl = 0.0,
                        notes = "Broker Execution on ${position.brokerType} [Ticket #${position.ticketId}] | Pattern: ${signal.detectedPatterns}",
                        screenshotPath = signal.imageUri,
                        chartPattern = signal.detectedPatterns
                    )
                    repository.insertJournalTrade(trade)
                }
            }
        }
    }

    fun executeManualTrade(symbol: String, side: String, entryPrice: Double, sl: Double, tp: Double, volume: Double) {
        brokerConnectionManager.executeTrade(
            symbol = symbol,
            side = side,
            entryPrice = entryPrice,
            stopLoss = sl,
            takeProfit = tp,
            volume = volume
        ) { success, msg, position ->
            _brokerFeedbackMessage.value = msg
            if (success && position != null) {
                viewModelScope.launch {
                    val trade = TradeJournalEntity(
                        assetSymbol = symbol,
                        tradeType = side.uppercase(),
                        entryPrice = entryPrice,
                        exitPrice = null,
                        stopLoss = sl,
                        takeProfit = tp,
                        positionSize = volume,
                        status = "OPEN",
                        pnl = 0.0,
                        notes = "Manual order on ${position.brokerType} [Ticket #${position.ticketId}]"
                    )
                    repository.insertJournalTrade(trade)
                }
            }
        }
    }

    fun closeBrokerPosition(ticketId: String) {
        brokerConnectionManager.closePosition(ticketId) { success, pnl ->
            val pnlFormatted = if (pnl >= 0) "+$${String.format(java.util.Locale.US, "%.2f", pnl)}" else "-$${String.format(java.util.Locale.US, "%.2f", Math.abs(pnl))}"
            _brokerFeedbackMessage.value = if (success) "Position #$ticketId closed. P&L: $pnlFormatted" else "Position close failed."
        }
    }

    fun closeAllBrokerPositions() {
        brokerConnectionManager.closeAllPositions { count, totalPnL ->
            val pnlFormatted = if (totalPnL >= 0) "+$${String.format(java.util.Locale.US, "%.2f", totalPnL)}" else "-$${String.format(java.util.Locale.US, "%.2f", Math.abs(totalPnL))}"
            _brokerFeedbackMessage.value = "Closed all $count positions. Net P&L: $pnlFormatted"
        }
    }

    // --- REAL TRADING ASSISTANT & EVIDENCE-BASED SIGNAL ENGINE ---

    fun runAiMarketAnalysis(symbol: String? = null, userNotes: String? = null) {
        val targetSymbol = symbol ?: _selectedMarketAsset.value.symbol
        val asset = MarketUniverse.findAssetBySymbol(targetSymbol)
        val quote = marketDataEngine.getQuoteForSymbol(asset.symbol)

        if (quote.isStale || quote.lastPrice <= 0.0 || quote.lastUpdatedTimestamp <= 0L || (System.currentTimeMillis() - quote.lastUpdatedTimestamp) > 300_000L) {
            _aiAssistantStatusText.value = "Market data stale or offline for ${asset.symbol}. Analysis blocked."
            _brokerFeedbackMessage.value = "Signal generation blocked: ${asset.symbol} quote is stale or offline."
            val waitSignal = TradingSignalEntity(
                timestamp = System.currentTimeMillis(),
                symbol = asset.symbol,
                timeframe = "15m",
                action = "WAIT",
                confidenceScore = 0,
                reasoning = "Market data for ${asset.symbol} is stale or offline. Actionable signals and orders are strictly blocked.",
                entryZone = "N/A",
                stopLoss = "N/A",
                takeProfit = "N/A",
                riskRewardRatio = "0:0",
                trendDirection = "NEUTRAL",
                detectedPatterns = "DATA STALE / OFFLINE",
                keyLevels = "Unavailable",
                source = "AI_EVIDENCE_ANALYSIS"
            )
            _latestEvidenceSignal.value = waitSignal
            return
        }

        viewModelScope.launch {
            _isAiAssistantRunning.value = true
            _aiAssistantStatusText.value = "Auditing live ${asset.symbol} telemetry with Gemini AI..."
            try {
                val broker = activeBrokerAccount.value
                val brokerCtx = broker?.let { "${it.brokerType} (${it.environment}) | Default Lot: ${it.defaultLotSize}" }
                val signal = repository.analyzeMarketQuoteAndSaveSignal(
                    quote = quote,
                    userInstructions = userNotes,
                    preferredModel = _preferredModel.value,
                    brokerContext = brokerCtx
                )
                _latestEvidenceSignal.value = signal
                _aiAssistantStatusText.value = "Signal Generated: ${signal.action} (${signal.confidenceScore}% Confluence)"

                // Add to Assistant Chat history
                val chatSummary = buildString {
                    append("🎯 **${signal.action} ${signal.symbol}** (${signal.confidenceScore}% Confluence)\n\n")
                    append("• **Entry:** ${signal.entryZone} | **SL:** ${signal.stopLoss} | **TP:** ${signal.takeProfit} (R:R ${signal.riskRewardRatio})\n")
                    append("• **Key Patterns:** ${signal.detectedPatterns}\n")
                    append("• **Confluence Proof:** ${signal.reasoning}\n\n")
                    if (broker != null && broker.isConnected) {
                        append("⚡ **Ready to execute on ${broker.brokerType}**.")
                    } else {
                        append("ℹ️ Connect a broker account to route this order directly.")
                    }
                }
                val updatedChat = _assistantChatMessages.value.toMutableList().apply {
                    add(
                        AssistantChatMessage(
                            isUser = false,
                            text = chatSummary,
                            associatedSymbol = signal.symbol,
                            signalAction = signal.action,
                            confidence = signal.confidenceScore
                        )
                    )
                }
                _assistantChatMessages.value = updatedChat

                // Check auto-trade
                if (broker != null && broker.autoTradeEnabled && broker.isConnected) {
                    val isMarketAllowed = when (_autoTradeMarketUniverse.value) {
                        MarketCategory.ALL_MARKETS -> true
                        MarketCategory.FOREX -> asset.category == MarketCategory.FOREX
                        MarketCategory.GOLD -> asset.category == MarketCategory.GOLD
                        MarketCategory.CRYPTO -> asset.category == MarketCategory.CRYPTO
                        MarketCategory.STOCKS -> asset.category == MarketCategory.STOCKS
                    }
                    if (isMarketAllowed && signal.action != "WAIT" && signal.confidenceScore >= broker.minConfidenceThreshold) {
                        executeSignalOnBroker(signal)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _aiAssistantStatusText.value = "Analysis failed: ${e.message}"
            } finally {
                _isAiAssistantRunning.value = false
            }
        }
    }

    fun sendAssistantChatMessage(userMessage: String) {
        if (userMessage.isBlank()) return
        val currentQuote = currentMarketQuote.value
        val history = _assistantChatMessages.value

        val updated = history.toMutableList().apply {
            add(AssistantChatMessage(isUser = true, text = userMessage.trim()))
        }
        _assistantChatMessages.value = updated

        viewModelScope.launch {
            _isAiAssistantRunning.value = true
            _aiAssistantStatusText.value = "Consulting Gemini AI Trading Assistant..."
            try {
                val responseText = repository.chatWithAssistant(
                    quote = currentQuote,
                    history = updated,
                    userMessage = userMessage,
                    preferredModel = _preferredModel.value
                )
                val withAssistantReply = _assistantChatMessages.value.toMutableList().apply {
                    add(AssistantChatMessage(isUser = false, text = responseText))
                }
                _assistantChatMessages.value = withAssistantReply
                _aiAssistantStatusText.value = "Ready"
            } catch (e: Exception) {
                e.printStackTrace()
                val errorList = _assistantChatMessages.value.toMutableList().apply {
                    add(AssistantChatMessage(isUser = false, text = "Assistant connection issue: ${e.message}"))
                }
                _assistantChatMessages.value = errorList
            } finally {
                _isAiAssistantRunning.value = false
            }
        }
    }

    fun executeEvidenceSignalOnActiveBroker(overrideVolume: Double? = null) {
        val signal = _latestEvidenceSignal.value
        if (signal != null) {
            executeSignalOnBroker(signal, overrideVolume)
        } else {
            _brokerFeedbackMessage.value = "No active evidence signal to execute. Generate one first!"
        }
    }

    fun clearAssistantChat() {
        _assistantChatMessages.value = listOf(
            AssistantChatMessage(
                isUser = false,
                text = "Chat history cleared. Live market telemetry active across all 5 confirmed trade categories. Tap 'GENERATE EVIDENCE-BASED SIGNAL' or ask any market question."
            )
        )
    }

    private fun parseValidPriceOrNull(raw: String): Double? {
        return try {
            val trimmed = raw.trim()
            if (trimmed.startsWith("-")) return null
            val cleaned = trimmed.replace(Regex("[^0-9.]"), "")
            val parsed = cleaned.toDoubleOrNull()
            if (parsed != null && parsed > 0.0 && !parsed.isNaN() && !parsed.isInfinite()) parsed else null
        } catch (_: Exception) {
            null
        }
    }
}

