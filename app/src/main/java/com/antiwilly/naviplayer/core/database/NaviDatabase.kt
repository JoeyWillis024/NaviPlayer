package com.antiwilly.naviplayer.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.antiwilly.naviplayer.core.database.dao.CacheDao
import com.antiwilly.naviplayer.core.database.dao.DownloadDao
import com.antiwilly.naviplayer.core.database.dao.ServerProfileDao
import com.antiwilly.naviplayer.core.database.entity.CachedAlbumEntity
import com.antiwilly.naviplayer.core.database.entity.CachedArtistEntity
import com.antiwilly.naviplayer.core.database.entity.DownloadedSongEntity
import com.antiwilly.naviplayer.core.database.entity.ServerProfileEntity

@Database(
    entities = [
        ServerProfileEntity::class,
        DownloadedSongEntity::class,
        CachedAlbumEntity::class,
        CachedArtistEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NaviDatabase : RoomDatabase() {
    abstract fun serverProfileDao(): ServerProfileDao
    abstract fun downloadDao(): DownloadDao
    abstract fun cacheDao(): CacheDao
}
