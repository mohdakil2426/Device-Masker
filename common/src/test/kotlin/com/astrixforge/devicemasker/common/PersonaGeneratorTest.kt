package com.astrixforge.devicemasker.common

import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.Test

class PersonaGeneratorTest {

    @Test
    fun `generate is stable for same group and package`() {
        val group =
            SpoofGroup.createNew(name = "Persona")
                .withValue(SpoofType.DEVICE_PROFILE, DeviceProfilePreset.PRESETS.first().id)
                .withPersona(seed = "seed-123", generatedAt = 1000L)

        val first = PersonaGenerator.generate(group, "com.example.one")
        val second = PersonaGenerator.generate(group, "com.example.one")

        assertEquals(first, second)
        assertTrue(PersonaGenerator.validate(first).isValid)
    }

    @Test
    fun `android id is package scoped while tracking ids remain stable`() {
        val group = SpoofGroup.createNew(name = "Persona").withPersona(seed = "seed-android")

        val appOne = PersonaGenerator.generate(group, "com.example.one")
        val appTwo = PersonaGenerator.generate(group, "com.example.two")

        assertNotEquals(appOne.tracking.androidId, appTwo.tracking.androidId)
        assertEquals(appOne.tracking.gsfId, appTwo.tracking.gsfId)
        assertEquals(appOne.tracking.advertisingId, appTwo.tracking.advertisingId)
        assertEquals(appOne.tracking.mediaDrmId, appTwo.tracking.mediaDrmId)
    }

    @Test
    fun `shared pref keys include persona keys`() {
        val packageName = "com.example.target"

        assertTrue(SharedPrefsKeys.isPersonaBlobKey(SharedPrefsKeys.getPersonaBlobKey(packageName)))
        assertTrue(
            SharedPrefsKeys.isPersonaVersionKey(SharedPrefsKeys.getPersonaVersionKey(packageName))
        )
    }
}
