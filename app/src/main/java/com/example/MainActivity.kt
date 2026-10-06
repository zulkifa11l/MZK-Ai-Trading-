package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AnalysisScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PaperTradesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MZKAITradingTheme
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.SignalBuyGreen
import com.example.ui.theme.TextMuted
import com.example.ui.viewmodel.TradingViewModel

enum class NavigationDestination(val title: String) {
    DASHBOARD("Signals"),
    PAPER_TRADES("Paper Trades"),
    INDICATORS("Indicators"),
    SETTINGS("Settings")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MZKAITradingTheme {
                val viewModel: TradingViewModel = viewModel()
                val snackbarHostState = remember { SnackbarHostState() }
                val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

                LaunchedEffect(userMessage) {
                    userMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearMessage()
                    }
                }

                var currentTab by remember { mutableStateOf(NavigationDestination.DASHBOARD) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = ObsidianBackground,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        NavigationBar(
                            containerColor = ObsidianCard,
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = currentTab == NavigationDestination.DASHBOARD,
                                onClick = { currentTab = NavigationDestination.DASHBOARD },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = "Signals"
                                    )
                                },
                                label = {
                                    Text(
                                        "Signals",
                                        fontWeight = if (currentTab == NavigationDestination.DASHBOARD) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SignalBuyGreen,
                                    selectedTextColor = SignalBuyGreen,
                                    indicatorColor = SignalBuyGreen.copy(alpha = 0.15f),
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_signals")
                            )

                            NavigationBarItem(
                                selected = currentTab == NavigationDestination.PAPER_TRADES,
                                onClick = { currentTab = NavigationDestination.PAPER_TRADES },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = "Paper Trades"
                                    )
                                },
                                label = {
                                    Text(
                                        "Paper Trades",
                                        fontWeight = if (currentTab == NavigationDestination.PAPER_TRADES) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SignalBuyGreen,
                                    selectedTextColor = SignalBuyGreen,
                                    indicatorColor = SignalBuyGreen.copy(alpha = 0.15f),
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_paper_trades")
                            )

                            NavigationBarItem(
                                selected = currentTab == NavigationDestination.INDICATORS,
                                onClick = { currentTab = NavigationDestination.INDICATORS },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.ShowChart,
                                        contentDescription = "Indicators"
                                    )
                                },
                                label = {
                                    Text(
                                        "Indicators",
                                        fontWeight = if (currentTab == NavigationDestination.INDICATORS) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SignalBuyGreen,
                                    selectedTextColor = SignalBuyGreen,
                                    indicatorColor = SignalBuyGreen.copy(alpha = 0.15f),
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_indicators")
                            )

                            NavigationBarItem(
                                selected = currentTab == NavigationDestination.SETTINGS,
                                onClick = { currentTab = NavigationDestination.SETTINGS },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings"
                                    )
                                },
                                label = {
                                    Text(
                                        "Settings",
                                        fontWeight = if (currentTab == NavigationDestination.SETTINGS) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SignalBuyGreen,
                                    selectedTextColor = SignalBuyGreen,
                                    indicatorColor = SignalBuyGreen.copy(alpha = 0.15f),
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_settings")
                            )
                        }
                    }
                ) { innerPadding ->
                    when (currentTab) {
                        NavigationDestination.DASHBOARD -> DashboardScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                        NavigationDestination.PAPER_TRADES -> PaperTradesScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                        NavigationDestination.INDICATORS -> AnalysisScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                        NavigationDestination.SETTINGS -> SettingsScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
