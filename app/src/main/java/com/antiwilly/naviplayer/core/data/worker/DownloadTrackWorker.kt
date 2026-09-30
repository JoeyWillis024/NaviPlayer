package com.antiwilly.naviplayer.core.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.antiwilly.naviplayer.core.database.dao.DownloadDao
import com.antiwilly.naviplayer.core.database.dao.ServerProfileDao
import com.antiwilly.naviplayer.core.database.entity.DownloadedSongEntity
import com.antiwilly.naviplayer.core.network.SubsonicAuthInterceptor
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

@HiltWorker
class DownloadTrackWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloadDao: DownloadDao,
    private val serverProfileDao: ServerProfileDao,
    private val okHttpClient: OkHttpClient
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val songId = inputData.getString("songId") ?: return@withContext Result.failure()
        val title = inputData.getString("title") ?: "Unknown"
        val artist = inputData.getString("artist") ?: ""
        val artistId = inputData.getString("artistId") ?: ""
        val album = inputData.getString("album") ?: ""
        val albumId = inputData.getString("albumId") ?: ""
        val durationSec = inputData.getInt("durationSec", 0)
        val trackNumber = inputData.getInt("trackNumber", 0)
        val discNumber = inputData.getInt("discNumber", 1)
        val year = inputData.getInt("year", 0)
        val genre = inputData.getString("genre") ?: ""
        val coverArtId = inputData.getString("coverArtId")
        val suffix = inputData.getString("suffix") ?: "mp3"
        val contentType = inputData.getString("contentType") ?: "audio/mpeg"

        val server = serverProfileDao.getActiveProfileSync() ?: return@withContext Result.retry()

        val downloadDir = File(context.filesDir, "downloads/${server.id}")
        if (!downloadDir.exists()) downloadDir.mkdirs()
        val audioFile = File(downloadDir, "$songId.$suffix")

        val salt = SubsonicAuthInterceptor.generateSalt()
        val token = SubsonicAuthInterceptor.computeToken(server.token, salt)
        val base = server.baseUrl.trimEnd('/')
        // For offline downloads, download the original direct stream without bitrate cap
        val streamUrl = "$base/rest/stream.view?id=$songId&u=${server.username}&t=$token&s=$salt&v=1.16.1&c=NaviPlayer"

        try {
            val request = Request.Builder().url(streamUrl).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.retry()
            }

            val body = response.body ?: return@withContext Result.failure()
            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(audioFile)

            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            val entity = DownloadedSongEntity(
                id = songId,
                serverId = server.id,
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
                localCoverArtPath = null,
                localFilePath = audioFile.absolutePath,
                sizeBytes = audioFile.length(),
                contentType = contentType,
                suffix = suffix
            )
            downloadDao.insert(entity)

            Result.success()
        } catch (_: Exception) {
            if (audioFile.exists()) audioFile.delete()
            Result.retry()
        }
    }
}
