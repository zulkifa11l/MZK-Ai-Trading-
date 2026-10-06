package com.example.data.db

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class TradingStatistics(
    val totalTrades: Int = 0,
    val winningTrades: Int = 0,
    val losingTrades: Int = 0,
    val expiredTrades: Int = 0,
    val winRatePercent: Double = 0.0,
    val totalPnlUsd: Double = 0.0,
    val totalPnlPercent: Double = 0.0,
    val averageWinPercent: Double = 0.0,
    val averageLossPercent: Double = 0.0,
    val profitFactor: Double = 0.0
)

class TradeRepository(private val dao: PaperTradeDao) {

    val activeTrades: Flow<List<PaperTradeEntity>> = dao.getActiveTradesFlow()
    val closedTrades: Flow<List<PaperTradeEntity>> = dao.getClosedTradesFlow()
    val allTrades: Flow<List<PaperTradeEntity>> = dao.getAllTradesFlow()

    suspend fun getActiveTradesList(): List<PaperTradeEntity> = dao.getActiveTrades()

    suspend fun insertTrade(trade: PaperTradeEntity): Long = dao.insertTrade(trade)

    suspend fun updateTrade(trade: PaperTradeEntity) = dao.updateTrade(trade)

    suspend fun deleteTrade(id: Long) = dao.deleteTradeById(id)

    suspend fun clearHistory() = dao.clearAllTrades()

    val statistics: Flow<TradingStatistics> = dao.getClosedTradesFlow().map { trades ->
        if (trades.isEmpty()) {
            return@map TradingStatistics()
        }

        var wins = 0
        var losses = 0
        var expired = 0
        var totalPnlUsd = 0.0
        var totalPnlPct = 0.0
        var sumWinPct = 0.0
        var sumLossPct = 0.0
        var grossProfit = 0.0
        var grossLoss = 0.0

        for (trade in trades) {
            totalPnlUsd += trade.pnlAmount
            totalPnlPct += trade.pnlPercent

            when (trade.result) {
                "WIN" -> {
                    wins++
                    sumWinPct += trade.pnlPercent
                    grossProfit += trade.pnlAmount
                }
                "LOSS" -> {
                    losses++
                    sumLossPct += trade.pnlPercent
                    grossLoss += Math.abs(trade.pnlAmount)
                }
                else -> {
                    if (trade.pnlPercent > 0) {
                        wins++
                        sumWinPct += trade.pnlPercent
                        grossProfit += trade.pnlAmount
                    } else if (trade.pnlPercent < 0) {
                        losses++
                        sumLossPct += trade.pnlPercent
                        grossLoss += Math.abs(trade.pnlAmount)
                    } else {
                        expired++
                    }
                }
            }
        }

        val totalDecided = wins + losses
        val winRate = if (totalDecided > 0) (wins.toDouble() / totalDecided) * 100.0 else 0.0
        val avgWin = if (wins > 0) sumWinPct / wins else 0.0
        val avgLoss = if (losses > 0) sumLossPct / losses else 0.0
        val profitFactor = if (grossLoss > 0) grossProfit / grossLoss else if (grossProfit > 0) 99.9 else 0.0

        TradingStatistics(
            totalTrades = trades.size,
            winningTrades = wins,
            losingTrades = losses,
            expiredTrades = expired,
            winRatePercent = winRate,
            totalPnlUsd = totalPnlUsd,
            totalPnlPercent = totalPnlPct,
            averageWinPercent = avgWin,
            averageLossPercent = avgLoss,
            profitFactor = profitFactor
        )
    }
}
