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
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
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
import com.example.model.IndicatorScoreItem
import com.example.model.SignalScoreBreakdown
import com.example.ui.theme.DividerColor
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
import kotlin.math.abs

@Composable
fun SignalScoreBreakdownCard(
    breakdown: SignalScoreBreakdown?,
    modifier: Modifier = Modifier
) {
    if (breakdown == null) return

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(SignalBuyGreen.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = null,
                            tint = SignalBuyGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TRANSPARENT SCORING ENGINE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                // Net Score Badge
                val netColor = when {
                    breakdown.netScore >= 2 -> SignalBuyGreen
                    breakdown.netScore <= -2 -> SignalSellRed
                    else -> SignalHoldAmber
                }
                Surface(
                    color = netColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, netColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "NET: ${if (breakdown.netScore > 0) "+${breakdown.netScore}" else "${breakdown.netScore}"} / 8",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = netColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Score Summary Row: Bullish vs Bearish
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurface, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("BULLISH SCORE", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 10.sp)
                    Text("+${breakdown.totalBullishScore} / 8", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = SignalBuyGreen)
                }
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .width(1.dp)
                        .background(DividerColor)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("BEARISH SCORE", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 10.sp)
                    Text("-${breakdown.totalBearishScore} / 8", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = SignalSellRed)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Items breakdown
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                breakdown.items.forEach { item ->
                    IndicatorScoreRow(item)
                }
            }
        }
    }
}

@Composable
private fun IndicatorScoreRow(item: IndicatorScoreItem) {
    val scoreColor = when {
        item.score > 0 -> SignalBuyGreen
        item.score < 0 -> SignalSellRed
        else -> TextMuted
    }

    val scoreStr = when {
        item.score > 0 -> "+${item.score}"
        item.score < 0 -> "${item.score}"
        else -> "0"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ObsidianElevated.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "(Weight ${item.weight})",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }

            Surface(
                color = scoreColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = scoreStr,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = scoreColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.explanation,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    }
}
