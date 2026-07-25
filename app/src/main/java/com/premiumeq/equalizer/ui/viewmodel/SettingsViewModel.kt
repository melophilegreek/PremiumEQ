package com.premiumeq.equalizer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.premiumeq.equalizer.data.repository.AppSettings
import com.premiumeq.equalizer.data.repository.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: AppSettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { repository.setDynamicColor(enabled) }
    fun setAmoledMode(enabled: Boolean) = viewModelScope.launch { repository.setAmoledMode(enabled) }
    fun setCustomAccent(argb: Int?) = viewModelScope.launch { repository.setCustomAccent(argb) }
    fun setCornerRadius(dp: Int) = viewModelScope.launch { repository.setCornerRadius(dp) }
    fun setHapticsEnabled(enabled: Boolean) = viewModelScope.launch { repository.setHapticsEnabled(enabled) }
    fun setVisualizerAnimationSpeed(percent: Int) =
        viewModelScope.launch { repository.setVisualizerAnimationSpeed(percent) }
    fun setVisualizerSensitivity(percent: Int) =
        viewModelScope.launch { repository.setVisualizerSensitivity(percent) }
}
