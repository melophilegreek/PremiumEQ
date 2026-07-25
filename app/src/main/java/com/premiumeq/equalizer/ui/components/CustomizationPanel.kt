package com.premiumeq.equalizer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.premiumeq.equalizer.data.repository.AccentSwatches
import com.premiumeq.equalizer.data.repository.AppSettings

/**
 * Every control here actually changes something visible: dynamic color and
 * AMOLED mode flow into [com.premiumeq.equalizer.ui.theme.PremiumEQTheme]'s color
 * scheme, the accent swatches override the primary color, and the corner radius
 * slider drives [androidx.compose.material3.Shapes] app-wide - none of these are
 * placeholder toggles.
 */
@Composable
fun CustomizationPanel(
    settings: AppSettings,
    onDynamicColorChange: (Boolean) -> Unit,
    onAmoledChange: (Boolean) -> Unit,
    onAccentChange: (Int?) -> Unit,
    onCornerRadiusChange: (Int) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onVisualizerSpeedChange: (Int) -> Unit,
    onVisualizerSensitivityChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Customize", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            ToggleRow("Material You dynamic color", settings.useDynamicColor, onDynamicColorChange)
            ToggleRow("AMOLED true black", settings.useAmoledMode, onAmoledChange)
            ToggleRow("Haptic feedback on sliders", settings.hapticsEnabled, onHapticsChange)

            SectionSpacer()
            Text(text = "Accent color", style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AccentSwatches.forEach { argb ->
                    ColorSwatch(
                        argb = argb,
                        selected = settings.customAccentArgb == argb,
                        onClick = { onAccentChange(argb) }
                    )
                }
                // "Default" swatch clears the override so Material You / brand color takes over again.
                ColorSwatch(
                    argb = null,
                    selected = settings.customAccentArgb == null,
                    onClick = { onAccentChange(null) }
                )
            }

            SectionSpacer()
            LabeledSliderRow(
                label = "Corner radius",
                value = settings.cornerRadiusDp,
                range = 0..32,
                unit = "dp",
                onValueChange = onCornerRadiusChange
            )

            SectionSpacer()
            LabeledSliderRow(
                label = "Visualizer animation speed",
                value = settings.visualizerAnimationSpeedPercent,
                range = 25..300,
                unit = "%",
                onValueChange = onVisualizerSpeedChange
            )

            SectionSpacer()
            LabeledSliderRow(
                label = "Visualizer sensitivity",
                value = settings.visualizerSensitivityPercent,
                range = 25..300,
                unit = "%",
                onValueChange = onVisualizerSensitivityChange
            )
        }
    }
}

@Composable
private fun SectionSpacer() {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LabeledSliderRow(label: String, value: Int, range: IntRange, unit: String, onValueChange: (Int) -> Unit) {
    Column {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(text = "$value$unit", style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat()
        )
    }
}

@Composable
private fun ColorSwatch(argb: Int?, selected: Boolean, onClick: () -> Unit) {
    val color = argb?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = if (argb == null) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
            )
        }
    }
}
