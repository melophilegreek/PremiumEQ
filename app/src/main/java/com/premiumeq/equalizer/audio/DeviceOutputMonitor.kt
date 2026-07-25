package com.premiumeq.equalizer.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.core.content.getSystemService
import com.premiumeq.equalizer.data.model.OutputDeviceType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reports the currently active audio output device type using the real
 * [AudioManager.getDevices] / [AudioDeviceCallback] APIs (no polling, no guessing).
 *
 * This class is wired up and functional end-to-end for detection. What is
 * intentionally NOT yet built is the automatic-preset-application step in the
 * ViewModel layer (i.e. actually calling [AudioEffectManager] when a switch is
 * detected) - that requires the Smart Profiles preference store, which is a
 * follow-up piece so profile-switching behavior can be tuned/tested on its own
 * rather than bundled sight-unseen into this first drop.
 */
@Singleton
class DeviceOutputMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager: AudioManager? = context.getSystemService()

    /** Emits the current output device type immediately, then again on every change. */
    fun observeActiveOutputDevice(): Flow<OutputDeviceType> = callbackFlow {
        val manager = audioManager
        if (manager == null) {
            trySend(OutputDeviceType.UNKNOWN)
            awaitClose { }
            return@callbackFlow
        }

        fun emitCurrent() {
            trySend(currentOutputDeviceType(manager))
        }

        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = emitCurrent()
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = emitCurrent()
        }

        manager.registerAudioDeviceCallback(callback, null)
        emitCurrent()

        awaitClose { manager.unregisterAudioDeviceCallback(callback) }
    }.distinctUntilChanged()

    private fun currentOutputDeviceType(manager: AudioManager): OutputDeviceType {
        val devices = manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

        // Priority order matters: prefer the most specific / most likely "currently in use"
        // device. The platform doesn't expose a single "active" flag pre-API 31 in a
        // universally reliable way, so we rank by what a user is most likely listening on.
        val priorityTypes = listOf(
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        )

        for (type in priorityTypes) {
            if (devices.any { it.type == type }) {
                return type.toOutputDeviceType()
            }
        }
        return OutputDeviceType.UNKNOWN
    }

    private fun Int.toOutputDeviceType(): OutputDeviceType = when (this) {
        AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE -> OutputDeviceType.USB_DAC
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> OutputDeviceType.BLUETOOTH
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> OutputDeviceType.WIRED_HEADPHONES
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> OutputDeviceType.SPEAKER
        else -> OutputDeviceType.UNKNOWN
    }
}
