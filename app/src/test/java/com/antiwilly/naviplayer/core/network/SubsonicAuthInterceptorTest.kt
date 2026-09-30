package com.antiwilly.naviplayer.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubsonicAuthInterceptorTest {

    @Test
    fun md5Hex_producesCorrectHashForKnownString() {
        // "password" MD5 is 5f4dcc3b5aa765d61d8327deb882cf99
        val hash = SubsonicAuthInterceptor.md5Hex("password")
        assertEquals("5f4dcc3b5aa765d61d8327deb882cf99", hash)
    }

    @Test
    fun computeToken_concatenatesPasswordAndSaltBeforeHashing() {
        val password = "myNavidromePassword"
        val salt = "c1a2b3d4"
        val expected = SubsonicAuthInterceptor.md5Hex(password + salt)
        val actual = SubsonicAuthInterceptor.computeToken(password, salt)
        assertEquals(expected, actual)
    }

    @Test
    fun generateSalt_producesRandomStringWithExpectedLength() {
        val salt1 = SubsonicAuthInterceptor.generateSalt(12)
        val salt2 = SubsonicAuthInterceptor.generateSalt(12)

        assertEquals(12, salt1.length)
        assertEquals(12, salt2.length)
        assertNotEquals(salt1, salt2)
        assertTrue(salt1.all { it.isLetterOrDigit() })
    }
}
