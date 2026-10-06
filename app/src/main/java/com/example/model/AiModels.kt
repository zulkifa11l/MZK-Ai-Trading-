package com.example.model

enum class TradeSignal(val displayName: String) {
    BUY("BUY"),
    SELL("SELL"),
    HOLD("HOLD")
}

enum class RiskLevel(val displayName: String) {
    LOW("LOW"),
    MEDIUM("MEDIUM"),
    HIGH("HIGH")
}

enum class AiProviderType(val displayName: String, val description: String) {
    GEMINI("Google Gemini", "Gemini 2.5 Flash analysis with real technical context"),
    OPENROUTER("OpenRouter", "Multi-model router (Llama 3.3, DeepSeek, Claude)"),
    GROQ("Groq Cloud", "Ultra-fast Llama 3.3 70B inference"),
    RULE_ENGINE("Quantitative Quant Engine", "Deterministic algorithmic scoring (Offline & Free)")
}

data class AiAnalysisResult(
    val symbol: String,
    val timeframe: String,
    val signal: TradeSignal,
    val confidence: Int, // 0-100%
    val action: String, // e.g., "BUY NOW — PAPER TRADE ONLY"
    val currentPrice: Double,
    val entry: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val trend: MarketTrend,
    val risk: RiskLevel,
    val durationMinutes: Int,
    val reasons: List<String>,
    val scoreBreakdown: SignalScoreBreakdown? = null,
    val provider: AiProviderType = AiProviderType.RULE_ENGINE,
    val generatedAt: Long = System.currentTimeMillis(),
    val rawResponse: String? = null
)

enum class PaperTradeStatus {
    ACTIVE,
    CLOSED
}

enum class PaperTradeResult {
    PENDING,
    WIN,
    LOSS,
    EXPIRED
}
