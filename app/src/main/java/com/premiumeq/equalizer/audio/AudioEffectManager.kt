package com.premiumeq.equalizer.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.util.Log
import com.premiumeq.equalizer.audio.model.BandInfo
import com.premiumeq.equalizer.audio.model.EffectSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Owns the platform [android.media.audiofx.AudioEffect] instances and exposes their
 * state as [StateFlow]s for the rest of the app to observe.
 *
 * Effects are attached to audio session **0**, which the platform treats as the
 * global output mix rather than a single app's playback session. This is the same
 * mechanism used by other system-wide equalizer apps on the Play Store. It is not
 * a documented guarantee for every source (for example some spatial-audio or
 * offloaded playback paths on newer OEM builds may not route through it), which is
 * why every effect is verified live with try/catch rather than assumed to work.
 *
 * A single instance lives for the whole process (see [com.premiumeq.equalizer.di.AppModule]).
 */
@Singleton
class AudioEffectManager @Inject constructor() {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private val _capabilities = MutableStateFlow(EqualizerCapabilities())
    val capabilities: StateFlow<EqualizerCapabilities> = _capabilities.asStateFlow()

    private val _bands = MutableStateFlow<List<BandInfo>>(emptyList())
    val bands: StateFlow<List<BandInfo>> = _bands.asStateFlow()

    private val _globalEnabled = MutableStateFlow(false)
    val globalEnabled: StateFlow<Boolean> = _globalEnabled.asStateFlow()

    private val _bassBoostStrength = MutableStateFlow<Short>(0)
    val bassBoostStrength: StateFlow<Short> = _bassBoostStrength.asStateFlow()

    private val _virtualizerStrength = MutableStateFlow<Short>(0)
    val virtualizerStrength: StateFlow<Short> = _virtualizerStrength.asStateFlow()

    private val _loudnessGainMilliBel = MutableStateFlow(0)
    val loudnessGainMilliBel: StateFlow<Int> = _loudnessGainMilliBel.asStateFlow()

    /** Uniform gain (millibel) applied on top of every band's own level. Clamped per-band in [setPreamp]. */
    private val _preampMilliBel = MutableStateFlow(0)
    val preampMilliBel: StateFlow<Int> = _preampMilliBel.asStateFlow()

    private var initialized = false

    // --- Undo/redo history -------------------------------------------------
    private val undoStack = ArrayDeque<EffectSnapshot>()
    private val redoStack = ArrayDeque<EffectSnapshot>()
    private var isApplyingSnapshot = false

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // --- A/B compare slots ---------------------------------------------------
    private val _slotA = MutableStateFlow<EffectSnapshot?>(null)
    val slotA: StateFlow<EffectSnapshot?> = _slotA.asStateFlow()

    private val _slotB = MutableStateFlow<EffectSnapshot?>(null)
    val slotB: StateFlow<EffectSnapshot?> = _slotB.asStateFlow()

    private val _activeSlot = MutableStateFlow<Char?>(null) // 'A', 'B', or null if diverged
    val activeSlot: StateFlow<Char?> = _activeSlot.asStateFlow()

    /**
     * Attempts to attach every supported effect to the global session. Safe to call
     * multiple times; subsequent calls are no-ops. Never throws - every failure is
     * caught per-effect so one unsupported effect can't take down the others.
     */
    @Synchronized
    fun initialize() {
        if (initialized) return
        initialized = true

        val eqSupported = initEqualizer()
        val bassSupported = initBassBoost()
        val virtualizerSupported = initVirtualizer()
        val loudnessSupported = initLoudnessEnhancer()

        _capabilities.value = _capabilities.value.copy(
            isEqualizerSupported = eqSupported,
            isBassBoostSupported = bassSupported,
            isVirtualizerSupported = virtualizerSupported,
            isLoudnessEnhancerSupported = loudnessSupported,
            detectionComplete = true
        )
    }

    private fun initEqualizer(): Boolean = try {
        val eq = Equalizer(0, GLOBAL_SESSION_ID)
        equalizer = eq
        val range = eq.bandLevelRange
        val presetCount = eq.numberOfPresets.toInt()
        val presetNames = (0 until presetCount).map { eq.getPresetName(it.toShort()) }

        _capabilities.value = _capabilities.value.copy(
            numberOfBands = eq.numberOfBands.toInt(),
            bandLevelRangeMilliBel = range[0]..range[1],
            numberOfDevicePresets = presetCount,
            devicePresetNames = presetNames
        )
        refreshBandsFromEngine()
        eq.enabled = false
        true
    } catch (t: Throwable) {
        Log.w(TAG, "Equalizer not supported on this device", t)
        equalizer = null
        false
    }

