package com.antiwilly.naviplayer.core.network

import com.antiwilly.naviplayer.core.network.dto.SubsonicRootResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SubsonicDtoSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun parsePingResponse_success() {
        val rawJson = """
            {
                "subsonic-response": {
                    "status": "ok",
                    "version": "1.16.1",
                    "type": "navidrome",
                    "serverVersion": "0.53.3",
                    "openSubsonic": true
                }
            }
        """.trimIndent()

        val root = json.decodeFromString<SubsonicRootResponse>(rawJson)
        assertEquals("ok", root.response.status)
        assertEquals("1.16.1", root.response.version)
        assertEquals("navidrome", root.response.type)
        assertEquals(true, root.response.openSubsonic)
    }

    @Test
    fun parseAlbumList2Response_success() {
        val rawJson = """
            {
                "subsonic-response": {
                    "status": "ok",
                    "version": "1.16.1",
                    "albumList2": {
                        "album": [
                            {
                                "id": "alb-101",
                                "name": "Random Access Memories",
                                "artist": "Daft Punk",
                                "artistId": "art-50",
                                "coverArt": "cov-101",
                                "songCount": 13,
                                "duration": 4440,
                                "year": 2013,
                                "genre": "Electronic",
                                "starred": "2024-01-01T00:00:00Z"
                            }
                        ]
                    }
                }
            }
        """.trimIndent()

        val root = json.decodeFromString<SubsonicRootResponse>(rawJson)
        val albums = root.response.albumList2?.album
        assertNotNull(albums)
        assertEquals(1, albums!!.size)

        val album = albums[0]
        assertEquals("alb-101", album.id)
        assertEquals("Random Access Memories", album.name)
        assertEquals("Daft Punk", album.artist)
        assertEquals(2013, album.year)
        assertEquals(13, album.songCount)
        assertNotNull(album.starred)
    }

    @Test
    fun parseSearchResult3Response_success() {
        val rawJson = """
            {
                "subsonic-response": {
                    "status": "ok",
                    "searchResult3": {
                        "artist": [
                            {"id": "a-1", "name": "Radiohead", "albumCount": 9}
                        ],
                        "album": [
                            {"id": "al-1", "title": "OK Computer", "artist": "Radiohead"}
                        ],
                        "song": [
                            {"id": "s-1", "title": "Paranoid Android", "artist": "Radiohead", "duration": 387}
                        ]
                    }
                }
            }
        """.trimIndent()

        val root = json.decodeFromString<SubsonicRootResponse>(rawJson)
        val search = root.response.searchResult3
        assertNotNull(search)
        assertEquals(1, search!!.artist.size)
        assertEquals("Radiohead", search.artist[0].name)
        assertEquals(1, search.album.size)
        assertEquals("OK Computer", search.album[0].title)
        assertEquals(1, search.song.size)
        assertEquals("Paranoid Android", search.song[0].title)
    }
}
