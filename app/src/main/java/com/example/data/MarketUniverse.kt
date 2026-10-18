package com.example.data

enum class MarketCategory(
    val categoryId: Int,
    val title: String,
    val shortName: String,
    val badgeLabel: String
) {
    ALL_MARKETS(5, "ALL OF THEM (ALL MARKETS)", "ALL OF THEM", "5. ALL OF THEM"),
    FOREX(1, "FOREX (EUR/USD, GBP/USD, USD/JPY)", "FOREX", "1. FOREX"),
    GOLD(2, "GOLD (XAU/USD)", "GOLD", "2. GOLD"),
    CRYPTO(3, "CRYPTO (BTC/USD, ETH/USD)", "CRYPTO", "3. CRYPTO"),
    STOCKS(4, "STOCKS (NVIDIA, TESLA, APPLE)", "STOCKS", "4. STOCKS")
}

data class MarketAsset(
    val symbol: String,
    val name: String,
    val category: MarketCategory,
    val basePrice: Double,
    val decimalDigits: Int,
    val pipUnit: String,
    val defaultLot: Double,
    val typicalDailyRange: String,
    val keyFeatures: String
)

object MarketUniverse {

    val EUR_USD = MarketAsset(
        symbol = "EUR/USD",
        name = "Euro / US Dollar",
        category = MarketCategory.FOREX,
        basePrice = 1.0865,
        decimalDigits = 4,
        pipUnit = "Pips (0.0001)",
        defaultLot = 0.50,
        typicalDailyRange = "65-90 pips",
        keyFeatures = "Most liquid global currency pair, high volume tight spreads"
    )

    val GBP_USD = MarketAsset(
        symbol = "GBP/USD",
        name = "British Pound / US Dollar",
        category = MarketCategory.FOREX,
        basePrice = 1.3042,
        decimalDigits = 4,
        pipUnit = "Pips (0.0001)",
        defaultLot = 0.40,
        typicalDailyRange = "80-120 pips",
        keyFeatures = "High volatility Cable pair, strong momentum breakout moves"
    )

    val USD_JPY = MarketAsset(
        symbol = "USD/JPY",
        name = "US Dollar / Japanese Yen",
        category = MarketCategory.FOREX,
        basePrice = 152.85,
        decimalDigits = 2,
        pipUnit = "Pips (0.01)",
        defaultLot = 0.50,
        typicalDailyRange = "90-140 pips",
        keyFeatures = "Key interest rate differential driver, clean trend follow setups"
    )

    val XAU_USD = MarketAsset(
        symbol = "XAU/USD",
        name = "Gold Spot / US Dollar",
        category = MarketCategory.GOLD,
        basePrice = 2658.40,
        decimalDigits = 2,
        pipUnit = "Points ($0.10)",
        defaultLot = 0.20,
        typicalDailyRange = "$25 - $45",
        keyFeatures = "Safe haven commodity, massive trending swings and order block retests"
    )

    val BTC_USD = MarketAsset(
        symbol = "BTC/USD",
        name = "Bitcoin / US Dollar",
        category = MarketCategory.CRYPTO,
        basePrice = 65420.00,
        decimalDigits = 2,
        pipUnit = "Points ($1.00)",
        defaultLot = 0.10,
        typicalDailyRange = "$1,800 - $3,500",
        keyFeatures = "Digital gold benchmark, 24/7 liquidity sweeps and EMA expansions"
    )

    val ETH_USD = MarketAsset(
        symbol = "ETH/USD",
        name = "Ethereum / US Dollar",
        category = MarketCategory.CRYPTO,
        basePrice = 2645.50,
        decimalDigits = 2,
        pipUnit = "Points ($1.00)",
        defaultLot = 0.30,
        typicalDailyRange = "$90 - $180",
        keyFeatures = "Smart contract blue chip, high beta correlation with Bitcoin"
    )

    val NVDA = MarketAsset(
        symbol = "NVDA",
        name = "NVIDIA Corporation",
        category = MarketCategory.STOCKS,
        basePrice = 134.80,
        decimalDigits = 2,
        pipUnit = "Shares ($0.01)",
        defaultLot = 10.0,
        typicalDailyRange = "$4.50 - $8.00",
        keyFeatures = "AI semiconductor leader, high institutional volume and breakout power"
    )

    val TSLA = MarketAsset(
        symbol = "TSLA",
        name = "Tesla, Inc.",
        category = MarketCategory.STOCKS,
        basePrice = 248.60,
        decimalDigits = 2,
        pipUnit = "Shares ($0.01)",
        defaultLot = 5.0,
        typicalDailyRange = "$8.00 - $16.00",
        keyFeatures = "High retail & momentum beta, explosive support/resistance bounces"
    )

    val AAPL = MarketAsset(
        symbol = "AAPL",
        name = "Apple Inc.",
        category = MarketCategory.STOCKS,
        basePrice = 231.25,
        decimalDigits = 2,
        pipUnit = "Shares ($0.01)",
        defaultLot = 10.0,
        typicalDailyRange = "$3.00 - $5.50",
        keyFeatures = "Global tech titan, steady trend structure and reliable technical pivots"
    )

    val allAssets: List<MarketAsset> = listOf(
        EUR_USD,
        GBP_USD,
        USD_JPY,
        XAU_USD,
        BTC_USD,
        ETH_USD,
        NVDA,
        TSLA,
        AAPL
    )

    fun getAssetsForCategory(category: MarketCategory): List<MarketAsset> {
        return when (category) {
            MarketCategory.ALL_MARKETS -> allAssets
            MarketCategory.FOREX -> listOf(EUR_USD, GBP_USD, USD_JPY)
            MarketCategory.GOLD -> listOf(XAU_USD)
            MarketCategory.CRYPTO -> listOf(BTC_USD, ETH_USD)
            MarketCategory.STOCKS -> listOf(NVDA, TSLA, AAPL)
        }
    }

    fun findAssetBySymbol(symbol: String): MarketAsset {
        val clean = symbol.trim().uppercase()
        return allAssets.find { 
            it.symbol.equals(clean, ignoreCase = true) ||
            clean.contains(it.symbol, ignoreCase = true) ||
            (it.symbol == "NVDA" && clean.contains("NVIDIA")) ||
            (it.symbol == "TSLA" && clean.contains("TESLA")) ||
            (it.symbol == "AAPL" && clean.contains("APPLE")) ||
            (it.symbol == "XAU/USD" && clean.contains("GOLD"))
        } ?: BTC_USD
    }
}
