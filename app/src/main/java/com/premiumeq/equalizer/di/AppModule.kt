package com.premiumeq.equalizer.di

import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.Module

/**
 * Every injectable class in this project (AudioEffectManager, PresetRepository,
 * DeviceOutputMonitor, VisualizerEngine, view models) uses constructor injection
 * with @Inject / @Singleton directly, so there are no manual @Provides bindings
 * needed yet. This module is kept as the designated extension point for future
 * bindings that DO need one (e.g. binding an interface to an implementation, or
 * providing a DataStore<Preferences> instance).
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule
