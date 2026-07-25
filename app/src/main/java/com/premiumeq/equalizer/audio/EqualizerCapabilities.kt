package com.premiumeq.equalizer.audio

/**
 * Snapshot of what this specific device/OS build actually supports, discovered at
 * runtime by attempting to instantiate each [android.media.audiofx.AudioEffect]
 * subtype. Nothing here is assumed from a spec sheet - every flag reflects a real,
 * successful (or failed) call against the platform on this exact device.
 *
 * The UI must branch on these flags and hide/disable controls rather than showing
 * a control that silently does nothing.
 */
data class EqualizerCapabilities(
    val isEqualizerSupported: Boolean = false,
    val numberOfBands: Int = 0,
    val bandLevelRangeMilliBel: ClosedRange<Short> = 0.toShort()..0.toShort(),
    val numberOfDevicePresets: Int = 0,
    val devicePresetNames: List<String> = emptyList(),

    val isBassBoostSupported: Boolean = false,
    val isBassBoostStrengthAdjustable: Boolean = false,

    val isVirtualizerSupported: Boolean = false,
    val isVirtualizerStrengthAdjustable: Boolean = false,

    val isLoudnessEnhancerSupported: Boolean = false,

    /** True once capability detection has run at least once. */
    val detectionComplete: Boolean = false
) {
    /** Best-effort classification of the underlying hardware EQ into a named mode. */
    val nearestStandardMode: Int
        get() = when {
            numberOfBands >= 31 -> 31
            numberOfBands >= 15 -> 15
            numberOfBands >= 10 -> 10
            numberOfBands > 0 -> 5
            else -> 0
        }
}
