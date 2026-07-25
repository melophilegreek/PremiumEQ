package com.premiumeq.equalizer.audio.model

/**
 * Represents one band of the platform [android.media.audiofx.Equalizer].
 *
 * @param index band index as reported by the platform (0-based)
 * @param centerFreqHz center frequency in Hz (converted from the platform's milliHertz units)
 * @param minLevelMilliBel minimum gain supported by this band, in millibels (1 dB = 100 mB)
 * @param maxLevelMilliBel maximum gain supported by this band, in millibels
 * @param currentLevelMilliBel the currently applied gain, in millibels
 */
data class BandInfo(
    val index: Int,
    val centerFreqHz: Int,
    val minLevelMilliBel: Short,
    val maxLevelMilliBel: Short,
    val currentLevelMilliBel: Short
) {
    /** Convenience accessor for UI: current gain in whole dB with 0.1 dB precision. */
    val currentLevelDb: Double get() = currentLevelMilliBel / 100.0
    val minLevelDb: Double get() = minLevelMilliBel / 100.0
    val maxLevelDb: Double get() = maxLevelMilliBel / 100.0

    /** Human readable label, e.g. "60 Hz", "1.2 kHz". */
    val frequencyLabel: String
        get() = if (centerFreqHz >= 1000) {
            "%.1f kHz".format(centerFreqHz / 1000.0)
        } else {
            "$centerFreqHz Hz"
        }
}
