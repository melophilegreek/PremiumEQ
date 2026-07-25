package com.premiumeq.equalizer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.premiumeq.equalizer.audio.AudioEffectManager
import com.premiumeq.equalizer.audio.EqualizerCapabilities
import com.premiumeq.equalizer.data.model.OutputDeviceType

/**
 * Shows exactly what the app detected and attached on THIS device: session id,
 * which effects actually succeeded, band count, and the currently detected
 * output route. Meant for troubleshooting on unusual OEM builds where "why
 * isn't this working" needs a real answer instead of a guess.
 */
@Composable
fun DiagnosticsPanel(
    capabilities: EqualizerCapabilities,
    isEnabled: Boolean,
    activeOutputDevice: OutputDeviceType,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Diagnostics", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            DiagnosticRow("Audio session", "Global mix (session ${AudioEffectManager.GLOBAL_SESSION_ID})")
            DiagnosticRow("Master state", if (isEnabled) "Enabled" else "Disabled")
            DiagnosticRow("Detected output", activeOutputDevice.toLabel())
            DiagnosticRow(
                "Equalizer",
                if (capabilities.isEqualizerSupported) {
                    "${capabilities.numberOfBands}-band, ${capabilities.bandLevelRangeMilliBel.start / 100}..${capabilities.bandLevelRangeMilliBel.endInclusive / 100} dB"
                } else "Not supported"
            )
            DiagnosticRow(
                "Bass Boost",
                if (capabilities.isBassBoostSupported) {
                    if (capabilities.isBassBoostStrengthAdjustable) "Supported (variable strength)" else "Supported (on/off only)"
                } else "Not supported"
            )
            DiagnosticRow(
                "Virtualizer",
                if (capabilities.isVirtualizerSupported) {
                    if (capabilities.isVirtualizerStrengthAdjustable) "Supported (variable strength)" else "Supported (on/off only)"
                } else "Not supported"
            )
            DiagnosticRow(
                "Loudness Enhancer",
                if (capabilities.isLoudnessEnhancerSupported) "Supported" else "Not supported"
            )
            DiagnosticRow(
                "Device EQ presets",
                if (capabilities.numberOfDevicePresets > 0) {
                    "${capabilities.numberOfDevicePresets} built-in (${capabilities.devicePresetNames.joinToString()})"
                } else "None reported"
            )
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun OutputDeviceType.toLabel(): String = when (this) {
    OutputDeviceType.SPEAKER -> "Device speaker"
    OutputDeviceType.WIRED_HEADPHONES -> "Wired headphones"
    OutputDeviceType.BLUETOOTH -> "Bluetooth"
    OutputDeviceType.USB_DAC -> "USB DAC"
    OutputDeviceType.UNKNOWN -> "Unknown"
}
