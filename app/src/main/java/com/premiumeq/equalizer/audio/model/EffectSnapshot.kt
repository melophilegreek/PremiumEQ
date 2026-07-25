package com.premiumeq.equalizer.audio.model

/**
 * A full point-in-time capture of every adjustable value in [com.premiumeq.equalizer.audio.AudioEffectManager],
 * used to implement undo/redo history and A/B compare slots. Deliberately keyed by
 * center frequency (not raw band index) so a snapshot taken on one device's band
 * layout still applies sanely if capabilities ever change at runtime.
 */
data class EffectSnapshot(
    val bandLevelsMilliBelByFreq: Map<Int, Short>,
    val preampMilliBel: Int,
    val bassBoostStrength: Short,
    val virtualizerStrength: Short,
    val loudnessGainMilliBel: Int,
    val capturedAtEpochMillis: Long = System.currentTimeMillis()
)