    private fun initBassBoost(): Boolean = try {
        val bb = BassBoost(0, GLOBAL_SESSION_ID)
        bassBoost = bb
        _capabilities.value = _capabilities.value.copy(
            isBassBoostStrengthAdjustable = bb.strengthSupported
        )
        bb.enabled = false
        true
    } catch (t: Throwable) {
        Log.w(TAG, "BassBoost not supported on this device", t)
        bassBoost = null
        false
    }

    private fun initVirtualizer(): Boolean = try {
        val vr = Virtualizer(0, GLOBAL_SESSION_ID)
        virtualizer = vr
        _capabilities.value = _capabilities.value.copy(
            isVirtualizerStrengthAdjustable = vr.strengthSupported
        )
        vr.enabled = false
        true
    } catch (t: Throwable) {
        Log.w(TAG, "Virtualizer not supported on this device", t)
        virtualizer = null
        false
    }

    private fun initLoudnessEnhancer(): Boolean = try {
        val le = LoudnessEnhancer(GLOBAL_SESSION_ID)
        loudnessEnhancer = le
        le.enabled = false
        true
    } catch (t: Throwable) {
        Log.w(TAG, "LoudnessEnhancer not supported on this device", t)
        loudnessEnhancer = null
        false
    }

    private fun refreshBandsFromEngine() {
        val eq = equalizer ?: return
        val range = eq.bandLevelRange
        _bands.value = (0 until eq.numberOfBands.toInt()).map { i ->
            BandInfo(
                index = i,
                centerFreqHz = eq.getCenterFreq(i.toShort()) / 1000,
                minLevelMilliBel = range[0],
                maxLevelMilliBel = range[1],
                currentLevelMilliBel = eq.getBandLevel(i.toShort())
            )
        }
    }

    /** Master on/off for all attached effects. */
    fun setGlobalEnabled(enabled: Boolean) {
        equalizer?.enabled = enabled
        bassBoost?.enabled = enabled && (_bassBoostStrength.value > 0)
        virtualizer?.enabled = enabled && (_virtualizerStrength.value > 0)
        loudnessEnhancer?.enabled = enabled && (_loudnessGainMilliBel.value != 0)
        _globalEnabled.value = enabled
    }

    /** Sets a single band's gain in millibel (100 mB = 1 dB), honoring 0.1 dB precision. */
    fun setBandLevel(bandIndex: Int, milliBel: Short) {
        val eq = equalizer ?: return
        recordUndoPoint()
        val clamped = clampToRange(milliBel)
        eq.setBandLevel(bandIndex.toShort(), clamped)
        _bands.value = _bands.value.map { band ->
            if (band.index == bandIndex) band.copy(currentLevelMilliBel = clamped) else band
        }
    }

    /** Applies a device preset by index (from [EqualizerCapabilities.devicePresetNames]). */
    fun useDevicePreset(presetIndex: Int) {
        val eq = equalizer ?: return
        recordUndoPoint()
        eq.usePreset(presetIndex.toShort())
        refreshBandsFromEngine()
    }

    /**
     * Applies a uniform offset across every band to simulate a preamp, since the
     * platform Equalizer has no dedicated preamp control. Each band is clamped to
     * the hardware's supported range (the "gain limiter") so the offset can never
     * push a band into clipping territory the hardware doesn't support.
     */
    fun setPreamp(milliBel: Int) {
        val eq = equalizer ?: return
        recordUndoPoint()
        val previousPreamp = _preampMilliBel.value
        _preampMilliBel.value = milliBel
        val range = eq.bandLevelRange
        _bands.value.forEach { band ->
            val baseLevel = band.currentLevelMilliBel - previousPreamp
            val target = (baseLevel + milliBel).coerceIn(range[0].toInt(), range[1].toInt()).toShort()
            eq.setBandLevel(band.index.toShort(), target)
        }
        refreshBandsFromEngine()
    }

    fun setBassBoostStrength(strength: Short) {
        recordUndoPoint()
        _bassBoostStrength.value = strength
        val bb = bassBoost ?: return
        bb.setStrength(strength)
        bb.enabled = _globalEnabled.value && strength > 0
    }

    fun setVirtualizerStrength(strength: Short) {
        recordUndoPoint()
        _virtualizerStrength.value = strength
        val vr = virtualizer ?: return
        vr.setStrength(strength)
        vr.enabled = _globalEnabled.value && strength > 0
    }

    /** Sets Loudness Enhancer target gain. Positive values only, per the platform API. */
    fun setLoudnessGain(milliBel: Int) {
        recordUndoPoint()
        val safe = milliBel.coerceIn(0, MAX_LOUDNESS_GAIN_MILLIBEL)
        _loudnessGainMilliBel.value = safe
        val le = loudnessEnhancer ?: return
        le.setTargetGain(safe.toFloat())
        le.enabled = _globalEnabled.value && safe != 0
    }

    private fun clampToRange(value: Short): Short {
        val range = _capabilities.value.bandLevelRangeMilliBel
        return value.coerceIn(range.start, range.endInclusive)
    }

