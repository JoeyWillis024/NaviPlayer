package com.antiwilly.naviplayer.core.network

import com.antiwilly.naviplayer.core.model.AudioQualityConfig
import com.antiwilly.naviplayer.core.model.ServerProfile
import com.antiwilly.naviplayer.core.model.TranscodeFormat

object SubsonicUrlHelper {

    fun buildStreamUrl(
        profile: ServerProfile,
        songId: String,
        qualityConfig: AudioQualityConfig
    ): String {
        val base = profile.baseUrl.trimEnd('/')
        val salt = SubsonicAuthInterceptor.generateSalt()
        val token = SubsonicAuthInterceptor.computeToken(profile.token, salt)

        val builder = StringBuilder("$base/rest/stream.view?")
            .append("id=").append(songId)
            .append("&u=").append(profile.username)
            .append("&t=").append(token)
            .append("&s=").append(salt)
            .append("&v=1.16.1&c=NaviPlayer")

        if (qualityConfig.format != TranscodeFormat.ORIGINAL) {
            builder.append("&format=").append(qualityConfig.format.name.lowercase())
        }
        if (qualityConfig.maxBitRate != null && qualityConfig.maxBitRate > 0) {
            builder.append("&maxBitRate=").append(qualityConfig.maxBitRate)
        }

        return builder.toString()
    }

    fun buildCoverArtUrl(
        profile: ServerProfile,
        coverArtId: String,
        size: Int = 600
    ): String {
        val base = profile.baseUrl.trimEnd('/')
        val salt = SubsonicAuthInterceptor.generateSalt()
        val token = SubsonicAuthInterceptor.computeToken(profile.token, salt)

        return "$base/rest/getCoverArt.view?id=$coverArtId&size=$size&u=${profile.username}&t=$token&s=$salt&v=1.16.1&c=NaviPlayer"
    }
}
