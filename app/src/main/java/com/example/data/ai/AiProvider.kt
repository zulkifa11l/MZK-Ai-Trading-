package com.example.data.ai

import com.example.analysis.TechnicalAnalysisEngine
import com.example.data.settings.AppSettings
import com.example.model.AiAnalysisResult
import com.example.model.AiProviderType
import com.example.model.Candle
import com.example.model.MarketTrend
import com.example.model.RiskLevel
import com.example.model.TechnicalIndicators
import com.example.model.TradeSignal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

sealed class AiResult {
    data class Success(val result: AiAnalysisResult) : AiResult()
    data class Error(val message: String, val canFallbackToRuleEngine: Boolean = true) : AiResult()
}

interface AiProvider {
    val providerType: AiProviderType
    suspend fun analyze(
        symbol: String,
        timeframe: String,
        durationMinutes: Int,
        candles: List<Candle>,
        indicators: TechnicalIndicators,
        settings: AppSettings
    ): AiResult
}

class RuleEngineAiProvider : AiProvider {
    override val providerType: AiProviderType = AiProviderType.RULE_ENGINE

    override suspend fun analyze(
        symbol: String,
        timeframe: String,
        durationMinutes: Int,
        candles: List<Candle>,
        indicators: TechnicalIndicators,
        settings: AppSettings
    ): AiResult {
        val (breakdown, result) = TechnicalAnalysisEngine.generateSignalAndScore(
            symbol = symbol,
            timeframe = timeframe,
            durationMinutes = durationMinutes,
            indicators = indicators
        )
        return AiResult.Success(result)
    }
}

class GeminiAiProvider : AiProvider {
    override val providerType: AiProviderType = AiProviderType.GEMINI

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    override suspend fun analyze(
        symbol: String,
        timeframe: String,
        durationMinutes: Int,
        candles: List<Candle>,
        indicators: TechnicalIndicators,
        settings: AppSettings
    ): AiResult = withContext(Dispatchers.IO) {
        val apiKey = settings.geminiApiKey
        if (apiKey.isBlank()) {
            return@withContext AiResult.Error(
                message = "Google Gemini API key is not configured. Please add your Gemini API key in Settings, or use the Quantitative Rule Engine.",
                canFallbackToRuleEngine = true
            )
        }

        val prompt = buildAnalysisPrompt(symbol, timeframe, durationMinutes, indicators)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        val requestBodyJson = JSONObject().apply {
            val contents = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            }
            put("contents", contents)
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.2)
            })
        }

        try {
            val request = Request.Builder()
                .url(url)
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: throw IOException("Empty response from Gemini")
                if (!response.isSuccessful) {
                    val errJson = try { JSONObject(bodyStr) } catch (_: Exception) { null }
                    val errMsg = errJson?.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                    return@withContext AiResult.Error("Gemini API Error: $errMsg", canFallbackToRuleEngine = true)
                }

                val jsonResponse = JSONObject(bodyStr)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext AiResult.Error("No analysis candidates returned by Gemini", canFallbackToRuleEngine = true)
                }

                val text = candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                parseAiJsonResponse(text, symbol, timeframe, durationMinutes, indicators, AiProviderType.GEMINI)
            }
        } catch (e: Exception) {
            AiResult.Error("Failed to reach Gemini: ${e.localizedMessage ?: "Unknown error"}", canFallbackToRuleEngine = true)
        }
    }
}

class OpenRouterAiProvider : AiProvider {
    override val providerType: AiProviderType = AiProviderType.OPENROUTER

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    override suspend fun analyze(
        symbol: String,
        timeframe: String,
        durationMinutes: Int,
        candles: List<Candle>,
        indicators: TechnicalIndicators,
        settings: AppSettings
    ): AiResult = withContext(Dispatchers.IO) {
        val apiKey = settings.openRouterApiKey
        if (apiKey.isBlank()) {
            return@withContext AiResult.Error(
                message = "OpenRouter API key is not configured. Please enter your OpenRouter key in Settings.",
                canFallbackToRuleEngine = true
            )
        }

        val prompt = buildAnalysisPrompt(symbol, timeframe, durationMinutes, indicators)
        val url = "https://openrouter.ai/api/v1/chat/completions"

        val requestBodyJson = JSONObject().apply {
            put("model", settings.openRouterModel)
            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are an elite quantitative trading analyst. Respond strictly in valid raw JSON.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            }
            put("messages", messages)
            put("response_format", JSONObject().apply { put("type", "json_object") })
            put("temperature", 0.2)
        }

