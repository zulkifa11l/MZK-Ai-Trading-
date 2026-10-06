package com.example.analysis

import com.example.model.AiAnalysisResult
import com.example.model.AiProviderType
import com.example.model.BollingerBands
import com.example.model.Candle
import com.example.model.IndicatorScoreItem
import com.example.model.MacdData
import com.example.model.MarketTrend
import com.example.model.RiskLevel
import com.example.model.SignalScoreBreakdown
import com.example.model.TechnicalIndicators
import com.example.model.TradeSignal
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

object TechnicalAnalysisEngine {

    fun calculateIndicators(candles: List<Candle>): TechnicalIndicators? {
        if (candles.size < 30) return null

        val closes = candles.map { it.close }
        val currentPrice = closes.last()

        val rsi = calculateRSI(closes, 14)
        val macd = calculateMACD(closes, 12, 26, 9)
        val ema20 = calculateEMA(closes, 20)
        val ema50 = calculateEMA(closes, 50)
        val ema200 = if (closes.size >= 100) calculateEMA(closes, min(200, closes.size)) else ema50
        val bb = calculateBollingerBands(closes, 20, 2.0)
        val atr = calculateATR(candles, 14)

        // Volume trend
        val recentVolume = candles.takeLast(3).map { it.volume }.average()
        val avgVolume20 = candles.takeLast(20).map { it.volume }.average()
        val volumeChangePercent = if (avgVolume20 > 0) ((recentVolume - avgVolume20) / avgVolume20) * 100.0 else 0.0
        val volumeTrend = when {
            volumeChangePercent > 20.0 -> "INCREASING"
            volumeChangePercent < -20.0 -> "DECREASING"
            else -> "AVERAGE"
        }

        // Support and resistance
        val (support, resistance) = calculateSupportResistance(candles, currentPrice)

        // Momentum
        val prevClose10 = if (closes.size > 10) closes[closes.size - 10] else closes.first()
        val momentumPercent = if (prevClose10 > 0) ((currentPrice - prevClose10) / prevClose10) * 100.0 else 0.0

        // Overall trend
        val trend = when {
            currentPrice > ema20 && ema20 > ema50 -> MarketTrend.BULLISH
            currentPrice < ema20 && ema20 < ema50 -> MarketTrend.BEARISH
            else -> MarketTrend.NEUTRAL
        }

        return TechnicalIndicators(
            currentPrice = currentPrice,
            rsi = rsi,
            macd = macd,
            ema20 = ema20,
            ema50 = ema50,
            ema200 = ema200,
            bollingerBands = bb,
            volumeTrend = volumeTrend,
            volumeChangePercent = volumeChangePercent,
            support = support,
            resistance = resistance,
            momentumPercent = momentumPercent,
            trend = trend,
            atr = atr
        )
    }

