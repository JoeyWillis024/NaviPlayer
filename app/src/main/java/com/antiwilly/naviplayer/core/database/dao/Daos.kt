package com.antiwilly.naviplayer.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.antiwilly.naviplayer.core.database.entity.CachedAlbumEntity
import com.antiwilly.naviplayer.core.database.entity.CachedArtistEntity
import com.antiwilly.naviplayer.core.database.entity.DownloadedSongEntity
import com.antiwilly.naviplayer.core.database.entity.ServerProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerProfileDao {

    @Query("SELECT * FROM server_profiles ORDER BY name ASC")
    fun getAllProfiles(): Flow<List<ServerProfileEntity>>

    @Query("SELECT * FROM server_profiles WHERE isActive = 1 LIMIT 1")
    fun getActiveProfile(): Flow<ServerProfileEntity?>

    @Query("SELECT * FROM server_profiles WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProfileSync(): ServerProfileEntity?

    @Query("SELECT * FROM server_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Long): ServerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ServerProfileEntity): Long

    @Update
    suspend fun update(profile: ServerProfileEntity)

    @Delete
    suspend fun delete(profile: ServerProfileEntity)

    @Query("UPDATE server_profiles SET isActive = 0")
    suspend fun clearActiveProfiles()

    @Query("UPDATE server_profiles SET isActive = 1 WHERE id = :id")
    suspend fun markActiveProfile(id: Long)

    @Transaction
    suspend fun setActiveProfile(id: Long) {
        clearActiveProfiles()
        markActiveProfile(id)
    }
}

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloaded_songs ORDER BY downloadedAt DESC")
    fun getAllDownloadedSongs(): Flow<List<DownloadedSongEntity>>

    @Query("SELECT * FROM downloaded_songs WHERE id = :id LIMIT 1")
    suspend fun getDownloadedSong(id: String): DownloadedSongEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_songs WHERE id = :id)")
    fun isSongDownloaded(id: String): Flow<Boolean>

    @Query("SELECT * FROM downloaded_songs WHERE albumId = :albumId ORDER BY trackNumber ASC")
    fun getDownloadedSongsByAlbum(albumId: String): Flow<List<DownloadedSongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: DownloadedSongEntity)

    @Query("DELETE FROM downloaded_songs WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM downloaded_songs")
    fun getDownloadedCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM downloaded_songs")
    fun getTotalDownloadedSizeBytes(): Flow<Long>
}

@Dao
interface CacheDao {

    @Query("SELECT * FROM cached_albums WHERE serverId = :serverId ORDER BY title ASC")
    fun getCachedAlbums(serverId: Long): Flow<List<CachedAlbumEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbums(albums: List<CachedAlbumEntity>)

    @Query("SELECT * FROM cached_artists WHERE serverId = :serverId ORDER BY name ASC")
    fun getCachedArtists(serverId: Long): Flow<List<CachedArtistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtists(artists: List<CachedArtistEntity>)

    @Query("DELETE FROM cached_albums WHERE serverId = :serverId")
    suspend fun clearAlbumCache(serverId: Long)

    @Query("DELETE FROM cached_artists WHERE serverId = :serverId")
    suspend fun clearArtistCache(serverId: Long)

    @Transaction
    suspend fun clearCacheForServer(serverId: Long) {
        clearAlbumCache(serverId)
        clearArtistCache(serverId)
    }
}
