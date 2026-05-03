package com.astrixforge.devicemasker.xposed.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsLogBufferTest {
    @Test
    fun `log buffer is capped and dropped count increments`() {
        val buffer = DiagnosticsLogBuffer(maxLogs = 500, nowMillis = { 1_700_000_000_000 })

        repeat(505) { buffer.append("Test", "entry-$it", INFO) }

        val logs = buffer.getLogs(600)
        assertEquals(500, logs.size)
        assertTrue(logs.first().contains("entry-5"))
        assertEquals(5, buffer.droppedCount)
    }

    @Test
    fun `invalid structured json stays available as plain service log`() {
        val buffer = DiagnosticsLogBuffer(maxLogs = 500, nowMillis = { 1_700_000_000_000 })

        buffer.append("Test", "{not-json", WARN)

        assertTrue(buffer.getLogs(10).any { it.contains("{not-json") })
    }

    @Test
    fun `clear records clear event after clearing`() {
        val buffer = DiagnosticsLogBuffer(maxLogs = 500, nowMillis = { 1_700_000_000_000 })
        buffer.append("Test", "before-clear", INFO)

        buffer.clear()

        val logs = buffer.getLogs(10)
        assertEquals(1, logs.size)
        assertTrue(logs.single().contains("Diagnostics cleared"))
    }

    private companion object {
        private const val INFO = 4
        private const val WARN = 5
    }
}
