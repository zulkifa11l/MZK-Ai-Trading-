package com.example.analysis

import com.example.model.AiAnalysisResult
import com.example.model.Candle
import com.example.model.MarketTrend
import com.example.model.PaperDuration
import com.example.model.RiskLevel
import com.example.model.TechnicalIndicators
import com.example.model.Timeframe
import com.example.model.TradeSignal
import com.example.model.TradingPair
import kotlin.math.roundToInt

data class ScoringBreakdown(
    val bullishScore: Int,
    val bearishScore: Int,
    val netScore: Int,
    val reasons: List<String>
)

object SignalScoringEngine {

    fun calculateSignal(
        pair: TradingPair,
        timeframe: Timeframe,
        indicators: TechnicalIndicators,
        candles: List<Candle>,
        paperDuration: PaperDuration = PaperDuration.MIN_10
    ): AiAnalysisResult {
        val currentPrice = indicators.currentPrice
        val breakdown = computeScoring(indicators, candles)

        val bullish = breakdown.bullishScore
        val bearish = breakdown.bearishScore
        val diff = bullish - bearish

        val signal = when {
            diff >= 3 -> TradeSignal.BUY
            diff <= -3 -> TradeSignal.SELL
            else -> TradeSignal.HOLD
        }

        val confidence = when (signal) {
            TradeSignal.BUY -> {
                val ratio = bullish.toDouble() / (bullish + bearish).coerceAtLeast(1).toDouble()
                (ratio * 100.0).roundToInt().coerceIn(68, 88)
            }
            TradeSignal.SELL -> {
                val ratio = bearish.toDouble() / (bullish + bearish).coerceAtLeast(1).toDouble()
                (ratio * 100.0).roundToInt().coerceIn(68, 88)
            }
            TradeSignal.HOLD -> {
                50
            }
        }

        val trend = when {
            diff >= 2 -> MarketTrend.BULLISH
            diff <= -2 -> MarketTrend.BEARISH
            else -> MarketTrend.NEUTRAL
        }

        val risk = when {
            confidence >= 78 -> RiskLevel.LOW
            confidence >= 65 -> RiskLevel.MEDIUM
            else -> RiskLevel.HIGH
        }

        val action = when (signal) {
            TradeSignal.BUY -> "BUY NOW — PAPER TRADE ONLY"
            TradeSignal.SELL -> "SELL NOW — PAPER TRADE ONLY"
            TradeSignal.HOLD -> "HOLD / WAIT — INSUFFICIENT EVIDENCE"
        }

        // Realistic Support / Resistance Entry, SL, TP
        val entry = currentPrice
        val (sl, tp) = when (signal) {
            TradeSignal.BUY -> {
                val stop = minOf(indicators.support, currentPrice * 0.985)
                val riskDist = (currentPrice - stop).coerceAtLeast(currentPrice * 0.005)
                val target = currentPrice + (riskDist * 1.8) // 1 : 1.8 R:R
                Pair(stop, target)
            }
            TradeSignal.SELL -> {
                val stop = maxOf(indicators.resistance, currentPrice * 1.015)
                val riskDist = (stop - currentPrice).coerceAtLeast(currentPrice * 0.005)
                val target = currentPrice - (riskDist * 1.8)
                Pair(stop, target)
            }
            TradeSignal.HOLD -> {
                Pair(indicators.support, indicators.resistance)
            }
        }

        val enrichedReasons = breakdown.reasons.toMutableList()
        enrichedReasons.add(
            "Overall Score: Bullish $bullish | Bearish $bearish (Bias: ${if (diff > 0) "+$diff Bullish" else if (diff < 0) "$diff Bearish" else "Neutral"})"
        )

        return AiAnalysisResult(
            symbol = pair.symbol,
            timeframe = timeframe.label,
            signal = signal,
            confidence = confidence,
            action = action,
            entry = entry,
            stopLoss = sl,
            takeProfit = tp,
            trend = trend,
            risk = risk,
            reasons = enrichedReasons,
            providerUsed = "MZK Signal Engine (Deterministic Quant)"
        )
    }

    private fun computeScoring(
        indicators: TechnicalIndicators,
        candles: List<Candle>
    ): ScoringBreakdown {
        var bullish = 0
        var bearish = 0
        val reasons = mutableListOf<String>()

        val price = indicators.currentPrice
        val ema20 = indicators.ema20
        val ema50 = indicators.ema50
        val ema200 = indicators.ema200

        // 1. EMA Trend (+2 / -2)
        if (price > ema20 && ema20 > ema50 && ema50 > ema200 && ema200 > 0) {
            bullish += 2
            reasons.add("EMA trend is strongly bullish (+2): Price > EMA20 > EMA50 > EMA200")
        } else if (price < ema20 && ema20 < ema50 && ema50 < ema200 && ema200 > 0) {
            bearish += 2
            reasons.add("EMA trend is strongly bearish (-2): Price < EMA20 < EMA50 < EMA200")
        } else if (price > ema50) {
            bullish += 1
            reasons.add("EMA trend is moderately bullish (+1): Price above EMA50")
        } else {
            bearish += 1
            reasons.add("EMA trend is moderately bearish (-1): Price below EMA50")
        }

        // 2. MACD (+2 / -2)
        val macd = indicators.macd
        if (macd.histogram > 0 && macd.macd > macd.signal) {
            bullish += 2
            reasons.add("MACD is positive with bullish crossover (+2): Hist = +${String.format("%.2f", macd.histogram)}")
        } else if (macd.histogram < 0 && macd.macd < macd.signal) {
            bearish += 2
            reasons.add("MACD is negative with bearish crossover (-2): Hist = ${String.format("%.2f", macd.histogram)}")
        } else if (macd.histogram > 0) {
            bullish += 1
            reasons.add("MACD histogram is expanding positively (+1)")
        } else {
            bearish += 1
            reasons.add("MACD histogram is expanding negatively (-1)")
        }

        // 3. RSI (+1 / -1)
        val rsi = indicators.rsi
        when {
            rsi in 52.0..68.0 -> {
                bullish += 1
                reasons.add("RSI supports bullish momentum (+1): ${String.format("%.1f", rsi)}")
            }
            rsi <= 30.0 -> {
                bullish += 1
                reasons.add("RSI is in oversold rebound territory (+1): ${String.format("%.1f", rsi)}")
            }
            rsi in 32.0..48.0 -> {
                bearish += 1
                reasons.add("RSI indicates bearish continuation (-1): ${String.format("%.1f", rsi)}")
            }
            rsi >= 70.0 -> {
                bearish += 1
                reasons.add("RSI indicates overbought exhaustion risk (-1): ${String.format("%.1f", rsi)}")
            }
            else -> {
                reasons.add("RSI is balanced (Neutral): ${String.format("%.1f", rsi)}")
            }
        }

        // 4. Volume (+1 / -1)
        val lastCandle = candles.lastOrNull()
        val isGreenCandle = lastCandle != null && lastCandle.close >= lastCandle.open
        if (indicators.volumeChangePct > 15.0) {
            if (isGreenCandle) {
                bullish += 1
                reasons.add("Volume is increasing (+1): +${String.format("%.1f%%", indicators.volumeChangePct)} on green candle")
            } else {
                bearish += 1
                reasons.add("Volume is increasing on selling pressure (-1): +${String.format("%.1f%%", indicators.volumeChangePct)}")
            }
        } else {
            reasons.add("Volume trend is normal (${String.format("%.1f%%", indicators.volumeChangePct)})")
        }

        // 5. Support / Resistance (+1 / -1)
        val distToSupport = if (price > 0) (price - indicators.support) / price else 0.0
        val distToResistance = if (price > 0) (indicators.resistance - price) / price else 0.0
        if (price >= indicators.support && distToSupport < distToResistance) {
            bullish += 1
            reasons.add("Price is holding above key dynamic support (+1) at $${String.format("%,.1f", indicators.support)}")
        } else if (price <= indicators.resistance && distToResistance < distToSupport) {
            bearish += 1
            reasons.add("Price is facing dynamic resistance (-1) at $${String.format("%,.1f", indicators.resistance)}")
        }

        // 6. Price Momentum (+1 / -1)
        if (candles.size >= 3) {
            val last3 = candles.takeLast(3)
            val c1 = last3[0].close
            val c2 = last3[1].close
            val c3 = last3[2].close
            if (c3 > c2 && c2 > c1) {
                bullish += 1
                reasons.add("Price momentum shows consecutive rising closes (+1)")
            } else if (c3 < c2 && c2 < c1) {
                bearish += 1
                reasons.add("Price momentum shows consecutive declining closes (-1)")
            }
        }

        return ScoringBreakdown(
            bullishScore = bullish,
            bearishScore = bearish,
            netScore = bullish - bearish,
            reasons = reasons
        )
    }
}
