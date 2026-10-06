package com.example

import com.example.analysis.SignalScoringEngine
import com.example.analysis.TechnicalAnalysisEngine
import com.example.model.Candle
import com.example.model.Timeframe
import com.example.model.TradeSignal
import com.example.model.TradingPair
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testTechnicalIndicatorsCalculation() {
    val candles = mutableListOf<Candle>()
    var price = 100000.0
    for (i in 0 until 50) {
      price += if (i % 2 == 0) 150.0 else -50.0
      candles.add(
        Candle(
          openTime = i * 60000L,
          open = price - 20.0,
          high = price + 50.0,
          low = price - 50.0,
          close = price,
          volume = 1200.0
        )
      )
    }

    val indicators = TechnicalAnalysisEngine.calculateIndicators(candles)
    assertNotNull(indicators)
    assertTrue("RSI should be between 0 and 100", indicators.rsi in 0.0..100.0)
    assertTrue("EMA20 should be greater than 0", indicators.ema20 > 0.0)
    assertTrue("EMA50 should be greater than 0", indicators.ema50 > 0.0)
    assertTrue("Bollinger Bands Upper should be >= Lower", indicators.bollingerBands.upper >= indicators.bollingerBands.lower)
    assertTrue("Support should be > 0", indicators.support > 0.0)
    assertTrue("Resistance should be > 0", indicators.resistance > 0.0)
  }

  @Test
  fun testDeterministicSignalScoring() {
    // Generate a strong upward trend for testing BUY signal
    val bullishCandles = mutableListOf<Candle>()
    var p = 110000.0
    for (i in 0 until 60) {
      p += 120.0
      bullishCandles.add(
        Candle(
          openTime = i * 300000L,
          open = p - 50.0,
          high = p + 80.0,
          low = p - 60.0,
          close = p,
          volume = 1500.0
        )
      )
    }

    val indicators = TechnicalAnalysisEngine.calculateIndicators(bullishCandles)
    val result = SignalScoringEngine.calculateSignal(
      pair = TradingPair.BTC_USDT,
      timeframe = Timeframe.M5,
      indicators = indicators,
      candles = bullishCandles
    )

    assertNotNull(result)
    assertEquals("BTC/USDT", result.symbol)
    assertTrue("Signal should be BUY or HOLD on strong uptrend", result.signal == TradeSignal.BUY || result.signal == TradeSignal.HOLD)
    assertTrue("Confidence should be between 50 and 95", result.confidence in 50..95)
    assertTrue("Stop loss should be below entry for buy", result.stopLoss < result.entry)
    assertTrue("Take profit should be above entry for buy", result.takeProfit > result.entry)
    assertTrue("Should have multiple explainable reasons", result.reasons.size >= 4)
  }
}
