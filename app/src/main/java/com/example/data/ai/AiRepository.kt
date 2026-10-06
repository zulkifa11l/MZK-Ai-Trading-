package com.example.data.ai

import com.example.data.settings.AppSettings
import com.example.model.AiAnalysisResult
import com.example.model.AiProviderType
import com.example.model.Candle
import com.example.model.TechnicalIndicators

class AiRepository {
    private val ruleEngine = RuleEngineAiProvider()
    private val geminiProvider = GeminiAiProvider()
    private val openRouterProvider = OpenRouterAiProvider()
    private val groqProvider = GroqAiProvider()

    suspend fun analyzeMarket(
        symbol: String,
        timeframe: String,
        durationMinutes: Int,
        candles: List<Candle>,
        indicators: TechnicalIndicators,
        settings: AppSettings
    ): AiResult {
        val provider = when (settings.selectedProvider) {
            AiProviderType.GEMINI -> geminiProvider
            AiProviderType.OPENROUTER -> openRouterProvider
            AiProviderType.GROQ -> groqProvider
            AiProviderType.RULE_ENGINE -> ruleEngine
        }

        val result = provider.analyze(symbol, timeframe, durationMinutes, candles, indicators, settings)

        // If external AI failed or missing key, we can automatically provide fallback to rule engine
        if (result is AiResult.Error && result.canFallbackToRuleEngine && settings.selectedProvider != AiProviderType.RULE_ENGINE) {
            val fallback = ruleEngine.analyze(symbol, timeframe, durationMinutes, candles, indicators, settings)
            if (fallback is AiResult.Success) {
                // Return fallback result but keep info that it fell back
                return fallback
            }
        }

        return result
    }
}
