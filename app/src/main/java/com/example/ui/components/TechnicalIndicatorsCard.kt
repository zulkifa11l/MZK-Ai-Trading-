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
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TechnicalIndicators
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SignalBuyGreen
import com.example.ui.theme.SignalHoldAmber
import com.example.ui.theme.SignalSellRed
import com.example.ui.theme.TechCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TechnicalIndicatorsCard(
    indicators: TechnicalIndicators?,
    modifier: Modifier = Modifier
) {
    if (indicators == null) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(TechCyan.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = TechCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TECHNICAL INDICATOR MATRIX",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // RSI Gauge Bar
            val rsiVal = indicators.rsi.toFloat()
            val rsiColor = when {
                rsiVal < 30f -> SignalBuyGreen // Oversold
                rsiVal > 70f -> SignalSellRed // Overbought
                rsiVal >= 50f -> SignalBuyGreen
                else -> SignalSellRed
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurface, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("RSI (14-PERIOD)", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${String.format("%.1f", indicators.rsi)} ${when {
                            indicators.rsi < 30 -> "(OVERSOLD)"
                            indicators.rsi > 70 -> "(OVERBOUGHT)"
                            indicators.rsi >= 50 -> "(BULLISH)"
                            else -> "(BEARISH)"
                        }}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = rsiColor
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (rsiVal / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = rsiColor,
                    trackColor = ObsidianElevated
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
                    Text("30 (Oversold)", style = MaterialTheme.typography.labelSmall, color = SignalBuyGreen.copy(alpha = 0.7f), fontSize = 9.sp)
                    Text("50", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
                    Text("70 (Overbought)", style = MaterialTheme.typography.labelSmall, color = SignalSellRed.copy(alpha = 0.7f), fontSize = 9.sp)
                    Text("100", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // MACD Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurface, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("MACD LINE", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 10.sp)
                    Text(String.format("%.2f", indicators.macd.macd), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Column {
                    Text("SIGNAL LINE", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 10.sp)
                    Text(String.format("%.2f", indicators.macd.signal), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextSecondary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("HISTOGRAM", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 10.sp)
                    val histColor = if (indicators.macd.histogram >= 0) SignalBuyGreen else SignalSellRed
                    Text(
                        text = "${if (indicators.macd.histogram >= 0) "+" else ""}${String.format("%.2f", indicators.macd.histogram)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = histColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // EMA Ribbon Matrix
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurface, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text("EXPONENTIAL MOVING AVERAGES (EMA)", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    EmaItem("EMA 20", indicators.ema20, indicators.currentPrice)
                    EmaItem("EMA 50", indicators.ema50, indicators.currentPrice)
                    EmaItem("EMA 200", indicators.ema200, indicators.currentPrice)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bollinger Bands & Support / Resistance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(ObsidianSurface, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text("BOLLINGER BANDS", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Upper: $${formatPrice(indicators.bollingerBands.upper)}", style = MaterialTheme.typography.labelSmall, color = SignalSellRed, fontSize = 11.sp)
                    Text("Mid: $${formatPrice(indicators.bollingerBands.middle)}", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 11.sp)
                    Text("Lower: $${formatPrice(indicators.bollingerBands.lower)}", style = MaterialTheme.typography.labelSmall, color = SignalBuyGreen, fontSize = 11.sp)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(ObsidianSurface, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text("KEY PRICE LEVELS", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Resistance: $${formatPrice(indicators.resistance)}", style = MaterialTheme.typography.labelSmall, color = SignalSellRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Support: $${formatPrice(indicators.support)}", style = MaterialTheme.typography.labelSmall, color = SignalBuyGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Volume: ${indicators.volumeTrend}", style = MaterialTheme.typography.labelSmall, color = TechCyan, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun EmaItem(label: String, emaValue: Double, currentPrice: Double) {
    val isAbove = currentPrice >= emaValue
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 10.sp)
        Text(
            text = "$${formatPrice(emaValue)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isAbove) SignalBuyGreen else SignalSellRed
        )
        Text(
            text = if (isAbove) "Price > EMA" else "Price < EMA",
            style = MaterialTheme.typography.labelSmall,
            color = if (isAbove) SignalBuyGreen.copy(alpha = 0.8f) else SignalSellRed.copy(alpha = 0.8f),
            fontSize = 9.sp
        )
    }
}

private fun formatPrice(price: Double): String {
    return if (price >= 100) String.format("%,.2f", price) else String.format("%.4f", price)
}
