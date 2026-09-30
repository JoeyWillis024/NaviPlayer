package com.antiwilly.naviplayer.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antiwilly.naviplayer.core.data.repository.MusicRepository
import com.antiwilly.naviplayer.core.data.repository.PlaybackRepository
import com.antiwilly.naviplayer.core.data.repository.ServerRepository
import com.antiwilly.naviplayer.core.model.Album
import com.antiwilly.naviplayer.core.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playbackRepository: PlaybackRepository,
    private val serverRepository: ServerRepository
) : ViewModel() {

    private val _recentlyAdded = MutableStateFlow<List<Album>>(emptyList())
    val recentlyAdded: StateFlow<List<Album>> = _recentlyAdded.asStateFlow()

    private val _mostPlayed = MutableStateFlow<List<Album>>(emptyList())
    val mostPlayed: StateFlow<List<Album>> = _mostPlayed.asStateFlow()

    private val _randomAlbums = MutableStateFlow<List<Album>>(emptyList())
    val randomAlbums: StateFlow<List<Album>> = _randomAlbums.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            serverRepository.activeProfile.collect { profile ->
                if (profile != null) {
                    refreshData()
                } else {
                    _isLoading.value = false
                }
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true

            launch {
                musicRepository.getAlbums(type = "newest", size = 20)
                    .catch { emit(emptyList()) }
                    .collect { _recentlyAdded.value = it }
            }

            launch {
                musicRepository.getAlbums(type = "frequent", size = 20)
                    .catch { emit(emptyList()) }
                    .collect { _mostPlayed.value = it }
            }

            launch {
                musicRepository.getAlbums(type = "random", size = 20)
                    .catch { emit(emptyList()) }
                    .collect {
                        _randomAlbums.value = it
                        _isLoading.value = false
                    }
            }
        }
    }

    fun playAlbum(album: Album) {
        viewModelScope.launch {
            musicRepository.getAlbumDetail(album.id)
                .catch { /* handle error */ }
                .collect { (_, songs) ->
                    if (songs.isNotEmpty()) {
                        playbackRepository.playSong(songs.first(), songs)
                    }
                }
        }
    }
}
