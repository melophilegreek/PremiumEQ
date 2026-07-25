package com.premiumeq.equalizer.widget

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.premiumeq.equalizer.audio.AudioEffectManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Quick Settings tile that toggles the global equalizer on/off in one tap,
 * without opening the app. Shares the same [AudioEffectManager] singleton as the
 * rest of the app via Hilt field injection, so the tile and the in-app switch
 * always agree on state - there is no separate/duplicated state to go stale.
 */
@AndroidEntryPoint
class EqualizerTileService : TileService() {

    @Inject
    lateinit var audioEffectManager: AudioEffectManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stateCollectionJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        audioEffectManager.initialize()
        stateCollectionJob?.cancel()
        stateCollectionJob = audioEffectManager.globalEnabled
            .onEach { enabled -> updateTile(enabled) }
            .launchIn(serviceScope)
    }

    override fun onStopListening() {
        stateCollectionJob?.cancel()
        stateCollectionJob = null
        super.onStopListening()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onClick() {
        super.onClick()
        serviceScope.launch {
            val newState = !audioEffectManager.globalEnabled.value
            audioEffectManager.setGlobalEnabled(newState)
            updateTile(newState)
        }
    }

    private fun updateTile(enabled: Boolean) {
        qsTile?.apply {
            state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = "Premium EQ"
            updateTile()
        }
    }
}
