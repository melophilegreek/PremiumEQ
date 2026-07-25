package com.premiumeq.equalizer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.premiumeq.equalizer.audio.AudioEffectManager
import com.premiumeq.equalizer.audio.DeviceOutputMonitor
import com.premiumeq.equalizer.audio.EqualizerCapabilities
import com.premiumeq.equalizer.audio.dbToMilliBel
import com.premiumeq.equalizer.audio.model.BandInfo
import com.premiumeq.equalizer.data.model.EqualizerPreset
import com.premiumeq.equalizer.data.model.OutputDeviceType
import com.premiumeq.equalizer.data.model.PresetFolder
import com.premiumeq.equalizer.data.repository.PresetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EqualizerUiState(
    val capabilities: EqualizerCapabilities = EqualizerCapabilities(),
    val bands: List<BandInfo> = emptyList(),
    val isEnabled: Boolean = false,
    val preampMilliBel: Int = 0,
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val loudnessGainMilliBel: Int = 0,
    val presets: List<EqualizerPreset> = emptyList(),
    val folders: List<PresetFolder> = emptyList(),
    val activeOutputDevice: OutputDeviceType = OutputDeviceType.UNKNOWN,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val hasSlotA: Boolean = false,
    val hasSlotB: Boolean = false,
    val activeSlot: Char? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val audioEffectManager: AudioEffectManager,
    private val presetRepository: PresetRepository,
    private val deviceOutputMonitor: DeviceOutputMonitor
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)

    val uiState: StateFlow<EqualizerUiState> = combine(
        audioEffectManager.capabilities,
        audioEffectManager.bands,
        audioEffectManager.globalEnabled,
        audioEffectManager.preampMilliBel,
        audioEffectManager.bassBoostStrength,
        audioEffectManager.virtualizerStrength,
        audioEffectManager.loudnessGainMilliBel,
        presetRepository.presets,
        presetRepository.folders,
        _isLoading,
        audioEffectManager.canUndo,
        audioEffectManager.canRedo,
        audioEffectManager.slotA,
        audioEffectManager.slotB,
        audioEffectManager.activeSlot
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        EqualizerUiState(
            capabilities = values[0] as EqualizerCapabilities,
            bands = values[1] as List<BandInfo>,
            isEnabled = values[2] as Boolean,
            preampMilliBel = values[3] as Int,
            bassBoostStrength = (values[4] as Short).toInt(),
            virtualizerStrength = (values[5] as Short).toInt(),
            loudnessGainMilliBel = values[6] as Int,
            presets = values[7] as List<EqualizerPreset>,
            folders = values[8] as List<PresetFolder>,
            isLoading = values[9] as Boolean,
            canUndo = values[10] as Boolean,
            canRedo = values[11] as Boolean,
            hasSlotA = values[12] != null,
            hasSlotB = values[13] != null,
            activeSlot = values[14] as Char?
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EqualizerUiState())

    val activeOutputDevice: StateFlow<OutputDeviceType> = deviceOutputMonitor
        .observeActiveOutputDevice()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OutputDeviceType.UNKNOWN)

    init {
        viewModelScope.launch {
            audioEffectManager.initialize()
            presetRepository.load()
            _isLoading.value = false
        }
    }

    fun setEnabled(enabled: Boolean) = audioEffectManager.setGlobalEnabled(enabled)

    /** [dB] is applied with the full 0.1 dB precision the platform millibel unit allows. */
    fun setBandLevelDb(bandIndex: Int, dB: Double) =
        audioEffectManager.setBandLevel(bandIndex, dB.dbToMilliBel())

    fun setPreampDb(dB: Double) = audioEffectManager.setPreamp(dB.dbToMilliBel().toInt())

    fun setBassBoostStrength(strength: Int) =
        audioEffectManager.setBassBoostStrength(strength.coerceIn(0, 1000).toShort())

    fun setVirtualizerStrength(strength: Int) =
        audioEffectManager.setVirtualizerStrength(strength.coerceIn(0, 1000).toShort())

    fun setLoudnessGainDb(dB: Double) = audioEffectManager.setLoudnessGain(dB.dbToMilliBel().toInt())

    fun useDevicePreset(index: Int) = audioEffectManager.useDevicePreset(index)

    fun undo() = audioEffectManager.undo()
    fun redo() = audioEffectManager.redo()
    fun resetBand(bandIndex: Int) = audioEffectManager.resetBand(bandIndex)
    fun resetAll() = audioEffectManager.resetAll()

    fun saveToSlotA() = audioEffectManager.saveToSlotA()
    fun saveToSlotB() = audioEffectManager.saveToSlotB()
    fun recallSlotA() = audioEffectManager.recallSlotA()
    fun recallSlotB() = audioEffectManager.recallSlotB()
    fun toggleAB() = audioEffectManager.toggleAB()

    fun saveCurrentAsPreset(name: String, folderId: String? = null) {
        val state = uiState.value
        val preset = EqualizerPreset(
            name = name,
            folderId = folderId,
            bandLevelsMilliBel = state.bands.associate { it.centerFreqHz to it.currentLevelMilliBel.toInt() },
            preampMilliBel = state.preampMilliBel,
            bassBoostStrength = state.bassBoostStrength,
            virtualizerStrength = state.virtualizerStrength,
            loudnessGainMilliBel = state.loudnessGainMilliBel
        )
        viewModelScope.launch { presetRepository.savePreset(preset) }
    }

    fun applyPreset(preset: EqualizerPreset) {
        val currentBands = uiState.value.bands
        preset.bandLevelsMilliBel.forEach { (freqHz, milliBel) ->
            val closestBand = currentBands.minByOrNull { kotlin.math.abs(it.centerFreqHz - freqHz) }
            closestBand?.let { audioEffectManager.setBandLevel(it.index, milliBel.toShort()) }
        }
        audioEffectManager.setPreamp(preset.preampMilliBel)
        audioEffectManager.setBassBoostStrength(preset.bassBoostStrength.toShort())
        audioEffectManager.setVirtualizerStrength(preset.virtualizerStrength.toShort())
        audioEffectManager.setLoudnessGain(preset.loudnessGainMilliBel)
    }

    fun deletePreset(id: String) = viewModelScope.launch { presetRepository.deletePreset(id) }
    fun renamePreset(id: String, name: String) = viewModelScope.launch { presetRepository.renamePreset(id, name) }
    fun duplicatePreset(id: String) = viewModelScope.launch { presetRepository.duplicatePreset(id) }
    fun setFavorite(id: String, favorite: Boolean) =
        viewModelScope.launch { presetRepository.setFavorite(id, favorite) }

    fun createFolder(name: String) = viewModelScope.launch { presetRepository.createFolder(name) }
    fun moveToFolder(presetId: String, folderId: String?) =
        viewModelScope.launch { presetRepository.moveToFolder(presetId, folderId) }

    fun exportPresetsJson(presetIds: Set<String>? = null): String = presetRepository.exportToJson(presetIds)
    fun importPresetsJson(jsonText: String, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch { onResult(presetRepository.importFromJson(jsonText)) }
    }

    override fun onCleared() {
        super.onCleared()
        // Effects intentionally stay attached to the global session after the ViewModel/UI
        // is destroyed, since the whole point of a system-wide EQ is that it keeps applying
        // while the user is in another app. audioEffectManager.release() is only called from
        // an explicit "disable everything" action, never from lifecycle teardown.
    }
}
