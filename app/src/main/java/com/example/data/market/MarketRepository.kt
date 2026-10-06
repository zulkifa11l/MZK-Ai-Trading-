package com.example.data.market

import com.example.model.Candle
import com.example.model.Ticker24h
import com.example.model.Timeframe
import com.example.model.TradingPair
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class MarketState {
    object Loading : MarketState()
    data class Success(val ticker: Ticker24h, val candles: List<Candle>) : MarketState()
    data class Unavailable(val message: String) : MarketState()
}

class MarketRepository(
    private val dataProvider: MarketDataProvider = BinancePublicMarketDataProvider()
) {
    private val _marketState = MutableStateFlow<MarketState>(MarketState.Loading)
    val marketState: StateFlow<MarketState> = _marketState.asStateFlow()

    suspend fun refreshMarketData(pair: TradingPair, timeframe: Timeframe): MarketState {
        _marketState.value = MarketState.Loading

        val tickerResult = dataProvider.fetchTicker(pair)
        if (tickerResult is MarketDataResult.Error) {
            val state = MarketState.Unavailable(tickerResult.message)
            _marketState.value = state
            return state
        }

        val candlesResult = dataProvider.fetchCandles(pair, timeframe, limit = 100)
        if (candlesResult is MarketDataResult.Error) {
            val state = MarketState.Unavailable(candlesResult.message)
            _marketState.value = state
            return state
        }

        val ticker = (tickerResult as MarketDataResult.Success).data
        val candles = (candlesResult as MarketDataResult.Success).data

        val state = MarketState.Success(ticker, candles)
        _marketState.value = state
        return state
    }
}
