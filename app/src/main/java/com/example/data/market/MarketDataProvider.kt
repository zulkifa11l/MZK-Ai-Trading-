package com.example.data.market

import com.example.model.Candle
import com.example.model.Ticker24h
import com.example.model.Timeframe
import com.example.model.TradingPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class MarketDataResult<out T> {
    data class Success<T>(val data: T) : MarketDataResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : MarketDataResult<Nothing>()
}

interface MarketDataProvider {
    val name: String
    suspend fun fetchTicker(pair: TradingPair): MarketDataResult<Ticker24h>
    suspend fun fetchCandles(pair: TradingPair, timeframe: Timeframe, limit: Int = 100): MarketDataResult<List<Candle>>
}

class BinancePublicMarketDataProvider : MarketDataProvider {
    override val name: String = "Binance Public Market API"

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    // Primary and fallback endpoints
    private val baseUrls = listOf(
        "https://api.binance.com",
        "https://data-api.binance.vision",
        "https://api1.binance.com",
        "https://api2.binance.com",
        "https://api3.binance.com"
    )

    override suspend fun fetchTicker(pair: TradingPair): MarketDataResult<Ticker24h> = withContext(Dispatchers.IO) {
        var lastError: Exception? = null

        for (baseUrl in baseUrls) {
            val url = "$baseUrl/api/v3/ticker/24hr?symbol=${pair.binanceSymbol}"
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "MZKAITrading/1.0 (Android)")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: throw IOException("Empty response body")
                        val json = JSONObject(body)

                        val ticker = Ticker24h(
                            symbol = pair.symbol,
                            lastPrice = json.optDouble("lastPrice", 0.0),
                            priceChange = json.optDouble("priceChange", 0.0),
                            priceChangePercent = json.optDouble("priceChangePercent", 0.0),
                            high24h = json.optDouble("highPrice", 0.0),
                            low24h = json.optDouble("lowPrice", 0.0),
                            volume24h = json.optDouble("volume", 0.0),
                            quoteVolume24h = json.optDouble("quoteVolume", 0.0),
                            lastUpdated = System.currentTimeMillis()
                        )
                        return@withContext MarketDataResult.Success(ticker)
                    }
                }
            } catch (e: Exception) {
                lastError = e
            }
        }

        MarketDataResult.Error(
            message = "MARKET DATA UNAVAILABLE: Unable to connect to Binance market endpoints (${lastError?.localizedMessage ?: "Network error"})",
            cause = lastError
        )
    }

    override suspend fun fetchCandles(
        pair: TradingPair,
        timeframe: Timeframe,
        limit: Int
    ): MarketDataResult<List<Candle>> = withContext(Dispatchers.IO) {
        var lastError: Exception? = null

        for (baseUrl in baseUrls) {
            val url = "$baseUrl/api/v3/klines?symbol=${pair.binanceSymbol}&interval=${timeframe.binanceInterval}&limit=$limit"
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "MZKAITrading/1.0 (Android)")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: throw IOException("Empty response body")
                        val array = JSONArray(body)
                        val candles = mutableListOf<Candle>()

                        for (i in 0 until array.length()) {
                            val kline = array.getJSONArray(i)
                            // [openTime, open, high, low, close, volume, closeTime, ...]
                            val candle = Candle(
                                openTime = kline.getLong(0),
                                open = kline.getString(1).toDoubleOrNull() ?: 0.0,
                                high = kline.getString(2).toDoubleOrNull() ?: 0.0,
                                low = kline.getString(3).toDoubleOrNull() ?: 0.0,
                                close = kline.getString(4).toDoubleOrNull() ?: 0.0,
                                volume = kline.getString(5).toDoubleOrNull() ?: 0.0,
                                closeTime = kline.getLong(6)
                            )
                            candles.add(candle)
                        }

                        if (candles.isNotEmpty()) {
                            return@withContext MarketDataResult.Success(candles)
                        }
                    }
                }
            } catch (e: Exception) {
                lastError = e
            }
        }

        MarketDataResult.Error(
            message = "MARKET DATA UNAVAILABLE: Failed to fetch candlestick data from Binance (${lastError?.localizedMessage ?: "Connection failed"})",
            cause = lastError
        )
    }
}
