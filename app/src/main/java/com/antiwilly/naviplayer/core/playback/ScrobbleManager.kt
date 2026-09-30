package com.antiwilly.naviplayer.core.playback

import android.content.Context
import android.content.Intent
import android.util.Log
import com.antiwilly.naviplayer.core.model.Song
import com.antiwilly.naviplayer.core.network.SubsonicApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ScrobbleManager(
    private val context: Context,
    private val apiServiceProvider: () -> SubsonicApiService?,
    private val scope: CoroutineScope
) {
    private var currentSong: Song? = null
    private var hasSubmittedScrobble = false
    private var trackingJob: Job? = null

    fun onSongChanged(song: Song?) {
        trackingJob?.cancel()
        currentSong = song
        hasSubmittedScrobble = false

        if (song != null) {
            // Notify server of "Now Playing"
            sendNowPlaying(song)
            // Broadcast local intent for Pano Scrobbler / Simple Last.fm
            broadcastMetaChanged(song, isPlaying = true)
        }
    }

    fun onPlaybackStateChanged(isPlaying: Boolean, currentPositionMs: Long) {
        val song = currentSong ?: return
        broadcastPlayStateChanged(song, isPlaying, currentPositionMs)

        if (isPlaying && !hasSubmittedScrobble) {
            startScrobbleTracking(song, currentPositionMs)
        } else if (!isPlaying) {
            trackingJob?.cancel()
        }
    }

    private fun startScrobbleTracking(song: Song, startPositionMs: Long) {
        trackingJob?.cancel()
        trackingJob = scope.launch(Dispatchers.IO) {
            val totalDurationMs = song.durationSec * 1000L
            val thresholdMs = if (totalDurationMs > 0) {
                minOf(240_000L, totalDurationMs / 2) // 4 min or 50%
            } else {
                30_000L // Fallback 30s
            }

            var playedMs = startPositionMs
            while (isActive && playedMs < thresholdMs) {
                delay(1000)
                playedMs += 1000
            }

            if (isActive && !hasSubmittedScrobble) {
                submitScrobble(song)
                hasSubmittedScrobble = true
            }
        }
    }

    private fun sendNowPlaying(song: Song) {
        scope.launch(Dispatchers.IO) {
            try {
                apiServiceProvider()?.scrobble(
                    id = song.id,
                    time = System.currentTimeMillis(),
                    submission = false
                )
            } catch (e: Exception) {
                Log.w("ScrobbleManager", "Failed to send now playing: ${e.message}")
            }
        }
    }

    private fun submitScrobble(song: Song) {
        scope.launch(Dispatchers.IO) {
            try {
                apiServiceProvider()?.scrobble(
                    id = song.id,
                    time = System.currentTimeMillis(),
                    submission = true
                )
                Log.d("ScrobbleManager", "Scrobbled song: ${song.title}")
            } catch (e: Exception) {
                Log.w("ScrobbleManager", "Failed to submit scrobble: ${e.message}")
            }
        }
    }

    private fun broadcastMetaChanged(song: Song, isPlaying: Boolean) {
        try {
            val intent = Intent("com.android.music.metachanged").apply {
                putExtra("id", song.id)
                putExtra("artist", song.artist)
                putExtra("album", song.album)
                putExtra("track", song.title)
                putExtra("playing", isPlaying)
                putExtra("duration", song.durationSec * 1000L)
            }
            context.sendBroadcast(intent)
        } catch (_: Exception) {}
    }

    private fun broadcastPlayStateChanged(song: Song, isPlaying: Boolean, positionMs: Long) {
        try {
            val intent = Intent("com.android.music.playstatechanged").apply {
                putExtra("id", song.id)
                putExtra("artist", song.artist)
                putExtra("album", song.album)
                putExtra("track", song.title)
                putExtra("playing", isPlaying)
                putExtra("position", positionMs)
            }
            context.sendBroadcast(intent)
        } catch (_: Exception) {}
    }
}
