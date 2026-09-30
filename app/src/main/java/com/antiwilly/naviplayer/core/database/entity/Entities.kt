package com.antiwilly.naviplayer.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.antiwilly.naviplayer.core.model.Album
import com.antiwilly.naviplayer.core.model.Artist
import com.antiwilly.naviplayer.core.model.AudioQualityConfig
import com.antiwilly.naviplayer.core.model.ServerProfile
import com.antiwilly.naviplayer.core.model.Song
import com.antiwilly.naviplayer.core.model.TranscodeFormat
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "server_profiles")
data class ServerProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val baseUrl: String,
    val username: String,
    val token: String,
    val salt: String,
    val customHeadersJson: String = "{}",
    val wifiFormat: String = TranscodeFormat.ORIGINAL.name,
    val wifiMaxBitRate: Int? = null,
    val cellularFormat: String = TranscodeFormat.OPUS.name,
    val cellularMaxBitRate: Int? = 192,
    val isActive: Boolean = false
) {
    fun toDomainModel(): ServerProfile {
        val headers = try {
            Json.decodeFromString<Map<String, String>>(customHeadersJson)
        } catch (_: Exception) {
            emptyMap()
        }
        return ServerProfile(
            id = id,
            name = name,
            baseUrl = baseUrl,
            username = username,
            token = token,
            salt = salt,
            customHeaders = headers,
            wifiQuality = AudioQualityConfig(
                format = TranscodeFormat.valueOf(wifiFormat),
                maxBitRate = wifiMaxBitRate
            ),
            cellularQuality = AudioQualityConfig(
                format = TranscodeFormat.valueOf(cellularFormat),
                maxBitRate = cellularMaxBitRate
            ),
            isActive = isActive
        )
    }

    companion object {
        fun fromDomainModel(profile: ServerProfile): ServerProfileEntity {
            val headersJson = Json.encodeToString(profile.customHeaders)
            return ServerProfileEntity(
                id = profile.id,
                name = profile.name,
                baseUrl = profile.baseUrl,
                username = profile.username,
                token = profile.token,
                salt = profile.salt,
                customHeadersJson = headersJson,
                wifiFormat = profile.wifiQuality.format.name,
                wifiMaxBitRate = profile.wifiQuality.maxBitRate,
                cellularFormat = profile.cellularQuality.format.name,
                cellularMaxBitRate = profile.cellularQuality.maxBitRate,
                isActive = profile.isActive
            )
        }
    }
}

@Entity(tableName = "downloaded_songs")
data class DownloadedSongEntity(
    @PrimaryKey
    val id: String,
    val serverId: Long,
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
    val localCoverArtPath: String? = null,
    val localFilePath: String,
    val sizeBytes: Long = 0L,
    val contentType: String = "audio/mpeg",
    val suffix: String = "mp3",
    val downloadedAt: Long = System.currentTimeMillis(),
    val isStarred: Boolean = false
) {
    fun toDomainModel(): Song {
        return Song(
            id = id,
            title = title,
            artist = artist,
            artistId = artistId,
            album = album,
            albumId = albumId,
            durationSec = durationSec,
            trackNumber = trackNumber,
            discNumber = discNumber,
            year = year,
            genre = genre,
            coverArtId = coverArtId,
            sizeBytes = sizeBytes,
            bitRate = 0,
            contentType = contentType,
            suffix = suffix,
            isStarred = isStarred,
            isDownloaded = true,
            localFilePath = localFilePath
        )
    }
}

@Entity(tableName = "cached_albums")
data class CachedAlbumEntity(
    @PrimaryKey
    val id: String,
    val serverId: Long,
    val title: String,
    val artist: String = "",
    val artistId: String = "",
    val coverArtId: String? = null,
    val songCount: Int = 0,
    val durationSec: Int = 0,
    val year: Int = 0,
    val genre: String = "",
    val isStarred: Boolean = false,
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): Album {
        return Album(
            id = id,
            title = title,
            artist = artist,
            artistId = artistId,
            coverArtId = coverArtId,
            songCount = songCount,
            durationSec = durationSec,
            year = year,
            genre = genre,
            isStarred = isStarred
        )
    }
}

@Entity(tableName = "cached_artists")
data class CachedArtistEntity(
    @PrimaryKey
    val id: String,
    val serverId: Long,
    val name: String,
    val albumCount: Int = 0,
    val coverArtId: String? = null,
    val isStarred: Boolean = false,
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): Artist {
        return Artist(
            id = id,
            name = name,
            albumCount = albumCount,
            coverArtId = coverArtId,
            isStarred = isStarred
        )
    }
}
