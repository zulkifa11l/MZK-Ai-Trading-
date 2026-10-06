package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.PaperTradeEntity
import com.example.data.db.TradingStatistics
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BearishRedBg
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.BullishGreenBg
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    trades: List<PaperTradeEntity>,
    statistics: TradingStatistics,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredTrades = remember(trades, selectedFilter) {
        when (selectedFilter) {
            "WINS" -> trades.filter { it.result.equals("WIN", ignoreCase = true) || it.profitLoss > 0 }
            "LOSSES" -> trades.filter { it.result.equals("LOSS", ignoreCase = true) || it.profitLoss < 0 }
            "EXPIRED" -> trades.filter { it.result.equals("EXPIRED", ignoreCase = true) }
            else -> trades
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TRADING PERFORMANCE",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonCyan
                    )
                    Text(
                        text = "Persistent Paper Trading History & Analytics",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                if (trades.isNotEmpty()) {
                    IconButton(
                        onClick = onClearHistory,
                        modifier = Modifier.testTag("clear_history_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear History",
                            tint = TextMuted
                        )
                    }
                }
            }
        }

        // Performance Statistics Overview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "KEY METRICS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatTile(
                            label = "Win Rate",
                            value = String.format("%.1f%%", statistics.winRatePercent),
                            valueColor = if (statistics.winRatePercent >= 50.0) BullishGreen else WarningAmber,
                            subtext = "${statistics.winningTrades}W / ${statistics.losingTrades}L",
                            modifier = Modifier.weight(1f)
                        )

                        val isProfit = statistics.totalProfitLoss >= 0
                        StatTile(
                            label = "Total Simulated P/L",
                            value = String.format("%s$%,.2f", if (isProfit) "+" else "", statistics.totalProfitLoss),
                            valueColor = if (isProfit) BullishGreen else BearishRed,
                            subtext = "Based on $1K trades",
                            modifier = Modifier.weight(1f)
                        )

                        StatTile(
                            label = "Max Drawdown",
                            value = String.format("-$%,.2f", statistics.maxDrawdown),
                            valueColor = BearishRed,
                            subtext = "Peak to trough",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Total Paper Trades: ${statistics.totalTrades}", fontSize = 11.sp, color = TextMuted)
                        Text(text = "Expired: ${statistics.expiredTrades}", fontSize = 11.sp, color = TextMuted)
                    }
                }
            }
        }

        // Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL", "WINS", "LOSSES", "EXPIRED").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) NeonCyan else DarkSurface)
                            .border(1.dp, if (isSelected) NeonCyan else DarkBorder, RoundedCornerShape(6.dp))
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = filter,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) DarkBg else TextSecondary
                        )
                    }
                }
            }
        }

        // Trades List
        if (filteredTrades.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.height(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No trades match this filter",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(filteredTrades, key = { it.id }) { trade ->
                HistoryTradeItem(trade = trade)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    valueColor: Color,
    subtext: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Text(text = label, fontSize = 10.sp, color = TextMuted)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor)
        Text(text = subtext, fontSize = 9.sp, color = TextSecondary)
    }
}

@Composable
fun HistoryTradeItem(trade: PaperTradeEntity) {
    val isWin = trade.result.equals("WIN", ignoreCase = true) || trade.profitLoss > 0
    val isLoss = trade.result.equals("LOSS", ignoreCase = true) || trade.profitLoss < 0

    val resultBg = when {
        isWin -> BullishGreenBg
        isLoss -> BearishRedBg
        else -> DarkSurfaceElevated
    }

    val resultColor = when {
        isWin -> BullishGreen
        isLoss -> BearishRed
        else -> WarningAmber
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val dateString = remember(trade.startTimeMillis) { dateFormat.format(Date(trade.startTimeMillis)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Pair, Signal, Result Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = trade.symbol,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = trade.signal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (trade.signal.equals("BUY", ignoreCase = true)) BullishGreen else BearishRed
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = trade.timeframe,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                // Result Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(resultBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = trade.result,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = resultColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Middle: Entry, Exit, P/L, Confidence
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Entry", fontSize = 9.sp, color = TextMuted)
                    Text(formatPrice(trade.entryPrice), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Exit / Last", fontSize = 9.sp, color = TextMuted)
                    Text(formatPrice(trade.exitPrice ?: trade.currentPrice), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Confidence", fontSize = 9.sp, color = TextMuted)
                    Text("${trade.confidence}%", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NeonCyan)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Simulated P/L", fontSize = 9.sp, color = TextMuted)
                    Text(
                        text = String.format("%+,.2f (%+.2f%%)", trade.profitLoss, trade.profitLossPercent),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = resultColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Footer: Date & Trade Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = dateString, fontSize = 10.sp, color = TextMuted)
                Text(text = "Duration: ${trade.durationMinutes} min", fontSize = 10.sp, color = TextMuted)
            }
        }
    }
}

private fun formatPrice(price: Double): String {
    return when {
        price >= 1000.0 -> String.format("$%,.1f", price)
        price >= 1.0 -> String.format("$%.3f", price)
        else -> String.format("$%.5f", price)
    }
}
