package com.example.model

enum class TradingPair(
    val symbol: String,
    val baseAsset: String,
    val quoteAsset: String,
    val binanceSymbol: String,
    val pricePrecision: Int = 2
) {
    BTC_USDT("BTC/USDT", "BTC", "USDT", "BTCUSDT", 2),
    ETH_USDT("ETH/USDT", "ETH", "USDT", "ETHUSDT", 2),
    SOL_USDT("SOL/USDT", "SOL", "USDT", "SOLUSDT", 2),
    BNB_USDT("BNB/USDT", "BNB", "USDT", "BNBUSDT", 2),
    XRP_USDT("XRP/USDT", "XRP", "USDT", "XRPUSDT", 4),
    DOGE_USDT("DOGE/USDT", "DOGE", "USDT", "DOGEUSDT", 5),
    ADA_USDT("ADA/USDT", "ADA", "USDT", "ADAUSDT", 4);

    companion object {
        fun fromSymbol(symbol: String): TradingPair {
            return entries.find { it.symbol.equals(symbol, ignoreCase = true) || it.binanceSymbol.equals(symbol, ignoreCase = true) }
                ?: BTC_USDT
        }
    }
}

enum class Timeframe(
    val id: String,
    val displayName: String,
    val binanceInterval: String,
    val minutes: Int
) {
    TF_1M("1m", "1 MIN", "1m", 1),
    TF_5M("5m", "5 MIN", "5m", 5),
    TF_15M("15m", "15 MIN", "15m", 15),
    TF_30M("30m", "30 MIN", "30m", 30),
    TF_1H("1h", "1 HOUR", "1h", 60),
    TF_4H("4h", "4 HOUR", "4h", 240),
    TF_1D("1d", "1 DAY", "1d", 1440);

    companion object {
        fun fromId(id: String): Timeframe {
            return entries.find { it.id.equals(id, ignoreCase = true) || it.binanceInterval.equals(id, ignoreCase = true) }
                ?: TF_5M
        }
    }
}

enum class PaperDuration(
    val minutes: Int,
    val displayName: String
) {
    MIN_5(5, "5 MIN"),
    MIN_10(10, "10 MIN"),
    MIN_15(15, "15 MIN"),
    MIN_30(30, "30 MIN"),
    HOUR_1(60, "1 HOUR"),
    HOUR_4(240, "4 HOURS");

    companion object {
        fun fromMinutes(minutes: Int): PaperDuration {
            return entries.find { it.minutes == minutes } ?: MIN_10
        }
    }
}

data class Candle(
    val openTime: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val closeTime: Long = 0L
)

data class Ticker24h(
    val symbol: String,
    val lastPrice: Double,
    val priceChange: Double,
    val priceChangePercent: Double,
    val high24h: Double,
    val low24h: Double,
    val volume24h: Double,
    val quoteVolume24h: Double = 0.0,
    val lastUpdated: Long = System.currentTimeMillis()
)
