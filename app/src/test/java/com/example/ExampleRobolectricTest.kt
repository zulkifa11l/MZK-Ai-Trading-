package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.analysis.TechnicalAnalysisEngine
import com.example.data.ai.JsonParserHelper
import com.example.model.Candle
import com.example.model.Timeframe
import com.example.model.TradeSignal
import com.example.model.TradingPair
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("MZK AI Trading", appName)
  }

  @Test
  fun `parse ai analysis json under android runtime`() {
    val rawJson = """
      {
        "symbol": "BTC/USDT",
        "timeframe": "5m",
        "signal": "BUY",
        "confidence": 78,
        "action": "BUY — paper trade only",
        "entry": 118200.0,
        "stop_loss": 117500.0,
        "take_profit": 119800.0,
        "trend": "BULLISH",
        "risk": "MEDIUM",
        "reasons": [
          "EMA trend is bullish",
          "MACD momentum is positive",
          "Volume is increasing"
        ]
      }
    """.trimIndent()

    val dummyCandles = listOf(
      Candle(0L, 118000.0, 118500.0, 117900.0, 118200.0, 500.0)
    )
    val indicators = TechnicalAnalysisEngine.calculateIndicators(dummyCandles)

    val result = JsonParserHelper.parseAnalysisJson(
      rawJsonText = rawJson,
      pair = TradingPair.BTC_USDT,
      timeframe = Timeframe.M5,
      indicators = indicators,
      providerName = "Test Provider"
    )

    assertEquals("BTC/USDT", result.symbol)
    assertEquals(TradeSignal.BUY, result.signal)
    assertEquals(78, result.confidence)
    assertEquals(118200.0, result.entry, 0.1)
    assertEquals(117500.0, result.stopLoss, 0.1)
    assertEquals(119800.0, result.takeProfit, 0.1)
    assertEquals(3, result.reasons.size)
  }
}
