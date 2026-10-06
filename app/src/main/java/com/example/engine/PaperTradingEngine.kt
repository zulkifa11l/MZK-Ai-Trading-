package com.example.engine

import com.example.data.db.PaperTradeEntity
import com.example.data.db.TradeRepository
import com.example.data.settings.SettingsManager
import com.example.model.AiAnalysisResult
import com.example.model.TradeSignal

class PaperTradingEngine(
    private val tradeRepository: TradeRepository,
    private val settingsManager: SettingsManager
) {

    suspend fun openTradeFromSignal(signalResult: AiAnalysisResult, customDurationMinutes: Int? = null): Long {
        if (signalResult.signal == TradeSignal.HOLD) {
            return -1L
        }

        val durationMinutes = customDurationMinutes ?: signalResult.durationMinutes
        val now = System.currentTimeMillis()
        val expiry = now + (durationMinutes * 60 * 1000L)

        val entity = PaperTradeEntity(
            symbol = signalResult.symbol,
            signal = signalResult.signal.name,
            entryPrice = signalResult.entry,
            exitPrice = signalResult.entry,
            stopLoss = signalResult.stopLoss,
            takeProfit = signalResult.takeProfit,
            timeframe = signalResult.timeframe,
            durationMinutes = durationMinutes,
            confidence = signalResult.confidence,
            entryTime = now,
            expiryTime = expiry,
            status = "ACTIVE",
            result = "PENDING",
            pnlAmount = 0.0,
            pnlPercent = 0.0,
            closeReason = "Open",
            provider = signalResult.provider.displayName
        )

        return tradeRepository.insertTrade(entity)
    }

    suspend fun onPriceTick(symbol: String, currentPrice: Double) {
        val activeTrades = tradeRepository.getActiveTradesList()
        val posSize = settingsManager.settingsFlow.value.virtualPositionSizeUsd
        val now = System.currentTimeMillis()

        for (trade in activeTrades) {
            if (!trade.symbol.equals(symbol, ignoreCase = true)) continue

            val isBuy = trade.signal == "BUY"
            val pnlPercent: Double
            val pnlUsd: Double

            if (isBuy) {
                pnlPercent = ((currentPrice - trade.entryPrice) / trade.entryPrice) * 100.0
                pnlUsd = (currentPrice - trade.entryPrice) * (posSize / trade.entryPrice)
            } else {
                pnlPercent = ((trade.entryPrice - currentPrice) / trade.entryPrice) * 100.0
                pnlUsd = (trade.entryPrice - currentPrice) * (posSize / trade.entryPrice)
            }

            var shouldClose = false
            var resultStr = "PENDING"
            var closeReason = ""

            // Check Take Profit
            if (isBuy && currentPrice >= trade.takeProfit) {
                shouldClose = true
                resultStr = "WIN"
                closeReason = "Take Profit Hit ($${formatPrice(trade.takeProfit)})"
            } else if (!isBuy && currentPrice <= trade.takeProfit) {
                shouldClose = true
                resultStr = "WIN"
                closeReason = "Take Profit Hit ($${formatPrice(trade.takeProfit)})"
            }
            // Check Stop Loss
            else if (isBuy && currentPrice <= trade.stopLoss) {
                shouldClose = true
                resultStr = "LOSS"
                closeReason = "Stop Loss Hit ($${formatPrice(trade.stopLoss)})"
            } else if (!isBuy && currentPrice >= trade.stopLoss) {
                shouldClose = true
                resultStr = "LOSS"
                closeReason = "Stop Loss Hit ($${formatPrice(trade.stopLoss)})"
            }
            // Check Expiration
            else if (now >= trade.expiryTime) {
                shouldClose = true
                resultStr = when {
                    pnlPercent > 0.05 -> "WIN"
                    pnlPercent < -0.05 -> "LOSS"
                    else -> "EXPIRED"
                }
                closeReason = "Duration Expired (${trade.durationMinutes}m)"
            }

            if (shouldClose) {
                val updated = trade.copy(
                    exitPrice = currentPrice,
                    closeTime = now,
                    status = "CLOSED",
                    result = resultStr,
                    pnlAmount = pnlUsd,
                    pnlPercent = pnlPercent,
                    closeReason = closeReason
                )
                tradeRepository.updateTrade(updated)
            } else {
                // Update live floating P/L
                val updated = trade.copy(
                    exitPrice = currentPrice,
                    pnlAmount = pnlUsd,
                    pnlPercent = pnlPercent
                )
                tradeRepository.updateTrade(updated)
            }
        }
    }

    suspend fun manualCloseTrade(tradeId: Long, currentPrice: Double) {
        val activeTrades = tradeRepository.getActiveTradesList()
        val trade = activeTrades.find { it.id == tradeId } ?: return
        val posSize = settingsManager.settingsFlow.value.virtualPositionSizeUsd
        val now = System.currentTimeMillis()

        val isBuy = trade.signal == "BUY"
        val pnlPercent = if (isBuy) {
            ((currentPrice - trade.entryPrice) / trade.entryPrice) * 100.0
        } else {
            ((trade.entryPrice - currentPrice) / trade.entryPrice) * 100.0
        }
        val pnlUsd = if (isBuy) {
            (currentPrice - trade.entryPrice) * (posSize / trade.entryPrice)
        } else {
            (trade.entryPrice - currentPrice) * (posSize / trade.entryPrice)
        }

        val resultStr = when {
            pnlPercent > 0.05 -> "WIN"
            pnlPercent < -0.05 -> "LOSS"
            else -> "EXPIRED"
        }

        val updated = trade.copy(
            exitPrice = currentPrice,
            closeTime = now,
            status = "CLOSED",
            result = resultStr,
            pnlAmount = pnlUsd,
            pnlPercent = pnlPercent,
            closeReason = "Manual Market Exit"
        )
        tradeRepository.updateTrade(updated)
    }

    private fun formatPrice(price: Double): String {
        return if (price >= 100) String.format("%,.2f", price) else String.format("%.4f", price)
    }
}
