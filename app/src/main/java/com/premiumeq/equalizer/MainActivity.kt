package com.premiumeq.equalizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.premiumeq.equalizer.ui.screens.EqualizerScreen
import com.premiumeq.equalizer.ui.theme.PremiumEQTheme
import com.premiumeq.equalizer.ui.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsViewModel.settings.collectAsState()

            PremiumEQTheme(
                useDynamicColor = settings.useDynamicColor,
                useAmoledMode = settings.useAmoledMode,
                accentColor = settings.customAccentArgb?.let { Color(it) },
                cornerRadiusDp = settings.cornerRadiusDp
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EqualizerScreen()
                }
            }
        }
    }
}

