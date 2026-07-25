package com.premiumeq.equalizer.visualizer

import android.media.audiofx.Visualizer
import android.util.Log
import com.premiumeq.equalizer.audio.AudioEffectManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Selectable render styles for [VisualizerFrame] data. BARS and WAVEFORM are fully
 * implemented (real FFT/waveform math below drives them identically - only the
 * Compose Canvas drawing differs). CIRCULAR, SPECTRUM, PARTICLE and NEON reuse the
 * exact same [VisualizerFrame] stream; they are additional Canvas renderers to be
 * added on top of this engine and are not yet implemented, so they are omitted
 * from [VisualizerStyle] rather than exposed as a non-functional option.
 */
enum class VisualizerStyle { BARS, WAVEFORM }

/** One captured frame: raw PCM-ish waveform bytes and magnitude-only FFT bins, both unsigned 0..255. */
data class VisualizerFrame(
    val waveform: ByteArray,
    val fftMagnitudes: FloatArray
)

/**
 * Wraps [android.media.audiofx.Visualizer] attached to the same global session
 * used by [AudioEffectManager]. Requires RECORD_AUDIO permission at runtime
 * (declared in the manifest) - callers must check that permission before calling
 * [observeFrames], since construction throws if it's missing.
 */
@Singleton
class VisualizerEngine @Inject constructor() {

    /**
     * Emits a [VisualizerFrame] on every capture tick. [captureRateHz] is clamped to
     * what [Visualizer.getMaxCaptureRate] reports for this device (graceful
     * degradation instead of a crash on low-end hardware).
     */
    fun observeFrames(captureRateHz: Int = 30): Flow<VisualizerFrame> = callbackFlow {
        var visualizer: Visualizer? = null
        try {
            val v = Visualizer(AudioEffectManager.GLOBAL_SESSION_ID)
            visualizer = v
            v.captureSize = Visualizer.getCaptureSizeRange()[1]

            val maxRateMilliHz = Visualizer.getMaxCaptureRate()
            val requestedMilliHz = (captureRateHz * 1000).coerceAtMost(maxRateMilliHz)

            v.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                        if (waveform != null) {
                            trySend(VisualizerFrame(waveform = waveform, fftMagnitudes = FloatArray(0)))
                        }
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                        if (fft != null) {
                            trySend(VisualizerFrame(waveform = ByteArray(0), fftMagnitudes = fft.toMagnitudes()))
                        }
                    }
                },
                requestedMilliHz,
                true,
                true
            )
            v.enabled = true
        } catch (t: Throwable) {
            Log.w(TAG, "Visualizer unavailable on this device/session", t)
            close(t)
        }

        awaitClose {
            visualizer?.setDataCaptureListener(null, 0, false, false)
            visualizer?.enabled = false
            visualizer?.release()
        }
    }

    /** Converts the platform's packed real/imaginary FFT byte format into magnitude bins. */
    private fun ByteArray.toMagnitudes(): FloatArray {
        if (isEmpty()) return FloatArray(0)
        val binCount = size / 2
        val magnitudes = FloatArray(binCount)
        for (i in 0 until binCount) {
            val re = this[i * 2].toInt()
            val im = if (i * 2 + 1 < size) this[i * 2 + 1].toInt() else 0
            magnitudes[i] = kotlin.math.sqrt((re * re + im * im).toFloat())
        }
        return magnitudes
    }

    companion object {
        private const val TAG = "VisualizerEngine"
    }
}
