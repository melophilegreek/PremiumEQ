package com.premiumeq.equalizer.data.model

import kotlinx.serialization.Serializable

/**
 * Output device categories the platform can distinguish via
 * [android.media.AudioDeviceInfo]. Used to drive the "Smart Profiles" feature:
 * each type can remember its own preferred preset and the app auto-applies it
 * when [com.premiumeq.equalizer.audio.DeviceOutputMonitor] reports a change.
 */
enum class OutputDeviceType {
    SPEAKER,
    WIRED_HEADPHONES,
    BLUETOOTH,
    USB_DAC,
    UNKNOWN
}

@Serializable
data class SmartProfileAssignment(
    val deviceType: OutputDeviceType,
    val presetId: String?,
    val autoSwitchEnabled: Boolean = true
)
