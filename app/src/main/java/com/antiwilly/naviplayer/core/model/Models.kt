package com.antiwilly.naviplayer.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class TranscodeFormat {
    ORIGINAL,
    OPUS,
    MP3,
    AAC,
    FLAC
}

@Serializable
data class AudioQualityConfig(
    val format: TranscodeFormat = TranscodeFormat.ORIGINAL,
    val maxBitRate: Int? = null // in kbps, e.g. 192, 320, null for original
)

@Serializable
data class ServerProfile(
    val id: Long = 0,
    val name: String,
    val baseUrl: String,
    val username: String,
    val token: String,
    val salt: String,
    val customHeaders: Map<String, String> = emptyMap(),
    val wifiQuality: AudioQualityConfig = AudioQualityConfig(TranscodeFormat.ORIGINAL, null),
    val cellularQuality: AudioQualityConfig = AudioQualityConfig(TranscodeFormat.OPUS, 192),
    val isActive: Boolean = false
)

@Serializable
data class Song(
    val id: String,
    val title: String,
    val artist: String = "",
    val artistId: String = "",
    val album: String = "",
    val albumId: String = "",
    val durationSec: Int = 0,
    val trackNumber: Int = 0,
    val discNumber: Int = 1,
    val year: Int = 0,
    val genre: String = "",
    val coverArtId: String? = null,
    val sizeBytes: Long = 0L,
    val bitRate: Int = 0,
    val contentType: String = "audio/mpeg",
    val suffix: String = "mp3",
    val isStarred: Boolean = false,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null
)

@Serializable
data class Album(
    val id: String,
    val title: String,
    val artist: String = "",
    val artistId: String = "",
    val coverArtId: String? = null,
    val songCount: Int = 0,
    val durationSec: Int = 0,
    val year: Int = 0,
    val genre: String = "",
    val isStarred: Boolean = false
)

@Serializable
data class Artist(
    val id: String,
    val name: String,
    val albumCount: Int = 0,
    val coverArtId: String? = null,
    val isStarred: Boolean = false
)

@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val songCount: Int = 0,
    val durationSec: Int = 0,
    val comment: String = "",
    val coverArtId: String? = null,
    val isPublic: Boolean = false
)

@Serializable
data class SearchResult(
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val songs: List<Song> = emptyList()
)

data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isBuffering: Boolean = false,
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = -1,
    val repeatMode: Int = 0, // 0 = off, 1 = one, 2 = all
    val shuffleMode: Boolean = false,
    val sleepTimerRemainingSec: Long? = null
)