    fun generateSignalAndScore(
        symbol: String,
        timeframe: String,
        durationMinutes: Int,
        indicators: TechnicalIndicators
    ): Pair<SignalScoreBreakdown, AiAnalysisResult> {
        val items = mutableListOf<IndicatorScoreItem>()
        val reasons = mutableListOf<String>()

        val price = indicators.currentPrice
        val ema20 = indicators.ema20
        val ema50 = indicators.ema50
        val ema200 = indicators.ema200
        val rsi = indicators.rsi
        val macd = indicators.macd
        val bb = indicators.bollingerBands
        val support = indicators.support
        val resistance = indicators.resistance
        val atr = if (indicators.atr > 0) indicators.atr else price * 0.015

        // 1. EMA Trend (weight 2)
        val emaScore: Int
        val emaReason: String
        if (price > ema20 && ema20 > ema50 && ema50 > ema200) {
            emaScore = 2
            emaReason = "Strong Bullish EMA alignment (Price > EMA20 > EMA50 > EMA200)"
            reasons.add("EMA alignment is strongly bullish with price above 20/50/200 EMAs")
        } else if (price > ema20 && ema20 > ema50) {
            emaScore = 1
            emaReason = "Bullish short-term EMA structure (Price > EMA20 > EMA50)"
            reasons.add("Price holds comfortably above EMA20 and EMA50")
        } else if (price < ema20 && ema20 < ema50 && ema50 < ema200) {
            emaScore = -2
            emaReason = "Strong Bearish EMA alignment (Price < EMA20 < EMA50 < EMA200)"
            reasons.add("EMA alignment is strongly bearish with price below all major EMAs")
        } else if (price < ema20 && ema20 < ema50) {
            emaScore = -1
            emaReason = "Bearish short-term EMA structure (Price < EMA20 < EMA50)"
            reasons.add("Price trades below EMA20 and EMA50 resistance")
        } else {
            emaScore = 0
            emaReason = "EMA lines converging / sideways compression"
        }
        items.add(IndicatorScoreItem("EMA Trend", 2, emaScore, emaScore > 0, emaScore < 0, emaReason))

        // 2. MACD (weight 2)
        val macdScore: Int
        val macdReason: String
        if (macd.isBullishCrossover || (macd.macd > macd.signal && macd.histogram > 0)) {
            macdScore = 2
            macdReason = "MACD is positive with expanding upward histogram (${String.format("%.2f", macd.histogram)})"
            reasons.add("MACD momentum is strongly positive and expanding")
        } else if (macd.macd > macd.signal) {
            macdScore = 1
            macdReason = "MACD line above signal line"
            reasons.add("MACD line maintains position above signal line")
        } else if (macd.isBearishCrossover || (macd.macd < macd.signal && macd.histogram < 0)) {
            macdScore = -2
            macdReason = "MACD is negative with expanding downward histogram (${String.format("%.2f", macd.histogram)})"
            reasons.add("MACD momentum is negative and accelerating downward")
        } else if (macd.macd < macd.signal) {
            macdScore = -1
            macdReason = "MACD line below signal line"
            reasons.add("MACD line remains suppressed below signal line")
        } else {
            macdScore = 0
            macdReason = "MACD histogram near zero baseline"
        }
        items.add(IndicatorScoreItem("MACD Momentum", 2, macdScore, macdScore > 0, macdScore < 0, macdReason))

        // 3. RSI (weight 1)
        val rsiScore: Int
        val rsiReason: String
        val rsiFormatted = String.format("%.1f", rsi)
        if (rsi in 50.0..68.0) {
            rsiScore = 1
            rsiReason = "RSI at $rsiFormatted shows healthy bullish momentum without overbought risk"
            reasons.add("RSI ($rsiFormatted) indicates sustained bullish momentum")
        } else if (rsi < 30.0) {
            rsiScore = 1
            rsiReason = "RSI at $rsiFormatted is oversold — prime bounce territory"
            reasons.add("RSI is oversold ($rsiFormatted), signaling potential mean-reversion")
        } else if (rsi in 32.0..49.9) {
            rsiScore = -1
            rsiReason = "RSI at $rsiFormatted shows bearish control below 50 midpoint"
            reasons.add("RSI ($rsiFormatted) remains pinned below the neutral 50 level")
        } else if (rsi > 70.0) {
            rsiScore = -1
            rsiReason = "RSI at $rsiFormatted is overbought — elevated pullback hazard"
            reasons.add("RSI is overbought ($rsiFormatted), warning of impending consolidation")
        } else {
            rsiScore = 0
            rsiReason = "RSI at $rsiFormatted is neutral around 50"
        }
        items.add(IndicatorScoreItem("RSI (14)", 1, rsiScore, rsiScore > 0, rsiScore < 0, rsiReason))

        // 4. Volume Trend (weight 1)
        val volScore: Int
        val volReason: String
        val volPctStr = "${if (indicators.volumeChangePercent >= 0) "+" else ""}${String.format("%.1f", indicators.volumeChangePercent)}%"
        if (indicators.volumeChangePercent > 15.0 && indicators.momentumPercent > 0) {
            volScore = 1
            volReason = "Volume expansion ($volPctStr) confirms upward price movement"
            reasons.add("Trading volume is expanding ($volPctStr) alongside upward price action")
        } else if (indicators.volumeChangePercent > 15.0 && indicators.momentumPercent < 0) {
            volScore = -1
            volReason = "Volume expansion ($volPctStr) confirms heavy selling pressure"
            reasons.add("Selling volume is elevated ($volPctStr) confirming downward distribution")
        } else if (indicators.volumeChangePercent < -20.0 && indicators.momentumPercent > 0) {
            volScore = -1
            volReason = "Price climbing on dry volume ($volPctStr) — low-conviction divergence"
        } else {
            volScore = 0
            volReason = "Volume is tracking near the 20-period baseline ($volPctStr)"
        }
        items.add(IndicatorScoreItem("Volume Trend", 1, volScore, volScore > 0, volScore < 0, volReason))

        // 5. Support / Resistance (weight 1)
        val srScore: Int
        val srReason: String
        val distToSupport = price - support
        val distToResistance = resistance - price
        if (distToSupport < distToResistance && price > support) {
            srScore = 1
            srReason = "Price is holding firm above major support ($${formatPrice(support)})"
            reasons.add("Price is well-supported above key structural floor ($${formatPrice(support)})")
        } else if (distToResistance < distToSupport && price < resistance) {
            srScore = -1
            srReason = "Price is pushing directly against major ceiling resistance ($${formatPrice(resistance)})"
            reasons.add("Price is encountering stiff structural resistance ($${formatPrice(resistance)})")
        } else {
            srScore = 0
            srReason = "Price is centered in trading range between support and resistance"
        }
        items.add(IndicatorScoreItem("Support / Resistance", 1, srScore, srScore > 0, srScore < 0, srReason))

        // 6. Bollinger Bands (weight 1)
        val bbScore: Int
        val bbReason: String
        if (bb.percentB in 0.55..0.85) {
            bbScore = 1
            bbReason = "Price is riding the upper Bollinger channel (%B = ${String.format("%.2f", bb.percentB)})"
        } else if (bb.percentB in 0.15..0.45) {
            bbScore = -1
            bbReason = "Price is compressed in the lower Bollinger channel (%B = ${String.format("%.2f", bb.percentB)})"
        } else if (bb.percentB > 0.95) {
            bbScore = -1
            bbReason = "Price pierced upper Bollinger band ($${formatPrice(bb.upper)}) — high risk of mean pullback"
        } else if (bb.percentB < 0.05) {
            bbScore = 1
            bbReason = "Price tagged lower Bollinger band ($${formatPrice(bb.lower)}) — oversold bounce potential"
        } else {
            bbScore = 0
            bbReason = "Price oscillating near the middle 20-period moving average"
        }
        items.add(IndicatorScoreItem("Bollinger Bands", 1, bbScore, bbScore > 0, bbScore < 0, bbReason))

        // Calculate Totals
        val totalBullish = items.filter { it.score > 0 }.sumOf { it.score }
        val totalBearish = items.filter { it.score < 0 }.sumOf { abs(it.score) }
        val netScore = items.sumOf { it.score }

        val signal: TradeSignal
        val confidence: Int
        val action: String
        val risk: RiskLevel
        val stopLoss: Double
        val takeProfit: Double

        when {
            netScore >= 2 -> {
                signal = TradeSignal.BUY
                confidence = (62 + (netScore - 2) * 6).coerceIn(65, 88)
                action = "BUY NOW — PAPER TRADE ONLY"
                risk = if (confidence >= 80) RiskLevel.LOW else RiskLevel.MEDIUM
                stopLoss = max(price - 1.8 * atr, support * 0.998)
                takeProfit = min(price + 2.4 * atr, resistance * 1.002)
            }
            netScore <= -2 -> {
                signal = TradeSignal.SELL
                confidence = (62 + (abs(netScore) - 2) * 6).coerceIn(65, 88)
                action = "SELL NOW — PAPER TRADE ONLY"
                risk = if (confidence >= 80) RiskLevel.LOW else RiskLevel.MEDIUM
                stopLoss = min(price + 1.8 * atr, resistance * 1.002)
                takeProfit = max(price - 2.4 * atr, support * 0.998)
            }
            else -> {
                signal = TradeSignal.HOLD
                confidence = 52
                action = "HOLD — WAIT FOR BETTER SETUP"
                risk = RiskLevel.MEDIUM
                stopLoss = price - 2.0 * atr
                takeProfit = price + 2.0 * atr
                reasons.add("Technical indicators present mixed or conflicting signals — no clear edge")
            }
        }

        val breakdown = SignalScoreBreakdown(
            items = items,
            totalBullishScore = totalBullish,
            totalBearishScore = totalBearish,
            netScore = netScore,
            maxPossibleScore = 8,
            summary = "Bullish: +$totalBullish | Bearish: -$totalBearish | Net: ${if (netScore > 0) "+$netScore" else "$netScore"}"
        )

        val result = AiAnalysisResult(
            symbol = symbol,
            timeframe = timeframe,
            signal = signal,
            confidence = confidence,
            action = action,
            currentPrice = price,
            entry = price,
            stopLoss = stopLoss,
            takeProfit = takeProfit,
            trend = indicators.trend,
            risk = risk,
            durationMinutes = durationMinutes,
            reasons = if (reasons.isNotEmpty()) reasons else listOf("Technical setup indicates $signal bias with net score $netScore/8"),
            scoreBreakdown = breakdown,
            provider = AiProviderType.RULE_ENGINE,
            generatedAt = System.currentTimeMillis()
        )

        return Pair(breakdown, result)
    }

