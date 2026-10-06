package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.market.MarketState
import com.example.data.db.PaperTradeEntity
import com.example.ui.components.ActiveTradeItemCard
import com.example.ui.components.MarketControls
import com.example.ui.components.PrimarySignalCard
import com.example.ui.components.SignalScoreBreakdownCard
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.SignalSellRed
import com.example.ui.theme.SignalSellRedContainer
import com.example.ui.theme.TechCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodel.AiAnalysisState
import com.example.ui.viewmodel.TradingViewModel

@Composable
fun DashboardScreen(
    viewModel: TradingViewModel,
    modifier: Modifier = Modifier
) {
    val selectedPair by viewModel.selectedPair.collectAsStateWithLifecycle()
    val selectedTimeframe by viewModel.selectedTimeframe.collectAsStateWithLifecycle()
    val selectedDuration by viewModel.selectedDuration.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val marketState by viewModel.marketState.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val signalResult by viewModel.activeSignalResult.collectAsStateWithLifecycle()
    val scoreBreakdown by viewModel.scoreBreakdown.collectAsStateWithLifecycle()
    val activeTrades by viewModel.activeTrades.collectAsStateWithLifecycle()

    val ticker = (marketState as? MarketState.Success)?.ticker
    val isAnalyzing = aiState is AiAnalysisState.Analyzing

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .testTag("dashboard_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp)
    ) {
        // Market & Model Controls
        item {
            MarketControls(
                selectedPair = selectedPair,
                onPairSelected = { viewModel.selectPair(it) },
                selectedTimeframe = selectedTimeframe,
                onTimeframeSelected = { viewModel.selectTimeframe(it) },
                selectedDuration = selectedDuration,
                onDurationSelected = { viewModel.selectDuration(it) },
                selectedProvider = settings.selectedProvider,
                onProviderSelected = { viewModel.selectProvider(it) },
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Market Data Unavailable Warning Banner
        if (marketState is MarketState.Unavailable) {
            val errorMsg = (marketState as MarketState.Unavailable).message
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = SignalSellRedContainer),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = SignalSellRed,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "MARKET DATA UNAVAILABLE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SignalSellRed
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = errorMsg,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.refresh() },
                            colors = ButtonDefaults.buttonColors(containerColor = SignalSellRed)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Retry Connection", modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
            }
        }

        // PRIMARY SIGNAL CARD (The main feature)
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                PrimarySignalCard(
                    ticker = ticker,
                    signalResult = signalResult,
                    isAnalyzing = isAnalyzing,
                    onRefresh = { viewModel.refresh() },
                    onStartPaperTrade = { viewModel.startPaperTrade() }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Active Trades Quick Glance (if any)
        if (activeTrades.isNotEmpty()) {
            item {
                Text(
                    text = "ACTIVE PAPER POSITIONS (${activeTrades.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
            items(activeTrades.size) { index ->
                val trade = activeTrades[index]
                ActiveTradeItemCard(
                    trade = trade,
                    onCloseTrade = { viewModel.manualCloseTrade(it) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            item {
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // Transparent Scoring Breakdown Card
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                SignalScoreBreakdownCard(breakdown = scoreBreakdown)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
