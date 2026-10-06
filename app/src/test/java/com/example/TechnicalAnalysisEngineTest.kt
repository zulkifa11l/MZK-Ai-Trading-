package com.example

import com.example.analysis.TechnicalAnalysisEngine
import com.example.model.Candle
import com.example.model.TradeSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TechnicalAnalysisEngineTest {

    @Test
    fun testTechnicalAnalysisCalculations() {
        val candles = mutableListOf<Candle>()
        val basePrice = 50000.0
        val now = System.currentTimeMillis()

        // Generate 60 test candles with an upward trend
        for (i in 0 until 60) {
            val price = basePrice + (i * 100.0)
            candles.add(
                Candle(
                    openTime = now - ((60 - i) * 60000L),
                    open = price - 20.0,
                    high = price + 50.0,
                    low = price - 30.0,
                    close = price,
                    volume = 100.0 + (i * 2.0),
                    closeTime = now - ((60 - i) * 60000L) + 59000L
                )
            )
        }

        val indicators = TechnicalAnalysisEngine.calculateIndicators(candles)
        assertNotNull("Indicators should not be null for 60 candles", indicators)
        indicators?.let {
            assertTrue("RSI should be between 0 and 100", it.rsi in 0.0..100.0)
            assertTrue("EMA20 should be greater than 0", it.ema20 > 0.0)
            assertTrue("EMA50 should be greater than 0", it.ema50 > 0.0)
            assertTrue("Bollinger Bands upper > lower", it.bollingerBands.upper >= it.bollingerBands.lower)

            val (breakdown, signalResult) = TechnicalAnalysisEngine.generateSignalAndScore(
                symbol = "BTC/USDT",
                timeframe = "5 MIN",
                durationMinutes = 10,
                indicators = it
            )

            assertNotNull("Breakdown must not be null", breakdown)
            assertNotNull("SignalResult must not be null", signalResult)
            assertTrue("Confidence should be positive", signalResult.confidence > 50)
            assertEquals("Symbol should match", "BTC/USDT", signalResult.symbol)
            assertTrue("Signal should be valid enum", signalResult.signal in TradeSignal.entries)
        }
    }
}
