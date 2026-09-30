package com.antiwilly.naviplayer.core.playback

import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SleepTimerManager(
    private val scope: CoroutineScope,
    private val playerProvider: () -> ExoPlayer?
) {
    private var timerJob: Job? = null
    private val _remainingSeconds = MutableStateFlow<Long?>(null)
    val remainingSeconds: StateFlow<Long?> = _remainingSeconds.asStateFlow()

    fun startTimer(minutes: Int) {
        cancelTimer()
        val totalSec = minutes * 60L
        _remainingSeconds.value = totalSec

        timerJob = scope.launch(Dispatchers.Main) {
            var timeLeft = totalSec
            val player = playerProvider()
            val initialVolume = player?.volume ?: 1f

            while (timeLeft > 0 && isActive) {
                delay(1000)
                timeLeft--
                _remainingSeconds.value = timeLeft

                // Fade out volume over the last 30 seconds
                if (timeLeft in 1..30 && player != null) {
                    val fadeFactor = timeLeft.toFloat() / 30f
                    player.volume = initialVolume * fadeFactor
                }
            }

            if (isActive) {
                player?.pause()
                player?.volume = initialVolume // Reset volume for next playback
                _remainingSeconds.value = null
            }
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        val player = playerProvider()
        player?.volume = 1f
        _remainingSeconds.value = null
    }
}
