package com.premiumeq.equalizer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.premiumeq.equalizer.data.model.EqualizerPreset
import com.premiumeq.equalizer.util.BandDiffEntry
import com.premiumeq.equalizer.util.computePresetDiff

/**
 * Small sparkline-style rendering of a preset's band curve, used in preset list
 * rows so users can recognize a preset's shape at a glance instead of only its name.
 */
@Composable
fun PresetThumbnail(
    preset: EqualizerPreset,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    val points = remember(preset) {
        preset.bandLevelsMilliBel.toSortedMap().values.map { it / 100.0 }
    }

    Canvas(modifier = modifier.size(width = 56.dp, height = 28.dp)) {
        if (points.size < 2) return@Canvas
        val maxAbs = (points.maxOf { kotlin.math.abs(it) }).coerceAtLeast(1.0)
        val midY = size.height / 2f
        val stepX = size.width / (points.size - 1)

        val path = androidx.compose.ui.graphics.Path()
        points.forEachIndexed { i, db ->
            val x = i * stepX
            val y = midY - (db / maxAbs).toFloat() * midY
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
        // Zero line for reference
        drawLine(
            color = lineColor.copy(alpha = 0.2f),
            start = Offset(0f, midY),
            end = Offset(size.width, midY),
            strokeWidth = 1.dp.toPx()
        )
    }
}

/**
 * Read-only band-by-band and effect-by-effect comparison between two presets.
 * Intended for a confirmation dialog before overwriting a preset, or just to
 * inspect how two saved presets differ.
 */
@Composable
fun PresetDiffView(oldPreset: EqualizerPreset, newPreset: EqualizerPreset, modifier: Modifier = Modifier) {
    val diff = remember(oldPreset, newPreset) { computePresetDiff(oldPreset, newPreset) }

    Column(modifier = modifier) {
        Text(
            text = "\"${oldPreset.name}\" → \"${newPreset.name}\"",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))

        if (diff.isIdentical) {
            Text(
                text = "These presets are identical.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        diff.bandDiffs.filter { it.changed }.forEach { entry ->
            DiffRow(entry)
        }

        if (kotlin.math.abs(diff.preampDeltaDb) > 0.05) {
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            LabeledDeltaRow("Preamp", diff.preampDeltaDb)
        }
        if (diff.bassBoostDeltaPercent != 0) {
            LabeledDeltaRow("Bass Boost", diff.bassBoostDeltaPercent.toDouble(), unit = "%")
        }
        if (diff.virtualizerDeltaPercent != 0) {
            LabeledDeltaRow("Virtualizer", diff.virtualizerDeltaPercent.toDouble(), unit = "%")
        }
        if (kotlin.math.abs(diff.loudnessDeltaDb) > 0.05) {
            LabeledDeltaRow("Loudness", diff.loudnessDeltaDb)
        }
    }
}

@Composable
private fun DiffRow(entry: BandDiffEntry) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        Text(text = "${entry.freqHz} Hz", style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "%.1f dB → %.1f dB (%s%.1f)".format(
                entry.oldDb ?: 0.0,
                entry.newDb ?: 0.0,
                if (entry.deltaDb >= 0) "+" else "",
                entry.deltaDb
            ),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun LabeledDeltaRow(label: String, delta: Double, unit: String = "dB") {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "${if (delta >= 0) "+" else ""}%.1f %s".format(delta, unit),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
