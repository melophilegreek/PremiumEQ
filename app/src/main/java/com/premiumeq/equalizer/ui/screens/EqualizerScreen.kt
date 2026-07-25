package com.premiumeq.equalizer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.premiumeq.equalizer.audio.model.BandInfo
import com.premiumeq.equalizer.ui.components.BandSlider
import com.premiumeq.equalizer.ui.components.CustomizationPanel
import com.premiumeq.equalizer.ui.components.DiagnosticsPanel
import com.premiumeq.equalizer.ui.viewmodel.EqualizerViewModel
import com.premiumeq.equalizer.ui.viewmodel.SettingsViewModel

@Composable
fun EqualizerScreen(
    viewModel: EqualizerViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val activeOutputDevice by viewModel.activeOutputDevice.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()

    if (state.isLoading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        MasterControlRow(
            enabled = state.isEnabled,
            onEnabledChange = viewModel::setEnabled
        )

        Spacer(modifier = Modifier.height(8.dp))

        HistoryAndResetRow(
            canUndo = state.canUndo,
            canRedo = state.canRedo,
            onUndo = viewModel::undo,
            onRedo = viewModel::redo,
            onResetAll = viewModel::resetAll
        )

        Spacer(modifier = Modifier.height(12.dp))

        ABCompareRow(
            hasSlotA = state.hasSlotA,
            hasSlotB = state.hasSlotB,
            activeSlot = state.activeSlot,
            onSaveA = viewModel::saveToSlotA,
            onSaveB = viewModel::saveToSlotB,
            onToggle = viewModel::toggleAB
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!state.capabilities.isEqualizerSupported) {
            UnsupportedNotice(text = "Multi-band equalizer is not supported on this device.")
        } else {
            BandRow(
                bands = state.bands,
                onBandChange = viewModel::setBandLevelDb,
                onBandReset = viewModel::resetBand,
                hapticsEnabled = settings.hapticsEnabled
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        PreampRow(
            preampDb = state.preampMilliBel / 100.0,
            onPreampChange = viewModel::setPreampDb
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.capabilities.isBassBoostSupported) {
            EffectStrengthRow(
                title = "Bass Boost",
                strength = state.bassBoostStrength,
                adjustable = state.capabilities.isBassBoostStrengthAdjustable,
                onStrengthChange = viewModel::setBassBoostStrength
            )
        } else {
            UnsupportedNotice(text = "Bass Boost is not supported on this device.")
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (state.capabilities.isVirtualizerSupported) {
            EffectStrengthRow(
                title = "Virtualizer",
                strength = state.virtualizerStrength,
                adjustable = state.capabilities.isVirtualizerStrengthAdjustable,
                onStrengthChange = viewModel::setVirtualizerStrength
            )
        } else {
            UnsupportedNotice(text = "Virtualizer is not supported on this device.")
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (state.capabilities.isLoudnessEnhancerSupported) {
            LoudnessRow(
                gainDb = state.loudnessGainMilliBel / 100.0,
                onGainChange = viewModel::setLoudnessGainDb
            )
        } else {
            UnsupportedNotice(text = "Loudness Enhancer is not supported on this device.")
        }

        Spacer(modifier = Modifier.height(24.dp))

        DiagnosticsPanel(
            capabilities = state.capabilities,
            isEnabled = state.isEnabled,
            activeOutputDevice = activeOutputDevice
        )

        Spacer(modifier = Modifier.height(16.dp))

        CustomizationPanel(
            settings = settings,
            onDynamicColorChange = settingsViewModel::setDynamicColor,
            onAmoledChange = settingsViewModel::setAmoledMode,
            onAccentChange = settingsViewModel::setCustomAccent,
            onCornerRadiusChange = settingsViewModel::setCornerRadius,
            onHapticsChange = settingsViewModel::setHapticsEnabled,
            onVisualizerSpeedChange = settingsViewModel::setVisualizerAnimationSpeed,
            onVisualizerSensitivityChange = settingsViewModel::setVisualizerSensitivity
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun HistoryAndResetRow(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onResetAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row {
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(Icons.Filled.Undo, contentDescription = "Undo")
            }
            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(Icons.Filled.Redo, contentDescription = "Redo")
            }
        }
        TextButton(onClick = onResetAll) {
            Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
            Text("Reset all")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ABCompareRow(
    hasSlotA: Boolean,
    hasSlotB: Boolean,
    activeSlot: Char?,
    onSaveA: () -> Unit,
    onSaveB: () -> Unit,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = activeSlot == 'A',
            onClick = onSaveA,
            label = { Text(if (hasSlotA) "A ✓" else "Save A") }
        )
        FilterChip(
            selected = activeSlot == 'B',
            onClick = onSaveB,
            label = { Text(if (hasSlotB) "B ✓" else "Save B") }
        )
        OutlinedButton(onClick = onToggle, enabled = hasSlotA || hasSlotB) {
            Text("A/B")
        }
    }
}

@Composable
private fun MasterControlRow(enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Equalizer", style = MaterialTheme.typography.headlineMedium)
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    }
}

@Composable
private fun BandRow(
    bands: List<BandInfo>,
    onBandChange: (Int, Double) -> Unit,
    onBandReset: (Int) -> Unit,
    hapticsEnabled: Boolean
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(290.dp)
    ) {
        items(bands, key = { it.index }) { band ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BandSlider(
                    label = band.frequencyLabel,
                    valueDb = band.currentLevelDb,
                    minDb = band.minLevelDb,
                    maxDb = band.maxLevelDb,
                    onValueChange = { db -> onBandChange(band.index, db) },
                    modifier = Modifier.height(240.dp),
                    hapticsEnabled = hapticsEnabled
                )
                IconButton(
                    onClick = { onBandReset(band.index) },
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(
                        Icons.Filled.RestartAlt,
                        contentDescription = "Reset ${band.frequencyLabel} to 0 dB",
                        modifier = Modifier.padding(0.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PreampRow(preampDb: Double, onPreampChange: (Double) -> Unit) {
    Column {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Preamp", style = MaterialTheme.typography.titleMedium)
            Text(text = "%.1f dB".format(preampDb), style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = preampDb.toFloat(),
            onValueChange = { onPreampChange(it.toDouble()) },
            valueRange = -15f..15f
        )
    }
}

@Composable
private fun EffectStrengthRow(
    title: String,
    strength: Int,
    adjustable: Boolean,
    onStrengthChange: (Int) -> Unit
) {
    Column {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = "${(strength / 10)}%", style = MaterialTheme.typography.bodyMedium)
        }
        if (adjustable) {
            Slider(
                value = strength.toFloat(),
                onValueChange = { onStrengthChange(it.toInt()) },
                valueRange = 0f..1000f
            )
        } else {
            Text(
                text = "This device only supports on/off for $title, not variable strength.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Switch(
                checked = strength > 0,
                onCheckedChange = { onStrengthChange(if (it) 1000 else 0) }
            )
        }
    }
}

@Composable
private fun LoudnessRow(gainDb: Double, onGainChange: (Double) -> Unit) {
    Column {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Loudness Enhancer", style = MaterialTheme.typography.titleMedium)
            Text(text = "+%.1f dB".format(gainDb), style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = gainDb.toFloat(),
            onValueChange = { onGainChange(it.toDouble()) },
            valueRange = 0f..20f
        )
    }
}

@Composable
private fun UnsupportedNotice(text: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}
