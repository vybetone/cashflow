package com.example

import com.example.data.GeminiTradingEngine
import com.example.data.MarketCategory
import com.example.data.MarketUniverse
import com.example.data.RealMarketDataEngine
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testMarketUniverse_containsAllFiveCategories() {
    val forex = MarketUniverse.getAssetsForCategory(MarketCategory.FOREX)
    val gold = MarketUniverse.getAssetsForCategory(MarketCategory.GOLD)
    val crypto = MarketUniverse.getAssetsForCategory(MarketCategory.CRYPTO)
    val stocks = MarketUniverse.getAssetsForCategory(MarketCategory.STOCKS)
    val all = MarketUniverse.getAssetsForCategory(MarketCategory.ALL_MARKETS)

    assertEquals(3, forex.size)
    assertEquals(1, gold.size)
    assertEquals(2, crypto.size)
    assertEquals(3, stocks.size)
    assertEquals(9, all.size)
  }

  @Test
  fun testRealMarketDataEngine_placeholderSafeWhenNotConnected() {
    val engine = RealMarketDataEngine()
    val eurQuote = engine.getQuoteForSymbol("EUR/USD")
    assertNotNull(eurQuote)
    assertEquals("EUR/USD", eurQuote.asset.symbol)
    // When awaiting initial API connection, price is safely held at 0 and telemetry guards against execution
    val telemetry = engine.getTelemetrySummaryForAi(eurQuote)
    assertTrue(telemetry.contains("EUR/USD"))
    assertTrue(telemetry.contains("NEVER execute trades or create fake signals"))
  }
}

