package com.antiwilly.naviplayer.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antiwilly.naviplayer.core.data.repository.MusicRepository
import com.antiwilly.naviplayer.core.model.Album
import com.antiwilly.naviplayer.core.model.Artist
import com.antiwilly.naviplayer.core.model.Playlist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists.asStateFlow()

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadLibrary()
    }

    fun loadLibrary() {
        viewModelScope.launch {
            _isLoading.value = true

            launch {
                musicRepository.getArtists()
                    .catch { emit(emptyList()) }
                    .collect { _artists.value = it }
            }

            launch {
                musicRepository.getAlbums(type = "alphabeticalByName", size = 100)
                    .catch { emit(emptyList()) }
                    .collect { _albums.value = it }
            }

            launch {
                musicRepository.getPlaylists()
                    .catch { emit(emptyList()) }
                    .collect {
                        _playlists.value = it
                        _isLoading.value = false
                    }
            }
        }
    }

    fun createPlaylist(name: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            musicRepository.createPlaylist(name)
            loadLibrary()
            onComplete()
        }
    }
}
