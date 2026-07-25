package com.premiumeq.equalizer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. Hilt generates the dependency graph from this class.
 *
 * No audio effect engine is initialized here. Effects are created lazily by
 * [com.premiumeq.equalizer.audio.AudioEffectManager] once the app has a valid
 * output audio session, since attaching effects eagerly at process start would
 * waste battery on devices where the user hasn't opened the equalizer yet.
 */
@HiltAndroidApp
class EqualizerApplication : Application()
