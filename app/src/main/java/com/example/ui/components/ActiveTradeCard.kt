package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.PaperTradeEntity
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BearishRedBg
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.BullishGreenBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.delay

@Composable
fun ActiveTradeCard(
    trade: PaperTradeEntity,
    currentLivePrice: Double,
    onCloseTrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBuy = trade.signal.equals("BUY", ignoreCase = true)
    val price = if (currentLivePrice > 0.0) currentLivePrice else trade.currentPrice

    val plPct = if (isBuy) {
        ((price - trade.entryPrice) / trade.entryPrice) * 100.0
    } else {
        ((trade.entryPrice - price) / trade.entryPrice) * 100.0
    }
    val plUsd = 1000.0 * (plPct / 100.0) // $1,000 simulated position size

    val isProfit = plPct >= 0.0
    val plColor = if (isProfit) BullishGreen else BearishRed

    // Countdown calculation
    var timeLeftText by remember { mutableStateOf("") }
    LaunchedEffect(trade.endTimeMillis) {
        while (true) {
            val remainingMs = trade.endTimeMillis - System.currentTimeMillis()
            if (remainingMs <= 0) {
                timeLeftText = "Expiring..."
                break
            } else {
                val mins = remainingMs / 60000
                val secs = (remainingMs % 60000) / 1000
                timeLeftText = String.format("%02dm %02ds left", mins, secs)
            }
            delay(1000)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("active_trade_${trade.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Symbol, Signal Pill, Time Left
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = trade.symbol,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isBuy) BullishGreenBg else BearishRedBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = trade.signal,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isBuy) BullishGreen else BearishRed
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = trade.timeframe,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = timeLeftText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = WarningAmber
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Middle Stats: Entry, Current, P/L
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Entry Price", fontSize = 10.sp, color = TextMuted)
                    Text(formatPrice(trade.entryPrice), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Current Price", fontSize = 10.sp, color = TextMuted)
                    Text(formatPrice(price), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Simulated P/L", fontSize = 10.sp, color = TextMuted)
                    Text(
                        text = String.format("%+,.2f (%+.2f%%)", plUsd, plPct),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = plColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // TP & SL Thresholds
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SL: ${formatPrice(trade.stopLoss)}",
                    fontSize = 11.sp,
                    color = BearishRed
                )
                Text(
                    text = "TP: ${formatPrice(trade.takeProfit)}",
                    fontSize = 11.sp,
                    color = BullishGreen
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Close Button
            OutlinedButton(
                onClick = onCloseTrade,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("close_trade_button_${trade.id}"),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text("Close Position Early", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
