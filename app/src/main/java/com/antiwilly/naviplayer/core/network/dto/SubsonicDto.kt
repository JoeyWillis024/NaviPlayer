package com.antiwilly.naviplayer.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubsonicRootResponse(
    @SerialName("subsonic-response")
    val response: SubsonicResponse
)

@Serializable
data class SubsonicResponse(
    val status: String,
    val version: String? = null,
    val type: String? = null,
    val serverVersion: String? = null,
    val openSubsonic: Boolean? = null,
    val error: SubsonicError? = null,
    val artists: ArtistsListDto? = null,
    val artist: ArtistDetailDto? = null,
    val albumList2: AlbumList2Dto? = null,
    val album: AlbumDetailDto? = null,
    val searchResult3: SearchResult3Dto? = null,
    val playlists: PlaylistsDto? = null,
    val playlist: PlaylistDetailDto? = null,
    val genres: GenresDto? = null
)

@Serializable
data class SubsonicError(
    val code: Int,
    val message: String
)

@Serializable
data class ArtistsListDto(
    val ignoredArticles: String? = null,
    val index: List<ArtistIndexDto> = emptyList()
)

@Serializable
data class ArtistIndexDto(
    val name: String,
    val artist: List<ArtistDto> = emptyList()
)

@Serializable
data class ArtistDto(
    val id: String,
    val name: String,
    val coverArt: String? = null,
    val albumCount: Int? = 0,
    val starred: String? = null
)

@Serializable
data class ArtistDetailDto(
    val id: String,
    val name: String,
    val coverArt: String? = null,
    val albumCount: Int? = 0,
    val album: List<AlbumDto> = emptyList(),
    val starred: String? = null
)

@Serializable
data class AlbumList2Dto(
    val album: List<AlbumDto> = emptyList()
)

@Serializable
data class AlbumDto(
    val id: String,
    val name: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val artistId: String? = null,
    val coverArt: String? = null,
    val songCount: Int? = 0,
    val duration: Int? = 0,
    val playCount: Long? = 0,
    val year: Int? = null,
    val genre: String? = null,
    val starred: String? = null
)

@Serializable
data class AlbumDetailDto(
    val id: String,
    val name: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val artistId: String? = null,
    val coverArt: String? = null,
    val songCount: Int? = 0,
    val duration: Int? = 0,
    val year: Int? = null,
    val genre: String? = null,
    val song: List<ChildSongDto> = emptyList(),
    val starred: String? = null
)

@Serializable
data class ChildSongDto(
    val id: String,
    val parent: String? = null,
    val isDir: Boolean? = false,
    val title: String,
    val album: String? = null,
    val artist: String? = null,
    val track: Int? = null,
    val discNumber: Int? = null,
    val year: Int? = null,
    val genre: String? = null,
    val coverArt: String? = null,
    val size: Long? = null,
    val contentType: String? = null,
    val suffix: String? = null,
    val duration: Int? = null,
    val bitRate: Int? = null,
    val path: String? = null,
    val playCount: Long? = null,
    val starred: String? = null,
    val albumId: String? = null,
    val artistId: String? = null,
    val type: String? = null
)

@Serializable
data class SearchResult3Dto(
    val artist: List<ArtistDto> = emptyList(),
    val album: List<AlbumDto> = emptyList(),
    val song: List<ChildSongDto> = emptyList()
)

@Serializable
data class PlaylistsDto(
    val playlist: List<PlaylistDto> = emptyList()
)

@Serializable
data class PlaylistDto(
    val id: String,
    val name: String,
    val comment: String? = null,
    val owner: String? = null,
    val public: Boolean? = false,
    val songCount: Int? = 0,
    val duration: Int? = 0,
    val coverArt: String? = null,
    val created: String? = null,
    val changed: String? = null
)

@Serializable
data class PlaylistDetailDto(
    val id: String,
    val name: String,
    val comment: String? = null,
    val owner: String? = null,
    val public: Boolean? = false,
    val songCount: Int? = 0,
    val duration: Int? = 0,
    val coverArt: String? = null,
    val entry: List<ChildSongDto> = emptyList()
)

@Serializable
data class GenresDto(
    val genre: List<GenreDto> = emptyList()
)

@Serializable
data class GenreDto(
    val value: String,
    val songCount: Int? = 0,
    val albumCount: Int? = 0
)
