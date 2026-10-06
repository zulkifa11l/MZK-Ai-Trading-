package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.analysis.TechnicalAnalysisEngine
import com.example.data.ai.AiRepository
import com.example.data.ai.AiResult
import com.example.data.db.AppDatabase
import com.example.data.db.PaperTradeEntity
import com.example.data.db.TradeRepository
import com.example.data.db.TradingStatistics
import com.example.data.market.BinancePublicMarketDataProvider
import com.example.data.market.MarketDataProvider
import com.example.data.market.MarketRepository
import com.example.data.market.MarketState
import com.example.data.settings.AppSettings
import com.example.data.settings.SettingsManager
import com.example.engine.PaperTradingEngine
import com.example.model.AiAnalysisResult
import com.example.model.AiProviderType
import com.example.model.Candle
import com.example.model.PaperDuration
import com.example.model.SignalScoreBreakdown
import com.example.model.TechnicalIndicators
import com.example.model.Ticker24h
import com.example.model.Timeframe
import com.example.model.TradingPair
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed class AiAnalysisState {
    object Idle : AiAnalysisState()
    object Analyzing : AiAnalysisState()
    data class Success(val result: AiAnalysisResult) : AiAnalysisState()
    data class Error(val message: String, val canFallback: Boolean = true) : AiAnalysisState()
}

class TradingViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val settingsManager = SettingsManager(application)
    private val tradeRepository = TradeRepository(db.paperTradeDao())
    private val paperEngine = PaperTradingEngine(tradeRepository, settingsManager)
    private val marketRepository = MarketRepository(BinancePublicMarketDataProvider())
    private val aiRepository = AiRepository()

    // Settings State
    val settings: StateFlow<AppSettings> = settingsManager.settingsFlow

    // Current Selection
    private val _selectedPair = MutableStateFlow(settings.value.selectedPair)
    val selectedPair: StateFlow<TradingPair> = _selectedPair.asStateFlow()

    private val _selectedTimeframe = MutableStateFlow(settings.value.selectedTimeframe)
    val selectedTimeframe: StateFlow<Timeframe> = _selectedTimeframe.asStateFlow()

    private val _selectedDuration = MutableStateFlow(settings.value.defaultDuration)
    val selectedDuration: StateFlow<PaperDuration> = _selectedDuration.asStateFlow()

    // Market State
    val marketState: StateFlow<MarketState> = marketRepository.marketState

    // Live Technical Indicators & Scoring
    private val _indicators = MutableStateFlow<TechnicalIndicators?>(null)
    val indicators: StateFlow<TechnicalIndicators?> = _indicators.asStateFlow()

    private val _scoreBreakdown = MutableStateFlow<SignalScoreBreakdown?>(null)
    val scoreBreakdown: StateFlow<SignalScoreBreakdown?> = _scoreBreakdown.asStateFlow()

    // Primary AI Signal
    private val _aiState = MutableStateFlow<AiAnalysisState>(AiAnalysisState.Idle)
    val aiState: StateFlow<AiAnalysisState> = _aiState.asStateFlow()

    private val _activeSignalResult = MutableStateFlow<AiAnalysisResult?>(null)
    val activeSignalResult: StateFlow<AiAnalysisResult?> = _activeSignalResult.asStateFlow()

    // User Notification / Toast Message
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Trade History & Active Trades Flows
    val activeTrades: StateFlow<List<PaperTradeEntity>> = tradeRepository.activeTrades
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val closedTrades: StateFlow<List<PaperTradeEntity>> = tradeRepository.closedTrades
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tradingStats: StateFlow<TradingStatistics> = tradeRepository.statistics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TradingStatistics())

    private var pollingJob: Job? = null

    init {
        // Initial fetch
        loadMarketDataAndAnalyze()
        startPeriodicPolling()
    }

    fun selectPair(pair: TradingPair) {
        if (_selectedPair.value == pair) return
        _selectedPair.value = pair
        settingsManager.updateSelectedPair(pair)
        loadMarketDataAndAnalyze()
    }

    fun selectTimeframe(tf: Timeframe) {
        if (_selectedTimeframe.value == tf) return
        _selectedTimeframe.value = tf
        settingsManager.updateSelectedTimeframe(tf)
        loadMarketDataAndAnalyze()
    }

    fun selectDuration(duration: PaperDuration) {
        _selectedDuration.value = duration
        settingsManager.updateDuration(duration)
        // Refresh signal with new duration if available
        val currentInd = _indicators.value
        if (currentInd != null) {
            val (breakdown, res) = TechnicalAnalysisEngine.generateSignalAndScore(
                symbol = _selectedPair.value.symbol,
                timeframe = _selectedTimeframe.value.displayName,
                durationMinutes = duration.minutes,
                indicators = currentInd
            )
            _scoreBreakdown.value = breakdown
            _activeSignalResult.value = res
        }
    }

    fun selectProvider(provider: AiProviderType) {
        settingsManager.updateProvider(provider)
        requestAiAnalysis()
    }

    fun refresh() {
        loadMarketDataAndAnalyze()
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    private fun startPeriodicPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(10_000L) // Poll every 10 seconds for real market prices
                val pair = _selectedPair.value
                val tf = _selectedTimeframe.value
                val res = marketRepository.refreshMarketData(pair, tf)
                if (res is MarketState.Success) {
                    onMarketDataLoaded(res.ticker, res.candles)
                }
            }
        }
    }

    private fun loadMarketDataAndAnalyze() {
        viewModelScope.launch {
            val pair = _selectedPair.value
            val tf = _selectedTimeframe.value
            val res = marketRepository.refreshMarketData(pair, tf)
            if (res is MarketState.Success) {
                onMarketDataLoaded(res.ticker, res.candles)
                requestAiAnalysis()
            } else if (res is MarketState.Unavailable) {
                _indicators.value = null
                _scoreBreakdown.value = null
                _activeSignalResult.value = null
                _aiState.value = AiAnalysisState.Error("MARKET DATA UNAVAILABLE: Check internet connection or retry.")
            }
        }
    }

    private suspend fun onMarketDataLoaded(ticker: Ticker24h, candles: List<Candle>) {
        val calculated = TechnicalAnalysisEngine.calculateIndicators(candles)
        _indicators.value = calculated

        if (calculated != null) {
            // Update deterministic scoring breakdown
            val (breakdown, ruleResult) = TechnicalAnalysisEngine.generateSignalAndScore(
                symbol = _selectedPair.value.symbol,
                timeframe = _selectedTimeframe.value.displayName,
                durationMinutes = _selectedDuration.value.minutes,
                indicators = calculated
            )
            _scoreBreakdown.value = breakdown

            // If no external AI analysis active, use rule result
            if (_aiState.value !is AiAnalysisState.Success) {
                _activeSignalResult.value = ruleResult
            }

            // Update active paper trades P/L against live ticker price
            paperEngine.onPriceTick(ticker.symbol, ticker.lastPrice)
        }
    }

    fun requestAiAnalysis() {
        val currentInd = _indicators.value
        val mState = marketRepository.marketState.value
        if (currentInd == null || mState !is MarketState.Success) {
            return
        }

        val pair = _selectedPair.value
        val tf = _selectedTimeframe.value
        val duration = _selectedDuration.value
        val currentSettings = settings.value

        viewModelScope.launch {
            _aiState.value = AiAnalysisState.Analyzing

            val aiResult = aiRepository.analyzeMarket(
                symbol = pair.symbol,
                timeframe = tf.displayName,
                durationMinutes = duration.minutes,
                candles = mState.candles,
                indicators = currentInd,
                settings = currentSettings
            )

            when (aiResult) {
                is AiResult.Success -> {
                    _activeSignalResult.value = aiResult.result
                    _aiState.value = AiAnalysisState.Success(aiResult.result)
                }
                is AiResult.Error -> {
                    _aiState.value = AiAnalysisState.Error(aiResult.message, aiResult.canFallbackToRuleEngine)
                    _userMessage.value = aiResult.message
                    // Fall back to rule engine result so user is never left without a signal
                    val (_, fallbackResult) = TechnicalAnalysisEngine.generateSignalAndScore(
                        symbol = pair.symbol,
                        timeframe = tf.displayName,
                        durationMinutes = duration.minutes,
                        indicators = currentInd
                    )
                    _activeSignalResult.value = fallbackResult
                }
            }
        }
    }

    fun startPaperTrade() {
        val signal = _activeSignalResult.value ?: return
        viewModelScope.launch {
            val id = paperEngine.openTradeFromSignal(signal)
            if (id > 0) {
                _userMessage.value = "Paper trade opened for ${signal.symbol} at $${String.format("%,.2f", signal.entry)}"
            } else {
                _userMessage.value = "Cannot open trade when signal is HOLD"
            }
        }
    }

    fun manualCloseTrade(tradeId: Long) {
        val currentTicker = (marketState.value as? MarketState.Success)?.ticker ?: return
        viewModelScope.launch {
            paperEngine.manualCloseTrade(tradeId, currentTicker.lastPrice)
            _userMessage.value = "Trade manually closed at market price"
        }
    }

    fun clearTradeHistory() {
        viewModelScope.launch {
            tradeRepository.clearHistory()
            _userMessage.value = "Trade history cleared"
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
