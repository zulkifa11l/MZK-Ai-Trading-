package com.example.data.settings

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.model.AiProviderType
import com.example.model.PaperDuration
import com.example.model.Timeframe
import com.example.model.TradingPair
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val selectedProvider: AiProviderType = AiProviderType.RULE_ENGINE,
    val geminiApiKey: String = "",
    val openRouterApiKey: String = "",
    val groqApiKey: String = "",
    val openRouterModel: String = "meta-llama/llama-3.3-70b-instruct",
    val groqModel: String = "llama-3.3-70b-versatile",
    val defaultDuration: PaperDuration = PaperDuration.MIN_10,
    val virtualStartingBalance: Double = 10000.0,
    val virtualPositionSizeUsd: Double = 1000.0,
    val autoRefreshSeconds: Int = 10,
    val vibrationEnabled: Boolean = true,
    val selectedPair: TradingPair = TradingPair.BTC_USDT,
    val selectedTimeframe: Timeframe = Timeframe.TF_5M
)

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mzk_trading_settings", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<AppSettings> = _settingsFlow.asStateFlow()

    private fun loadSettings(): AppSettings {
        val providerStr = prefs.getString("selected_provider", AiProviderType.RULE_ENGINE.name)
        val provider = try {
            AiProviderType.valueOf(providerStr ?: AiProviderType.RULE_ENGINE.name)
        } catch (_: Exception) {
            AiProviderType.RULE_ENGINE
        }

        // Try getting GEMINI_API_KEY from BuildConfig if set
        val buildConfigGeminiKey = try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            val key = field.get(null) as? String ?: ""
            if (key == "MY_GEMINI_API_KEY") "" else key
        } catch (_: Exception) {
            ""
        }

        val storedGeminiKey = prefs.getString("gemini_api_key", "") ?: ""
        val effectiveGeminiKey = storedGeminiKey.ifEmpty { buildConfigGeminiKey }

        val storedOpenRouterKey = prefs.getString("openrouter_api_key", "") ?: ""
        val storedGroqKey = prefs.getString("groq_api_key", "") ?: ""

        val durationMinutes = prefs.getInt("default_duration_min", 10)
        val duration = PaperDuration.fromMinutes(durationMinutes)

        val pairStr = prefs.getString("selected_pair", TradingPair.BTC_USDT.name) ?: TradingPair.BTC_USDT.name
        val pair = try { TradingPair.valueOf(pairStr) } catch (_: Exception) { TradingPair.BTC_USDT }

        val tfStr = prefs.getString("selected_timeframe", Timeframe.TF_5M.name) ?: Timeframe.TF_5M.name
        val tf = try { Timeframe.valueOf(tfStr) } catch (_: Exception) { Timeframe.TF_5M }

        return AppSettings(
            selectedProvider = provider,
            geminiApiKey = effectiveGeminiKey,
            openRouterApiKey = storedOpenRouterKey,
            groqApiKey = storedGroqKey,
            openRouterModel = prefs.getString("openrouter_model", "meta-llama/llama-3.3-70b-instruct") ?: "meta-llama/llama-3.3-70b-instruct",
            groqModel = prefs.getString("groq_model", "llama-3.3-70b-versatile") ?: "llama-3.3-70b-versatile",
            defaultDuration = duration,
            virtualStartingBalance = prefs.getFloat("virtual_balance", 10000.0f).toDouble(),
            virtualPositionSizeUsd = prefs.getFloat("virtual_pos_size", 1000.0f).toDouble(),
            autoRefreshSeconds = prefs.getInt("refresh_sec", 10),
            vibrationEnabled = prefs.getBoolean("vibration_enabled", true),
            selectedPair = pair,
            selectedTimeframe = tf
        )
    }

    fun updateProvider(provider: AiProviderType) {
        prefs.edit().putString("selected_provider", provider.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(selectedProvider = provider)
    }

    fun updateGeminiApiKey(key: String) {
        prefs.edit().putString("gemini_api_key", key.trim()).apply()
        _settingsFlow.value = _settingsFlow.value.copy(geminiApiKey = key.trim())
    }

    fun updateOpenRouterApiKey(key: String) {
        prefs.edit().putString("openrouter_api_key", key.trim()).apply()
        _settingsFlow.value = _settingsFlow.value.copy(openRouterApiKey = key.trim())
    }

    fun updateGroqApiKey(key: String) {
        prefs.edit().putString("groq_api_key", key.trim()).apply()
        _settingsFlow.value = _settingsFlow.value.copy(groqApiKey = key.trim())
    }

    fun updateSelectedPair(pair: TradingPair) {
        prefs.edit().putString("selected_pair", pair.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(selectedPair = pair)
    }

    fun updateSelectedTimeframe(tf: Timeframe) {
        prefs.edit().putString("selected_timeframe", tf.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(selectedTimeframe = tf)
    }

    fun updateDuration(duration: PaperDuration) {
        prefs.edit().putInt("default_duration_min", duration.minutes).apply()
        _settingsFlow.value = _settingsFlow.value.copy(defaultDuration = duration)
    }

    fun updatePositionSize(size: Double) {
        prefs.edit().putFloat("virtual_pos_size", size.toFloat()).apply()
        _settingsFlow.value = _settingsFlow.value.copy(virtualPositionSizeUsd = size)
    }

    fun updateVibration(enabled: Boolean) {
        prefs.edit().putBoolean("vibration_enabled", enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(vibrationEnabled = enabled)
    }

    fun getEffectiveApiKey(provider: AiProviderType): String {
        return when (provider) {
            AiProviderType.GEMINI -> _settingsFlow.value.geminiApiKey
            AiProviderType.OPENROUTER -> _settingsFlow.value.openRouterApiKey
            AiProviderType.GROQ -> _settingsFlow.value.groqApiKey
            AiProviderType.RULE_ENGINE -> "N/A"
        }
    }
}
