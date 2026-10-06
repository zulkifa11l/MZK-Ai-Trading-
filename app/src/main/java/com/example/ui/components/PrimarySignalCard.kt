package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.example.model.AiAnalysisResult
import com.example.model.MarketTrend
import com.example.model.RiskLevel
import com.example.model.Ticker24h
import com.example.model.TradeSignal
import com.example.ui.theme.DividerColor
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SignalBuyGreen
import com.example.ui.theme.SignalBuyGreenContainer
import com.example.ui.theme.SignalHoldAmber
import com.example.ui.theme.SignalHoldAmberContainer
import com.example.ui.theme.SignalSellRed
import com.example.ui.theme.SignalSellRedContainer
import com.example.ui.theme.TechCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PrimarySignalCard(
    ticker: Ticker24h?,
    signalResult: AiAnalysisResult?,
    isAnalyzing: Boolean,
    onRefresh: () -> Unit,
    onStartPaperTrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("primary_signal_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: MZK AI TRADING badge & Refresh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(TechCyan.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = TechCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MZK AI TRADING",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                        color = TextPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = TechCyan,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("refresh_signal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Signal",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pair & Live Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = ticker?.symbol ?: signalResult?.symbol ?: "BTC/USDT",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "TIMEFRAME: ${signalResult?.timeframe ?: "5 MIN"} • DURATION: ${signalResult?.durationMinutes ?: 10} MIN",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        letterSpacing = 0.8.sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "CURRENT PRICE",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                    Text(
                        text = if (ticker != null) "$${formatPrice(ticker.lastPrice)}" else if (signalResult != null) "$${formatPrice(signalResult.currentPrice)}" else "---",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    if (ticker != null) {
                        val isPositive = ticker.priceChangePercent >= 0
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = if (isPositive) SignalBuyGreen else SignalSellRed,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${if (isPositive) "+" else ""}${String.format("%.2f", ticker.priceChangePercent)}% 24h",
                                color = if (isPositive) SignalBuyGreen else SignalSellRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Massive Signal Banner
            val signal = signalResult?.signal ?: TradeSignal.HOLD
            val signalColor = when (signal) {
                TradeSignal.BUY -> SignalBuyGreen
                TradeSignal.SELL -> SignalSellRed
                TradeSignal.HOLD -> SignalHoldAmber
            }
            val signalContainer = when (signal) {
                TradeSignal.BUY -> SignalBuyGreenContainer
                TradeSignal.SELL -> SignalSellRedContainer
                TradeSignal.HOLD -> SignalHoldAmberContainer
            }

            Surface(
                color = signalContainer,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, signalColor.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signal_badge")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SIGNAL",
                            style = MaterialTheme.typography.labelSmall,
                            color = signalColor.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = signal.name,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = signalColor,
                            letterSpacing = 2.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "CONFIDENCE",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${signalResult?.confidence ?: 50}%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(ObsidianElevated)
                        ) {
                            val conf = (signalResult?.confidence ?: 50) / 100f
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(conf)
                                    .height(6.dp)
                                    .background(signalColor)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ACTION Text
            Surface(
                color = ObsidianSurface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTION:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = signalResult?.action ?: "HOLD — WAIT FOR BETTER SETUP",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = signalColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metrics Grid: Entry, Stop Loss, Take Profit, Trend, Risk
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurface, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Row 1: Entry, Stop Loss, Take Profit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricBox(
                        label = "ENTRY",
                        value = if (signalResult != null) "$${formatPrice(signalResult.entry)}" else "---",
                        valueColor = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = "STOP LOSS",
                        value = if (signalResult != null) "$${formatPrice(signalResult.stopLoss)}" else "---",
                        valueColor = SignalSellRed,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = "TAKE PROFIT",
                        value = if (signalResult != null) "$${formatPrice(signalResult.takeProfit)}" else "---",
                        valueColor = SignalBuyGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = DividerColor, thickness = 1.dp)

                // Row 2: Trend, Risk, Signal Duration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricBox(
                        label = "TREND",
                        value = signalResult?.trend?.name ?: "NEUTRAL",
                        valueColor = when (signalResult?.trend) {
                            MarketTrend.BULLISH -> SignalBuyGreen
                            MarketTrend.BEARISH -> SignalSellRed
                            else -> TextSecondary
                        },
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = "RISK",
                        value = signalResult?.risk?.name ?: "MEDIUM",
                        valueColor = when (signalResult?.risk) {
                            RiskLevel.LOW -> SignalBuyGreen
                            RiskLevel.MEDIUM -> SignalHoldAmber
                            RiskLevel.HIGH -> SignalSellRed
                            else -> TextSecondary
                        },
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = "DURATION",
                        value = "${signalResult?.durationMinutes ?: 10} MIN",
                        valueColor = TechCyan,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // WHY Section (Bullet Points)
            Text(
                text = "WHY:",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            val reasons = signalResult?.reasons ?: listOf(
                "Calculating confluence across EMA, MACD, and RSI indicators"
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                reasons.forEach { reason ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            color = signalColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action: Start Paper Trade Button
            Button(
                onClick = onStartPaperTrade,
                enabled = signalResult != null && signalResult.signal != TradeSignal.HOLD,
                colors = ButtonDefaults.buttonColors(
                    containerColor = signalColor,
                    contentColor = Color.Black,
                    disabledContainerColor = ObsidianElevated,
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("start_paper_trade_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (signalResult?.signal == TradeSignal.HOLD) "CANNOT TRADE ON HOLD SIGNAL" else "START PAPER TRADE ($${if (signalResult != null) formatPrice(signalResult.entry) else ""})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Legal & Educational Disclaimer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Analysis & paper trading only. Confidence is not a guarantee of profit.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor
        )
    }
}

private fun formatPrice(price: Double): String {
    return if (price >= 100) String.format("%,.2f", price) else String.format("%.4f", price)
}
