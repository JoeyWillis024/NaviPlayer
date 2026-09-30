package com.antiwilly.naviplayer.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antiwilly.naviplayer.core.data.repository.ServerRepository
import com.antiwilly.naviplayer.core.model.AudioQualityConfig
import com.antiwilly.naviplayer.core.model.ServerProfile
import com.antiwilly.naviplayer.core.model.TranscodeFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val serverRepository: ServerRepository
) : ViewModel() {

    val allProfiles: StateFlow<List<ServerProfile>> = serverRepository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProfile: StateFlow<ServerProfile?> = serverRepository.activeProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _connectionTestResult = MutableStateFlow<String?>(null)
    val connectionTestResult: StateFlow<String?> = _connectionTestResult.asStateFlow()

    fun setActiveProfile(profileId: Long) {
        viewModelScope.launch {
            serverRepository.setActiveProfile(profileId)
        }
    }

    fun saveProfile(
        id: Long = 0,
        name: String,
        baseUrl: String,
        username: String,
        passwordOrToken: String,
        customHeaders: Map<String, String> = emptyMap(),
        wifiFormat: TranscodeFormat = TranscodeFormat.ORIGINAL,
        wifiBitRate: Int? = null,
        cellularFormat: TranscodeFormat = TranscodeFormat.OPUS,
        cellularBitRate: Int? = 192
    ) {
        viewModelScope.launch {
            val profile = ServerProfile(
                id = id,
                name = name,
                baseUrl = baseUrl,
                username = username,
                token = passwordOrToken,
                salt = "",
                customHeaders = customHeaders,
                wifiQuality = AudioQualityConfig(wifiFormat, wifiBitRate),
                cellularQuality = AudioQualityConfig(cellularFormat, cellularBitRate),
                isActive = true
            )
            serverRepository.saveProfile(profile)
        }
    }

    fun deleteProfile(profile: ServerProfile) {
        viewModelScope.launch {
            serverRepository.deleteProfile(profile)
        }
    }

    fun testConnection(
        baseUrl: String,
        username: String,
        token: String,
        customHeaders: Map<String, String> = emptyMap()
    ) {
        viewModelScope.launch {
            _connectionTestResult.value = "Testing connection..."
            val tempProfile = ServerProfile(
                name = "Test",
                baseUrl = baseUrl,
                username = username,
                token = token,
                salt = "",
                customHeaders = customHeaders
            )
            val result = serverRepository.testConnection(tempProfile)
            _connectionTestResult.value = if (result.isSuccess) {
                "Connection successful! Connected to Navidrome."
            } else {
                "Failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun clearConnectionTestResult() {
        _connectionTestResult.value = null
    }
}
