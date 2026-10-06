# MZK AI Trading — Android APK & Source Project

**MZK AI Trading** is a professional, mobile-first Android application designed for real-time cryptocurrency technical market analysis and simulated paper trading.

> **CRITICAL DISCLAIMER:**  
> This application is strictly for **technical analysis, education, and paper/demo trading only**. It **never connects to real-money trading accounts** and **never executes real orders automatically**. All confidence values are mathematical analytical metrics based on indicator alignment, NOT a guarantee of profit.

---

## 1. How to Build the Release APK

The project is structured as a standard modern Android Gradle project (Kotlin + Jetpack Compose). You can build it on any development machine or within Termux on Android.

### Prerequisites:
- JDK 17 (or JDK 11+)
- Android SDK Platform 36 & Build-Tools
- Gradle 8+

### Build Command:
Run the standard Gradle release task from the project root:

```bash
# On Linux / macOS / Termux:
gradle :app:assembleRelease

# Or to build the debug APK:
gradle :app:assembleDebug
```

> **Note on Signing:**  
> The `app/build.gradle.kts` configuration is pre-configured to automatically sign the release APK using your custom upload keystore (`my-upload-key.jks`) if present, or automatically fall back to the built-in development keystore if building in a local testing environment.

---

## 2. Release APK Location

After running `gradle :app:assembleRelease`, the output APK will be located at:

```
app/build/outputs/apk/release/app-release.apk
```

For debug builds (`gradle :app:assembleDebug`), the APK will be located at:

```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 3. How to Install the APK

### Via ADB (Android Debug Bridge):
```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

### Direct Device Installation:
1. Copy `app-release.apk` to your Android device via USB, Google Drive, or local storage.
2. Open your device's File Manager and tap `app-release.apk`.
3. If prompted, enable **"Allow from this source"** in Android Settings to permit installing apps from your file manager.
4. Tap **Install** and launch **MZK AI Trading**.

---

## 4. How to Configure API Keys

The application supports multiple AI inference providers without requiring hard-coded secrets.

### Option A: App Settings UI (Recommended)
1. Open the app and navigate to the **Settings** tab.
2. Select your desired AI engine:
   - **Quant Algorithm (Default)**: Operates 100% offline using the built-in quantitative signal scoring engine without requiring an external API key.
   - **Google Gemini**: Enter your Google AI Studio Gemini API key.
   - **OpenRouter**: Enter your OpenRouter API key (supports DeepSeek, Claude, etc.).
   - **Groq**: Enter your Groq API key (supports ultra-low-latency Llama 3.3 70B).
3. Tap **Save**. Keys are stored in private Android `SharedPreferences` with password masking.

### Option B: AI Studio Secrets (`.env`)
If running or building via Google AI Studio:
1. Open the **Secrets panel** in AI Studio.
2. Add `GEMINI_API_KEY=your_key_here`.
3. The app automatically injects this key into `BuildConfig.GEMINI_API_KEY` at build time.

---

## 5. How to Configure Market Data Providers

The application connects to **real live crypto market data APIs** and strictly forbids random or fabricated prices.

- **Primary Provider**: Binance Public Market API (`https://api.binance.com/api/v3/`).
  - Fetches real-time price tickers, 24-hour high/low/volume metrics, and multi-timeframe OHLCV candles (`1m`, `5m`, `15m`, `30m`, `1h`, `4h`, `1d`).
  - Requires **no API key**.
- **Secondary Fallback**: CoinGecko Public API (`https://api.coingecko.com/api/v3/`).
  - Automatically activated if Binance is restricted by regional ISP rules or network firewalls.
- **Data Status Display**:
  - The live status header indicates `MARKET DATA: CONNECTED` and `LAST UPDATE: HH:mm:ss`.
  - If network access is lost, the app explicitly displays `MARKET DATA UNAVAILABLE` and `SIGNAL: UNAVAILABLE` rather than generating fake prices.

---

## 6. Primary Architecture & Signal Scoring Flow

1. **Market Selector**: BTC/USDT, ETH/USDT, SOL/USDT, BNB/USDT, XRP/USDT, DOGE/USDT.
2. **Timeframe Engine**: 1m, 5m, 15m, 30m, 1h, 4h, 1d.
3. **Indicator Calculation**:
   - RSI (14-period Wilder's smoothing)
   - MACD (12, 26, 9 with Signal & Histogram)
   - EMA 20, EMA 50, EMA 200
   - Bollinger Bands (20, 2 with Bandwidth %)
   - Volume Trend (vs 20-period moving average)
   - Dynamic Support & Resistance Pivot Levels
4. **Deterministic Signal Scoring (`SignalScoringEngine.kt`)**:
   - EMA Trend: +2 / -2
   - MACD Momentum: +2 / -2
   - RSI Momentum: +1 / -1
   - Volume Surge: +1 / -1
   - Dynamic Support/Resistance: +1 / -1
   - Price Momentum: +1 / -1
   - Net Score determines **BUY**, **SELL**, or **HOLD**.
5. **AI Interpretation**:
   - Selected AI model formats structured narrative and risk parameters.
6. **Paper Trading Simulation Engine (`PaperTradingEngine.kt`)**:
   - 1-tap simulated trade execution.
   - Monitors live price against Stop Loss, Take Profit, and Duration Expiry.
   - Persistent trade history and statistics saved in Room SQLite database.
