package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PaperTradeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: PaperTradeEntity): Long

    @Update
    suspend fun updateTrade(trade: PaperTradeEntity)

    @Query("SELECT * FROM paper_trades ORDER BY entryTime DESC")
    fun getAllTradesFlow(): Flow<List<PaperTradeEntity>>

    @Query("SELECT * FROM paper_trades WHERE status = 'ACTIVE' ORDER BY entryTime DESC")
    fun getActiveTradesFlow(): Flow<List<PaperTradeEntity>>

    @Query("SELECT * FROM paper_trades WHERE status = 'ACTIVE'")
    suspend fun getActiveTrades(): List<PaperTradeEntity>

    @Query("SELECT * FROM paper_trades WHERE status = 'CLOSED' ORDER BY closeTime DESC")
    fun getClosedTradesFlow(): Flow<List<PaperTradeEntity>>

    @Query("SELECT * FROM paper_trades WHERE id = :id LIMIT 1")
    suspend fun getTradeById(id: Long): PaperTradeEntity?

    @Query("DELETE FROM paper_trades WHERE id = :id")
    suspend fun deleteTradeById(id: Long)

    @Query("DELETE FROM paper_trades")
    suspend fun clearAllTrades()
}