    // Calculations
    private fun calculateRSI(closes: List<CandleClose>, period: Int = 14): Double {
        if (closes.size <= period) return 50.0

        var gains = 0.0
        var losses = 0.0

        for (i in 1..period) {
            val diff = closes[i] - closes[i - 1]
            if (diff >= 0) gains += diff else losses += abs(diff)
        }

        var avgGain = gains / period
        var avgLoss = losses / period

        for (i in (period + 1) until closes.size) {
            val diff = closes[i] - closes[i - 1]
            val gain = if (diff >= 0) diff else 0.0
            val loss = if (diff < 0) abs(diff) else 0.0

            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
        }

        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return (100.0 - (100.0 / (1.0 + rs))).coerceIn(0.0, 100.0)
    }

    private fun calculateEMA(closes: List<CandleClose>, period: Int): Double {
        if (closes.isEmpty()) return 0.0
        if (closes.size <= period) return closes.average()

        val k = 2.0 / (period + 1.0)
        var ema = closes.take(period).average()

        for (i in period until closes.size) {
            ema = (closes[i] * k) + (ema * (1.0 - k))
        }
        return ema
    }

    private fun calculateMACD(closes: List<CandleClose>, fast: Int = 12, slow: Int = 26, signalPeriod: Int = 9): MacdData {
        if (closes.size < slow + signalPeriod) {
            return MacdData(0.0, 0.0, 0.0)
        }

        // Calculate series of MACD line points to get EMA of MACD
        val kFast = 2.0 / (fast + 1.0)
        val kSlow = 2.0 / (slow + 1.0)
        var emaFast = closes.take(fast).average()
        var emaSlow = closes.take(slow).average()

        val macdSeries = mutableListOf<Double>()
        for (i in slow until closes.size) {
            emaFast = (closes[i] * kFast) + (emaFast * (1.0 - kFast))
            emaSlow = (closes[i] * kSlow) + (emaSlow * (1.0 - kSlow))
            macdSeries.add(emaFast - emaSlow)
        }

        val currentMacd = macdSeries.lastOrNull() ?: 0.0
        val signal = calculateEMA(macdSeries, signalPeriod)
        val histogram = currentMacd - signal

        val prevMacd = if (macdSeries.size >= 2) macdSeries[macdSeries.size - 2] else currentMacd
        val prevHistogram = prevMacd - signal

        return MacdData(
            macd = currentMacd,
            signal = signal,
            histogram = histogram,
            isBullishCrossover = prevHistogram <= 0 && histogram > 0,
            isBearishCrossover = prevHistogram >= 0 && histogram < 0
        )
    }