    // --- Undo / redo ---------------------------------------------------------

    private fun captureSnapshot(): EffectSnapshot = EffectSnapshot(
        bandLevelsMilliBelByFreq = _bands.value.associate { it.centerFreqHz to it.currentLevelMilliBel },
        preampMilliBel = _preampMilliBel.value,
        bassBoostStrength = _bassBoostStrength.value,
        virtualizerStrength = _virtualizerStrength.value,
        loudnessGainMilliBel = _loudnessGainMilliBel.value
    )

    private fun recordUndoPoint() {
        if (isApplyingSnapshot) return
        undoStack.addLast(captureSnapshot())
        if (undoStack.size > MAX_HISTORY) undoStack.removeFirst()
        redoStack.clear()
        _canUndo.value = true
        _canRedo.value = false
        // Any manual edit means we're no longer guaranteed to match either A/B slot exactly.
        _activeSlot.value = null
    }

    private fun applySnapshot(snapshot: EffectSnapshot) {
        isApplyingSnapshot = true
        try {
            snapshot.bandLevelsMilliBelByFreq.forEach { (freqHz, milliBel) ->
                val closest = _bands.value.minByOrNull { kotlin.math.abs(it.centerFreqHz - freqHz) }
                closest?.let { setBandLevel(it.index, milliBel) }
            }
            setPreamp(snapshot.preampMilliBel)
            setBassBoostStrength(snapshot.bassBoostStrength)
            setVirtualizerStrength(snapshot.virtualizerStrength)
            setLoudnessGain(snapshot.loudnessGainMilliBel)
        } finally {
            isApplyingSnapshot = false
        }
    }

    /** Steps one entry back in history. No-op if there's nothing to undo. */
    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(captureSnapshot())
        applySnapshot(previous)
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = true
    }

    /** Steps one entry forward again after an [undo]. No-op if there's nothing to redo. */
    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(captureSnapshot())
        applySnapshot(next)
        _canRedo.value = redoStack.isNotEmpty()
        _canUndo.value = true
    }

    /** Resets a single band to 0 dB (recorded as an undoable action). */
    fun resetBand(bandIndex: Int) = setBandLevel(bandIndex, 0.toShort())

    /** Resets every band, the preamp, and every effect strength back to a neutral flat state. */
    fun resetAll() {
        recordUndoPoint()
        isApplyingSnapshot = true // the individual setters below already recorded the one point above
        try {
            _bands.value.forEach { setBandLevel(it.index, 0.toShort()) }
            setPreamp(0)
            setBassBoostStrength(0.toShort())
            setVirtualizerStrength(0.toShort())
            setLoudnessGain(0)
        } finally {
            isApplyingSnapshot = false
        }
    }

    // --- A/B compare -----------------------------------------------------------

    fun saveToSlotA() {
        _slotA.value = captureSnapshot()
        _activeSlot.value = 'A'
    }

    fun saveToSlotB() {
        _slotB.value = captureSnapshot()
        _activeSlot.value = 'B'
    }

    /** Recalls slot A if it's been saved. The recall itself is undoable. */
    fun recallSlotA() {
        val snapshot = _slotA.value ?: return
        recordUndoPoint()
        applySnapshot(snapshot)
        _activeSlot.value = 'A'
    }

    /** Recalls slot B if it's been saved. The recall itself is undoable. */
    fun recallSlotB() {
        val snapshot = _slotB.value ?: return
        recordUndoPoint()
        applySnapshot(snapshot)
        _activeSlot.value = 'B'
    }

    /** Toggles between whichever slots are populated, for rapid A/B listening comparisons. */
    fun toggleAB() {
        when (_activeSlot.value) {
            'A' -> if (_slotB.value != null) recallSlotB() else recallSlotA()
            'B' -> if (_slotA.value != null) recallSlotA() else recallSlotB()
            else -> (_slotA.value)?.let { recallSlotA() } ?: _slotB.value?.let { recallSlotB() }
        }
    }

    fun release() {
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
        loudnessEnhancer?.release()
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
        initialized = false
    }

    companion object {
        private const val TAG = "AudioEffectManager"

        /** Session 0 = platform global output mix, not tied to one app's playback. */
        const val GLOBAL_SESSION_ID = 0

        /** LoudnessEnhancer accepts arbitrarily large gain; we cap it to avoid destructive clipping. */
        const val MAX_LOUDNESS_GAIN_MILLIBEL = 2000 // +20 dB ceiling

        /** Cap on undo history depth to bound memory use during long editing sessions. */
        const val MAX_HISTORY = 50
    }
}

/** 0.1 dB step expressed in millibel, used by the UI slider for fine adjustment. */
fun Double.dbToMilliBel(): Short = (this * 100.0).roundToInt().toShort()
fun Short.milliBelToDb(): Double = this / 100.0
