package com.example.model

enum class MarketTrend(val displayName: String) {
    BULLISH("BULLISH"),
    BEARISH("BEARISH"),
    NEUTRAL("NEUTRAL")
}

data class BollingerBands(
    val upper: Double,
    val middle: Double,
    val lower: Double,
    val bandwidthPercent: Double = 0.0,
    val percentB: Double = 0.5
)

data class MacdData(
    val macd: Double,
    val signal: Double,
    val histogram: Double,
    val isBullishCrossover: Boolean = false,
    val isBearishCrossover: Boolean = false
)

data class TechnicalIndicators(
    val currentPrice: Double,
    val rsi: Double,
    val macd: MacdData,
    val ema20: Double,
    val ema50: Double,
    val ema200: Double,
    val bollingerBands: BollingerBands,
    val volumeTrend: String,
    val volumeChangePercent: Double,
    val support: Double,
    val resistance: Double,
    val momentumPercent: Double,
    val trend: MarketTrend,
    val atr: Double = 0.0
)

data class IndicatorScoreItem(
    val name: String,
    val weight: Int,
    val score: Int, // e.g. +2, -2, 0, +1, -1
    val isBullish: Boolean,
    val isBearish: Boolean,
    val explanation: String
)

data class SignalScoreBreakdown(
    val items: List<IndicatorScoreItem>,
    val totalBullishScore: Int,
    val totalBearishScore: Int,
    val netScore: Int,
    val maxPossibleScore: Int = 8,
    val summary: String
)
