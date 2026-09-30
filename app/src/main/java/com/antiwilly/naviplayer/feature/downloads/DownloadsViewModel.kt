package com.antiwilly.naviplayer.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antiwilly.naviplayer.core.data.repository.DownloadRepository
import com.antiwilly.naviplayer.core.data.repository.PlaybackRepository
import com.antiwilly.naviplayer.core.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val playbackRepository: PlaybackRepository
) : ViewModel() {

    val downloadedSongs: StateFlow<List<Song>> = downloadRepository.downloadedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSizeBytes: StateFlow<Long> = downloadRepository.totalSizeBytes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun playSong(song: Song) {
        val list = downloadedSongs.value
        playbackRepository.playSong(song, list)
    }

    fun deleteDownload(song: Song) {
        viewModelScope.launch {
            downloadRepository.deleteDownload(song.id)
        }
    }
}
