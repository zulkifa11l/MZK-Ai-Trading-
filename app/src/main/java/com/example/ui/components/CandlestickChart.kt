package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.analysis.TechnicalAnalysisEngine
import com.example.model.Candle
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.ChartGridColor
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.Ema20Color
import com.example.ui.theme.Ema50Color
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun CandlestickChart(
    candles: List<Candle>,
    modifier: Modifier = Modifier,
    currentPrice: Double = 0.0,
    supportLevel: Double? = null,
    resistanceLevel: Double? = null,
    entryLevel: Double? = null,
    stopLossLevel: Double? = null,
    takeProfitLevel: Double? = null,
    showEma20: Boolean = true,
    showEma50: Boolean = true
) {
    if (candles.isEmpty()) {
        Box(
            modifier = modifier
                .background(DarkSurface, RoundedCornerShape(12.dp))
                .height(180.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Real candle data loading...", color = TextSecondary, fontSize = 12.sp)
        }
        return
    }

    val displayCandles = remember(candles) { candles.takeLast(35) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val textMeasurer = rememberTextMeasurer()

    val closes = remember(displayCandles) { displayCandles.map { it.close } }
    val ema20Series = remember(closes) { TechnicalAnalysisEngine.calculateEMASeries(closes, 20) }
    val ema50Series = remember(closes) { TechnicalAnalysisEngine.calculateEMASeries(closes, 50) }

    val activeCandle = selectedIndex?.let { displayCandles.getOrNull(it) } ?: displayCandles.lastOrNull()

    Column(
        modifier = modifier
            .background(DarkSurface, RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        // Legend & Candle values header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (activeCandle != null) {
                val isUp = activeCandle.close >= activeCandle.open
                val candleColor = if (isUp) BullishGreen else BearishRed
                Text(text = "O: ${formatPrice(activeCandle.open)}", color = TextMuted, fontSize = 9.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "H: ${formatPrice(activeCandle.high)}", color = TextMuted, fontSize = 9.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "L: ${formatPrice(activeCandle.low)}", color = TextMuted, fontSize = 9.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "C: ${formatPrice(activeCandle.close)}", color = candleColor, fontSize = 9.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "EMA20", color = Ema20Color, fontSize = 9.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "EMA50", color = Ema50Color, fontSize = 9.sp)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .pointerInput(displayCandles) {
                    detectTapGestures(
                        onPress = { offset ->
                            val candleWidth = size.width / displayCandles.size
                            val idx = (offset.x / candleWidth).toInt().coerceIn(0, displayCandles.size - 1)
                            selectedIndex = idx
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                val priceAreaHeight = height * 0.80f
                val volumeAreaHeight = height * 0.18f
                val volumeBaseY = height

                val count = displayCandles.size
                if (count == 0) return@Canvas

                val minCandle = displayCandles.minOf { it.low }
                val maxCandle = displayCandles.maxOf { it.high }

                // Include trade lines in range bounds if defined
                var minPrice = minCandle * 0.998
                var maxPrice = maxCandle * 1.002
                if (supportLevel != null && supportLevel > 0) minPrice = minOf(minPrice, supportLevel * 0.999)
                if (resistanceLevel != null && resistanceLevel > 0) maxPrice = maxOf(maxPrice, resistanceLevel * 1.001)
                if (stopLossLevel != null && stopLossLevel > 0) minPrice = minOf(minPrice, stopLossLevel * 0.999)
                if (takeProfitLevel != null && takeProfitLevel > 0) maxPrice = maxOf(maxPrice, takeProfitLevel * 1.001)

                val priceRange = if (maxPrice > minPrice) maxPrice - minPrice else 1.0
                val maxVolume = displayCandles.maxOf { it.volume }.coerceAtLeast(1.0)

                val stepX = width / count
                val candleBodyWidth = (stepX * 0.65f).coerceAtLeast(2.5f)

                fun priceToY(price: Double): Float {
                    return (priceAreaHeight - ((price - minPrice) / priceRange * priceAreaHeight)).toFloat()
                }

                // Grid Lines
                val gridLines = 3
                for (g in 0..gridLines) {
                    val y = (priceAreaHeight / gridLines) * g
                    val priceLabel = maxPrice - (priceRange / gridLines) * g
                    drawLine(
                        color = ChartGridColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = formatPrice(priceLabel),
                        topLeft = Offset(width - 54.dp.toPx(), y - 12.sp.toPx()),
                        style = TextStyle(color = TextMuted, fontSize = 8.sp)
                    )
                }

                // Draw Volume Bars
                for (i in displayCandles.indices) {
                    val candle = displayCandles[i]
                    val isGreen = candle.close >= candle.open
                    val volColor = if (isGreen) BullishGreen.copy(alpha = 0.35f) else BearishRed.copy(alpha = 0.35f)
                    val barHeight = ((candle.volume / maxVolume) * volumeAreaHeight).toFloat().coerceAtLeast(2f)
                    val x = i * stepX + (stepX - candleBodyWidth) / 2f
                    val y = volumeBaseY - barHeight
                    drawRect(color = volColor, topLeft = Offset(x, y), size = Size(candleBodyWidth, barHeight))
                }

                // Draw Horizontal Trade / Support / Resistance Lines
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)

                supportLevel?.let { sup ->
                    if (sup > minPrice && sup < maxPrice) {
                        val y = priceToY(sup)
                        drawLine(BullishGreen.copy(alpha = 0.5f), Offset(0f, y), Offset(width, y), 1.2f, pathEffect = dashEffect)
                        drawText(textMeasurer, "Support", Offset(6.dp.toPx(), y - 11.sp.toPx()), TextStyle(color = BullishGreen, fontSize = 8.sp))
                    }
                }

                resistanceLevel?.let { res ->
                    if (res > minPrice && res < maxPrice) {
                        val y = priceToY(res)
                        drawLine(BearishRed.copy(alpha = 0.5f), Offset(0f, y), Offset(width, y), 1.2f, pathEffect = dashEffect)
                        drawText(textMeasurer, "Resistance", Offset(6.dp.toPx(), y - 11.sp.toPx()), TextStyle(color = BearishRed, fontSize = 8.sp))
                    }
                }

                takeProfitLevel?.let { tp ->
                    if (tp > minPrice && tp < maxPrice) {
                        val y = priceToY(tp)
                        drawLine(BullishGreen, Offset(0f, y), Offset(width, y), 1.5f, pathEffect = dashEffect)
                        drawText(textMeasurer, "TP", Offset(width - 80.dp.toPx(), y - 11.sp.toPx()), TextStyle(color = BullishGreen, fontSize = 8.sp))
                    }
                }

                stopLossLevel?.let { sl ->
                    if (sl > minPrice && sl < maxPrice) {
                        val y = priceToY(sl)
                        drawLine(BearishRed, Offset(0f, y), Offset(width, y), 1.5f, pathEffect = dashEffect)
                        drawText(textMeasurer, "SL", Offset(width - 80.dp.toPx(), y - 11.sp.toPx()), TextStyle(color = BearishRed, fontSize = 8.sp))
                    }
                }

                // Current Price horizontal marker
                if (currentPrice > minPrice && currentPrice < maxPrice) {
                    val y = priceToY(currentPrice)
                    drawLine(NeonCyan.copy(alpha = 0.8f), Offset(0f, y), Offset(width, y), 1.5f)
                }

                // Draw EMAs
                val ema20Path = Path()
                val ema50Path = Path()
                var ema20Started = false
                var ema50Started = false

                for (i in displayCandles.indices) {
                    val x = i * stepX + stepX / 2f
                    if (showEma20 && i < ema20Series.size) {
                        val y = priceToY(ema20Series[i])
                        if (!ema20Started) { ema20Path.moveTo(x, y); ema20Started = true } else ema20Path.lineTo(x, y)
                    }
                    if (showEma50 && i < ema50Series.size) {
                        val y = priceToY(ema50Series[i])
                        if (!ema50Started) { ema50Path.moveTo(x, y); ema50Started = true } else ema50Path.lineTo(x, y)
                    }
                }
                if (showEma20 && ema20Started) drawPath(ema20Path, color = Ema20Color, style = Stroke(width = 1.8f))
                if (showEma50 && ema50Started) drawPath(ema50Path, color = Ema50Color, style = Stroke(width = 1.8f))

                // Draw Candlesticks
                for (i in displayCandles.indices) {
                    val candle = displayCandles[i]
                    val isGreen = candle.close >= candle.open
                    val color = if (isGreen) BullishGreen else BearishRed

                    val centerX = i * stepX + stepX / 2f
                    val highY = priceToY(candle.high)
                    val lowY = priceToY(candle.low)
                    val openY = priceToY(candle.open)
                    val closeY = priceToY(candle.close)

                    val bodyTop = minOf(openY, closeY)
                    val bodyHeight = kotlin.math.abs(openY - closeY).coerceAtLeast(1.5f)
                    val bodyLeft = centerX - candleBodyWidth / 2f

                    drawLine(color = color, start = Offset(centerX, highY), end = Offset(centerX, lowY), strokeWidth = 1.5f)
                    drawRect(color = color, topLeft = Offset(bodyLeft, bodyTop), size = Size(candleBodyWidth, bodyHeight))
                }

                // Crosshair on touch
                selectedIndex?.let { idx ->
                    if (idx in displayCandles.indices) {
                        val touchX = idx * stepX + stepX / 2f
                        val touchY = priceToY(displayCandles[idx].close)
                        drawLine(NeonCyan.copy(alpha = 0.5f), Offset(touchX, 0f), Offset(touchX, height), 1f, pathEffect = dashEffect)
                        drawLine(NeonCyan.copy(alpha = 0.5f), Offset(0f, touchY), Offset(width, touchY), 1f, pathEffect = dashEffect)
                        drawCircle(NeonCyan, 3f, Offset(touchX, touchY))
                    }
                }
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
