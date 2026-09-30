package com.antiwilly.naviplayer.core.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.antiwilly.naviplayer.core.data.worker.DownloadTrackWorker
import com.antiwilly.naviplayer.core.database.dao.DownloadDao
import com.antiwilly.naviplayer.core.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao
) {
    private val workManager = WorkManager.getInstance(context)

    val downloadedSongs: Flow<List<Song>> = downloadDao.getAllDownloadedSongs().map { entities ->
        entities.map { it.toDomainModel() }
    }

    val totalSizeBytes: Flow<Long> = downloadDao.getTotalDownloadedSizeBytes()

    fun isSongDownloaded(songId: String): Flow<Boolean> = downloadDao.isSongDownloaded(songId)

    fun downloadSong(song: Song) {
        val data = workDataOf(
            "songId" to song.id,
            "title" to song.title,
            "artist" to song.artist,
            "artistId" to song.artistId,
            "album" to song.album,
            "albumId" to song.albumId,
            "durationSec" to song.durationSec,
            "trackNumber" to song.trackNumber,
            "discNumber" to song.discNumber,
            "year" to song.year,
            "genre" to song.genre,
            "coverArtId" to song.coverArtId,
            "suffix" to song.suffix,
            "contentType" to song.contentType
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<DownloadTrackWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .addTag("download_${song.id}")
            .build()

        workManager.enqueueUniqueWork("download_${song.id}", ExistingWorkPolicy.KEEP, request)
    }

    fun downloadAlbum(songs: List<Song>) {
        songs.forEach { downloadSong(it) }
    }

    suspend fun deleteDownload(songId: String) = withContext(Dispatchers.IO) {
        val downloaded = downloadDao.getDownloadedSong(songId)
        if (downloaded != null) {
            val file = File(downloaded.localFilePath)
            if (file.exists()) file.delete()
            downloadDao.delete(songId)
        }
    }
}
