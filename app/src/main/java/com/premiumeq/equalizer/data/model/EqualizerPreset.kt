package com.premiumeq.equalizer.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A fully self-contained equalizer configuration: every value needed to reproduce
 * the exact sound is stored here, so presets remain valid even if imported onto a
 * device with a different number of hardware bands (see [bandLevelsMilliBel] which
 * is keyed by approximate frequency rather than raw index).
 */
@Serializable
data class EqualizerPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val folderId: String? = null,
    val isFavorite: Boolean = false,
    /** Map of center-frequency-in-Hz -> gain-in-millibel, so it survives band-count differences. */
    val bandLevelsMilliBel: Map<Int, Int> = emptyMap(),
    val preampMilliBel: Int = 0,
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val loudnessGainMilliBel: Int = 0,
    val balance: Float = 0f, // -1f (full left) .. 1f (full right)
    val isMono: Boolean = false,
    val isChannelSwapped: Boolean = false,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = System.currentTimeMillis()
)

@Serializable
data class PresetFolder(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)

/** The full exportable/importable JSON payload for sharing or backup. */
@Serializable
data class PresetBackup(
    val schemaVersion: Int = 1,
    val presets: List<EqualizerPreset>,
    val folders: List<PresetFolder> = emptyList()
)
