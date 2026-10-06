package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.DividerColor
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SignalBuyGreen
import com.example.ui.theme.SignalBuyGreenContainer
import com.example.ui.theme.SignalHoldAmber
import com.example.ui.theme.SignalSellRed
import com.example.ui.theme.SignalSellRedContainer
import com.example.ui.theme.TechCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun ActiveTradeItemCard(
    trade: PaperTradeEntity,
    onCloseTrade: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val isBuy = trade.signal == "BUY"
    val isProfit = trade.pnlPercent >= 0
    val pnlColor = if (isProfit) SignalBuyGreen else SignalSellRed

    val now = System.currentTimeMillis()
    val remainingMillis = (trade.expiryTime - now).coerceAtLeast(0L)
    val remainingMinutes = remainingMillis / (60 * 1000L)
    val remainingSeconds = (remainingMillis % (60 * 1000L)) / 1000L

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("active_trade_${trade.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (isBuy) SignalBuyGreenContainer else SignalSellRedContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = trade.signal,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = if (isBuy) SignalBuyGreen else SignalSellRed,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = trade.symbol,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${trade.timeframe})",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                // Live Floating P/L
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${if (isProfit) "+" else ""}${String.format("%.2f", trade.pnlPercent)}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = pnlColor
                    )
                    Text(
                        text = "${if (isProfit) "+" else ""}$${String.format("%.2f", trade.pnlAmount)} USD",
                        style = MaterialTheme.typography.labelSmall,
                        color = pnlColor.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pricing Grid: Entry, Current/Exit, TP, SL
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurface, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("ENTRY", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
                    Text("$${formatPrice(trade.entryPrice)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Column {
                    Text("STOP LOSS", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
                    Text("$${formatPrice(trade.stopLoss)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = SignalSellRed)
                }
                Column {
                    Text("TAKE PROFIT", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
                    Text("$${formatPrice(trade.takeProfit)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = SignalBuyGreen)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("TIME LEFT", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
                    Text(
                        text = String.format("%02d:%02d", remainingMinutes, remainingSeconds),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = TechCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Manual Market Close Button
            OutlinedButton(
                onClick = { onCloseTrade(trade.id) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SignalSellRed),
                border = androidx.compose.foundation.BorderStroke(1.dp, SignalSellRed.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("close_trade_button_${trade.id}")
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("CLOSE POSITION AT MARKET", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ClosedTradeItemCard(
    trade: PaperTradeEntity,
    modifier: Modifier = Modifier
) {
    val isWin = trade.result == "WIN"
    val isLoss = trade.result == "LOSS"
    val resultColor = when (trade.result) {
        "WIN" -> SignalBuyGreen
        "LOSS" -> SignalSellRed
        else -> SignalHoldAmber
    }
    val resultContainer = when (trade.result) {
        "WIN" -> SignalBuyGreenContainer
        "LOSS" -> SignalSellRedContainer
        else -> ObsidianElevated
    }

    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(trade.entryTime))

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = resultContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = trade.result,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = resultColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${trade.symbol} • ${trade.signal}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                // PnL
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${if (trade.pnlPercent >= 0) "+" else ""}${String.format("%.2f", trade.pnlPercent)}%",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = resultColor
                    )
                    Text(
                        text = "${if (trade.pnlAmount >= 0) "+" else ""}$${String.format("%.2f", trade.pnlAmount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Entry: $${formatPrice(trade.entryPrice)} → Exit: $${formatPrice(trade.exitPrice)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }

            if (trade.closeReason.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = trade.closeReason,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun StatisticsSummaryCard(
    stats: TradingStatistics,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "PAPER TRADING PERFORMANCE",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("TOTAL TRADES", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
                    Text("${stats.totalTrades}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Column {
                    Text("WIN RATE", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
                    val wrColor = if (stats.winRatePercent >= 50) SignalBuyGreen else SignalHoldAmber
                    Text("${String.format("%.1f", stats.winRatePercent)}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = wrColor)
                }
                Column {
                    Text("WINS / LOSSES", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
                    Text("${stats.winningTrades}W / ${stats.losingTrades}L", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextSecondary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("NET SIMULATED P/L", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
                    val pnlColor = if (stats.totalPnlUsd >= 0) SignalBuyGreen else SignalSellRed
                    Text(
                        text = "${if (stats.totalPnlUsd >= 0) "+" else ""}$${String.format("%.2f", stats.totalPnlUsd)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = pnlColor
                    )
                }
            }
        }
    }
}

private fun formatPrice(price: Double): String {
    return if (price >= 100) String.format("%,.2f", price) else String.format("%.4f", price)
}
