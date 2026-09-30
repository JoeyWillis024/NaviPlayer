package com.antiwilly.naviplayer.core.data.repository

import com.antiwilly.naviplayer.core.database.dao.ServerProfileDao
import com.antiwilly.naviplayer.core.database.entity.ServerProfileEntity
import com.antiwilly.naviplayer.core.model.ServerProfile
import com.antiwilly.naviplayer.core.network.SubsonicAuthInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepository @Inject constructor(
    private val serverProfileDao: ServerProfileDao
) {

    val allProfiles: Flow<List<ServerProfile>> = serverProfileDao.getAllProfiles().map { entities ->
        entities.map { it.toDomainModel() }
    }

    val activeProfile: Flow<ServerProfile?> = serverProfileDao.getActiveProfile().map { entity ->
        entity?.toDomainModel()
    }

    suspend fun saveProfile(profile: ServerProfile): Long = withContext(Dispatchers.IO) {
        val entity = ServerProfileEntity.fromDomainModel(profile)
        val id = serverProfileDao.insert(entity)
        if (profile.isActive || serverProfileDao.getActiveProfileSync() == null) {
            serverProfileDao.setActiveProfile(if (entity.id != 0L) entity.id else id)
        }
        id
    }

    suspend fun deleteProfile(profile: ServerProfile) = withContext(Dispatchers.IO) {
        serverProfileDao.delete(ServerProfileEntity.fromDomainModel(profile))
    }

    suspend fun setActiveProfile(id: Long) = withContext(Dispatchers.IO) {
        serverProfileDao.setActiveProfile(id)
    }

    suspend fun testConnection(profile: ServerProfile): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val salt = SubsonicAuthInterceptor.generateSalt()
            val token = SubsonicAuthInterceptor.computeToken(profile.token, salt)
            val base = profile.baseUrl.trimEnd('/')
            val url = "$base/rest/ping.view?u=${profile.username}&t=$token&s=$salt&v=1.16.1&c=NaviPlayer&f=json"

            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val requestBuilder = Request.Builder().url(url)
            profile.customHeaders.forEach { (k, v) -> requestBuilder.addHeader(k, v) }

            val response = client.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                if (body.contains("\"status\":\"ok\"")) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("Navidrome returned error status: $body"))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
