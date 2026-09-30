package com.antiwilly.naviplayer.core.network

import com.antiwilly.naviplayer.core.model.ServerProfile
import okhttp3.Interceptor
import okhttp3.Response
import java.security.MessageDigest
import java.util.UUID

class SubsonicAuthInterceptor(
    private val serverProfileProvider: () -> ServerProfile?
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val profile = serverProfileProvider() ?: return chain.proceed(originalRequest)

        val salt = generateSalt()
        val token = computeToken(profile.token, salt)

        val url = originalRequest.url.newBuilder()
            .setQueryParameter("u", profile.username)
            .setQueryParameter("t", token)
            .setQueryParameter("s", salt)
            .setQueryParameter("v", "1.16.1")
            .setQueryParameter("c", "NaviPlayer")
            .setQueryParameter("f", "json")
            .build()

        val requestBuilder = originalRequest.newBuilder().url(url)

        // Inject custom headers (e.g. Cloudflare Access, Basic Auth, Reverse Proxy)
        profile.customHeaders.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }

        return chain.proceed(requestBuilder.build())
    }

    companion object {
        fun generateSalt(length: Int = 12): String {
            val allowedChars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
            return (1..length)
                .map { allowedChars.random() }
                .joinToString("")
        }

        fun computeToken(password: String, salt: String): String {
            return md5Hex(password + salt)
        }

        fun md5Hex(input: String): String {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(input.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
