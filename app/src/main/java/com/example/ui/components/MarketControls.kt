package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiProviderType
import com.example.model.PaperDuration
import com.example.model.Timeframe
import com.example.model.TradingPair
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.SignalBuyGreen
import com.example.ui.theme.TechCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MarketControls(
    selectedPair: TradingPair,
    onPairSelected: (TradingPair) -> Unit,
    selectedTimeframe: Timeframe,
    onTimeframeSelected: (Timeframe) -> Unit,
    selectedDuration: PaperDuration,
    onDurationSelected: (PaperDuration) -> Unit,
    selectedProvider: AiProviderType,
    onProviderSelected: (AiProviderType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Pairs Horizontal Chips
        Text(
            text = "MARKET PAIR",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TradingPair.entries.forEach { pair ->
                val isSelected = pair == selectedPair
                FilterChip(
                    selected = isSelected,
                    onClick = { onPairSelected(pair) },
                    label = {
                        Text(
                            text = pair.symbol,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TechCyan.copy(alpha = 0.2f),
                        selectedLabelColor = TechCyan,
                        containerColor = ObsidianCard,
                        labelColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("pair_chip_${pair.name}")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Timeframe Horizontal Chips
        Text(
            text = "ANALYSIS TIMEFRAME",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Timeframe.entries.forEach { tf ->
                val isSelected = tf == selectedTimeframe
                FilterChip(
                    selected = isSelected,
                    onClick = { onTimeframeSelected(tf) },
                    label = {
                        Text(
                            text = tf.displayName,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SignalBuyGreen.copy(alpha = 0.2f),
                        selectedLabelColor = SignalBuyGreen,
                        containerColor = ObsidianCard,
                        labelColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("timeframe_chip_${tf.id}")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row of Duration & AI Provider Selectors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Paper Trade Duration Dropdown
            DurationDropdown(
                selectedDuration = selectedDuration,
                onDurationSelected = onDurationSelected,
                modifier = Modifier.weight(1f)
            )

            // AI Provider Selector Dropdown
            ProviderDropdown(
                selectedProvider = selectedProvider,
                onProviderSelected = onProviderSelected,
                modifier = Modifier.weight(1.3f)
            )
        }
    }
}

@Composable
fun DurationDropdown(
    selectedDuration: PaperDuration,
    onDurationSelected: (PaperDuration) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            color = ObsidianCard,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("duration_selector")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Duration",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("DURATION", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
                        Text(selectedDuration.displayName, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(ObsidianElevated)
        ) {
            PaperDuration.entries.forEach { duration ->
                DropdownMenuItem(
                    text = { Text(duration.displayName, color = TextPrimary) },
                    onClick = {
                        onDurationSelected(duration)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ProviderDropdown(
    selectedProvider: AiProviderType,
    onProviderSelected: (AiProviderType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            color = ObsidianCard,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("provider_selector")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (selectedProvider == AiProviderType.RULE_ENGINE) Icons.Default.Bolt else Icons.Default.Psychology,
                        contentDescription = "Provider",
                        tint = if (selectedProvider == AiProviderType.RULE_ENGINE) SignalBuyGreen else TechCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("AI ENGINE", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
                        Text(
                            text = when (selectedProvider) {
                                AiProviderType.RULE_ENGINE -> "Quant Engine"
                                AiProviderType.GEMINI -> "Google Gemini"
                                AiProviderType.OPENROUTER -> "OpenRouter"
                                AiProviderType.GROQ -> "Groq Cloud"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(ObsidianElevated)
        ) {
            AiProviderType.entries.forEach { provider ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(provider.displayName, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text(provider.description, color = TextMuted, fontSize = 11.sp)
                        }
                    },
                    onClick = {
                        onProviderSelected(provider)
                        expanded = false
                    }
                )
            }
        }
    }
}
