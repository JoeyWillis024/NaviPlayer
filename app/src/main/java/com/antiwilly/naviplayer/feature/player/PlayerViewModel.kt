package com.antiwilly.naviplayer.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antiwilly.naviplayer.core.data.repository.DownloadRepository
import com.antiwilly.naviplayer.core.data.repository.MusicRepository
import com.antiwilly.naviplayer.core.data.repository.PlaybackRepository
import com.antiwilly.naviplayer.core.model.PlaybackState
import com.antiwilly.naviplayer.core.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackRepository: PlaybackRepository,
    private val musicRepository: MusicRepository,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playbackRepository.playbackState

    fun playSong(song: Song, queue: List<Song> = listOf(song)) {
        playbackRepository.playSong(song, queue)
    }

    fun togglePlayPause() {
        val state = playbackState.value
        if (state.isPlaying) {
            playbackRepository.pause()
        } else {
            playbackRepository.play()
        }
    }

    fun seekTo(positionMs: Long) {
        playbackRepository.seekTo(positionMs)
    }

    fun skipNext() {
        playbackRepository.skipToNext()
    }

    fun skipPrevious() {
        playbackRepository.skipToPrevious()
    }

    fun toggleShuffle() {
        playbackRepository.toggleShuffle()
    }

    fun cycleRepeatMode() {
        playbackRepository.cycleRepeatMode()
    }

    fun setEqualizerPreset(preset: String) {
        playbackRepository.setEqualizerPreset(preset)
    }

    fun setBassBoost(strength: Int) {
        playbackRepository.setBassBoost(strength)
    }

    fun startSleepTimer(minutes: Int) {
        playbackRepository.startSleepTimer(minutes)
    }

    fun cancelSleepTimer() {
        playbackRepository.cancelSleepTimer()
    }

    fun toggleStar(song: Song) {
        viewModelScope.launch {
            musicRepository.toggleStar(songId = song.id, star = !song.isStarred)
        }
    }

    fun downloadSong(song: Song) {
        downloadRepository.downloadSong(song)
    }
}