    private fun calculateBollingerBands(closes: List<CandleClose>, period: Int = 20, multiplier: Double = 2.0): BollingerBands {
        if (closes.size < period) {
            val avg = closes.average()
            return BollingerBands(avg, avg, avg, 0.0, 0.5)
        }

        val slice = closes.takeLast(period)
        val sma = slice.average()
        val variance = slice.map { (it - sma).pow(2) }.average()
        val stdDev = sqrt(variance)

        val upper = sma + (multiplier * stdDev)
        val lower = sma - (multiplier * stdDev)
        val bandwidth = if (sma > 0) ((upper - lower) / sma) * 100.0 else 0.0

        val currentPrice = closes.last()
        val percentB = if (upper != lower) ((currentPrice - lower) / (upper - lower)).coerceIn(0.0, 1.0) else 0.5

        return BollingerBands(
            upper = upper,
            middle = sma,
            lower = lower,
            bandwidthPercent = bandwidth,
            percentB = percentB
        )
    }

    private fun calculateATR(candles: List<Candle>, period: Int = 14): Double {
        if (candles.size < period + 1) return 0.0

        val trueRanges = mutableListOf<Double>()
        for (i in 1 until candles.size) {
            val current = candles[i]
            val prev = candles[i - 1]
            val tr = max(
                current.high - current.low,
                max(abs(current.high - prev.close), abs(current.low - prev.close))
            )
            trueRanges.add(tr)
        }

        return trueRanges.takeLast(period).average()
    }

    private fun calculateSupportResistance(candles: List<Candle>, currentPrice: Double): Pair<Double, Double> {
        val window = candles.takeLast(min(50, candles.size))
        val highs = window.map { it.high }
        val lows = window.map { it.low }

        val swingHighs = mutableListOf<Double>()
        val swingLows = mutableListOf<Double>()

        for (i in 2 until window.size - 2) {
            val c = window[i]
            if (c.high >= window[i - 1].high && c.high >= window[i - 2].high &&
                c.high >= window[i + 1].high && c.high >= window[i + 2].high) {
                swingHighs.add(c.high)
            }
            if (c.low <= window[i - 1].low && c.low <= window[i - 2].low &&
                c.low <= window[i + 1].low && c.low <= window[i + 2].low) {
                swingLows.add(c.low)
            }
        }

        val support = swingLows.filter { it < currentPrice }.maxOrNull() ?: (lows.minOrNull() ?: (currentPrice * 0.98))
        val resistance = swingHighs.filter { it > currentPrice }.minOrNull() ?: (highs.maxOrNull() ?: (currentPrice * 1.02))

        return Pair(support, resistance)
    }

    private fun formatPrice(price: Double): String {
        return if (price >= 100) String.format("%,.2f", price) else String.format("%.4f", price)
    }
}

typealias CandleClose = Double
