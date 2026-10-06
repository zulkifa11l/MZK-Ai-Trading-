package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "paper_trades")
data class PaperTradeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val symbol: String,
    val signal: String, // BUY, SELL
    val entryPrice: Double,
    val exitPrice: Double = 0.0,
    val stopLoss: Double,
    val takeProfit: Double,
    val timeframe: String,
    val durationMinutes: Int,
    val confidence: Int,
    val entryTime: Long = System.currentTimeMillis(),
    val expiryTime: Long,
    val closeTime: Long = 0L,
    val status: String = "ACTIVE", // ACTIVE, CLOSED
    val result: String = "PENDING", // PENDING, WIN, LOSS, EXPIRED
    val pnlAmount: Double = 0.0,
    val pnlPercent: Double = 0.0,
    val closeReason: String = "",
    val provider: String = "Quant Engine"
)