        try {
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $apiKey")
                .header("HTTP-Referer", "https://github.com/mzk-ai-trading")
                .header("X-Title", "MZK AI Trading")
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: throw IOException("Empty response")
                if (!response.isSuccessful) {
                    return@withContext AiResult.Error("OpenRouter Error: HTTP ${response.code} $bodyStr", canFallbackToRuleEngine = true)
                }

                val jsonResponse = JSONObject(bodyStr)
                val text = jsonResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")

                parseAiJsonResponse(text, symbol, timeframe, durationMinutes, indicators, AiProviderType.OPENROUTER)
            }
        } catch (e: Exception) {
            AiResult.Error("OpenRouter Error: ${e.localizedMessage}", canFallbackToRuleEngine = true)
        }
    }
}

class GroqAiProvider : AiProvider {
    override val providerType: AiProviderType = AiProviderType.GROQ

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    override suspend fun analyze(
        symbol: String,
        timeframe: String,
        durationMinutes: Int,
        candles: List<Candle>,
        indicators: TechnicalIndicators,
        settings: AppSettings
    ): AiResult = withContext(Dispatchers.IO) {
        val apiKey = settings.groqApiKey
        if (apiKey.isBlank()) {
            return@withContext AiResult.Error(
                message = "Groq API key is not configured. Please enter your Groq API key in Settings.",
                canFallbackToRuleEngine = true
            )
        }

        val prompt = buildAnalysisPrompt(symbol, timeframe, durationMinutes, indicators)
        val url = "https://api.groq.com/openai/v1/chat/completions"

        val requestBodyJson = JSONObject().apply {
            put("model", settings.groqModel)
            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are an elite crypto quantitative analyst. Output ONLY valid JSON.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            }
            put("messages", messages)
            put("response_format", JSONObject().apply { put("type", "json_object") })
            put("temperature", 0.2)
        }

        try {
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $apiKey")
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: throw IOException("Empty response")
                if (!response.isSuccessful) {
                    return@withContext AiResult.Error("Groq Error: HTTP ${response.code} $bodyStr", canFallbackToRuleEngine = true)
                }

                val jsonResponse = JSONObject(bodyStr)
                val text = jsonResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")

                parseAiJsonResponse(text, symbol, timeframe, durationMinutes, indicators, AiProviderType.GROQ)
            }
        } catch (e: Exception) {
            AiResult.Error("Groq Error: ${e.localizedMessage}", canFallbackToRuleEngine = true)
        }
    }
}

private fun buildAnalysisPrompt(
    symbol: String,
    timeframe: String,
    durationMinutes: Int,
    indicators: TechnicalIndicators
): String {
    return """
    Analyze the following LIVE REAL MARKET TECHNICAL DATA for $symbol on timeframe $timeframe.
    This analysis is STRICTLY for educational PAPER TRADING only. Never promise guaranteed returns.
    
    Current Price: ${indicators.currentPrice}
    RSI (14): ${String.format("%.2f", indicators.rsi)}
    MACD Line: ${String.format("%.2f", indicators.macd.macd)}
    MACD Signal: ${String.format("%.2f", indicators.macd.signal)}
    MACD Histogram: ${String.format("%.2f", indicators.macd.histogram)}
    EMA 20: ${String.format("%.2f", indicators.ema20)}
    EMA 50: ${String.format("%.2f", indicators.ema50)}
    EMA 200: ${String.format("%.2f", indicators.ema200)}
    Bollinger Upper: ${String.format("%.2f", indicators.bollingerBands.upper)}
    Bollinger Lower: ${String.format("%.2f", indicators.bollingerBands.lower)}
    Bollinger %B: ${String.format("%.2f", indicators.bollingerBands.percentB)}
    Volume Trend: ${indicators.volumeTrend} (${String.format("%.1f", indicators.volumeChangePercent)}% vs 20-period average)
    Support Floor: ${String.format("%.2f", indicators.support)}
    Resistance Ceiling: ${String.format("%.2f", indicators.resistance)}
    ATR: ${String.format("%.2f", indicators.atr)}
    Paper Duration: $durationMinutes minutes
    
    RULES:
    1. Determine signal: BUY (bullish setup), SELL (bearish setup), or HOLD (mixed/weak/unclear evidence).
    2. Provide confidence (integer between 50 and 88). Do NOT guarantee profit.
    3. Action must be: "BUY NOW — PAPER TRADE ONLY", "SELL NOW — PAPER TRADE ONLY", or "HOLD — WAIT FOR BETTER SETUP".
    4. Provide realistic stop_loss and take_profit based on the support/resistance and ATR levels.
    5. Provide 3 to 5 concise bullet reasons explaining EMA trend, MACD, RSI, and volume evidence.
    
    Respond STRICTLY with this JSON format:
    {
      "symbol": "$symbol",
      "timeframe": "$timeframe",
      "signal": "BUY" | "SELL" | "HOLD",
      "confidence": 78,
      "action": "BUY NOW — PAPER TRADE ONLY",
      "entry": ${indicators.currentPrice},
      "stop_loss": 0.0,
      "take_profit": 0.0,
      "trend": "BULLISH" | "BEARISH" | "NEUTRAL",
      "risk": "LOW" | "MEDIUM" | "HIGH",
      "reasons": [
        "EMA trend is bullish",
        "MACD momentum is positive",
        "Volume is increasing"
      ]
    }
    """.trimIndent()
}

