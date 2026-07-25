package com.premiumeq.equalizer.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "appearance_settings")

/** Every user-tunable visual/behavioral preference, all with sane defaults. */
data class AppSettings(
    val useDynamicColor: Boolean = true,
    val useAmoledMode: Boolean = false,
    /** ARGB int, or null to use the built-in brand color / Material You color. */
    val customAccentArgb: Int? = null,
    val cornerRadiusDp: Int = 16,
    val hapticsEnabled: Boolean = true,
    val visualizerAnimationSpeedPercent: Int = 100, // 50 = half speed, 200 = double speed
    val visualizerSensitivityPercent: Int = 100
)

/** A handful of curated accent swatches shown in the customization UI, in addition to Material You / default. */
val AccentSwatches = listOf(
    0xFF7C4DFF.toInt(), // violet (brand default)
    0xFF00E5C3.toInt(), // teal
    0xFFFF5370.toInt(), // rose
    0xFFFFB300.toInt(), // amber
    0xFF29B6F6.toInt(), // sky blue
    0xFF66BB6A.toInt()  // green
)

@Singleton
class AppSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val AMOLED_MODE = booleanPreferencesKey("use_amoled_mode")
        val ACCENT_ARGB = intPreferencesKey("custom_accent_argb")
        val CORNER_RADIUS = intPreferencesKey("corner_radius_dp")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val VIS_SPEED = intPreferencesKey("visualizer_speed_percent")
        val VIS_SENSITIVITY = intPreferencesKey("visualizer_sensitivity_percent")
        const val NO_CUSTOM_ACCENT = 0
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            useDynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
            useAmoledMode = prefs[Keys.AMOLED_MODE] ?: false,
            customAccentArgb = prefs[Keys.ACCENT_ARGB]?.takeIf { it != Keys.NO_CUSTOM_ACCENT },
            cornerRadiusDp = prefs[Keys.CORNER_RADIUS] ?: 16,
            hapticsEnabled = prefs[Keys.HAPTICS] ?: true,
            visualizerAnimationSpeedPercent = prefs[Keys.VIS_SPEED] ?: 100,
            visualizerSensitivityPercent = prefs[Keys.VIS_SENSITIVITY] ?: 100
        )
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setAmoledMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.AMOLED_MODE] = enabled }
    }

    /** Pass null to clear the override and fall back to Material You / the default brand color. */
    suspend fun setCustomAccent(argb: Int?) {
        context.settingsDataStore.edit { it[Keys.ACCENT_ARGB] = argb ?: Keys.NO_CUSTOM_ACCENT }
    }

    suspend fun setCornerRadius(dp: Int) {
        context.settingsDataStore.edit { it[Keys.CORNER_RADIUS] = dp.coerceIn(0, 32) }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.HAPTICS] = enabled }
    }

    suspend fun setVisualizerAnimationSpeed(percent: Int) {
        context.settingsDataStore.edit { it[Keys.VIS_SPEED] = percent.coerceIn(25, 300) }
    }

    suspend fun setVisualizerSensitivity(percent: Int) {
        context.settingsDataStore.edit { it[Keys.VIS_SENSITIVITY] = percent.coerceIn(25, 300) }
    }
}
