package com.premiumeq.equalizer.util

import com.premiumeq.equalizer.data.model.EqualizerPreset

/** One band's difference between two presets. Null on either side means that preset didn't define this frequency. */
data class BandDiffEntry(
    val freqHz: Int,
    val oldDb: Double?,
    val newDb: Double?
) {
    val deltaDb: Double get() = (newDb ?: 0.0) - (oldDb ?: 0.0)
    val changed: Boolean get() = kotlin.math.abs(deltaDb) > 0.05
}

data class PresetDiffResult(
    val bandDiffs: List<BandDiffEntry>,
    val preampDeltaDb: Double,
    val bassBoostDeltaPercent: Int,
    val virtualizerDeltaPercent: Int,
    val loudnessDeltaDb: Double
) {
    /** True if nothing meaningfully differs between the two presets. */
    val isIdentical: Boolean
        get() = bandDiffs.none { it.changed } &&
            kotlin.math.abs(preampDeltaDb) <= 0.05 &&
            bassBoostDeltaPercent == 0 &&
            virtualizerDeltaPercent == 0 &&
            kotlin.math.abs(loudnessDeltaDb) <= 0.05
}

/**
 * Computes a band-by-band and effect-by-effect diff between two presets, matching
 * bands by frequency (not index) so presets captured on different hardware still
 * compare sensibly.
 */
fun computePresetDiff(old: EqualizerPreset, new: EqualizerPreset): PresetDiffResult {
    val allFreqs = (old.bandLevelsMilliBel.keys + new.bandLevelsMilliBel.keys).toSortedSet()
    val bandDiffs = allFreqs.map { freq ->
        BandDiffEntry(
            freqHz = freq,
            oldDb = old.bandLevelsMilliBel[freq]?.let { it / 100.0 },
            newDb = new.bandLevelsMilliBel[freq]?.let { it / 100.0 }
        )
    }

    return PresetDiffResult(
        bandDiffs = bandDiffs,
        preampDeltaDb = (new.preampMilliBel - old.preampMilliBel) / 100.0,
        bassBoostDeltaPercent = (new.bassBoostStrength - old.bassBoostStrength) / 10,
        virtualizerDeltaPercent = (new.virtualizerStrength - old.virtualizerStrength) / 10,
        loudnessDeltaDb = (new.loudnessGainMilliBel - old.loudnessGainMilliBel) / 100.0
    )
}
