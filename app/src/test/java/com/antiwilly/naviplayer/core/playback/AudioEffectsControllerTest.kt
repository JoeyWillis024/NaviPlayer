package com.antiwilly.naviplayer.core.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioEffectsControllerTest {

    private val controller = AudioEffectsController()

    @Test
    fun presets_containsStandardProfiles() {
        val expected = listOf("Flat", "Bass Boost", "Rock", "Pop", "Jazz", "Electronic", "Vocal")
        expected.forEach { preset ->
            assertTrue("Expected preset '$preset' to exist", controller.presets.containsKey(preset))
            val bands = controller.presets[preset]
            assertNotNull(bands)
            assertEquals(5, bands!!.size)
            // Each band in dB millibels should be within reasonable equalizer limits (-1500 to +1500 mB)
            bands.forEach { mB ->
                assertTrue("Band $mB out of range", mB in -1500..1500)
            }
        }
    }

    @Test
    fun flatPreset_hasZeroGains() {
        val flat = controller.presets["Flat"]
        assertNotNull(flat)
        assertEquals(listOf(0, 0, 0, 0, 0), flat)
    }
}