private fun parseAiJsonResponse(
    jsonText: String,
    symbol: String,
    timeframe: String,
    durationMinutes: Int,
    indicators: TechnicalIndicators,
    provider: AiProviderType
): AiResult {
    try {
        val cleanJson = jsonText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val obj = JSONObject(cleanJson)

        val signalStr = obj.optString("signal", "HOLD").uppercase()
        val signal = try { TradeSignal.valueOf(signalStr) } catch (_: Exception) { TradeSignal.HOLD }

        val confidence = obj.optInt("confidence", 65).coerceIn(50, 95)
        val action = obj.optString("action", when (signal) {
            TradeSignal.BUY -> "BUY NOW — PAPER TRADE ONLY"
            TradeSignal.SELL -> "SELL NOW — PAPER TRADE ONLY"
            TradeSignal.HOLD -> "HOLD — WAIT FOR BETTER SETUP"
        })

        val entry = obj.optDouble("entry", indicators.currentPrice)
        val stopLoss = obj.optDouble("stop_loss", indicators.currentPrice * 0.985)
        val takeProfit = obj.optDouble("take_profit", indicators.currentPrice * 1.025)

        val trendStr = obj.optString("trend", indicators.trend.name).uppercase()
        val trend = try { MarketTrend.valueOf(trendStr) } catch (_: Exception) { indicators.trend }

        val riskStr = obj.optString("risk", "MEDIUM").uppercase()
        val risk = try { RiskLevel.valueOf(riskStr) } catch (_: Exception) { RiskLevel.MEDIUM }

        val reasonsList = mutableListOf<String>()
        val reasonsArray = obj.optJSONArray("reasons")
        if (reasonsArray != null) {
            for (i in 0 until reasonsArray.length()) {
                val r = reasonsArray.optString(i)
                if (r.isNotBlank()) reasonsList.add(r)
            }
        }
        if (reasonsList.isEmpty()) {
            reasonsList.add("Synthesized from EMA, MACD, and RSI confluence")
        }

        // Attach deterministic breakdown for transparency
        val (breakdown, _) = TechnicalAnalysisEngine.generateSignalAndScore(
            symbol = symbol,
            timeframe = timeframe,
            durationMinutes = durationMinutes,
            indicators = indicators
        )

        val result = AiAnalysisResult(
            symbol = symbol,
            timeframe = timeframe,
            signal = signal,
            confidence = confidence,
            action = action,
            currentPrice = indicators.currentPrice,
            entry = entry,
            stopLoss = stopLoss,
            takeProfit = takeProfit,
            trend = trend,
            risk = risk,
            durationMinutes = durationMinutes,
            reasons = reasonsList,
            scoreBreakdown = breakdown,
            provider = provider,
            generatedAt = System.currentTimeMillis(),
            rawResponse = jsonText
        )

        return AiResult.Success(result)
    } catch (e: Exception) {
        return AiResult.Error("Failed to parse AI structured response: ${e.localizedMessage}")
    }
}
