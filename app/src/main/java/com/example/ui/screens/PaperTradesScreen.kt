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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ActiveTradeItemCard
import com.example.ui.components.ClosedTradeItemCard
import com.example.ui.components.StatisticsSummaryCard
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.SignalBuyGreen
import com.example.ui.theme.SignalSellRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.TradingViewModel

@Composable
fun PaperTradesScreen(
    viewModel: TradingViewModel,
    modifier: Modifier = Modifier
) {
    val activeTrades by viewModel.activeTrades.collectAsStateWithLifecycle()
    val closedTrades by viewModel.closedTrades.collectAsStateWithLifecycle()
    val stats by viewModel.tradingStats.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Trade History?", color = TextPrimary) },
            text = { Text("This will permanently delete all completed paper trade history and reset your statistics.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearTradeHistory()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = SignalSellRed)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = ObsidianCard
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .testTag("paper_trades_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp)
    ) {
        // Performance Stats Overview
        item {
            Box(modifier = Modifier.padding(16.dp)) {
                StatisticsSummaryCard(stats = stats)
            }
        }

        // Tabs: Active vs Completed
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = ObsidianBackground,
                    contentColor = SignalBuyGreen,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = SignalBuyGreen
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = {
                            Text(
                                "ACTIVE (${activeTrades.size})",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTabIndex == 0) SignalBuyGreen else TextMuted
                            )
                        }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = {
                            Text(
                                "COMPLETED (${closedTrades.size})",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTabIndex == 1) SignalBuyGreen else TextMuted
                            )
                        }
                    )
                }

                if (selectedTabIndex == 1 && closedTrades.isNotEmpty()) {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear History", tint = TextMuted)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (selectedTabIndex == 0) {
            if (activeTrades.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No Active Paper Trades",
                        subtitle = "Go to the Dashboard and click 'Start Paper Trade' when a BUY or SELL signal is generated."
                    )
                }
            } else {
                items(activeTrades.size) { index ->
                    val trade = activeTrades[index]
                    ActiveTradeItemCard(
                        trade = trade,
                        onCloseTrade = { viewModel.manualCloseTrade(it) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        } else {
            if (closedTrades.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No Completed Trades Yet",
                        subtitle = "Once your active paper trades reach Take Profit, Stop Loss, or duration expires, they will appear here with full win/loss analytics."
                    )
                }
            } else {
                items(closedTrades.size) { index ->
                    val trade = closedTrades[index]
                    ClosedTradeItemCard(
                        trade = trade,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                lineHeight = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
