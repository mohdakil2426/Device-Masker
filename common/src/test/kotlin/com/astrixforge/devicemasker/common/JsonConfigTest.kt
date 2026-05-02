package com.astrixforge.devicemasker.common

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class JsonConfigTest {

    @Test
    fun `parseCatching returns failure for invalid json`() {
        val result = JsonConfig.parseCatching("{ invalid json }")

        assertTrue(result.isFailure)
    }

    @Test
    fun `parseOrDefault reports failure before returning default config`() {
        var failureCount = 0

        val config = JsonConfig.parseOrDefault("{ invalid json }") { failureCount += 1 }

        assertEquals(1, failureCount)
        assertTrue(config.groups.isEmpty())
        assertTrue(config.appConfigs.isEmpty())
    }

    @Test
    fun `parseCatching succeeds for valid json`() {
        val config = JsonConfig(isModuleEnabled = false)

        val result = JsonConfig.parseCatching(config.toJsonString())

        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow().isModuleEnabled)
    }
}
