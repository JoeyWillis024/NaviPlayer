package com.antiwilly.naviplayer.core.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.util.Log

class AudioEffectsController {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var currentSessionId: Int = 0

    val presets = mapOf(
        "Flat" to listOf(0, 0, 0, 0, 0),
        "Bass Boost" to listOf(600, 400, 100, 0, 0),
        "Rock" to listOf(400, 200, -100, 200, 500),
        "Pop" to listOf(-100, 200, 500, 200, -100),
        "Jazz" to listOf(300, 100, 0, 200, 400),
        "Electronic" to listOf(500, 300, 0, 200, 400),
        "Vocal" to listOf(-200, 0, 500, 400, 100)
    )

    fun attachAudioSession(audioSessionId: Int) {
        if (audioSessionId == 0 || audioSessionId == currentSessionId) return
        currentSessionId = audioSessionId

        release()

        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
            }
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = true
            }
        } catch (e: Exception) {
            Log.e("AudioEffectsController", "Failed to initialize audio effects: ${e.message}")
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        try {
            equalizer?.enabled = enabled
        } catch (e: Exception) {
            Log.e("AudioEffectsController", "Error setting EQ enabled: ${e.message}")
        }
    }

    fun getBandFrequencies(): List<Int> {
        val eq = equalizer ?: return listOf(60, 230, 910, 3600, 14000)
        return try {
            (0 until eq.numberOfBands).map { band ->
                eq.getCenterFreq(band.toShort()) / 1000 // In Hz
            }
        } catch (_: Exception) {
            listOf(60, 230, 910, 3600, 14000)
        }
    }

    fun setBandLevel(bandIndex: Int, levelMilliBels: Int) {
        try {
            equalizer?.setBandLevel(bandIndex.toShort(), levelMilliBels.toShort())
        } catch (e: Exception) {
            Log.e("AudioEffectsController", "Error setting band level: ${e.message}")
        }
    }

    fun applyPreset(presetName: String) {
        val bands = presets[presetName] ?: return
        val eq = equalizer ?: return
        val numBands = minOf(bands.size, eq.numberOfBands.toInt())
        for (i in 0 until numBands) {
            try {
                eq.setBandLevel(i.toShort(), bands[i].toShort())
            } catch (e: Exception) {
                Log.e("AudioEffectsController", "Error applying preset band: ${e.message}")
            }
        }
    }

    fun setBassBoost(strength: Int) { // 0 to 1000
        try {
            bassBoost?.apply {
                if (strengthSupported) {
                    setStrength(strength.coerceIn(0, 1000).toShort())
                    enabled = strength > 0
                }
            }
        } catch (e: Exception) {
            Log.e("AudioEffectsController", "Error setting bass boost: ${e.message}")
        }
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
        } catch (_: Exception) {}
        equalizer = null
        bassBoost = null
    }
}
