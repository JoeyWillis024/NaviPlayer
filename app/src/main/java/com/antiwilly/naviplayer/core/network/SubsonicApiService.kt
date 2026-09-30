package com.antiwilly.naviplayer.core.network

import com.antiwilly.naviplayer.core.network.dto.SubsonicRootResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface SubsonicApiService {

    @GET("rest/ping.view")
    suspend fun ping(): SubsonicRootResponse

    @GET("rest/getArtists.view")
    suspend fun getArtists(): SubsonicRootResponse

    @GET("rest/getArtist.view")
    suspend fun getArtist(
        @Query("id") artistId: String
    ): SubsonicRootResponse

    @GET("rest/getAlbumList2.view")
    suspend fun getAlbumList2(
        @Query("type") type: String, // "newest", "recent", "frequent", "random", "alphabeticalByName", "byGenre"
        @Query("size") size: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("genre") genre: String? = null
    ): SubsonicRootResponse

    @GET("rest/getAlbum.view")
    suspend fun getAlbum(
        @Query("id") albumId: String
    ): SubsonicRootResponse

    @GET("rest/search3.view")
    suspend fun search3(
        @Query("query") query: String,
        @Query("artistCount") artistCount: Int = 20,
        @Query("albumCount") albumCount: Int = 20,
        @Query("songCount") songCount: Int = 50
    ): SubsonicRootResponse

    @GET("rest/getPlaylists.view")
    suspend fun getPlaylists(): SubsonicRootResponse

    @GET("rest/getPlaylist.view")
    suspend fun getPlaylist(
        @Query("id") playlistId: String
    ): SubsonicRootResponse

    @GET("rest/createPlaylist.view")
    suspend fun createPlaylist(
        @Query("name") name: String,
        @Query("songId") songIds: List<String> = emptyList()
    ): SubsonicRootResponse

    @GET("rest/updatePlaylist.view")
    suspend fun updatePlaylist(
        @Query("playlistId") playlistId: String,
        @Query("name") name: String? = null,
        @Query("comment") comment: String? = null,
        @Query("public") isPublic: Boolean? = null,
        @Query("songIdToAdd") songIdToAdd: List<String>? = null,
        @Query("songIndexToRemove") songIndexToRemove: List<Int>? = null
    ): SubsonicRootResponse

    @GET("rest/deletePlaylist.view")
    suspend fun deletePlaylist(
        @Query("id") id: String
    ): SubsonicRootResponse

    @GET("rest/star.view")
    suspend fun star(
        @Query("id") id: String? = null,
        @Query("albumId") albumId: String? = null,
        @Query("artistId") artistId: String? = null
    ): SubsonicRootResponse

    @GET("rest/unstar.view")
    suspend fun unstar(
        @Query("id") id: String? = null,
        @Query("albumId") albumId: String? = null,
        @Query("artistId") artistId: String? = null
    ): SubsonicRootResponse

    @GET("rest/scrobble.view")
    suspend fun scrobble(
        @Query("id") id: String,
        @Query("time") time: Long? = null,
        @Query("submission") submission: Boolean = true
    ): SubsonicRootResponse

    @GET("rest/getGenres.view")
    suspend fun getGenres(): SubsonicRootResponse
}
