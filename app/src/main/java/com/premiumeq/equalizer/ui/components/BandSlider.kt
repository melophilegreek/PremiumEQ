package com.premiumeq.equalizer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.hapticfeedback.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * A single-band vertical fader. Value is reported with 0.1 dB precision. This
 * composable only knows the *displayed* min/max range; the caller (ViewModel /
 * AudioEffectManager) is responsible for clamping to the band's real hardware
 * range before applying it.
 */
@Composable
fun BandSlider(
    label: String,
    valueDb: Double,
    minDb: Double,
    maxDb: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    hapticsEnabled: Boolean = true
) {
    val haptics = LocalHapticFeedback.current
    var trackHeightPx by remember { mutableFloatStateOf(1f) }
    var lastSnappedTenth by remember { mutableFloatStateOf((valueDb * 10).roundToInt().toFloat()) }

    val fraction = (((valueDb - minDb) / (maxDb - minDb)).coerceIn(0.0, 1.0)).toFloat()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = "%.1f".format(valueDb),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .padding(vertical = 4.dp)
                .width(40.dp)
                .fillMaxHeight()
                .onSizeChanged { trackHeightPx = it.height.toFloat().coerceAtLeast(1f) }
                .pointerInput(minDb, maxDb) {
                    detectVerticalDragGestures { change, _ ->
                        change.consume()
                        val fractionFromTop = (change.position.y / trackHeightPx).coerceIn(0f, 1f)
                        val newFraction = 1f - fractionFromTop
                        val newValue = minDb + newFraction * (maxDb - minDb)
                        val snappedTenth = (newValue * 10).roundToInt().toFloat()
                        if (snappedTenth != lastSnappedTenth) {
                            lastSnappedTenth = snappedTenth
                            if (hapticsEnabled) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                        onValueChange((snappedTenth / 10.0).coerceIn(minDb, maxDb))
                    }
                }
        ) {
            // Background track
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            // Filled portion, growing from the bottom, with the thumb pinned to its top edge
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight(fraction)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary)
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
