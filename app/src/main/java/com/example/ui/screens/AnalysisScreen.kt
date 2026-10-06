package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.MarketControls
import com.example.ui.components.SignalScoreBreakdownCard
import com.example.ui.components.TechnicalIndicatorsCard
import com.example.ui.theme.ObsidianBackground
import com.example.ui.viewmodel.TradingViewModel

@Composable
fun AnalysisScreen(
    viewModel: TradingViewModel,
    modifier: Modifier = Modifier
) {
    val selectedPair by viewModel.selectedPair.collectAsStateWithLifecycle()
    val selectedTimeframe by viewModel.selectedTimeframe.collectAsStateWithLifecycle()
    val selectedDuration by viewModel.selectedDuration.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val indicators by viewModel.indicators.collectAsStateWithLifecycle()
    val scoreBreakdown by viewModel.scoreBreakdown.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .testTag("analysis_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp)
    ) {
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

        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                TechnicalIndicatorsCard(indicators = indicators)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                SignalScoreBreakdownCard(breakdown = scoreBreakdown)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
