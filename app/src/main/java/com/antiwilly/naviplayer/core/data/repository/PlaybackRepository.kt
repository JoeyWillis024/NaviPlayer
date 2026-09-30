package com.antiwilly.naviplayer.core.data.repository

import android.content.ComponentName
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.antiwilly.naviplayer.core.database.dao.DownloadDao
import com.antiwilly.naviplayer.core.database.dao.ServerProfileDao
import com.antiwilly.naviplayer.core.model.PlaybackState
import com.antiwilly.naviplayer.core.model.Song
import com.antiwilly.naviplayer.core.network.SubsonicUrlHelper
import com.antiwilly.naviplayer.core.playback.NaviMediaService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serverProfileDao: ServerProfileDao,
    private val downloadDao: DownloadDao
) {
    private val repositoryScope = CoroutineScope(Dispatchers.Main)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var progressTrackingJob: Job? = null

    init {
        initializeController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(context, ComponentName(context, NaviMediaService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupControllerListener()
            } catch (_: Exception) {}
        }, MoreExecutors.directExecutor())
    }

    private fun setupControllerListener() {
        val controller = mediaController ?: return
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startProgressTracking() else stopProgressTracking()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val currentSong = _playbackState.value.queue.find { it.id == mediaItem?.mediaId }
                _playbackState.update {
                    it.copy(
                        currentSong = currentSong,
                        currentPositionMs = controller.currentPosition,
                        durationMs = controller.duration.coerceAtLeast(0)
                    )
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                _playbackState.update {
                    it.copy(
                        isBuffering = state == Player.STATE_BUFFERING,
                        durationMs = controller.duration.coerceAtLeast(0)
                    )
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _playbackState.update { it.copy(shuffleMode = shuffleModeEnabled) }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _playbackState.update { it.copy(repeatMode = repeatMode) }
            }
        })
    }

    private fun startProgressTracking() {
        progressTrackingJob?.cancel()
        progressTrackingJob = repositoryScope.launch {
            while (isActive) {
                mediaController?.let { controller ->
                    _playbackState.update {
                        it.copy(
                            currentPositionMs = controller.currentPosition,
                            durationMs = controller.duration.coerceAtLeast(0)
                        )
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracking() {
        progressTrackingJob?.cancel()
    }

    fun playSong(song: Song, queue: List<Song> = listOf(song)) {
        repositoryScope.launch(Dispatchers.IO) {
            val server = serverProfileDao.getActiveProfileSync()?.toDomainModel()
            val isWifi = isConnectedToWifi()
            val quality = if (isWifi) server?.wifiQuality else server?.cellularQuality

            val mediaItems = queue.map { item ->
                val localSong = downloadDao.getDownloadedSong(item.id)
                val uri = if (localSong != null) {
                    "file://${localSong.localFilePath}"
                } else if (server != null && quality != null) {
                    SubsonicUrlHelper.buildStreamUrl(server, item.id, quality)
                } else {
                    ""
                }

                val extras = Bundle().apply {
                    putString("coverArtId", item.coverArtId)
                    putLong("durationSec", item.durationSec.toLong())
                }

                MediaItem.Builder()
                    .setMediaId(item.id)
                    .setUri(uri)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(item.title)
                            .setArtist(item.artist)
                            .setAlbumTitle(item.album)
                            .setTrackNumber(item.trackNumber)
                            .setDiscNumber(item.discNumber)
                            .setExtras(extras)
                            .build()
                    )
                    .build()
            }

            val startIndex = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

            launch(Dispatchers.Main) {
                mediaController?.let { controller ->
                    controller.setMediaItems(mediaItems, startIndex, 0)
                    controller.prepare()
                    controller.play()
                    _playbackState.update {
                        it.copy(
                            currentSong = song,
                            queue = queue,
                            queueIndex = startIndex,
                            isPlaying = true
                        )
                    }
                }
            }
        }
    }

    fun play() = mediaController?.play()
    fun pause() = mediaController?.pause()
    fun seekTo(positionMs: Long) = mediaController?.seekTo(positionMs)
    fun skipToNext() = mediaController?.seekToNextMediaItem()
    fun skipToPrevious() = mediaController?.seekToPreviousMediaItem()

    fun toggleShuffle() {
        val current = mediaController?.shuffleModeEnabled ?: false
        mediaController?.shuffleModeEnabled = !current
    }

    fun cycleRepeatMode() {
        val current = mediaController?.repeatMode ?: Player.REPEAT_MODE_OFF
        val next = when (current) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        mediaController?.repeatMode = next
    }

    fun setEqualizerPreset(preset: String) {
        val command = SessionCommand(NaviMediaService.COMMAND_SET_EQ_PRESET, Bundle())
        val bundle = Bundle().apply { putString("preset", preset) }
        mediaController?.sendCustomCommand(command, bundle)
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        val command = SessionCommand(NaviMediaService.COMMAND_SET_EQ_ENABLED, Bundle())
        val bundle = Bundle().apply { putBoolean("enabled", enabled) }
        mediaController?.sendCustomCommand(command, bundle)
    }

    fun setBassBoost(strength: Int) {
        val command = SessionCommand(NaviMediaService.COMMAND_SET_BASS_BOOST, Bundle())
        val bundle = Bundle().apply { putInt("strength", strength) }
        mediaController?.sendCustomCommand(command, bundle)
    }

    fun startSleepTimer(minutes: Int) {
        val command = SessionCommand(NaviMediaService.COMMAND_START_SLEEP_TIMER, Bundle())
        val bundle = Bundle().apply { putInt("minutes", minutes) }
        mediaController?.sendCustomCommand(command, bundle)
    }

    fun cancelSleepTimer() {
        val command = SessionCommand(NaviMediaService.COMMAND_CANCEL_SLEEP_TIMER, Bundle())
        mediaController?.sendCustomCommand(command, Bundle())
    }

    private fun isConnectedToWifi(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
