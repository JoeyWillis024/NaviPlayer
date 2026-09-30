package com.antiwilly.naviplayer.core.database

import android.content.Context
import androidx.room.Room
import com.antiwilly.naviplayer.core.database.dao.CacheDao
import com.antiwilly.naviplayer.core.database.dao.DownloadDao
import com.antiwilly.naviplayer.core.database.dao.ServerProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideNaviDatabase(
        @ApplicationContext context: Context
    ): NaviDatabase {
        return Room.databaseBuilder(
            context,
            NaviDatabase::class.java,
            "navi_player.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideServerProfileDao(database: NaviDatabase): ServerProfileDao {
        return database.serverProfileDao()
    }

    @Provides
    fun provideDownloadDao(database: NaviDatabase): DownloadDao {
        return database.downloadDao()
    }

    @Provides
    fun provideCacheDao(database: NaviDatabase): CacheDao {
        return database.cacheDao()
    }
}
