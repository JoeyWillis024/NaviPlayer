package com.antiwilly.naviplayer.core.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.antiwilly.naviplayer.core.database.dao.DownloadDao
import com.antiwilly.naviplayer.core.database.dao.ServerProfileDao
import com.antiwilly.naviplayer.core.database.entity.DownloadedSongEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
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

        // The injected client adds Subsonic credentials and custom headers. Keep this URL
        // free of credentials so the auth interceptor only adds them once.
        val streamUrl = server.baseUrl.toHttpUrl().newBuilder()
            .addPathSegments("rest/stream.view")
            .addQueryParameter("id", songId)
            .build()

        try {
            val request = Request.Builder().url(streamUrl).build()
            val response = okHttpClient.newCall(request).execute()

            response.use {
                if (!it.isSuccessful) return@withContext Result.retry()

                val body = it.body ?: return@withContext Result.failure()
                val contentTypeHeader = body.contentType()?.toString().orEmpty()
                if (contentTypeHeader.contains("json", ignoreCase = true) ||
                    contentTypeHeader.contains("text", ignoreCase = true)) {
                    return@withContext Result.retry()
                }

                FileOutputStream(audioFile).use { output ->
                    body.byteStream().use { input -> input.copyTo(output) }
                }

                if (!audioFile.exists() || audioFile.length() == 0L) {
                    audioFile.delete()
                    return@withContext Result.retry()
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
            }
        } catch (_: Exception) {
            if (audioFile.exists()) audioFile.delete()
            Result.retry()
        }
    }
}
