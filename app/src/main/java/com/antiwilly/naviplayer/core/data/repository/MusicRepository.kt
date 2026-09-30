package com.antiwilly.naviplayer.core.data.repository

import com.antiwilly.naviplayer.core.database.dao.CacheDao
import com.antiwilly.naviplayer.core.database.dao.DownloadDao
import com.antiwilly.naviplayer.core.database.dao.ServerProfileDao
import com.antiwilly.naviplayer.core.database.entity.CachedAlbumEntity
import com.antiwilly.naviplayer.core.database.entity.CachedArtistEntity
import com.antiwilly.naviplayer.core.model.Album
import com.antiwilly.naviplayer.core.model.Artist
import com.antiwilly.naviplayer.core.model.Playlist
import com.antiwilly.naviplayer.core.model.SearchResult
import com.antiwilly.naviplayer.core.model.Song
import com.antiwilly.naviplayer.core.network.SubsonicApiService
import com.antiwilly.naviplayer.core.network.dto.AlbumDto
import com.antiwilly.naviplayer.core.network.dto.ArtistDto
import com.antiwilly.naviplayer.core.network.dto.ChildSongDto
import com.antiwilly.naviplayer.core.network.dto.PlaylistDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val api: SubsonicApiService,
    private val cacheDao: CacheDao,
    private val downloadDao: DownloadDao,
    private val serverProfileDao: ServerProfileDao
) {

    fun getAlbums(type: String, size: Int = 50): Flow<List<Album>> = flow {
        val server = serverProfileDao.getActiveProfileSync()
        try {
            val response = api.getAlbumList2(type = type, size = size)
            val list = response.response.albumList2?.album?.map { it.toDomainModel() } ?: emptyList()
            emit(list)

            if (server != null && list.isNotEmpty() && type == "newest") {
                val cached = list.map {
                    CachedAlbumEntity(
                        id = it.id,
                        serverId = server.id,
                        title = it.title,
                        artist = it.artist,
                        artistId = it.artistId,
                        coverArtId = it.coverArtId,
                        songCount = it.songCount,
                        durationSec = it.durationSec,
                        year = it.year,
                        genre = it.genre,
                        isStarred = it.isStarred
                    )
                }
                cacheDao.insertAlbums(cached)
            }
        } catch (_: Exception) {
            // Offline fallback
            if (server != null) {
                cacheDao.getCachedAlbums(server.id).collect { cached ->
                    emit(cached.map { it.toDomainModel() })
                }
            } else {
                emit(emptyList())
            }
        }
    }.flowOn(Dispatchers.IO)

    fun getArtists(): Flow<List<Artist>> = flow {
        val server = serverProfileDao.getActiveProfileSync()
        try {
            val response = api.getArtists()
            val artists = response.response.artists?.index?.flatMap { index ->
                index.artist.map { it.toDomainModel() }
            } ?: emptyList()
            emit(artists)

            if (server != null && artists.isNotEmpty()) {
                val cached = artists.map {
                    CachedArtistEntity(
                        id = it.id,
                        serverId = server.id,
                        name = it.name,
                        albumCount = it.albumCount,
                        coverArtId = it.coverArtId,
                        isStarred = it.isStarred
                    )
                }
                cacheDao.insertArtists(cached)
            }
        } catch (_: Exception) {
            if (server != null) {
                cacheDao.getCachedArtists(server.id).collect { cached ->
                    emit(cached.map { it.toDomainModel() })
                }
            } else {
                emit(emptyList())
            }
        }
    }.flowOn(Dispatchers.IO)

    fun getAlbumDetail(albumId: String): Flow<Pair<Album, List<Song>>> = flow {
        val response = api.getAlbum(albumId)
        val albumDto = response.response.album
            ?: throw IllegalStateException("Album not found")

        val album = Album(
            id = albumDto.id,
            title = albumDto.name ?: albumDto.title ?: "Unknown Album",
            artist = albumDto.artist ?: "Unknown Artist",
            artistId = albumDto.artistId ?: "",
            coverArtId = albumDto.coverArt,
            songCount = albumDto.songCount ?: albumDto.song.size,
            durationSec = albumDto.duration ?: 0,
            year = albumDto.year ?: 0,
            genre = albumDto.genre ?: "",
            isStarred = albumDto.starred != null
        )

        val songs = albumDto.song.map { songDto ->
            val isDownloaded = downloadDao.getDownloadedSong(songDto.id) != null
            songDto.toDomainModel(isDownloaded = isDownloaded)
        }

        emit(Pair(album, songs))
    }.flowOn(Dispatchers.IO)

    fun getArtistDetail(artistId: String): Flow<Pair<Artist, List<Album>>> = flow {
        val response = api.getArtist(artistId)
        val artistDto = response.response.artist
            ?: throw IllegalStateException("Artist not found")

        val artist = Artist(
            id = artistDto.id,
            name = artistDto.name,
            albumCount = artistDto.albumCount ?: artistDto.album.size,
            coverArtId = artistDto.coverArt,
            isStarred = artistDto.starred != null
        )

        val albums = artistDto.album.map { it.toDomainModel() }
        emit(Pair(artist, albums))
    }.flowOn(Dispatchers.IO)

    fun getPlaylists(): Flow<List<Playlist>> = flow {
        try {
            val response = api.getPlaylists()
            val list = response.response.playlists?.playlist?.map { it.toDomainModel() } ?: emptyList()
            emit(list)
        } catch (_: Exception) {
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    fun getPlaylistDetail(playlistId: String): Flow<Pair<Playlist, List<Song>>> = flow {
        val response = api.getPlaylist(playlistId)
        val pDto = response.response.playlist
            ?: throw IllegalStateException("Playlist not found")

        val playlist = Playlist(
            id = pDto.id,
            name = pDto.name,
            songCount = pDto.songCount ?: pDto.entry.size,
            durationSec = pDto.duration ?: 0,
            comment = pDto.comment ?: "",
            coverArtId = pDto.coverArt,
            isPublic = pDto.public ?: false
        )

        val songs = pDto.entry.map { it.toDomainModel() }
        emit(Pair(playlist, songs))
    }.flowOn(Dispatchers.IO)

    suspend fun createPlaylist(name: String, songIds: List<String> = emptyList()): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            api.createPlaylist(name, songIds)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePlaylist(
        playlistId: String,
        name: String? = null,
        songIdToAdd: List<String>? = null,
        songIndexToRemove: List<Int>? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            api.updatePlaylist(
                playlistId = playlistId,
                name = name,
                songIdToAdd = songIdToAdd,
                songIndexToRemove = songIndexToRemove
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun search(query: String): Flow<SearchResult> = flow {
        if (query.isBlank()) {
            emit(SearchResult())
            return@flow
        }
        try {
            val response = api.search3(query)
            val result = response.response.searchResult3
            val searchResult = SearchResult(
                artists = result?.artist?.map { it.toDomainModel() } ?: emptyList(),
                albums = result?.album?.map { it.toDomainModel() } ?: emptyList(),
                songs = result?.song?.map { it.toDomainModel() } ?: emptyList()
            )
            emit(searchResult)
        } catch (_: Exception) {
            emit(SearchResult())
        }
    }.flowOn(Dispatchers.IO)

    suspend fun toggleStar(
        songId: String? = null,
        albumId: String? = null,
        artistId: String? = null,
        star: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (star) {
                api.star(id = songId, albumId = albumId, artistId = artistId)
            } else {
                api.unstar(id = songId, albumId = albumId, artistId = artistId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Extension mapper functions
fun AlbumDto.toDomainModel(): Album = Album(
    id = id,
    title = name ?: title ?: "Unknown Album",
    artist = artist ?: "Unknown Artist",
    artistId = artistId ?: "",
    coverArtId = coverArt,
    songCount = songCount ?: 0,
    durationSec = duration ?: 0,
    year = year ?: 0,
    genre = genre ?: "",
    isStarred = starred != null
)

fun ArtistDto.toDomainModel(): Artist = Artist(
    id = id,
    name = name,
    albumCount = albumCount ?: 0,
    coverArtId = coverArt,
    isStarred = starred != null
)

fun PlaylistDto.toDomainModel(): Playlist = Playlist(
    id = id,
    name = name,
    songCount = songCount ?: 0,
    durationSec = duration ?: 0,
    comment = comment ?: "",
    coverArtId = coverArt,
    isPublic = public ?: false
)

fun ChildSongDto.toDomainModel(isDownloaded: Boolean = false, localFilePath: String? = null): Song = Song(
    id = id,
    title = title,
    artist = artist ?: "",
    artistId = artistId ?: "",
    album = album ?: "",
    albumId = albumId ?: "",
    durationSec = duration ?: 0,
    trackNumber = track ?: 0,
    discNumber = discNumber ?: 1,
    year = year ?: 0,
    genre = genre ?: "",
    coverArtId = coverArt,
    sizeBytes = size ?: 0L,
    bitRate = bitRate ?: 0,
    contentType = contentType ?: "audio/mpeg",
    suffix = suffix ?: "mp3",
    isStarred = starred != null,
    isDownloaded = isDownloaded,
    localFilePath = localFilePath
)
