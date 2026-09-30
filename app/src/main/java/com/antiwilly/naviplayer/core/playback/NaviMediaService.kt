package com.antiwilly.naviplayer.core.playback

import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.antiwilly.naviplayer.core.database.dao.DownloadDao
import com.antiwilly.naviplayer.core.database.dao.ServerProfileDao
import com.antiwilly.naviplayer.core.datastore.UserPreferencesDataStore
import com.antiwilly.naviplayer.core.model.Song
import com.antiwilly.naviplayer.core.network.SubsonicApiService
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class NaviMediaService : MediaLibraryService() {

    @Inject lateinit var serverProfileDao: ServerProfileDao
    @Inject lateinit var subsonicApiService: SubsonicApiService
    @Inject lateinit var downloadDao: DownloadDao
    @Inject lateinit var userPreferencesDataStore: UserPreferencesDataStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var exoPlayer: ExoPlayer
    private lateinit var mediaLibrarySession: MediaLibrarySession
    lateinit var audioEffectsController: AudioEffectsController
        private set
    lateinit var sleepTimerManager: SleepTimerManager
        private set
    lateinit var scrobbleManager: ScrobbleManager
        private set

    private var simpleCache: SimpleCache? = null

    companion object {
        const val ROOT_ID = "root"
        const val FAVORITES_ID = "favorites"
        const val RECENT_ID = "recent"
        const val PLAYLISTS_ID = "playlists"
        const val COMMAND_SET_EQ_PRESET = "com.antiwilly.naviplayer.SET_EQ_PRESET"
        const val COMMAND_SET_EQ_ENABLED = "com.antiwilly.naviplayer.SET_EQ_ENABLED"
        const val COMMAND_SET_BASS_BOOST = "com.antiwilly.naviplayer.SET_BASS_BOOST"
        const val COMMAND_START_SLEEP_TIMER = "com.antiwilly.naviplayer.START_SLEEP_TIMER"
        const val COMMAND_CANCEL_SLEEP_TIMER = "com.antiwilly.naviplayer.CANCEL_SLEEP_TIMER"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        // 1GB LRU audio playback cache
        val cacheDir = File(cacheDir, "media3_lru_cache")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        simpleCache = SimpleCache(cacheDir, LeastRecentlyUsedCacheEvictor(1024 * 1024 * 1024L))

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(30000)

        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(simpleCache!!)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val mediaSourceFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        audioEffectsController = AudioEffectsController()
        sleepTimerManager = SleepTimerManager(serviceScope) { exoPlayer }
        scrobbleManager = ScrobbleManager(
            context = applicationContext,
            apiServiceProvider = { subsonicApiService },
            scope = serviceScope
        )

        // Attach audio effects when player audioSessionId is available
        audioEffectsController.attachAudioSession(exoPlayer.audioSessionId)

        // Restore saved EQ & Bass settings
        serviceScope.launch {
            val preset = userPreferencesDataStore.equalizerPreset.firstOrNull() ?: "Flat"
            val bass = userPreferencesDataStore.bassBoostStrength.firstOrNull() ?: 0
            val enabled = userPreferencesDataStore.equalizerEnabled.firstOrNull() ?: false
            audioEffectsController.setEqualizerEnabled(enabled)
            audioEffectsController.applyPreset(preset)
            audioEffectsController.setBassBoost(bass)
        }

        exoPlayer.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.let {
                    val song = it.toSong()
                    scrobbleManager.onSongChanged(song)
                } ?: scrobbleManager.onSongChanged(null)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                scrobbleManager.onPlaybackStateChanged(isPlaying, exoPlayer.currentPosition)
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                audioEffectsController.attachAudioSession(audioSessionId)
            }
        })

        mediaLibrarySession = MediaLibrarySession.Builder(this, exoPlayer, LibrarySessionCallback())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {

        @OptIn(UnstableApi::class)
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val defaultResult = super<MediaLibrarySession.Callback>.onConnect(session, controller)
            val commands = listOf(
                COMMAND_SET_EQ_PRESET,
                COMMAND_SET_EQ_ENABLED,
                COMMAND_SET_BASS_BOOST,
                COMMAND_START_SLEEP_TIMER,
                COMMAND_CANCEL_SLEEP_TIMER
            ).fold(defaultResult.availableSessionCommands.buildUpon()) { builder, action ->
                builder.add(SessionCommand(action, Bundle()))
            }.build()

            return MediaSession.ConnectionResult.accept(commands, defaultResult.availablePlayerCommands)
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setTitle("NaviPlayer Library")
                        .build()
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            if (parentId == ROOT_ID) {
                val items = ImmutableList.of(
                    buildFolderItem(FAVORITES_ID, "Favorites"),
                    buildFolderItem(RECENT_ID, "Recently Played"),
                    buildFolderItem(PLAYLISTS_ID, "Playlists")
                )
                return Futures.immediateFuture(LibraryResult.ofItemList(items, params))
            }
            return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.of(), params))
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                COMMAND_SET_EQ_PRESET -> {
                    val preset = args.getString("preset", "Flat")
                    audioEffectsController.applyPreset(preset)
                    audioEffectsController.setEqualizerEnabled(true)
                    serviceScope.launch {
                        userPreferencesDataStore.setEqualizerPreset(preset)
                        userPreferencesDataStore.setEqualizerEnabled(true)
                    }
                }
                COMMAND_SET_EQ_ENABLED -> {
                    val enabled = args.getBoolean("enabled", false)
                    audioEffectsController.setEqualizerEnabled(enabled)
                    serviceScope.launch { userPreferencesDataStore.setEqualizerEnabled(enabled) }
                }
                COMMAND_SET_BASS_BOOST -> {
                    val strength = args.getInt("strength", 0)
                    audioEffectsController.setBassBoost(strength)
                    serviceScope.launch {
                        userPreferencesDataStore.setBassBoostStrength(strength)
                    }
                }
                COMMAND_START_SLEEP_TIMER -> {
                    val minutes = args.getInt("minutes", 30)
                    sleepTimerManager.startTimer(minutes)
                }
                COMMAND_CANCEL_SLEEP_TIMER -> {
                    sleepTimerManager.cancelTimer()
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        private fun buildFolderItem(id: String, title: String): MediaItem {
            return MediaItem.Builder()
                .setMediaId(id)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setTitle(title)
                        .build()
                )
                .build()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        audioEffectsController.release()
        sleepTimerManager.cancelTimer()
        mediaLibrarySession.release()
        exoPlayer.release()
        simpleCache?.release()
        super.onDestroy()
    }
}

// MediaItem extensions
fun MediaItem.toSong(): Song {
    val meta = mediaMetadata
    return Song(
        id = mediaId,
        title = meta.title?.toString() ?: "Unknown Track",
        artist = meta.artist?.toString() ?: "",
        album = meta.albumTitle?.toString() ?: "",
        trackNumber = meta.trackNumber ?: 0,
        discNumber = meta.discNumber ?: 1,
        coverArtId = meta.extras?.getString("coverArtId"),
        durationSec = (meta.extras?.getLong("durationSec") ?: 0L).toInt()
    )
}
